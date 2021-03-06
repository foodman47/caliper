# caliper-corda
caliper-corda aims to benchmark performances for Corda DLT from R3. The benchmark reports average, minimum, maximum response time
as well as the throughput.

## Table of Contents

1. [A glimpse into benchmarking cordapps](#one)
2. [Integration into Caliper](#two)
3. [Installation](#three)
4. [Usage](#four)
5. [Further work](#five)
6. [License](#six)


## A glimpse into benchmarking cordapps <a name="one"></a>

There are two ways to connect to Corda nodes: either through their shells with ssh or through rpc via a webserver. 

The first has previously been achieved with the caliper client to automate the process of sending transactions in a stressed manner.
The performances were astonishingly low due to the ssh connection overhead.

To bypass this and get better throughput, the use of corda native environment is necessary. That is the main reason why this package does not use
the Caliper client and monitor frameworks. The tool:
 - deploys the nodes as docker containers, 
 - start the webserver which is bound to its Cordapps,
 - send http-request to request benchmarks, measure performances in JVM and
 - log json results containing all information into a text file.
 
 
**Important to know: performances vary a lot between Corda OS (single-threaded) and 
Corda ENT (multi-threaded) [see here](https://medium.com/corda/throughput-a-corda-story-1bc2cb9b2b60).**

## Integration into Caliper <a name="two"></a>

This package has the following structure:
 ```
 /configurations
    /benchmark: config.yaml 
    /network: config-kt.yaml
/cordapps
    /finance: Here goes your cordapps code and jars
/lib
    corda.js
    cordaUtils.js
 ```

 
#### Docker containers

 To simplify the nodes deployments we use docker containers.
 Each container represents a node and contains its own:
 - cordapps
 - certificates
 - drivers
 - corda.jar (*)
 - Dockerfile
 - run-corda.sh (script executed on container start)
 
 The nodes can be generated directly from gradle while developing the Cordapp by running 
 ```./gradlew deployNodes``` and changing the code accordingly using [Dockerform](https://docs.corda.net/generating-a-node.html).

The given docker-compose file will build and create the containers for you on your local machine.

(*) if you want to get better throughput, you can use the Entreprise version by replacing the corda.jar file. You can request
a trial version [here](https://www.r3.com/platform/) and simply replace the jar in each node before to deploy the network.


## Installation <a name="three"></a>

#### Dependencies

- Caliper installed with its dependencies
- JVM 11
- gradle 

#### Steps to reproduce

1. clone this fork repository in ```$HOME```
2. install dependencies: go into /caliper and run:
    - ```npm install```
    - ```npm run repoclean```
    - ```npm run boostrap```
3. configure caliper: (see README in /packages/caliper-tests-integration), run:
    - ```npm install verdaccio -g```
    - ```npm run start_verdaccio```
    - ```npm run publish_packages```
    - ```npm run install_cli```
    - ```npm run cleanup```
4. go to /caliper-corda/ and run ```sudo chmod a+rwx -R cordapps```
4. go to /caliper-corda/cordapps/ and run  ```npm run configure_finance ``` or directly  ```./configure_finance_for_docker.sh ```.
    - it will modify the project to replace some files that were badly generated by plugin.Dockerform
5. configure your benchmark configurations in ./configurations/benchmark/config.yaml
6. go to the caliper root and run the benchmark (see Usage:Run Benchmark)


## Usage <a name="four"></a>

The benchmark sample is based on the [finance example from Corda](https://github.com/corda/corda/tree/master/finance).
It is a good base where to start and can be easily replicated on others Cordapps.


### Caution

- It is **strongly recommended** to develop your Cordapps in another intellij project to avoid problems.
Corda is huge and has a lot of gradle inter-dependencies and not everything is OS. Use this package only when you are sure you can generate your jars. If you try to reproduce 
a Corda official example, it is sometimes tedious to generate them. Some can be found [here](https://dl.bintray.com/r3/corda/net/corda/).

### Configurations files
- Network Configuration file:

```yaml
caliper:
  blockchain: corda
  command:
    start: docker-compose -f cordapps/finance/build/nodes/docker-compose.yml up -d;sleep 45s;
    end: docker-compose -f cordapps/finance/build/nodes/docker-compose.yml down;(test -z \"$(docker ps -aq)\") || docker rm $(docker ps -aq);(test -z \"$(docker images dev* -q)\") || docker rmi $(docker images dev* -q);rm -rf /tmp/hfc-*; kill $(lsof -t -i:10050);
```

(*) note that we wait 45 sec that the containers have time to configure Corda on their node. The time can vary from machines.


- Benchmark Configuration file:
```yaml
   rounds:
   - description: Test description
     txNumber: 1000
     sendingrate: 50
     threads: 5
   result_filename: result.txt
```

### Run benchmark 

After having completed properly the installation and prepared your configurations files, execute the following commands under the ```/caliper``` directory.

To execute the simple issuing transactions example: 

```bash
caliper benchmark run -w ./packages/caliper-corda -c configurations/benchmark/config.yaml -n configurations/network/corda-kt.yaml 
```

### Deployment on a cluster

You can create your nodes locally by running ```docker-compose up --build -d```. Use 
```docker export CONTAINER_NAME > CONTAINER_NAME.gz``` to export your container into a zip file and ```docker import CONTAINER_NAME.gz CONTAINER_NAME```
to import them back onto another machine. To bring your node up, modify the *docker-compose* file accordingly on each machine.

To deploy your network on a cluster, you can create docker swarms between nodes on different machines.

## Further work <a name="five"></a>

- This implementation only runs the official finance-sample from Corda with the OS corda.jar
    - create further examples adding new Cordapps and Benchmarks
- Integrate a multiple notaries example (not sustained yet)
- Work on integrate of Corda into Caliper (report side)
    - it would be nice to be able to launch the JSON output into Caliper report and generate graphs

## License <a name="six"></a>
Hyperledger Project source code files are made available under the Apache License, Version 2.0 (Apache-2.0), located in the [LICENSE](../../LICENSE) file. Hyperledger Project documentation files are made available under the Creative Commons Attribution 4.0 International License (CC-BY-4.0), available at http://creativecommons.org/licenses/by/4.0/.

