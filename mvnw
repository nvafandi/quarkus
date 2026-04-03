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

MVN_VERSION="3.9.5"
MVN_HOME="$HOME/.m2/wrapper/dists/apache-maven-${MVN_VERSION}"

if [ ! -d "$MVN_HOME" ]; then
    echo "Downloading Apache Maven ${MVN_VERSION}..."
    mkdir -p "$MVN_HOME"
    curl -sL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.zip" -o /tmp/maven.zip
    unzip -q /tmp/maven.zip -d /tmp
    mv /tmp/apache-maven-${MVN_VERSION}/* "$MVN_HOME"
    rm -rf /tmp/maven.zip /tmp/apache-maven-${MVN_VERSION}
fi

export MAVEN_HOME="$MVN_HOME"
exec "$MAVEN_HOME/bin/mvn" "$@"
