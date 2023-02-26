#!/bin/bash

# Pre-Build
git add .
git stash
git fetch
git checkout main
git pull origin main
./gradlew markdownToHtml

# Build
./gradlew clean buildPlugin
git add .
git stash
git stash clear
