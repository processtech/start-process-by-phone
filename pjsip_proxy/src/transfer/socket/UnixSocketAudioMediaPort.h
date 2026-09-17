#ifndef PJSIP_SANDBOX_UNIXSOCKETAUDIOMEDIAPORT_H
#define PJSIP_SANDBOX_UNIXSOCKETAUDIOMEDIAPORT_H

#include <pjsua2.hpp>

#include "UnixSocketChannel2.cpp"

using namespace pj;

class UnixSocketAudioMediaPort : public AudioMediaPort
{
    UnixSocketChannel* dataChannel_;

public:
    UnixSocketAudioMediaPort(UnixSocketChannel* dataChannel_)
    {
        this->dataChannel_ = dataChannel_;
    }

    ~UnixSocketAudioMediaPort() override
    {
        delete dataChannel_;
    }

    virtual void onFrameRequested(MediaFrame& frame);

    virtual void onFrameReceived(MediaFrame& frame);
};


#endif //PJSIP_SANDBOX_UNIXSOCKETAUDIOMEDIAPORT_H
