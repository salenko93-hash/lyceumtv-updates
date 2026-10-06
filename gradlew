#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$APP_HOME" || exit 1

if [ -z "$JAVA_HOME" ]; then
  echo "ERROR: JAVA_HOME is not set. JDK 17 is required." >&2
  exit 3
fi

exec "$JAVA_HOME/bin/java" -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
