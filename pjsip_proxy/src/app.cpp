#include <atomic>
#include <filesystem>
#include <pjsua2.hpp>
#include <iostream>
#include <string>

#include "sip/AppAccount.h"
#include "sip/AppCall.h"
#include "sip/AppEndpoint.h"

#define THIS_FILE       "app.cpp"

using namespace pj;

std::atomic_bool running = true;
// atomic_store(&running, false);

std::string getFromEnvOrDefault(const std::string& key, std::string default_value)
{
    if (const char* env_p = std::getenv(key.c_str()))
    {
        return std::string(env_p);
    }

    return default_value;
}

int run(std::string sipDomain, std::string sipUser, std::string sipPass, int sipPort,
        std::string sockDir, std::string stunServer)
{
    try
    {
        AppEndpoint endpoint;
        endpoint.libCreate();

        // Init library
        EpConfig ep_cfg;
        ep_cfg.logConfig.level = 5;
        if (!stunServer.empty())
            ep_cfg.uaConfig.stunServer.push_back(stunServer);
        endpoint.libInit(ep_cfg);

        // use no device
        AudDevManager& aud_dev_manager = endpoint.audDevManager();
        aud_dev_manager.setNullDev();

        // Transport
        TransportConfig tcfg;
        tcfg.port = sipPort;

         // указал дополнительно. На стенде не работало
        std::string publicIp = getFromEnvOrDefault("PUBLIC_IP", "");
        if (!publicIp.empty()) {
            tcfg.publicAddress = publicIp;
        }

        endpoint.transportCreate(PJSIP_TRANSPORT_UDP, tcfg);

        // Start library
        endpoint.libStart();
        std::cout << "*** PJSUA2 STARTED ***" << std::endl;

        // Add account
        AccountConfig acc_cfg;
        acc_cfg.idUri = "sip:" + sipUser + "@" + sipDomain;
        acc_cfg.regConfig.registrarUri = "sip:" + sipDomain;

          // Было: acc_cfg.natConfig.contactRewriteUse = 0;
          // Стало: включаем переписывание Contact-заголовка на публичный IP (через STUN)
        acc_cfg.natConfig.contactRewriteUse = 1; // или PJ_TRUE
        // Заставляет PJSIP переписывать IP также внутри SDP (строка c=) !!!
        acc_cfg.natConfig.sdpNatRewriteUse = true;
        /*
        acc_cfg.natConfig.iceEnabled = true;
        acc_cfg.natConfig.turnEnabled = true;
        acc_cfg.natConfig.turnConnType = PJ_TURN_TP_TCP;
        acc_cfg.natConfig.turnServer = "turn:global.relay.metered.ca:80";
        acc_cfg.natConfig.turnUserName = "bbb30a550266a72a49e7a605";
        acc_cfg.natConfig.turnPassword = "mSldRKq68urWpup6";
        */

        AuthCredInfo aci("digest", "*", sipUser, 0, sipPass);

        acc_cfg.sipConfig.authCreds.push_back(PJSUA2_MOVE(aci));
        auto acc(new AppAccount(sockDir));
        try
        {
            acc->create(acc_cfg);
        }
        catch (...)
        {
            std::cout << "Adding account failed" << std::endl;
        }

        while (atomic_load(&running))
        {
            pj_thread_sleep(496);

            if (-1 != acc->hangId)
            {
                // hang
                for (const auto& call : acc->calls)
                {
                    if (acc->hangId == call->getId())
                    {
                        call->hangup(true);
                        acc->hangId = -1;
                        acc->removeCall(call);
                    }
                }
            }


            if (!acc->callToSipAddr.empty())
            {
                Call* call = new AppCall(acc, PJSUA_INVALID_ID);
                acc->calls.push_back(call);

                CallOpParam prm(true);
                prm.opt.audioCount = 1;
                prm.opt.videoCount = 0;
                prm.opt.textCount = 1;

                call->makeCall(acc->callToSipAddr, prm);

                acc->callToSipAddr.clear();
            }
        }

        delete acc;
        endpoint.libDestroy();
    }
    catch (const std::exception& e)
    {
        std::cerr << e.what() << std::endl;
        return -1;
    }
    return 0;
}

int main(int argc, char* argv[])
{
    std::string sipDomain = getFromEnvOrDefault("SIP_DOMAIN", "");
    std::string sipUser = getFromEnvOrDefault("SIP_USER", "");
    std::string sipPass = getFromEnvOrDefault("SIP_PASS", "");
    int sipPort = std::stoi(getFromEnvOrDefault("SIP_PORT", "5060"));
    std::string stunServer = getFromEnvOrDefault("STUN_SERVER", ""); // stun.l.google.com:19302

    std::string sockDir = getFromEnvOrDefault("SOCK_DIR", "/tmp/sip_prox");
    std::filesystem::create_directories(sockDir);

    std::thread worker = std::thread(&run, sipDomain, sipUser, sipPass, sipPort, sockDir, stunServer);
    worker.join();

    return PJ_SUCCESS;
}
