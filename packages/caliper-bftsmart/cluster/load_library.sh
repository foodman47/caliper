#!/bin/bash
MACHINES=$1

trcp library-1.1-beta/config/hosts.config $MACHINES
trexec "mv hosts.config /home/worker/julien/library-1.1-beta/config" $MACHINES
