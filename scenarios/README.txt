instructions for creating a new scenario

Ideally, all the data relating to a specific scenario should be in a single folder eg. barwon_heads or halls_gap.

The input files consists of:

A configuration (xml) file.
A MatSim configuration (xml) file.
A road network (xml) file.
A MatSim plan file.

1. Creating the config file

The bushfire sim expects an input file (xml) that contains all the information necessary to run a sim scenario.

All data is enclosed in a <simulation></simulation> section.

It includes the following sections (example), all of which are required.

<logfile name="scenarios/barwon_heads/bushfire.log" />
<matsimfile name="scenarios/barwon_heads/barwon_heads_config_withinday.xml"/>
<firefile name="scenarios/barwon_heads/barwon_heads_fire_grid.txt" />
<bdisimfile name="scenarios/barwon_heads/simulation_parameters.txt"/>
<port number="-1"/>

The logfile defines where the bushfire simulation logfile will be written.
The matsimfile defines ...
The firefile defines ...
The bdisimfile ...

The port number defines which port will be used by the visualiser. If this is -1, then no visualiser will be used.

Additionally, this config file includes a list of locations, such as a school and the set of control points used to define evacuation routes. It also contains a list of regions that divide the town into areas that can be evacuated separately and the routes used by each region to conduct the evacuation.


2. Creating the MatSim config file

A file, such as barwon_heads_config_withinday.xml, is used to configure MatSim.

Amongst other information, this includes definitions for the (road) network and plans files. eg. halls_gap_network.xml and halls_gap_population.xml.



3. Creating the road network file

The road network file is created from an open street map (OSM) file for the area. (how is this sourced?)

To convert the OSM file, a utility (Osm2Net) is used. This can be found in the related repository bdi-abm-integration-support in the "example/BDIMATSimInterface/utils" folder.

Usage:   java -jar Osm2Net.jar "<Osm file>" "<Optional : Output directory>"

Example: java -jar .\utils\Osm2Net.jar ".\example\barwon_heads\inputs\osm2net_input\barwon.osm" "C:\User\Documents\example\barwon_heads\inputs\osm2net_output"





