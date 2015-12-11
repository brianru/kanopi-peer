FROM java:8
MAINTAINER Brian James Rubinton <brian@kanopi.io>

EXPOSE 8080

ADD target/kanopi-uberjar.jar /kanopi-uberjar.jar

CMD "java" "-jar" "kanopi-uberjar.jar"
