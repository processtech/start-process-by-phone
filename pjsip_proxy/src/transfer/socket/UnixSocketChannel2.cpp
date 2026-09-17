#pragma once

#include <string>
#include <vector>
#include <cstring>
#include <cerrno>
#include <csignal>
#include <iostream>

#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>
#include <fcntl.h>
#include <cstdint>

class UnixSocketChannel
{
public:
    explicit UnixSocketChannel(const std::string& path)
        : path_(path)
    {
        installSignalHandlers();
        recreateServer();
    }

    ~UnixSocketChannel()
    {
        closeAll();
        ::unlink(path_.c_str());
    }

    void sendArray(const std::vector<uint8_t>& data)
    {
        acceptIfNeeded();

        if (client_fd_ < 0) return;

        ssize_t rc = ::send(client_fd_, data.data(), data.size(), MSG_NOSIGNAL);

        if (rc <= 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
            dropClient();
        }
    }

    std::vector<uint8_t> receiveArray(size_t target_length)
    {
        acceptIfNeeded();

        std::vector<uint8_t> out;
        if (client_fd_ < 0) return out;

        out.resize(target_length);

        ssize_t rc = ::recv(client_fd_, out.data(), target_length, 0);

        if (rc > 0) {
            out.resize(rc);
            return out;
        }

        if (rc == 0 || (errno != EAGAIN && errno != EWOULDBLOCK)) {
            dropClient();
        }

        return {};
    }

private:
    int listen_fd_{-1};
    int client_fd_{-1};
    std::string path_;

    void installSignalHandlers()
    {
        ::signal(SIGPIPE, SIG_IGN);
    }

    void recreateServer()
    {
        closeAll();
        ::unlink(path_.c_str());

        listen_fd_ = ::socket(AF_UNIX, SOCK_STREAM, 0);
        if (listen_fd_ < 0)
            throw std::runtime_error("socket() failed");

        makeNonBlocking(listen_fd_);

        sockaddr_un addr{};
        addr.sun_family = AF_UNIX;
        std::snprintf(addr.sun_path, sizeof(addr.sun_path), "%s", path_.c_str());

        if (::bind(listen_fd_, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0)
            throw std::runtime_error(std::string("bind(): ") + strerror(errno));

        if (::listen(listen_fd_, 16) < 0)
            throw std::runtime_error(std::string("listen(): ") + strerror(errno));
    }

    void acceptIfNeeded()
    {
        if (client_fd_ >= 0) return;

        client_fd_ = ::accept(listen_fd_, nullptr, nullptr);
        if (client_fd_ >= 0) {
            makeNonBlocking(client_fd_);
        }
    }

    void dropClient()
    {
        if (client_fd_ >= 0) {
            ::close(client_fd_);
            client_fd_ = -1;
        }
    }

    void closeAll()
    {
        dropClient();
        if (listen_fd_ >= 0) {
            ::close(listen_fd_);
            listen_fd_ = -1;
        }
    }

    static void makeNonBlocking(int fd)
    {
        int flags = ::fcntl(fd, F_GETFL, 0);
        ::fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    }
};