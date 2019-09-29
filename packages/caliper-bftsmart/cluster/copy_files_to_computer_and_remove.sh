#!/bin/bash

MACHINES=$1

trexec "scp /home/worker/julien/library-1.1-beta/results/* /nas/scratch/julien/results_BFTSMaRt" $MACHINES
