#!/usr/bin/env bash
dir=$(dirname "$0") # directory of this script
for i in $(seq 1 20); do
  rdir=$dir/r.$i
  cmd="cd $rdir && ./run.sh && cd -"
  echo $cmd && eval $cmd
done
