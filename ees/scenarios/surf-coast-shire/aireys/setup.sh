#!/usr/bin/env bash
dir=$(dirname "$0") # directory of this script

# Install EES build if needed
ees_build=eeslib-2.1.1-SNAPSHOT
ees_dir=$dir/../../../target
if [ ! -d $ees_dir/$ees_build ] ; then
  unzip $ees_dir/${ees_build}-release.zip -d $ees_dir
  printf "\nInstalled EES in $ees_dir/$ees_build\n\n"
fi
