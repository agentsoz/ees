#!/usr/bin/env bash

case "$1" in
  "base")
    ;;
  "msg1")
    ;;
  *)
    echo "please supply ensemble name e.g., base"
    exit 1
    ;;
esac

dir=$(dirname "$0") # directory of this script
for i in $(seq 12 20); do
  rdir=$dir/r.$1.$i
  cmd='rm -f $rdir.zip'
  echo $cmd && eval $cmd
  cmd='find $rdir -name "*.*" | grep -v ITERS | grep -v jill | grep output | zip $rdir.zip -9 -@ '
  echo $cmd && eval $cmd
done
