FROM hub.sunteco.io/dockerhub-proxy/openjdk:17

# Fix resolving path when building with kaniko:
#ENV LD_LIBRARY_PATH=/usr/lib64:/usr/lib



#Import keytool
#COPY bin/ca.crt $HOME/ca.crt
#RUN mkdir -p /etc/security/tls
#RUN keytool -import -trustcacerts -alias root -file $HOME/ca.crt -keystore /etc/security/tls/keystore-system.jks -storepass password -noprompt


COPY target/*.jar ipvalidate-1-0.jar

EXPOSE 8081

CMD java -jar ipvalidate-1-0.jar
