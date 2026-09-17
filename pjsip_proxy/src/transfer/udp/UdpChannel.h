//
// Created by evgeny on 22.02.2026.
//

#ifndef PJSIP_SANDBOX_UDPCHANNEL_H
#define PJSIP_SANDBOX_UDPCHANNEL_H

#include <vector>
#include <deque>
#include <string>
#include <arpa/inet.h>
#include <thread>
#include <mutex>
#include <atomic>

class UdpChannel
{
private:
    int _sockfd;
    sockaddr_in _remote_addr;

    std::deque<uint8_t> _internal_buffer;
    std::mutex _buffer_mutex;

    std::atomic<bool> _stop_flag;
    std::thread _receiver_thread;

    const size_t MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10 МБ
    static const size_t MTU_SIZE = 65535;

    void _receive_loop();

public:
    UdpChannel(const std::string& ip, int port, int local_port = 0);

    ~UdpChannel();

    // МЕТОД 1: Отправить массив байт
    void sendArray(const std::vector<uint8_t>& data);

    // МЕТОД 2: Получить накопленный массив из очереди
    std::vector<uint8_t> receiveArray(size_t target_length);

    size_t available();
};

#endif //PJSIP_SANDBOX_UDPCHANNEL_H
