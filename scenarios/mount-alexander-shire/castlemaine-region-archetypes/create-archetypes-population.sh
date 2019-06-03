#!/usr/bin/env bash
INCSV=./archetypes.csv.gz
printf "Creating $INCSV\n"
Rscript --vanilla './create_archetypes_csv.R'

CMD="java -cp ../../../target/ees-2.1.1-SNAPSHOT.jar \
  io.github.agentsoz.ees.util.SynthpopToMatsim \
  --incsv ./archetypes.csv.gz \
  --outxml ./archetypes.xml
  --incrs \"EPSG:4326\" \
  --outcrs \"EPSG:28355\" \
  "
echo $CMD; eval $CMD
