#!/usr/bin/env bash
mvn -DskipTests=true clean package spring-aot:generate
java -DspringAot=true -jar target/mybatis-native-0.0.1-SNAPSHOT.jar
