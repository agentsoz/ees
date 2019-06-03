#!/usr/bin/env bash
INCSV=./population-archetypes.csv.gz
OUTXML=./population-archetypes.xml

Rscript --vanilla './create-archetypes-csv.R'

CMD="java -cp ../../../target/ees-2.1.1-SNAPSHOT.jar \
  io.github.agentsoz.ees.util.SynthpopToMatsim \
  --incsv $INCSV \
  --outxml $OUTXML \
  --incrs \"EPSG:4326\" \
  --outcrs \"EPSG:28355\" \
  "
echo $CMD; eval $CMD

CMD="gzip -f -9 $OUTXML"
echo $CMD; eval $CMD
