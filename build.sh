#!/bin/bash
cd /root/.qwenpaw/workspaces/LvFang/media/BetterPlayerHUD
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 /root/.qwenpaw/workspaces/LvFang/media/gradle-4.10.3/bin/gradle build 2>&1 | tail -15
