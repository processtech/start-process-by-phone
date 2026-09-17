#ifndef PJSIP_SANDBOX_APPENDPOINT_H
#define PJSIP_SANDBOX_APPENDPOINT_H

#include <pjsua2.hpp>

using namespace pj;

class AppEndpoint final : public Endpoint
{
public:
    AppEndpoint() : Endpoint()
    {
    };

    pj_status_t onCredAuth(OnCredAuthParam& prm) override;
};

#endif //PJSIP_SANDBOX_APPENDPOINT_H
