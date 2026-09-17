# ============================================
# ЭТАП 1: Сборка PJSIP и прокси
# ============================================
FROM ubuntu:24.04 AS builder

ENV DEBIAN_FRONTEND=noninteractive

# Установка всех зависимостей для сборки
RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    gdb \
    clang \
    wget \
    tar \
    python3 \
    python3-dev \
    swig \
    libasound2-dev \
    libssl-dev \
    libpulse-dev \
    libopus-dev \
    libvpx-dev \
    libspeex-dev \
    libspeexdsp-dev \
    libavcodec-dev \
    libavformat-dev \
    libavutil-dev \
    libswscale-dev \
    pkg-config \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Установка PJSIP 2.16
ENV PJ_VERSION="2.16"
WORKDIR /build

RUN wget -qO- https://github.com/pjsip/pjproject/archive/refs/tags/$PJ_VERSION.tar.gz | tar xz \
    && cd pjproject-$PJ_VERSION \
    && ./configure CFLAGS="-fPIC -g"  --prefix=/usr/local --enable-shared \
    && make dep && make \
    && make install

# Копирование исходников прокси
COPY . /build/proxy
WORKDIR /build/proxy

# Сборка прокси
RUN cmake -S . -B build -DCMAKE_BUILD_TYPE=Debug \
    && cmake --build build/

# ============================================
# ЭТАП 2: Минимальный runtime образ
# ============================================
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

# Установка только runtime зависимостей
RUN apt-get update && apt-get install -y \
    libasound2t64 \
    libssl3 \
    libpulse0 \
    libopus0 \
    libvpx9 \
    libspeex1 \
    libavcodec60 \
    libavformat60 \
    libavutil58 \
    libswscale7 \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Копирование скомпилированных библиотек PJSIP
COPY --from=builder /usr/local/lib/ /usr/local/lib/

# Копирование бинарника прокси
COPY --from=builder /build/proxy/build/pjsip_proxy /opt/pjsip_proxy

# Обновление кэша библиотек
RUN ldconfig

# Создание директории для сокетов
ENV SOCK_DIR=/tmp/sip_prox
RUN mkdir -p $SOCK_DIR && chmod 777 $SOCK_DIR

WORKDIR /opt

# Запуск прокси
ENTRYPOINT ["./pjsip_proxy"]