FROM azul/zulu-openjdk-alpine:17

WORKDIR /var/GeoIpServer

ADD target/GeoIpServer-0.1-dist.tar /var/GeoIpServer

CMD [ "java", "-jar", "lib/GeoIpServer.jar" ]
