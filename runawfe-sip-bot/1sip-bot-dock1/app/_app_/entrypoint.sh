#!/bin/bash

cleanup() {
    echo "останавливаем прокси и rhvoice, удалаем сокеты"
    kill $PROXY_PID 2>/dev/null || true
    kill $RHV_PID 2>/dev/null || true
    rm -rf /tmp/sip_prox/*
}

# rhvoice
export RHVOICE_PORT=9876
docker run --rm --name rhvoice -p $RHVOICE_PORT:8080 aculeasis/rhvoice-rest:latest &
RHV_PID=$!

# Переменные для sip прокси
export SIP_DOMAIN="******"
export SIP_USER="******"
export SIP_PASS="******"
export SIP_PORT="5060"
export STUN_SERVER="******"
# export STUN_SERVER=""

# останавливаем процесс на порту если есть
fuser -k 5060/udp

trap cleanup EXIT INT TERM

# Запускаем прокси в фоне
LD_LIBRARY_PATH=proxy/libs ./proxy/pjprox &
PROXY_PID=$!

# Переменные для основного процесса
export RHVOICE_BASE_URL="http://localhost:$RHVOICE_PORT"
export VOSK_MODEL_DIR="vosk-model-ru-0.42"
export APP_WFE_LOGIN="******"
export APP_WFE_PASS="****"
export APP_WFE_API_URL="https://testing.processtech.ru/restapi"
export APP_WFE_PROCESS_NAME="Такси"

# Запускаем основной процесс
java -jar app-1.0.0.jar

echo "Занятые порты"
ss -tulpn