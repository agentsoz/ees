#!/usr/bin/env bash

DIR=$(dirname "$0")
FIRESHP=../../../../ees-data/yarra-cardinia-bawbaw-shires/phoenix-shapefiles/20190926/Mount_little_joe_recreation_FDI25_generic_altered_wind_grid.shp
OUTFILE=$DIR/Mount_little_joe_recreation_FDI25_generic_altered_wind_grid.geojson
CMD="ogr2ogr -f GeoJSON -t_srs \"EPSG:32755\" $OUTFILE $FIRESHP"
echo $CMD; eval $CMD
