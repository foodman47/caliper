#!/bin/bash

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# create missing file
mkdir ./increment/build/
mkdir ./increment/build/nodes
touch ./increment/build/nodes/docker-compose.yml

# compile kotlin, generate node skeleton
cd ./increment/
./gradlew
./gradlew deployNodes
cd ./../

# replace faulty docker-compose-yml
rm ./increment/build/nodes/docker-compose.yml
cp ./utils/cordapp-for-docker_increment/docker-compose.yml ./increment/build/nodes/

# replace faulty Dockerfile
rm ./increment/build/nodes/Notary/Dockerfile
cp ./utils/cordapp-for-docker_increment/Dockerfile ./increment/build/nodes/Notary/
rm ./increment/build/nodes/PartyA/Dockerfile
cp ./utils/cordapp-for-docker_increment/Dockerfile ./increment/build/nodes/PartyA/
rm ./increment/build/nodes/PartyB/Dockerfile
cp ./utils/cordapp-for-docker_increment/Dockerfile ./increment/build/nodes/PartyB/
rm ./increment/build/nodes/PartyC/Dockerfile
cp ./utils/cordapp-for-docker_increment/Dockerfile ./increment/build/nodes/PartyC/

# replace faulty run-corda.sh
rm ./increment/build/nodes/Notary/run-corda.sh
cp ./utils/cordapp-for-docker_increment/Notary/run-corda.sh ./increment/build/nodes/Notary/
rm ./increment/build/nodes/PartyA/run-corda.sh
cp ./utils/cordapp-for-docker_increment/PartyA/run-corda.sh ./increment/build/nodes/PartyA/
rm ./increment/build/nodes/PartyB/run-corda.sh
cp ./utils/cordapp-for-docker_increment/PartyB/run-corda.sh ./increment/build/nodes/PartyB/
rm ./increment/build/nodes/PartyC/run-corda.sh
cp ./utils/cordapp-for-docker_increment/PartyC/run-corda.sh ./increment/build/nodes/PartyC/

# build nodes on docker
cd ./increment/build/nodes/
docker-compose up --build -d
# docker-compose down




