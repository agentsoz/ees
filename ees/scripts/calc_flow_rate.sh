#!/bin/bash
###
# Example command to run the script: 
# > ./calc_flow_rate.sh -start_time 48600 -link_id 19983
###

## Parsing input parameters
file_location="output/ITERS/it.0"
hour_start_time=
link_id=

while [ "$1" != "" ]; do
    case $1 in
        -start_time )		shift
				hour_start_time=$1
                        	;;
        -link_id )      	shift
                        	link_id=$1
                        	;;
	-events_directory )     shift
                        	file_location=$1
                        	;;
    esac
    shift
done

if [ "$hour_start_time" == "" -o "$link_id" == "" ]; then
    	echo "Please set below script variables:"
    	echo "	-start_time : start time of the hour to calculate flow/hr in seconds"
	echo "	-link_id : the ID of the link where the flow/hr should be calculated"
   	exit
fi

## Extracting events file
xml_file="$file_location/0.events.xml"
gz_file="$file_location/0.events.xml.gz"

if [ -f $xml_file ]
then
	echo "The file $xml_file is found."
else
	echo "The file $xml_file is not found."
	if [ -f $gz_file ]
	then
		echo "Extracting the file $gz_file"
		gunzip $gz_file
	else
		echo "No events file found in the location $file_location"
		exit
	fi
fi

## Collecting data
hour_end_time=$(( hour_start_time + 3600 ))
data_file="$file_location/all_info.txt"
echo "Start time: $hour_start_time, End time: $hour_end_time"

# store alll "left link" events made to the link to a separate file"
cat $xml_file | grep "left link" | grep "link=\"$link_id" > $data_file

flow="0";
re="event time=\"([0-9]+)\."

while IFS='' read -r line || [[ -n "$line" ]]; do
	if [[ $line =~ $re ]]; then
		if [ "${BASH_REMATCH[1]}" -ge "$hour_start_time" -a "${BASH_REMATCH[1]}" -le "$hour_end_time" ]; then
			flow=$(( flow + 1))
		fi
	fi
done < "$data_file"

echo " $flow vehicles left the link $link_id during the hour from $hour_start_time to $hour_end_time"
