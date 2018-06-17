#!/usr/bin/env bash

echo "start moving cache"

if [ -f "/home/node/.sbt" ]; then
    mv /home/node/.sbt /home/root/.sbt
fi

if [ -f "/home/node/.ivy2/cache" ]; then
    mv /home/node/.ivy2/cache /home/root/.ivy2/cache
fi

if [ -f "/home/node/.m2" ]; then
    mv /home/node/.m2 /home/root/.m2
fi

echo "end moving cache"
