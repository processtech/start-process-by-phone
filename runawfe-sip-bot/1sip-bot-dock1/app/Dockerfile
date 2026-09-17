FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update  \
        && apt-get install -y \
        unzip wget ffmpeg locales language-pack-ru \
        libgomp1 \
        libstdc++6 \
        openjdk-17-jdk \
        libasound2t64 alsa-utils libasound2-dev \
        libssl-dev \
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
    && rm -rf /var/lib/apt/lists/* \
    && locale-gen ru_RU.UTF-8

ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_ALL ru_RU.UTF-8

# Подготовка приложения
WORKDIR /app
COPY _app_ .

# Запуск
CMD ["sh", "entrypoint.sh"]
