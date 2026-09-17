#include <pjsua2.hpp>
#include <iostream>

#include "AppCall.h"
#include "AppAccount.h"
#include "../transfer/socket/UnixSocketAudioMediaPort.h"
#include "../transfer/udp/UDPAudioMediaPort.h"

using namespace pj;

AppCall::AppCall(Account& acc, int call_id)
    : Call(acc, call_id)
{
    med_port = NULL;
    this->acc = reinterpret_cast<AppAccount*>(&acc);
}

AppCall::AppCall(AppAccount* acc, int call_id)
    : Call(*acc, call_id)
{
    med_port = NULL;
    this->acc = acc;
}

AppCall::~AppCall()
{
    delete med_port;
}


void AppCall::onCallState(OnCallStateParam& prm)
{
    PJ_UNUSED_ARG(prm);

    const CallInfo ci = getInfo();
    std::cout << "*** Call: " << ci.remoteUri
        << " [" << ci.stateText << "]" << std::endl;

    // ***
    string msg = "onCallState: " + ci.remoteUri + "|" + ci.stateText;
    const std::vector<uint8_t> data(msg.begin(), msg.end());
    this->acc->controlChannel_->sendArray(data);

    // ***
    if (ci.state == PJSIP_INV_STATE_DISCONNECTED)
    {
        acc->removeCall(this);
        /* Delete the call */
        delete this;
    }
}

void AppCall::onCallMediaState(OnCallMediaStateParam& prm)
{
    PJ_UNUSED_ARG(prm);

    const CallInfo ci = getInfo();

    for (unsigned i = 0; i < ci.media.size(); i++)
    {
        if (ci.media[i].type == PJMEDIA_TYPE_AUDIO)
        {
            try
            {
                AudioMedia aud_med = getAudioMedia(i);
                MediaFormatAudio am_fmt = aud_med.getPortInfo().format;

                string fileName = std::to_string(ci.id) +
                    +"_" + std::to_string(am_fmt.clockRate)
                    + "_" + std::to_string(am_fmt.channelCount)
                    + "_" + std::to_string(am_fmt.frameTimeUsec)
                    + "_" + std::to_string(am_fmt.bitsPerSample);


                auto* dataChannel = new UnixSocketChannel(
                    this->acc->sockDir + "/" + fileName
                );

                med_port = new UnixSocketAudioMediaPort(dataChannel);
                med_port->createPort("med_port", am_fmt);
                med_port->startTransmit(aud_med);
                aud_med.startTransmit(*med_port);

                // ***
                string msg = "onCallMediaState: " + std::to_string(am_fmt.clockRate)
                    + "|" + std::to_string(am_fmt.channelCount)
                    + "|" + std::to_string(am_fmt.frameTimeUsec)
                    + "|" + std::to_string(am_fmt.bitsPerSample)
                    + "|" + std::to_string(ci.id);

                const std::vector<uint8_t> data(msg.begin(), msg.end());
                this->acc->controlChannel_->sendArray(data);
            }
            catch (const Error& e)
            {
                std::cerr << "Error: " << e.info() << std::endl;
            }
        }
    }
}

void AppCall::onCallTransferRequest(OnCallTransferRequestParam& prm)
{
    prm.newCall = new AppCall(*acc);
}

void AppCall::onCallReplaceRequest(OnCallReplaceRequestParam& prm)
{
    prm.newCall = new AppCall(*acc);
}

void AppCall::onCallRxText(OnCallRxTextParam& prm)
{
    if (prm.text.empty())
    {
        std::cout << "Received empty T140 block with seq " << prm.seq;
        std::cout << std::endl;

        // ***
        string msg = "onCallRxText: " + std::to_string(prm.seq) + "|" + prm.text;
        const std::vector<uint8_t> data(msg.begin(), msg.end());
        this->acc->controlChannel_->sendArray(data);
    }
    else
    {
        std::cout << "Incoming text with seq " << prm.seq << ": " << prm.text;
        std::cout << std::endl;

        // ***
        string msg = "onCallRxText: " + std::to_string(prm.seq) + "|" + prm.text;
        const std::vector<uint8_t> data(msg.begin(), msg.end());
        this->acc->controlChannel_->sendArray(data);
    }
}
