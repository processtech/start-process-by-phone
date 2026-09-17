#include <pjsua2.hpp>
#include <iostream>

#include "UnixSocketAudioMediaPort.h"

using namespace pj;


void UnixSocketAudioMediaPort::onFrameRequested(MediaFrame& frame)
{
    std::vector<uint8_t> udp_data = dataChannel_->receiveArray(frame.size);

    frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
    frame.buf.assign(udp_data.begin(), udp_data.end());

    //
    // std::string s(reinterpret_cast<const char*>(frame.buf.data()), frame.buf.size());
    // std::cout << "*** onFrameRequested: " << frame.size << ": " << s << std::flush;
}

void UnixSocketAudioMediaPort::onFrameReceived(MediaFrame& frame)
{
    PJ_UNUSED_ARG(frame);

    //
    // std::string s(reinterpret_cast<const char*>(frame.buf.data()), frame.buf.size());
    // std::cout << "*** onFrameReceived: " << frame.size << ": " << s << std::flush;

    dataChannel_->sendArray(frame.buf);
}
