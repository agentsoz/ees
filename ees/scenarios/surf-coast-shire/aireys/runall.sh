#!/usr/bin/env bash

case "$1" in
  "base")
    ex=run.sh
    ;;
  "msg1")
    ex=run-msg1.sh
    ;;
  *)
    echo "please supply ensemble name e.g., base"
    exit 1
    ;;
esac

dir=$(dirname "$0") # directory of this script
for i in $(seq 1 20); do
  rdir=$dir/r.$1.$i
  cmd="cd $rdir && ./$ex && cd -"
  echo $cmd && eval $cmd
done
