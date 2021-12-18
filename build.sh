#!/usr/bin/env bash
mvn -DskipTests=true -Pnative clean package && ./target/mybatis-native