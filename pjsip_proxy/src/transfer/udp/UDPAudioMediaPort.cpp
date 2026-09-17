#include <pjsua2.hpp>
#include <iostream>

#include "UDPAudioMediaPort.h"

using namespace pj;


void UDPAudioMediaPort::onFrameRequested(MediaFrame& frame)
{
    // Give the input frame here
    std::vector<uint8_t> udp_data = dataChannel_->receiveArray(frame.size);

    // для тестов
    // ffmpeg -i input17458829489450961267.opus -ar 8000 -ac 1 -f s16le input17458829489450961267.pcm
    // std::vector<uint8_t> udp_data = file_buffer_.readBytes(frame.size);

    frame.type = PJMEDIA_FRAME_TYPE_AUDIO;
    frame.buf.assign(udp_data.begin(), udp_data.end());

    //
    std::string s(reinterpret_cast<const char*>(frame.buf.data()), frame.buf.size());
    std::cout << "*** onFrameRequested: " << frame.size << ": " << s << std::endl;
}

void UDPAudioMediaPort::onFrameReceived(MediaFrame& frame)
{
    PJ_UNUSED_ARG(frame);

    //
    std::string s(reinterpret_cast<const char*>(frame.buf.data()), frame.buf.size());
    std::cout << "*** onFrameReceived: " << frame.size << ": " << s << std::endl;

    dataChannel_->sendArray(frame.buf);
}
