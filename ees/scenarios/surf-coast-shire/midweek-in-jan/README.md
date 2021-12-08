# Surf Coast Shire typical midweek evacuation scenario

The scenario in this directory is built around population expectations for Surf Coast Shire (SCS) for a *typical midweek in January*. The fire spread modelled is similar to, though not strictly like, the Ash Wednesday fire of 1983.


## Preparation of scenario files

Type | Data used | Scenario file created | Process for creating
-|-|-|-
Fire | Phoenix fire run [`DSSrun2_grid.shp`](https://github.com/agentsoz/ees-data/tree/eb49b83ea462a29d511c0386c639395761c64a8b/surf-coast-shire/evac-exercise-202112) | [`fire-epsg32754.geojson.gz`](fire-epsg32754.geojson.gz) | Ran the command `ogr2ogr -f "GeoJson" -t_srs EPSG:32754 fire-epsg32754.geojson DSSrun2_grid.shp && gzip -9 fire-epsg32754.geojson`
Road | OpenStreetMap data Apr/2021| [`surf-coast-shire-network-2021-epsg32754.xml.gz`](../surf-coast-shire-network-2021-epsg32754.xml.gz) | Ran the script [`create-surf-coast-shire-network.sh`](https://github.com/agentsoz/ees-data/blob/36cd139994b73d2c2d6ef4993caad023f92110a0/surf-coast-shire/osm/create-surf-coast-shire-network.sh) to construct the road network `.xml`; this was iteratively and manually corrected by SCSC using the JOSM MATSim plugin; and compressed using `gzip -9`
Population | Used [`these population data input files`](https://github.com/eesim/demand-seasonal/tree/74ab981347a6fa6ae701c4e1c057f9d39d145a5b/tests/data) co-created with SCSC | [`demand-seasonal-40k-50it-epsg32754-7886c91.xml.gz`](https://cloudstor.aarnet.edu.au/plus/s/poLPMcKNUZQz5mC?path=%2Fees%2Fscenarios%2Fsurf-coast-shire%2Fmidweek-in-jan)  | In the [`demand-seasonal`](https://github.com/eesim/demand-seasonal/tree/74ab981347a6fa6ae701c4e1c057f9d39d145a5b) repository, ran the command `Rscript -e 'setwd("R"); source("main.R"); runDemandGenerationExample(num.agents=40000)'` to construct the population `.xml`; then ran the generated demand through MATSim using [`this config`](https://github.com/eesim/demand-seasonal/blob/74ab981347a6fa6ae701c4e1c057f9d39d145a5b/data/matsim/config.xml) for 50 iterations
Evacuation Zones | [`Evac Zones 24 7-12-21 with Roads.shp`](https://github.com/agentsoz/ees-data/tree/eb49b83ea462a29d511c0386c639395761c64a8b/surf-coast-shire/evac-exercise-202112) | [`zones-epsg4326.geojson`](zones-epsg4326.geojson) | Ran the command `ogr2ogr -f "GeoJson" -t_srs EPSG:4326 zones-epsg4326.geojson Evac\ Zones\ 24 7-12-21\ with\ Roads.shp`
Traffic Management Points | [`Traffic Mgt Points.shp`](https://github.com/agentsoz/ees-data/tree/eb49b83ea462a29d511c0386c639395761c64a8b/surf-coast-shire/evac-exercise-202112) | [`traffic-mgt-points.csv`](traffic-mgt-points.csv), [`traffic-mgt-points.json`](traffic-mgt-points.json) | Manually mapped traffic management points to MATSim network link IDs

*The population file ([`demand-seasonal-40k-50it-epsg32754-7886c91.xml.gz
`](https://cloudstor.aarnet.edu.au/plus/s/poLPMcKNUZQz5mC?path=%2Fees%2Fscenarios%2Fsurf-coast-shire%2Fmidweek-in-jan)) is too large to store in GitHub and should first be downloaded and stored in the current dir.*

## Scenarios

### S1

This is the baseline scenario with no intervention by the emergency services. In this case, the response of the population to the advancing threat is based on individuals' situation awareness over time (proximity to the progressing fire and embers front), their attitudes, and circumstance (such as their location at the time and whether they are responsible for dependants who may be located elsewhere). To run the full scenario (takes `~30 mins`), do the following from the ees module dir (`../../../`).
```
mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario1IT
```

### S2

This scenario is the same as S1 with the addition of an `EVACUATE_NOW` message at `1215 hrs` sent to all zones. To run, do the following from the ees module dir (`../../../`).
```
mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario2IT
```

## Misc

To get a sorted list of zones to paste into the messages file, do:
```
python -m json.tool zones-epsg4326.geojson |\
    grep "ID_Name" |\
    awk -F'"' '{print $4}' |\
    sort |\
    awk '{print "        \""$0"\" : null,"}'

```
This returns something like:
```
        "1- Lorne" : null,
        "1-Aireys Inlet" : null,
        "1-Anglesea" : null,
        "1-Bellarine" : null,
        "1-Bellbrae" : null,
        "1-Bells Beach" : null,
        "1-Big Hill" : null,
        "1-Birregurra" : null,
        "1-Colac" : null,
        ...
```
