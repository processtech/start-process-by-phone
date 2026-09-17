//
// Created by evgeny on 21.02.2026.
//
#include <iostream>
#include <vector>
#include <deque>
#include <string>
#include <cstring>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <thread>
#include <mutex>
#include <atomic>

#include "UdpChannel.h"


// Фоновый метод для постоянного слушания сокета
void UdpChannel::_receive_loop()
{
    uint8_t network_buf[MTU_SIZE];

    while (!_stop_flag.load())
    {
        // Ожидаем данные (блокирующий вызов)
        ssize_t received = recvfrom(_sockfd, network_buf, MTU_SIZE, 0, nullptr, nullptr);

        if (received > 0)
        {
            std::lock_guard<std::mutex> lock(_buffer_mutex);

            // Логика вытеснения старых данных
            if (_internal_buffer.size() + received > MAX_BUFFER_SIZE)
            {
                size_t overflow = (_internal_buffer.size() + received) - MAX_BUFFER_SIZE;
                _internal_buffer.erase(_internal_buffer.begin(), _internal_buffer.begin() + overflow);
            }

            // Добавляем новые данные в конец
            _internal_buffer.insert(_internal_buffer.end(), network_buf, network_buf + received);
        }
    }
}


UdpChannel::UdpChannel(const std::string& ip, int port, int local_port) : _stop_flag(false)
{
    _sockfd = socket(AF_INET, SOCK_DGRAM, 0);

    // Настройка удаленного адреса (куда отправляем)
    memset(&_remote_addr, 0, sizeof(_remote_addr));
    _remote_addr.sin_family = AF_INET;
    _remote_addr.sin_port = htons(port);
    inet_pton(AF_INET, ip.c_str(), &_remote_addr.sin_addr);

    // Если нужно слушать конкретный порт, делаем bind
    if (local_port > 0)
    {
        sockaddr_in local_addr;
        memset(&local_addr, 0, sizeof(local_addr));
        local_addr.sin_family = AF_INET;
        local_addr.sin_addr.s_addr = INADDR_ANY;
        local_addr.sin_port = htons(local_port);
        bind(_sockfd, (struct sockaddr*)&local_addr, sizeof(local_addr));
    }

    // Запускаем фоновый поток чтения
    _receiver_thread = std::thread(&UdpChannel::_receive_loop, this);
}

UdpChannel::~UdpChannel()
{
    _stop_flag.store(true);
    shutdown(_sockfd, SHUT_RDWR); // Прерываем блокирующий recvfrom
    if (_receiver_thread.joinable())
    {
        _receiver_thread.join();
    }
    close(_sockfd);
}

// МЕТОД 1: Отправить массив байт
void UdpChannel::sendArray(const std::vector<uint8_t>& data)
{
    sendto(_sockfd, data.data(), data.size(), 0,
           (struct sockaddr*)&_remote_addr, sizeof(_remote_addr));
}

// МЕТОД 2: Получить накопленный массив из очереди
std::vector<uint8_t> UdpChannel::receiveArray(size_t target_length)
{
    std::lock_guard<std::mutex> lock(_buffer_mutex);

    size_t actual_read = std::min(target_length, _internal_buffer.size());
    std::vector<uint8_t> result;

    if (actual_read > 0)
    {
        result.assign(_internal_buffer.begin(), _internal_buffer.begin() + actual_read);
        _internal_buffer.erase(_internal_buffer.begin(), _internal_buffer.begin() + actual_read);
    }

    return result;
}

size_t UdpChannel::available()
{
    std::lock_guard<std::mutex> lock(_buffer_mutex);
    return _internal_buffer.size();
}
