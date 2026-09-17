//
// Created by evgeny on 21.02.2026.
//

#ifndef PJSIP_SANDBOX_APPACCOUNT_H
#define PJSIP_SANDBOX_APPACCOUNT_H

#include <pjsua2.hpp>
#include <thread>

#include "../transfer/socket/UnixSocketChannel2.cpp"

using namespace pj;

class AppAccount : public Account
{
    std::atomic<bool> _stop_flag;
    std::thread _receiver_thread;
    std::thread _im_ok_thread;

    void _receive_loop();
    void _im_ok_loop() const;

public:
    string sockDir;
    std::string callToSipAddr;
    int hangId = -1;

    UnixSocketChannel* controlChannel_;

    std::vector<Call*> calls;

    AppAccount(const string& sockDir);
    ~AppAccount();

    void removeCall(Call* call);

    virtual void onRegState(OnRegStateParam& prm);

    virtual void onIncomingCall(OnIncomingCallParam& iprm);
};

#endif //PJSIP_SANDBOX_APPACCOUNT_H
