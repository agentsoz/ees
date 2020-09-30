#!/usr/bin/env bash

dir=$(dirname "$0") # directory of this script

ees_data_repo=$dir/../../../../../ees-data

# Install EES build
ees_build=eeslib-2.1.1-SNAPSHOT
ees_dir=$dir/../../../target
if [ -f $ees_dir/${ees_build}-release.zip ] ; then
  rm -rf $ees_dir/${ees_build}
  unzip $ees_dir/${ees_build}-release.zip -d $ees_dir
  printf "\nInstalled EES in $ees_dir/$ees_build\n\n"
else
  printf "Aborting. EES build not found in $ees_dir/${ees_build}-release.zip\n"
  exit
fi

# Create the ensemble runs using the base scenario as the template
for i in $(seq 1 20); do
  rdir=$dir/r.$i
  #cmd="rm -rf $rdir"; echo $cmd && eval $cmd
  cmd="mkdir -p $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/ees.xml $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/matsim_config.xml $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/run.sh $rdir"; echo $cmd && eval $cmd
  cmd="cp $ees_data_repo/surf-coast-shire/plans/2020/plans-60k-$i.xml.gz $rdir/plans.xml.gz"; echo $cmd && eval $cmd
done
