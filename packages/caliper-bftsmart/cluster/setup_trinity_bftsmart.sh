#!/bin/bash

PATH=$1
MACHINES=$2

trexec "rm -r $PATH/library-1.1-beta" $MACHINES

trcp library-1.1-beta/ $MACHINES

trexec "mv ./../library-1.1-beta/ $PATH" $MACHINES
