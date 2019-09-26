#!/usr/bin/env bash

DIR=$(dirname "$0")
IN=../../../../ees-data/yarra-cardinia-bawbaw-shires/sa1s-shapefiles/sa1s-near-powelltown.shp
OUT=$DIR/SA1s-near-Powelltown.geojson
CMD="ogr2ogr -f GeoJSON -t_srs \"EPSG:4326\" $OUT $IN"
echo $CMD; eval $CMD
