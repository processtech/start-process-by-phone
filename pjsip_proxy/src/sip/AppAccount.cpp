#include <pjsua2.hpp>
#include <iostream>
#include "AppAccount.h"

#include "AppCall.h"

using namespace pj;


void AppAccount::_im_ok_loop() const
{
    while (!_stop_flag.load())
    {
        string init = "APP_ACCOUNT_READY";
        const std::vector<uint8_t> initBytes(init.begin(), init.end());
        controlChannel_->sendArray(initBytes);

        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    }
}

void AppAccount::_receive_loop()
{
    while (!_stop_flag.load())
    {
        std::vector<uint8_t> data = controlChannel_->receiveArray(32 * 1024);
        if (!data.empty())
        {
            std::string command(data.begin(), data.end());

            if (command.starts_with("HANGUP|"))
            {
                hangId = std::stoi(command.substr(strlen("HANGUP|")));
            }

            // MAKECALL|sip:201@266191.14.rt.ru
            if (command.starts_with("MAKECALL|") && calls.empty())
            {
                callToSipAddr = command.substr(strlen("MAKECALL|"));
            }
        }
    }
}

AppAccount::AppAccount(const string& sockDir)
    : _stop_flag(false)
{
    this->sockDir = sockDir;
    this->controlChannel_ = new UnixSocketChannel(sockDir + "/_control");

    _receiver_thread = std::thread(&AppAccount::_receive_loop, this);
    _im_ok_thread = std::thread(&AppAccount::_im_ok_loop, this);

    /*
    pj_thread_create2("_receive_loop",
        &AppAccount::_receive_loop, this,
        PJ_THREAD_DEFAULT_STACK_SIZE, PJ_THREAD_ALLOCATE_STACK, _receiver_thread);
    */
}

AppAccount::~AppAccount()
{
    std::cout << "*** Account is being deleted: No of calls="
        << calls.size() << std::endl;

    for (std::vector<Call*>::iterator it = calls.begin();
         it != calls.end();)
    {
        delete (*it);
        it = calls.erase(it);
    }


    _stop_flag.store(true);
    if (_receiver_thread.joinable())
    {
        _receiver_thread.join();
    }

    if (_im_ok_thread.joinable())
    {
        _im_ok_thread.join();
    }

    string init = "APP_ACCOUNT_DOWN";
    const std::vector<uint8_t> initBytes(init.begin(), init.end());
    controlChannel_->sendArray(initBytes);

    delete controlChannel_;
}


void AppAccount::removeCall(Call* call)
{
    for (std::vector<Call*>::iterator it = calls.begin();
         it != calls.end(); ++it)
    {
        if (*it == call)
        {
            calls.erase(it);
            break;
        }
    }
}

void AppAccount::onRegState(OnRegStateParam& prm)
{
    AccountInfo ai = getInfo();
    std::cout << (ai.regIsActive ? "*** Register: code=" : "*** Unregister: code=")
        << prm.code << std::endl;
}

void AppAccount::onIncomingCall(OnIncomingCallParam& iprm)
{
    Call* call = new AppCall(*this, iprm.callId);
    CallInfo ci = call->getInfo();
    CallOpParam prm;

    std::cout << "*** Incoming Call: " << ci.remoteUri
        << " [" << ci.stateText << "]" << std::endl;

    if (0 == calls.size())
    {
        // только 1 звонок
        calls.push_back(call);
        prm.statusCode = (pjsip_status_code)200;
        call->answer(prm);
    }
    else
    {
        // отклоняем пока все остальные
        prm.statusCode = (pjsip_status_code)200;
        call->hangup(true);
    }
}
