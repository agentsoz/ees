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

2. The following command will generate a population of 700 agents placed
randomly within the rectangular area specified by the two diagonal coordinates:
```
java -cp bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.GenerateInput \
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

