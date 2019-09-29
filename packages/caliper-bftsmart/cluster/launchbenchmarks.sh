#!/bin/bash
# subshell.sh

# 1. configurations: lauch number of nodes 1 2 4 8 16
# - modify hosts.config, system.config ips
# - set maxbatchsize, #faulty?

transactions=$1
client=cougar6
dir=results_BFTSMaRt_${transactions}

# create folder for results
echo "Create directory for results"
trexec "mkdir /nas/scratch/julien/$dir/" $client
trexec "mkdir /nas/scratch/julien/$dir/2servers" $client
trexec "mkdir /nas/scratch/julien/$dir/4servers" $client
trexec "mkdir /nas/scratch/julien/$dir/8servers" $client
trexec "mkdir /nas/scratch/julien/$dir/16servers" $client


echo "Start session"
servers2=(cougar1 cougar2)
servers4=(cougar1 cougar2 cougar3 cougar4)
servers8=(cougar1 cougar2 cougar3 cougar4 cougar7 cougar8 cheetah1 cheetah2)
servers12=(cougar1 cougar2 cougar3 cougar4 cougar7 cougar8 cheetah1 cheetah2 cheetah3 cheetah4 cheetah5 cheetah6)
servers16=(cougar1 cougar2 cougar3 cougar4 cougar7 cougar8 cheetah1 cheetah2 cheetah3 cheetah4 cheetah5 cheetah26 cheetah22 cheetah23 cheetah24 cheetah25)

declare -A ids=( ["cougar1"]=1001 ["cougar2"]=1002 ["cougar3"]=1003 ["cougar4"]=1004 ["cougar7"]=1005 ["cougar8"]=1006 ["cheetah1"]=1007 ["cheetah2"]=1008 ["cheetah3"]=1009 ["cheetah4"]=1010 ["cheetah5"]=1011 ["cheetah26"]=1012 ["cheetah22"]=1013 ["cheetah23"]=1014 ["cheetah24"]=1015 ["cheetah25"]=1016)

# 2. For this setup do benchmarks request_size x #cli
launch () {
  servers=($@)
  client=cougar6
  len=${#servers[@]}
  folder="$len"servers
  echo "name of folder: results_BFTSMaRt/$folder"

  for size in 4 256 1024
  do
    for cli in 1 10 50 100
    do
      echo "A. Benchmark on $len servers with $cli clients and requests size $size bytes -----"

      echo "B. BRING all servers up"
      for s in ${servers[@]}
      do
        echo "$s with id ${ids[$s]}"
        trexec "cd /home/worker/julien/library-1.1-beta/; ./runscripts/smartrun.sh bftsmart.demo.benchmark.ThroughputLatencyServer ${ids[$s]} $size false 0 0" $s;
        echo "Hello"
        sleep 2
      done

      echo "B2. Waiting 6 sec"
      sleep 6

      echo "C. Starting benchmark on $len servers with $cli clients, requests size $size bytes and $(($transactions/$cli)) txs"
      trexec "cd /home/worker/julien/library-1.1-beta/; ./runscripts/smartrun.sh bftsmart.demo.benchmark.ThroughputLatencyClient 1001 $cli $(($transactions/$cli)) $size 0 true 0" $client;

      #echo "Waiting $(($cli*20)) sec to complete (better too long than too short!)"
      #sleep $(($cli*20))
      sleep 2

      echo "D. Move results to /nas/scratch/results"
      # dont forget to change folder name 4servers
      trexec "mv /home/worker/julien/library-1.1-beta/results/* /nas/scratch/julien/$dir/$folder/" $client
    done

    echo "E. Kill servers"
    trexec "pkill -f 'java'" $client
    for s in ${servers[@]}
    do
      echo "kill java processes on ... $s"
      trexec "pkill -f 'java'" $s
    done

    echo "F. Wait 15 sec ---------------------------------------------------------"
    sleep 15

  done

}

# setup means replacing the system.config file before the benchmark launch
setup () {


  servers=($@)
  len=${#servers[@]}

  trexec "rm /home/worker/julien/library-1.1-beta/config/currentView" $client
  trexec "cp /nas/scratch/julien/SystemConfigs/$len/system.config /home/worker/julien/library-1.1-beta/config" $client
  trexec "cp /nas/scratch/julien/SystemConfigs/$len/hosts.config /home/worker/julien/library-1.1-beta/config" $client

  for s in ${servers[@]}
  do
    echo "setup $s"
    trexec "rm /home/worker/julien/library-1.1-beta/config/currentView" $s
    trexec "cp /nas/scratch/julien/SystemConfigs/$len/system.config /home/worker/julien/library-1.1-beta/config" $s
    trexec "cp /nas/scratch/julien/SystemConfigs/$len/hosts.config /home/worker/julien/library-1.1-beta/config" $s

  done

}

# benchmarks ------------------------------------------------------------------

setup ${servers2[@]}
launch ${servers2[@]}
sleep 10
setup ${servers4[@]}
launch ${servers4[@]}
sleep 10
setup ${servers8[@]}
launch ${servers8[@]}
sleep 10
setup ${servers16[@]}
launch ${servers16[@]}
sleep 10



#------------------------------------------------------------------------------
