#!/bin/bash
###
# Example command to run the script:
# > ./create-analysis-graphs.sh --matsim-output-dir
###

DIR=`dirname "$0"`

scenario_dir=

while [ "$1" != "" ]; do
	case $1 in
	--scenario-output-dir )     shift
		scenario_dir=$1
		;;
	esac
	shift
done

# Check that the scenario output directory was specified
if [ "$scenario_dir" == "" ]; then
	echo "usage: $0 --scenario-output-dir DIR"
	exit
fi

# Check that the scenaio output file exists
scenario_file=$scenario_dir/`basename $scenario_dir`.json
if [ ! -f $scenario_file ]; then
	echo "EES scenario output file $scenario_file not found."
	exit
fi

# Check that the MATSim experienced plans file exists
experienced_plans_file="$scenario_dir/scenario/scenario_matsim_output/ITERS/it.0/0.experienced_plans.xml.gz"
if [ ! -f $experienced_plans_file ]; then
	echo "MATSim experienced plans file $experienced_plans_file not found."
	exit
fi

# Analysis output directory and files
analysis_dir=$scenario_dir/analysis
departures_file=$analysis_dir/departuresInMinsPastEvacTime
arrivals_file=$analysis_dir/arrivalInMinsPastEvacTime
traveltime_file=$analysis_dir/travelTimeInMins
distance_file=$analysis_dir/distanceInKms

rm -rf $analysis_dir
mkdir -p $analysis_dir


# Extract the evacuation start time from the scenario JSON and store as minutes
evacTimeHH=`cat $scenario_file | python -c 'import json,sys;obj=json.load(sys.stdin);print(obj["evacTime"]["hh"])' | tr -d '\n'`
evacTimeMM=`cat $scenario_file | python -c 'import json,sys;obj=json.load(sys.stdin);print(obj["evacTime"]["mm"])' | tr -d '\n'`
evacTimeMins=`echo "scale=1; $evacTimeHH*60 + $evacTimeMM" | bc | tr -d '\n'`

# Extract the departure time of agents (in minutes) and subtract evacTimeMins to get the departures past start time
zcat $experienced_plans_file | grep -B1 'route type="links"' | grep -o "dep_time=.* trav_time" | cut -f2 -d'"' | awk -F':' '{printf "%.1f\n", ($1*60 + $2 + $3/60)-'$evacTimeMins'}' > $departures_file.csv

# Extract the arrival time of agents (in minutes) and subtract evacTimeMins to get the arrival past start time
# zcat $experienced_plans_file | grep -B1 'route type="links"' | grep -o "arr_time=.*\"" | cut -f2 -d'"' | awk -F':' '{printf "%.1f\n", ($1*60 + $2 + $3/60)-'$evacTimeMins'}' > $arrivals_file.csv

# Extract the travel time of agents (in minutes)
zcat $experienced_plans_file | grep -o 'route type="links".*trav_time=.* distance' | cut -f3 -d' ' | cut -f2 -d'"' | awk -F':' '{printf "%.1f\n", ($1*60 + $2 + $3/60)}' > $traveltime_file.csv

# Extract the distance travelled by agents (in kms)
zcat $experienced_plans_file | grep -o 'route type="links".*distance=.*">' | cut -f6 -d' ' | cut -f2 -d'"' | awk '{printf "%.1f\n", $1/1000}' > $distance_file.csv

# Extract the vehicles times past the safe lines
for file in $(find $scenario_dir/scenario/ -name "safeline*.out" -print); do
	cat $file | awk -F':' 'BEGIN{printf "0.0,0.0\n"}{printf "%.1f,%.1f\n", ($1/60 -'$evacTimeMins'), NR}' > $analysis_dir/`basename $file`.inMinsPastEvacTime.csv
done

