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

Scenario | Description | How to run from `../../../` |
-|-|-|
S1 | Roads near Lorne block when over-run by fire; no messaging or traffic management in place | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario1IT` |
S2 | Send EVACUATE_NOW at 1215 hrs to all zones; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario2IT` |
S3 | Send EVACUATE_NOW at 1215 hrs to all zones; implement [Traffic Management Plan](traffic-mgt-points.csv) at 1315 hrs; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario3IT` |
S4 | Send EVACUATE_NOW at 1215 hrs to all zones; implement Traffic management Points as per VicPol guidance; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario4IT` |
S5 | Send EVACUATE_NOW at 1215 hrs to high risk zones; implement [Traffic Management Plan](traffic-mgt-points.csv) at 1315 hrs; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario5IT` |
S6 | Send EVACUATE_NOW sequence to high risk areas: [Aireys](zones-epsg4326-aireys-high-risk.geojson) and [rural areas](zones-epsg4326-rural-high-risk.geojson) at 1215 hrs, [Anglesea](zones-epsg4326-anglesea-high-risk.geojson) at 1415 hrs; no msg to Lorne (too late to leave); implement [Traffic Management Plan](traffic-mgt-points.csv) at 1315 hrs; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario6IT` |
S7 | Send EVACUATE_NOW sequence to high risk areas: [Lorne](zones-epsg4326-lorne-high-risk.geojson) and [rural areas](zones-epsg4326-rural-high-risk.geojson) at 1215, [Aireys](zones-epsg4326-aireys-high-risk.geojson) at 1315 hrs, [Anglesea](zones-epsg4326-anglesea-high-risk.geojson) at 1415 hrs; implement [Traffic Management Plan](traffic-mgt-points.csv) at 1315 hrs; roads near Lorne block when over-run by fire same as S1 | `mvn test -Dskip.tests=true -Dskip.IT=false -Dtest=SCSMidweekInJanScenario7IT` |


## Miscellaneous

### Getting formatted list of zones for messages file

To get a sorted list of all zones to paste into the messages file, do:
```
python -m json.tool zones-epsg4326.geojson |\
    grep "IDName" |\
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

### Getting formatted list of high risk zones for messages file

```
cat zones-epsg4326.geojson |\
    grep "Red Zone" |\
    awk -F'"' '{print $22}' |\
    sort |\
    awk '{print "        \""$0"\" : null,"}'
```

### Pasting simulation movies side by side

Based on [this answer](https://unix.stackexchange.com/a/437044):

```
ffmpeg -i left.mp4 -i right.mp4 -filter_complex hstack output.mp4
```
