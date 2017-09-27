#!/bin/bash

DIR=$(dirname "$0")

java -cp /var/www/data/bushfire-2.0.2-SNAPSHOT/bushfire-2.0.2-SNAPSHOT.jar io.github.agentsoz.bushfire.matsimjill.Main --config $DIR/scenario/scenario_main.xml --logfile $DIR/scenario/scenario.log --loglevel TRACE --jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:300}],logLevel: WARN,logFile: \"$DIR/scenario/jill.log\",programOutputFile: \"$DIR/scenario/jill.out\"}"

dest1="Harcourt"
dest2="Lockwood South"

# Sample output:
#   Expected of vehicles to the destinations (%): 30 70
#   Drove to Harcourt: 92, Reached: 91
#   Drove to Lockwood South: 208, Reached: 208

split_expected=$(cat scenario/scenario_geography.xml | grep split | cut -f2 -d'>' | cut -f1 -d'<' | sed -e 's/^/ /g' | tr -d '\n')
echo "Expected of vehicles to the destinations (%):$split_expected"

for dest in 'Harcourt' 'Lockwood South' ; do
  to_dest=$(cat $DIR/scenario/jill.out | grep "driving" | grep "$dest" | wc -l | tr -d '\n')
  at_dest=$(cat $DIR/scenario/jill.out | grep "reached" | grep "$dest" | wc -l | tr -d '\n')
  echo "Drove to $dest: $to_dest, Reached: $at_dest"
done