for file in $(find $analysis_dir/ -name "safeline*.inMinsPastEvacTime.csv" -print); do
	timeOfLastVehicle=$(tail -1 $file | cut -f1 -d',' | tr -d '\n')
	numVehicles=$(tail -n+2 $file | wc -l | tr -d '\n')
	name=''
	name=$(echo $file | grep -o "safeline.*.inMinsPastEvacTime.csv" | cut -f2 -d'.' |  tr -d '\n')
	# Plot the histograms
	R --vanilla <<code

	library(ggplot2)

	tbl<-read.csv('$file', header=FALSE);
	tbl<-tbl[!duplicated(tbl[,1]), ] # remove duplicates when plotting else area fill gives spikes
	png('$file.png', width=600, height=600, units="px", res=100);
	ggplot(data=tbl, aes(x=V1, y=V2)) +
		geom_area(fill="deepskyblue4") +
		geom_point(color="red",size=2) +
		theme(
			plot.title=element_text(size=14,hjust=0.5),
			axis.title=element_text(size=14),
			axis.text.x = element_text(size=12),
			axis.text.y = element_text(size=12),
		) +
		xlab("Minutes after evacuation start") +
		ylab("Vehicles past safeline") +
		ggtitle("$name safeline\n$numVehicles vehicles crossed in $timeOfLastVehicle mins from start")
	graphics.off();
code
done

# Plot the histograms
R --vanilla <<code

library(ggplot2)

tbl<-read.csv('$departures_file.csv', header=FALSE);
png('$departures_file.png', width=600, height=600, units="px", res=100);
ggplot(data=tbl, aes(tbl)) +
	geom_histogram(bins=20, col="white", aes(fill=..count..)) +
	scale_fill_gradient("Count", low = "deepskyblue", high = "deepskyblue4") +
	theme(
		legend.title=element_text(size=14),
		legend.key.height=unit(1,"line"),
		legend.text=element_text(size=12),
		axis.title=element_text(size=14),
		axis.text.x = element_text(size=12),
		axis.text.y = element_text(size=12),
	) +
	xlab("Minutes after evacuation start") +
	ylab("Number of departures") +
	guides(fill=guide_legend(title="Vehicles"))
graphics.off();

# tbl<-read.csv('$arrivals_file.csv', header=FALSE);
# png('$arrivals_file.png', width=600, height=600, units="px", res=100);
# ggplot(data=tbl, aes(tbl)) +
# 	geom_histogram(bins=20, col="white", aes(fill=..count..)) +
# 	scale_fill_gradient("Count", low = "deepskyblue", high = "deepskyblue4") +
# 	theme(
# 		legend.title=element_text(size=14),
# 		legend.key.height=unit(1,"line"),
# 		legend.text=element_text(size=12),
# 		axis.title=element_text(size=14),
# 		axis.text.x = element_text(size=12),
# 		axis.text.y = element_text(size=12),
# 	) +
# 	xlab("Minutes after evacuation start") +
# 	ylab("Number of arrivals") +
# 	guides(fill=guide_legend(title="Vehicles"))
# graphics.off();

tbl<-read.csv('$traveltime_file.csv', header=FALSE);
png('$traveltime_file.png', width=600, height=600, units="px", res=100);
ggplot(data=tbl, aes(tbl)) +
	geom_histogram(bins=20,col="white", aes(fill=..count..)) +
	scale_fill_gradient("Count", low = "deepskyblue", high = "deepskyblue4") +
	theme(
		legend.title=element_text(size=14),
		legend.key.height=unit(1,"line"),
		legend.text=element_text(size=12),
		axis.title=element_text(size=14),
		axis.text.x = element_text(size=12),
		axis.text.y = element_text(size=12),
	) +
	xlab("Travel time per vehicle (minutes)") +
	ylab("Number of vehicles") +
	guides(fill=guide_legend(title="Vehicles"))
graphics.off();

tbl<-read.csv('$distance_file.csv', header=FALSE);
png('$distance_file.png', width=600, height=600, units="px", res=100);
ggplot(data=tbl, aes(tbl)) +
	geom_histogram(bins=20,col="white", aes(fill=..count..)) +
	scale_fill_gradient("Count", low = "deepskyblue", high = "deepskyblue4") +
	theme(
		legend.title=element_text(size=14),
		legend.key.height=unit(1,"line"),
		legend.text=element_text(size=12),
		axis.title=element_text(size=14),
		axis.text.x = element_text(size=12),
		axis.text.y = element_text(size=12),
	) +
	xlab("Distance travelled per vehicle (kms)") +
	ylab("Number of vehicles") +
	guides(fill=guide_legend(title="Vehicles"))
graphics.off();
code

# Plot
