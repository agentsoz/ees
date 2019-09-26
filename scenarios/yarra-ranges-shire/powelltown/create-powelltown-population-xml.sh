#!/usr/bin/env bash
DIR=$(dirname "$0")
INCSV=$DIR/population-archetypes.csv
OUTXML=$DIR/population-archetypes.xml


Rscript --vanilla "$DIR/create-powelltown-population-csv.R"

cp $DIR/$INCSV $DIR/_csv
gzip -f $DIR/_csv

CMD="unzip -o $DIR/../../../target/ees-2.1.1-SNAPSHOT-release.zip -d ."
echo $CMD; eval $CMD

CMD="java -cp $DIR/ees-2.1.1-SNAPSHOT/libs/*:$DIR/ees-2.1.1-SNAPSHOT/ees-2.1.1-SNAPSHOT.jar \
  io.github.agentsoz.ees.util.SynthpopToMatsim \
  --incsv $DIR/_csv.gz \
  --outxml $DIR/$OUTXML \
  --incrs \"EPSG:4326\" \
  --outcrs \"EPSG:32755\" \
  "
echo $CMD; eval $CMD

# clean up
rm $DIR/_csv.gz
rm -rf $DIR/ees-2.1.1-SNAPSHOT
CMD="gzip -f -9 $OUTXML"
echo $CMD; eval $CMD
