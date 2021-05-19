# Surf Coast Shire typical midweek evacuation scenario

The scenario in this directory is built around population expectations for Surf Coast Shire (SCS) for a *typical midweek in January*. The fire spread modelled is similar to, though not strictly like, the Ash Wednesday fire of 1983. The raw inputs for the scenario are shown below and can be found [here](https://github.com/agentsoz/ees-data/tree/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/from-scsc-20210506).

![Scenario Inputs](https://raw.githubusercontent.com/agentsoz/ees-data/a18c799c1d95816d15d7070e4f1d8f4d16ea5747/surf-coast-shire/from-scsc-20210506/scs-scenario-inputs.png)

## Preparation of scenario files

Type | Data used | Scenario file created | Process for creating
-|-|-|-
Fire | Phoenix fire run [`DSSrun2_grid.shp`](https://github.com/agentsoz/ees-data/tree/a18c799c1d95816d15d7070e4f1d8f4d16ea5747/surf-coast-shire/from-scsc-20210506) | [`fire-epsg32754.geojson.gz`](fire-epsg32754.geojson.gz) | Ran the command `ogr2ogr -f "GeoJson" -t_srs EPSG:32754 fire-epsg32754.geojson DSSrun2_grid.shp && gzip -9 fire-epsg32754.geojson`
Population | Synthetic population scenario [`typical-midweek-day-in-jan`](https://github.com/agentsoz/ees-synthetic-population/tree/a18c799c1d95816d15d7070e4f1d8f4d16ea5747/plan-algorithm/scenarios/typical-midweek-day-in-jan) based on [this initial rationale](https://github.com/agentsoz/ees-synthetic-population/blob/3ee29e8c6ab6986f3758e210479a57567bd2e227/plan-algorithm/scenarios/typical-midweek-day-in-jan/rationale.pdf) | [`typical-midweek-day-in-jan-plans-epsg32754.xml.gz`](typical-midweek-day-in-jan-plans-epsg32754.xml.gz) (and [this revision](https://github.com/agentsoz/ees-synthetic-population/blob/a18c799c1d95816d15d7070e4f1d8f4d16ea5747/plan-algorithm/scenarios/typical-midweek-day-in-jan/rationale2.pdf))| Ran the script [`run.sh`](https://github.com/agentsoz/ees-synthetic-population/blob/3ee29e8c6ab6986f3758e210479a57567bd2e227/plan-algorithm/run.sh) to construct the population `.xml`, compressed it using `gzip -9`, then renamed; 10% samples of the population were created using the MATSim Gui tool, and the person IDs of the resulting samples were fixed to be sequential using `awk '/person id=/{s=sprintf("\"%d\"", i++); gsub(/\".*\"/, s)} 1' plansin.xml > plansout.xml`
Road | OpenStreetMap data Apr/2021| [`surf-coast-shire-network-2021-epsg32754.xml.gz`](../surf-coast-shire-network-2021-epsg32754.xml.gz) | Ran the script [`create-surf-coast-shire-network.sh`](https://github.com/agentsoz/ees-data/blob/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/osm/create-surf-coast-shire-network.sh) to construct the road network `.xml`, compressed it using `gzip -9`, then renamed, then manually corrected by SCSC using the JOSM MATSim plugin
Evacuation Zones | [`Evac Zones Risk.mif`](https://github.com/agentsoz/ees-data/tree/9100290a251b3816a2ecf4942174928f3d7c80df/surf-coast-shire/from-scsc-20210506) | [`zones-epsg4326.geojson`](zones-epsg4326.geojson) | Ran the command `ogr2ogr -f "GeoJson" -t_srs EPSG:4326 zones-epsg4326.geojson Evac\ Zones\ Risk.mif`
Traffic Management Points | [`Traffic Mgt Points.shp`](https://github.com/agentsoz/ees-data/tree/9100290a251b3816a2ecf4942174928f3d7c80df/surf-coast-shire/from-scsc-20210506) | [`traffic-mgt-points.csv`](traffic-mgt-points.csv), [`traffic-mgt-points.json`](traffic-mgt-points.json) | Manually mapped traffic management points to MATSim network link IDs

## Scenario 1

This is the baseline scenario with no intervention by the emergency services. In this case, the response of the population to the advancing threat is based on individuals' situation awareness over time (proximity to the progressing fire and embers front), their attitudes, and circumstance (such as their location at the time and whether they are responsible for dependants who may be located elsewhere).

To run the full scenario (takes `~50 mins`), do the following. Alternatively, to run a 10% population sample scenario (takes `~7 mins`) substitute the test name below with `-Dtest=SCSMidweekInJanScenario1Pct10IT`.
```
mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario1IT
```
