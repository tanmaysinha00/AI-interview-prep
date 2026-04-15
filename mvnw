#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup script, version 3.3.2
#
# Required ENV vars:
# ------------------
#   JAVA_HOME - location of a JDK home dir
#
# Optional ENV vars
# -----------------
#   MAVEN_OPTS - parameters passed to the Java VM when running Maven
#     e.g. to debug Maven itself, use
#       set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000
#   MAVEN_SKIP_RC - flag to disable loading of mavenrc files
# ----------------------------------------------------------------------------

if [ -z "$MAVEN_SKIP_RC" ]; then
  if [ -f /usr/local/etc/mavenrc ]; then
    . /usr/local/etc/mavenrc
  fi
  if [ -f /etc/mavenrc ]; then
    . /etc/mavenrc
  fi
  if [ -f "$HOME/.mavenrc" ]; then
    . "$HOME/.mavenrc"
  fi
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
darwin=false
mingw=false
case "$(uname)" in
  CYGWIN*) cygwin=true ;;
  MINGW*) mingw=true ;;
  Darwin*) darwin=true ;;
esac

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(pwd)}"

wrapperJarPath="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
wrapperPropertiesPath="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ -r "$wrapperPropertiesPath" ]; then
  distributionUrl=$(grep '^distributionUrl' "$wrapperPropertiesPath" | cut -d= -f2-)
  wrapperUrl=$(grep '^wrapperUrl' "$wrapperPropertiesPath" | cut -d= -f2-)
fi

if [ ! -f "$wrapperJarPath" ]; then
  if command -v wget > /dev/null; then
    wget -q -O "$wrapperJarPath" "$wrapperUrl" || {
      echo "Failed to download maven-wrapper.jar via wget" >&2
      exit 1
    }
  elif command -v curl > /dev/null; then
    curl -fsSL -o "$wrapperJarPath" "$wrapperUrl" || {
      echo "Failed to download maven-wrapper.jar via curl" >&2
      exit 1
    }
  else
    echo "Neither wget nor curl is available. Cannot download maven-wrapper.jar." >&2
    exit 1
  fi
fi

JAVA_CMD="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
  JAVA_CMD="java"
fi

exec "$JAVA_CMD" \
  $MAVEN_OPTS \
  $MAVEN_DEBUG_OPTS \
  -classpath "$wrapperJarPath" \
  "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" \
  org.apache.maven.wrapper.MavenWrapperMain \
  "$@"
