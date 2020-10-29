FROM maven:3-jdk-8-slim

ENV HOME=/opt/hello-maven
WORKDIR $HOME

COPY settings.xml $HOME
# install dependency only
COPY pom.xml $HOME
RUN mvn -q -s settings.xml dependency:resolve

COPY . $HOME
RUN mvn -q -s settings.xml package -DskipTests

EXPOSE 80

RUN mvn compile
CMD mvn exec:java -Dexec.mainClass="com.wooohooo.luceneQuery.App"
