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

# Modify below parametes accordingly
NUM_AGENTS=100
INPUT_FILE='input/harrietville.shp'
OUT_FILE='scenarios/harrietville/harrietville_population.xml'
ID_PREFIX=''

PROGRAM='java -cp bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.util.PopulationGenerator'
DEFAULT_ARGS="-n ${NUM_AGENTS} -a ${INPUT_FILE} -o ${OUT_FILE} -p ${ID_PREFIX}"

# Print usage
$PROGRAM -h

# print args in use
printf "default args:\n  $DEFAULT_ARGS\n\n"

# print full command
CMD="${PROGRAM} ${DEFAULT_ARGS}"
printf "running:\n  "
printf "started on `date +"%B %d, %Y at %r"` \n  "
printf "$CMD\n  "
$CMD
printf "finished on `date +"%B %d, %Y at %r"` \n\n"
