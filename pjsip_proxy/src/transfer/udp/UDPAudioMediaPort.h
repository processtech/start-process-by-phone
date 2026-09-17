//
// Created by evgeny on 21.02.2026.
//

#ifndef PJSIP_SANDBOX_UDPAUDIOMEDIAPORT_H
#define PJSIP_SANDBOX_UDPAUDIOMEDIAPORT_H

#include <pjsua2.hpp>

#include "UdpChannel.h"

using namespace pj;

class UDPAudioMediaPort : public AudioMediaPort
{
    UdpChannel* dataChannel_;
    // FileBuffer file_buffer_;

public:
    UDPAudioMediaPort(UdpChannel* dataChannel_) // : file_buffer_("/home/evgeny/Загрузки/input17458829489450961267.pcm")
    {
        this->dataChannel_ = dataChannel_;
    }

    ~UDPAudioMediaPort() override
    {
        delete dataChannel_;
    }

    virtual void onFrameRequested(MediaFrame& frame);

    virtual void onFrameReceived(MediaFrame& frame);
};

#endif //PJSIP_SANDBOX_UDPAUDIOMEDIAPORT_H
