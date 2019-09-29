#!/bin/bash

servers16=(cougar1 cougar2 cougar3 cougar4 cougar7 cougar8 cheetah1 cheetah2 cheetah3 cheetah4 cheetah5 cheetah6 cheetah22 cheetah23 cheetah24 cheetah25 cheetah26)

echo "Kill java processes"
trexec "pkill -f 'java'" cougar6
for s in "${servers16[@]}"
do
  echo "kill on ... $s"
  trexec "pkill -f 'java'" $s
done
echo "done!"
