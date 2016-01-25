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
INPUT_FILE='input/harrietville.osm'
OUT_FILE='scenarios/harrietville/harrietville_network.xml'

# the ESRI well known string based on the UTM zone of the network. For Australia, the ESRI WKT can be generated using menu in lower left of http://spatialreference.org/ref/epsg/28354/ page. Default value is set to MGA zone 54
WKT='PROJCS["GDA94 / MGA zone 54",GEOGCS["GDA94",DATUM["D_GDA_1994",SPHEROID["GRS_1980",6378137,298.257222101]],PRIMEM["Greenwich",0],UNIT["Degree",0.017453292519943295]],PROJECTION["Transverse_Mercator"],PARAMETER["latitude_of_origin",0],PARAMETER["central_meridian",141],PARAMETER["scale_factor",0.9996],PARAMETER["false_easting",500000],PARAMETER["false_northing",10000000],UNIT["Meter",1]]"'

PROGRAM='java -cp bushfire-1.0.0.jar io.github.agentsoz.util.NetworkGenerator'

# print full command
printf "running:\n  "
printf "started on `date +"%B %d, %Y at %r"` \n  "
${PROGRAM} -i ${INPUT_FILE} -o ${OUT_FILE} -wkt "${WKT}"
printf "finished on `date +"%B %d, %Y at %r"` \n\n"
