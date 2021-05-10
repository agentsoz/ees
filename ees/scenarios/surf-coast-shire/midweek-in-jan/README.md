# Surf Coast Shire typical midweek evacuation scenario

The scenario in this directory is built around population expectations for Surf Coast Shire (SCS) for a *typical midweek in January*. The fire spread modelled is similar to, though not strictly like, the Ash Wednesday fire of 1983. The raw inputs for the scenario are shown below and can be found [here](https://github.com/agentsoz/ees-data/tree/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/from-scsc-20210506).

![d](https://raw.githubusercontent.com/agentsoz/ees-data/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/from-scsc-20210506/scs-scenario-inputs-20210506.png)

## Preparation of scenario files

Type | Data used | Scenario file created | Process for creating
-|-|-|-
Fire | Phoenix fire run [`DSSrun2_grid.shp`](https://github.com/agentsoz/ees-data/tree/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/from-scsc-20210506) | [`fire-epsg32754.geojson.gz`](fire-epsg32754.geojson.gz) | Ran the command `ogr2ogr -f "GeoJson" -t_srs EPSG:32754 fire-epsg32754.geojson DSSrun2_grid.shp && gzip -9 fire-epsg32754.geojson`
Population | Synthetic population scenario [`typical-midweek-day-in-jan`](https://github.com/agentsoz/ees-synthetic-population/tree/8b3fd5d6d6c09706d8b1c0ed34a2218cfdb9ab07/plan-algorithm/scenarios/typical-midweek-day-in-jan) based on [this rationale](https://github.com/agentsoz/ees-synthetic-population/blob/8b3fd5d6d6c09706d8b1c0ed34a2218cfdb9ab07/plan-algorithm/scenarios/typical-midweek-day-in-jan/rationale.pdf) | [`typical-midweek-day-in-jan-plans-epsg32754.xml.gz`](typical-midweek-day-in-jan-plans-epsg32754.xml.gz) | Ran the script [`run.sh`](https://github.com/agentsoz/ees-synthetic-population/blob/8b3fd5d6d6c09706d8b1c0ed34a2218cfdb9ab07/plan-algorithm/run.sh) to construct the population `.xml`, compressed it using `gzip -9`, then renamed
Road | OpenStreetMap data Apr/2021| [`surf-coast-shire-network-2021-epsg32754.xml.gz`](../surf-coast-shire-network-2021-epsg32754.xml.gz) | Ran the script [`create-surf-coast-shire-network.sh`](https://github.com/agentsoz/ees-data/blob/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/osm/create-surf-coast-shire-network.sh) to construct the road network `.xml`, compressed it using `gzip -9`, then renamed
Evacuation Zones | [`Evac Zones Risk.mif`](https://github.com/agentsoz/ees-data/tree/a183013b937102e676fd1f9ff6bfd177d61179e4/surf-coast-shire/from-scsc-20210506) |
