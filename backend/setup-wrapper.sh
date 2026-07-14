#!/usr/bin/env bash
# Run this once to download the Gradle wrapper JAR before building.
# Requires curl and internet access.
set -e

JAR_URL="https://github.com/gradle/gradle/raw/v8.10.0/gradle/wrapper/gradle-wrapper.jar"
JAR_PATH="gradle/wrapper/gradle-wrapper.jar"

if [ -f "$JAR_PATH" ]; then
  echo "gradle-wrapper.jar already exists, skipping download."
  exit 0
fi

echo "Downloading gradle-wrapper.jar..."
curl -L "$JAR_URL" -o "$JAR_PATH"
echo "Done. You can now run: ./gradlew build"
