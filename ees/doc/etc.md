# How to generate input for a given coordinate system.

NOTE: WSG84 is not recommended by MATSim. Make sure you use UTM always as
      it allows for precise calculations for distances. 

A very useful Google Map with UTM grid overlays is [here](https://mappingsupport.com/p/gmap4.php?ll=27.717297,7.832677&t=t1&z=2&coord=utm).


Say we want to use EPSG:28355 (VIC UTM 55) for everything. On the MATSim side, 
this means that the network and population files should both express coordinates 
in that system. 

1. You can tell MATSim to use EPSG:28355 using a global
   setting in the main config XML as follows:
   ```
   <module name="global">
      ...
      <param name="coordinateSystem" value="EPSG:28355" />
      ...
   </module>
   ```
   Then make sure that the network.xml and population.xml provided is
   using that same coordinate system. 

2. The follosing will generate the MATSim network file in the correct
   coordinate system from a given OpenStreetMap (.osm) file:
   ```
   java -cp bushfire-2.0.2-SNAPSHOT.jar io.github.agentsoz.util.NetworkGenerator \
      -i ./input.osm -o ./output.xml -wkt "EPSG:28355"
   ```
3. The following command will generate a population of 700 agents placed
   randomly within the rectangular area specified by the two diagonal coordinates:
   ```
   java -cp bushfire-2.0.2-SNAPSHOT.jar io.github.agentsoz.util.evac.GenerateInput \
      -outdir scenarios/maldon/ \
      -prefix maldon \
      -matsimpop "700/EPSG:28355/RECT/234274,5895647&246377,5919215"
   ```

# How to extract list of addresses from VicMap shapefile
```
shapefile2json.py -infile ADDRESS.shp | \
   egrep -o 'coordinates": \[ (.*), (.*) \]' | \
   cut -f3,4 -d' ' | sort | uniq > addresses.txt
```    

# How to convert Phoenix shapefiles to Google Earth format

The quickest way to generate the KML from SHP is this:
```
ogr2ogr -f KML \
   20160420_MtAlexShire_FDI75_Iso.kml \
   20160420_MtAlexShire_FDI75_Iso.shp
```

Some effort will be required to make it time variant. All the info is in the KML
but need to post process it.

# How to convert Pheonix shapefiles for use by the Fire Module

```
ogr2ogr -f GeoJSON \
  20160420_MtAlexShire_FDI75_Iso.json \ 
  20160420_MtAlexShire_FDI75_Iso.shp
```  

The above command will generate a output in GeoJSON format. The FireModule
class should be modified to accept input in that format, as follows:

```
<firefile name="fire.json" format="GeoJSON" coordinates="utm"/>
```

To convert the same file for loading in Google Maps which uses a
different coordinate system, do:

```
ogr2ogr -f GeoJSON \
  -t_srs "EPSG:4326" 
  -s_srs "EPSG:3111" \
  20160420_MtAlexShire_FDI75_Iso.json \ 
  20160420_MtAlexShire_FDI75_Iso.shp
```


# How to build scenario config files from bushfire UI produced JSON

The following command will create a new scenario called *test*, with files 
placed in a new directory `../somedir/test` (must not pre-exist), using the
input JSON file `./scenarios/template/sample_data/maldon_scenario.json` and the
scenario templates files in directory `./scenarios/template/`.

```
./scripts/build_scenario.py \
  -c ./scenarios/template/sample_data/maldon_scenario.json \
  -t ./scenarios/template/ \
  -o ../somedir \
  -n test \
  -v
```

# Notes on how to get vehicles count over time for safelines

1. Convert the safeline coords to UTM. 

2. From output_network.xml.gz, find the `NODES` with coords closest to 
   safeline; need a sensible way to pick multiple nodes when the safeline crosses
   multiple roads; output of this step is a list of nodes with coords and IDs.
   Here's the formula to find the distance of a point from a line (given by 
   two points): 
   https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line#Line_defined_by_two_points
   
   To get started, the following command will put the node ids and coords
   in a CSV that could be loaded in R to do the calculation:

   ```   
   zcat output_network.xml.gz | \
     grep "node id" | \
     cut -f2,4,6 -d '"' --output-delimiter="," \
     > nodes.csv
   ```
   
   Then for each node in the list, find the LINK that has that node as the 
   starting node, i.e., where `from=` contains that node id; the outout of this
   step is a list of link IDs; these are the links that we need to monitor for 
   vehicles counts.
   
3. For each link in our list, get a time-ordered list of vehicles going it, as: 
   ```
   zcat 0.events.xml.gz | \
     grep "entered link" | \
     grep 'link="477"' | \
     cut -f2 -d'"' | \
     cat - -n  | \
     awk '{print $2","$1}' \
     > link-NNN-traffic-count.csv
   ```
   where NNN is the link id; the first column in the file is the time of day 
   in secs, and the second column is the cumulative vehicle count at that time; 
   this can then be fed into R for plotting. 

4. Write an R script to take the CSV file and prodice a traffic count graph.
   
5. Repeat the above steps for each safe line. 

# Notes on useful files for results and analysis

* file `./scenario/scenario_matsim_output/ITERS/it.0/0.tripdurations.txt`
  has the following useful info that could be added to report:  
  average trip duration: 2695.73719376392 seconds = 00:44:55

* file `./scenario/scenario_matsim_output/ITERS/it.0/0.legHistogram.txt`
  has all the info to verify departure peak
  
* file `./scenario/scenario_matsim_output/ITERS/it.0/0.experienced_plans.xml.gz`
  has the saved route (list of links) taken by each vehicle; no timing though
  
* to get timing as well for the links, you can do:
  `zcat 0.events.xml.gz | grep "entered link" | grep 'person="99"'`
  which will give you the times at which person 99 entered each link on its route

