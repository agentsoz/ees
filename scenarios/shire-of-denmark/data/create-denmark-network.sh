#!/bin/bash

###
# Script to generate a new MATSim network for Shire of Denmark WA.
# Author: Dhirendra Singh, 7/Mar/18
#
# All files are saved relative to the directory of this script.
# Steps:
# 1. Download and install the latest osmosis binary if not already there
# 2. Download and unzip the latest map of Australia (OSM)
# 3. Create a detailed map of the Shire
# 4. Create a larger map of big roads to the major nearby cities
# 5. Combine the two into a final OSM map
# 6. Generate the MATSim network from the above
#
###

### CONFIG ###
OUTFILE_PREFIX=shire_of_denmark # in western australia
EPSG=EPSG:28350 # perth area
### END CONFIG ###

DIR=$(dirname "$0")


# download the latest osmosis binary if needed
OSMOSIS_ZIP=osmosis-latest.tgz
OSMOSIS_DIR=$DIR/osmosis
OSMOSIS_EXE=$DIR/osmosis/bin/osmosis
OSMOSIS_WEB=https://bretth.dev.openstreetmap.org/osmosis-build/${OSMOSIS_ZIP}
if [ ! -d $OSMOSIS_DIR ] ; then
  cd $DIR
  printf "\nGetting $OSMOSIS_WEB ...\n\n"
  wget -O $OSMOSIS_ZIP $OSMOSIS_WEB
  mkdir $OSMOSIS_DIR
  mv $OSMOSIS_ZIP $OSMOSIS_DIR
  cd $OSMOSIS_DIR
  tar xvfz $OSMOSIS_ZIP
  rm -f OSMOSIS_ZIP
  chmod a+x bin/osmosis
  printf "\nInstalled latest osmosis in $OSMOSIS_EXE\n\n"
  cd -
fi

# download the latest OSM extract for Australia if needed from:
# http://download.gisgraphy.com/openstreetmap/pbf/AU.tar.bz2
AU_PBF=AU.pbf
OSM_ZIP=AU.tar.bz2
OSM_WEB=http://download.gisgraphy.com/openstreetmap/pbf/$OSM_ZIP
if [ ! -f $DIR/$AU_PBF ] ; then
  cd $DIR
  printf "\nGetting $OSM_WEB ...\n\n"
  #wget -O $OSM_ZIP http://download.gisgraphy.com/openstreetmap/pbf/$OSM_ZIP
  printf "\nExtracting PBF from archive...\n\n"
  tar -jxvf $DIR/$OSM_ZIP
  mv AU $AU_PBF
  cd -
fi

# Generate the detailed map for the Shire and nearby areas
#--bounding-box top=-34.6908 left=116.5814 bottom=-35.1446 right=117.5826 \ # shire of denmark area
#--bounding-box top=-34.2186 left=116.0733 bottom=-35.1761 right=117.9767 \ # slighly bigger than SoDenmark to include Albany-Manjimup
if [ ! -f $DIR/.allroads.pbf ] ; then
  printf "\nExtracting detailed map for Shire of Denmark WA and nearby areas...\n\n"
  $OSMOSIS_EXE --rb file=$DIR/$AU_PBF \
    --bounding-box top=-34.2186 left=116.0733 bottom=-35.1761 right=117.9767 \
    completeWays=true --used-node --tf accept-ways \
    highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary,secondary_link,tertiary,tertiary_link,residential,unclassified  \
    --wb $DIR/.allroads.pbf
fi

# Generate a larger map of all the big roads to major cities
if [ ! -f $DIR/.bigroads.pbf ] ; then
  printf "\nExtracting larger map of all the big roads to major cities...\n\n"
  $OSMOSIS_EXE --rb file=$DIR/$AU_PBF \
    --bounding-box top=-30.543 left=114.082 bottom=-35.218 right=122.651 \
    completeWays=true --used-node --tf accept-ways \
    highway=motorway,motorway_link,trunk,trunk_link \
    --wb $DIR/.bigroads.pbf
fi

# Merge the two into a final map
if [ ! -f $DIR/.merged-network.osm ] ; then
printf "\nMerging the two into a final map...\n\n"
$OSMOSIS_EXE --rb file=$DIR/.allroads.pbf --rb file=$DIR/.bigroads.pbf --merge \
  --wx $DIR/.merged-network.osm
  cp $DIR/.merged-network.osm $DIR/${OUTFILE_PREFIX}_network.osm

fi

# Generate the MATSim network from the final map
printf "\nCreating the final MATSim network...\n\n"
cp $DIR/.merged-network.osm $DIR/../../..
cd $DIR/../../..
mvn exec:java -Dexec.mainClass="io.github.agentsoz.util.NetworkGenerator" \
  -Dexec.args="-i .merged-network.osm -o ${OUTFILE_PREFIX}_network.xml -wkt ${EPSG}"
cd -
rm -f $DIR/../../../.merged-network.osm
mv $DIR/../../../${OUTFILE_PREFIX}_network.xml $DIR
gzip -f -9 $DIR/${OUTFILE_PREFIX}_network.xml
printf "\nAll done. New network is in $DIR/${OUTFILE_PREFIX}_network.{xml.gz,osm}\n\n"
