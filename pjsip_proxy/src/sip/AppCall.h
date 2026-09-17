//
// Created by evgeny on 21.02.2026.
//

#ifndef PJSIP_SANDBOX_APPCALL_H
#define PJSIP_SANDBOX_APPCALL_H


#include <pjsua2.hpp>
#include "AppAccount.h"
#include "../transfer/udp/UdpChannel.h"

using namespace pj;

class AppCall : public Call
{
    AppAccount* acc;
    AudioMediaPort* med_port;

public:
    AppCall(Account& acc, int call_id = PJSUA_INVALID_ID);
    AppCall(AppAccount* app_account, int call_id = PJSUA_INVALID_ID);
    ~AppCall();

    virtual void onCallState(OnCallStateParam& prm);
    virtual void onCallTransferRequest(OnCallTransferRequestParam& prm);
    virtual void onCallReplaceRequest(OnCallReplaceRequestParam& prm);
    virtual void onCallMediaState(OnCallMediaStateParam& prm);
    virtual void onCallRxText(OnCallRxTextParam& prm);
};


#endif //PJSIP_SANDBOX_APPCALL_H
