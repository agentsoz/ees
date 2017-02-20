#!/bin/sh

###
# #%L
# BDI-ABM Integration Package
# %%
# Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Lesser Public License for more details.
# 
# You should have received a copy of the GNU General Lesser Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/lgpl-3.0.html>.
# #L%
###

DIR=`dirname "$0"`
PROGRAM='java -cp bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.BushfireMain'
#DEFAULT_ARGS='-c scenarios/halls_gap/halls_gap.xml -l halls-gap.log -level INFO'
DEFAULT_ARGS='-c scenarios/maldon/maldon.xml -l maldon.log -level INFO'

# java -cp bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.matsimjill.Main --config scenarios/maldon-simple/maldon.xml --logfile maldon.log --loglevel INFO --jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:700}],logLevel: WARN,logFile: Main.log,programOutputFile: Main.out}" 


# Print usage
$PROGRAM -h

# print args in use
printf "default args:\n  $DEFAULT_ARGS\n\n"

# print user args
USER_ARGS=""
if [ $# -ne 0 ]; then
  USER_ARGS="$@"
  DEFAULT_ARGS=""
fi
printf "user args (will override defaults):\n  $USER_ARGS\n\n"

# print full command
CMD="$PROGRAM $DEFAULT_ARGS $USER_ARGS"
printf "running:\n  "
printf "started on `date +"%B %d, %Y at %r"` \n  "
printf "$CMD\n  "
$CMD >/dev/null 2>&1
printf "finished on `date +"%B %d, %Y at %r"` \n\n"

# To run the Maldon scenario with Jill agents, do:
# java -cp target/bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.matsimjill.Main --config scenarios/maldon-simple/maldon.xml --logfile maldon.log --loglevel INFO --jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:700}],logLevel: WARN,logFile: Main.log,programOutputFile: Main.out}"
