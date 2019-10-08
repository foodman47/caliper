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

FOLDER_NAME=$1

# create missing file
mkdir ./$FOLDER_NAME/build/
mkdir ./$FOLDER_NAME/build/nodes
touch ./$FOLDER_NAME/build/nodes/docker-compose.yml

# compile kotlin, generate node skeleton
cd ./$FOLDER_NAME/
./gradlew
./gradlew deployNodes
cd ./../

# replace faulty docker-compose-yml
rm ./$FOLDER_NAME/build/nodes/docker-compose.yml
cp ./utils/cordapp-for-docker_$FOLDER_NAME/docker-compose.yml ./$FOLDER_NAME/build/nodes/

# replace faulty Dockerfile
rm ./$FOLDER_NAME/build/nodes/Notary/Dockerfile
cp ./utils/cordapp-for-docker_$FOLDER_NAME/Dockerfile ./$FOLDER_NAME/build/nodes/Notary/
rm ./$FOLDER_NAME/build/nodes/PartyA/Dockerfile
cp ./utils/cordapp-for-docker_$FOLDER_NAME/Dockerfile ./$FOLDER_NAME/build/nodes/PartyA/
rm ./$FOLDER_NAME/build/nodes/PartyB/Dockerfile
cp ./utils/cordapp-for-docker_$FOLDER_NAME/Dockerfile ./$FOLDER_NAME/build/nodes/PartyB/
rm ./$FOLDER_NAME/build/nodes/PartyC/Dockerfile
cp ./utils/cordapp-for-docker_$FOLDER_NAME/Dockerfile ./$FOLDER_NAME/build/nodes/PartyC/

# replace faulty run-corda.sh
rm ./$FOLDER_NAME/build/nodes/Notary/run-corda.sh
cp ./utils/cordapp-for-docker_$FOLDER_NAME/Notary/run-corda.sh ./$FOLDER_NAME/build/nodes/Notary/
rm ./$FOLDER_NAME/build/nodes/PartyA/run-corda.sh
cp ./utils/cordapp-for-docker_$FOLDER_NAME/PartyA/run-corda.sh ./$FOLDER_NAME/build/nodes/PartyA/
rm ./$FOLDER_NAME/build/nodes/PartyB/run-corda.sh
cp ./utils/cordapp-for-docker_$FOLDER_NAME/PartyB/run-corda.sh ./$FOLDER_NAME/build/nodes/PartyB/
rm ./$FOLDER_NAME/build/nodes/PartyC/run-corda.sh
cp ./utils/cordapp-for-docker_$FOLDER_NAME/PartyC/run-corda.sh ./$FOLDER_NAME/build/nodes/PartyC/

# build nodes on docker
cd ./$FOLDER_NAME/build/nodes/
docker-compose up --build -d
# docker-compose down




