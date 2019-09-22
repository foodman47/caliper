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
mkdir ./switchtrip/build/
mkdir ./switchtrip/build/nodes
touch ./switchtrip/build/nodes/docker-compose.yml

# compile kotlin, generate node skeleton
cd ./switchtrip/
./gradlew
./gradlew deployNodes
cd ./../

# replace faulty docker-compose-yml
rm ./switchtrip/build/nodes/docker-compose.yml
cp ./utils/cordapp-for-docker_switchtrip/docker-compose.yml ./switchtrip/build/nodes/

# replace faulty Dockerfile
rm ./switchtrip/build/nodes/Notary/Dockerfile
cp ./utils/cordapp-for-docker_switchtrip/Dockerfile ./switchtrip/build/nodes/Notary/
rm ./switchtrip/build/nodes/PartyA/Dockerfile
cp ./utils/cordapp-for-docker_switchtrip/Dockerfile ./switchtrip/build/nodes/PartyA/
rm ./switchtrip/build/nodes/PartyB/Dockerfile
cp ./utils/cordapp-for-docker_switchtrip/Dockerfile ./switchtrip/build/nodes/PartyB/
rm ./switchtrip/build/nodes/PartyC/Dockerfile
cp ./utils/cordapp-for-docker_switchtrip/Dockerfile ./switchtrip/build/nodes/PartyC/

# replace faulty run-corda.sh
rm ./switchtrip/build/nodes/Notary/run-corda.sh
cp ./utils/cordapp-for-docker_switchtrip/Notary/run-corda.sh ./switchtrip/build/nodes/Notary/
rm ./switchtrip/build/nodes/PartyA/run-corda.sh
cp ./utils/cordapp-for-docker_switchtrip/PartyA/run-corda.sh ./switchtrip/build/nodes/PartyA/
rm ./switchtrip/build/nodes/PartyB/run-corda.sh
cp ./utils/cordapp-for-docker_switchtrip/PartyB/run-corda.sh ./switchtrip/build/nodes/PartyB/
rm ./switchtrip/build/nodes/PartyC/run-corda.sh
cp ./utils/cordapp-for-docker_switchtrip/PartyC/run-corda.sh ./switchtrip/build/nodes/PartyC/

# build nodes on docker
cd ./switchtrip/build/nodes/
docker-compose up --build -d
# docker-compose down




