# Нативный прокси для ip-телефонии

Опробован на ubuntu 24.04 LTS

### Сначала устанавливаем зависимости
```shell
sudo su
apt-get update  \
    && apt-get install -y \
    build-essential cmake gdb clang \
    wget tar \
    python3 python3-dev \
    swig \
    openjdk-17-jdk \
    libasound2t64 alsa-utils libasound2-dev \
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
    ca-certificates
```

### Устанавливаем pjsip
```shell
export PJ_VERSION="2.16"

cd ~

wget https://github.com/pjsip/pjproject/archive/refs/tags/$PJ_VERSION.tar.gz \
    && tar -xvzf $PJ_VERSION.tar.gz \
    && cd pjproject-$PJ_VERSION

cd pjproject-$PJ_VERSION

./configure CFLAGS="-fPIC" --prefix=/usr/local --enable-shared
make dep && make

sudo make install

echo "/usr/local/lib" | tee /etc/ld.so.conf.d/pjsip.conf
ldconfig
```

### Собираем проект
```shell
cmake -S . -B build
cmake --build build/
```

### Ставим переменные среды и запускаем
```shell
export SIP_DOMAIN="*****"
export SIP_USER="*****"
export SIP_PASS="*****"
export SIP_PORT="5060"
export SOCK_DIR="/tmp/sip_prox"

./build/pjsip_proxy
```

И у нас в директории /tmp/sip_prox появится управляющий сокет _control (в нем данные о состоянии звонка и параметры)
в процессе работы рядом будут появляться сокеты самих звонков через которые можно обмениваться аудио-данными

При желании можно создать сервис systemd
```shell
echo 'export SIP_DOMAIN="*****"' | tee -a ~/.bashrc
echo 'export SIP_USER="*****"' | tee -a ~/.bashrc
echo 'export SIP_PASS="*****"' | tee -a ~/.bashrc
echo 'export SIP_PORT="5060"' | tee -a ~/.bashrc
echo 'export SOCK_DIR="/tmp/sip_prox"' | tee -a ~/.bashrc

source ~/.bashrc

#
sudo cp build/pjsip_proxy /opt

#
sudo tee /etc/systemd/system/sip_prox.service <<EOF
[Unit]
Description=SIP Proxy Service
After=network.target

[Service]
ExecStart=/opt/pjsip_proxy
Restart=always
User=root

[Install]
WantedBy=multi-user.target
EOF

#
sudo systemctl daemon-reload
sudo systemctl start sip_prox
sudo systemctl enable sip_prox

```

В принципе все, дальше надо работать через сокеты в /tmp/sip_prox



### Пакет поставки
нужен сам бинарник pjsip_proxy
и рядом в директорию lib скидать .so из библиотеки pjsip

```
[libg7221codec.so.2]
[libgsmcodec.so.2]
[libilbccodec.so.2]
[libpjlib-util.so.2]
[libpjmedia-audiodev.so.2]
[libpjmedia-codec.so.2]
[libpjmedia.so.2]
[libpjmedia-videodev.so.2]
[libpjnath.so.2]
[libpjsip-simple.so.2]
[libpjsip.so.2]
[libpjsip-ua.so.2]
[libpj.so.2]
[libpjsua2.so.2]
[libpjsua.so.2]
[libresample.so.2]
[libspeex.so.2]
[libsrtp.so.2]
[libwebrtc.so.2]
[libyuv.so.2]

[libg7221codec.so]
[libgsmcodec.so]
[libilbccodec.so]
[libpj.so]
[libpjlib-util.so]
[libpjmedia.so]
[libpjmedia-audiodev.so]
[libpjmedia-codec.so]
[libpjmedia-videodev.so]
[libpjnath.so]
[libpjsip.so]
[libpjsip-simple.so]
[libpjsip-ua.so]
[libpjsua.so]
[libpjsua2.so]
[libresample.so]
[libspeex.so]
[libsrtp.so]
[libwebrtc.so]
[libyuv.so]
```


теперь эту директорию можно просто скопировать и будет работать

Дополнение:       
// Стало: включаем переписывание Contact-заголовка на публичный IP (через STUN)
acc_cfg.natConfig.contactRewriteUse = 1; // если 0 (false), PJSIP игнорирует свой публичный IP