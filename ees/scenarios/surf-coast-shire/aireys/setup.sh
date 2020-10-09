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
  rdir=$dir/r.base.$i
  cmd="mkdir -p $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.xml $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.json $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.sh $rdir"; echo $cmd && eval $cmd
  cmd="cp $ees_data_repo/surf-coast-shire/plans/2020/plans-60k-$i.xml.gz $rdir/plans.xml.gz"; echo $cmd && eval $cmd
done

# Create the ensemble runs using the msg1 scenario as the template
for i in $(seq 1 20); do
  rdir=$dir/r.msg1.$i
  cmd="mkdir -p $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.xml $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.json $rdir"; echo $cmd && eval $cmd
  cmd="cp $dir/base/*.sh $rdir"; echo $cmd && eval $cmd
  cmd="cp $ees_data_repo/surf-coast-shire/plans/2020/plans-60k-$i.xml.gz $rdir/plans.xml.gz"; echo $cmd && eval $cmd
done
