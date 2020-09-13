#!/usr/bin/env bash

# Must be run from the directory containing this script

eesdir=./../../../../target/eeslib-2.1.1-SNAPSHOT
jars=${eesdir}/libs/*:${eesdir}/eeslib-2.1.1-SNAPSHOT.jar
cmd="java -cp $jars io.github.agentsoz.ees.Run --config ees.xml"
echo $cmd && eval $cmd
