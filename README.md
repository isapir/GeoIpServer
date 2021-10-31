GeoIpServer
===========

The GeoIpServer uses the Maxmind GeoLite2-City database and Apache Tomcat
as well as other open source libraries and technologies.

Geo IP data changes from time to time and the database here might be out of date
if not updated.  Also, if you require higher accuracy then please consider 
subscribing to Maxmind's service and download their commercial database.

TL;DR
-----

To run as a Docker container, simply run

    docker container run -p 63019:63019 isapir/geoipserver

To run as a Java application: 

  1. Make sure that you have Java 17 installed
  2. Download the GeoIpServer-*-dist.zip file
  3. Extract the zip and cd into the directory
  4. Run `./startup.sh`

If you are on Windows then alternatively run 

    java -jar lib/GeoIpServer.jar

Connect with an HTTP client to /ip/{address} at port 63019
e.g. to get the Geo IP location of IPv4 address 23.127.48.1

    curl http://localhost:63019/ip/23.127.48.1 

or for IPv6 address 2600:1700:99c0::1

    curl http://localhost:63019/ip/2600:1700:99c0::1

Minimum Runtime requirements:

  - Java 17
  - 256MB RAM
  
Build requirements also include:

  - Maven
  - Docker

To build:
---------

Open a console window at the project's directory and run

    mvn package

The target/GeoIpServer-<version>-dist.zip file contains everything you need

To run, extract the Zip file's contents to a directory and run:

    ./startup.sh

Which is really just a shorthand for the command

    java -jar lib/GeoIpServer.jar

Examples: 

    curl --include http://localhost:63019/ip/23.127.48.1

>``` 
> HTTP/1.1 200
> X-Time-Took: 0.036ms
> X-Server-Time: 2021-10-31T04:58:44.992+0000
> Content-Type: application/json;charset=ISO-8859-1
> Content-Length: 325
> Date: Sun, 31 Oct 2021 04:58:44 GMT
>
> {
>   "found": "true",
>   "country": "US",
>   "country_name": "United States",
>   "subdivision": "CA",
>   "subdivision_name": "California",
>   "city": "Beverly Hills",
>   "postal_code": "90210",
>   "is_eu": "false",
>   "latitude": "34.0911",
>   "longitude": "-118.4117",
>   "timezone": "America/Los_Angeles",
>   "accuracy_radius": "5",
>   "continent": "NA",
>   "continent_name": "North America"
> }
```