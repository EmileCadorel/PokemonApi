#!/usr/bin/env sh

base=$(pwd)

# Build front
cd ${base}/frontend
npm run build

# Copy to back
cd ${base}
rm ${base}/backend/src/main/resources/static/assets/*
rm ${base}/backend/src/main/resources/static/*

cp -r ${base}/frontend/dist/* ${base}/backend/src/main/resources/static

# build back
cd ${base}/backend
./gradlew build
./gradlew jar

mkdir ${base}/target
cp ${base}/backend/build/libs/pokemon-server.jar ${base}/target


