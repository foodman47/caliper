#!/bin/bash

MACHINES=$1

trexec "rm -r /home/worker/julien/library-1.1-beta" $MACHINES

trcp library-1.1-beta/ $MACHINES

trexec "mv library-1.1-beta/ /home/worker/julien" $MACHINES
