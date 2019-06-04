## Commit `e2ebe0b` | 4 Jun, 2019

* `CastlemaineRegionBaselineIT`: is now the renamed baseline scenario (from below).
* `CastlemaineRegionArchetypesIT`: this is the archetypes scenario.

A [basic comparison video of the two is here](https://cloudstor.aarnet.edu.au/plus/s/MXqKylC2JPnJXo5).

Some notes re the archetypes population:
* The population attributes in `population-archetypes.xml.gz` are initialised as per the agreed values.
* The behaviours are the same as the `Resident` class (below).
* So the main difference in the two populations is predominantly in the initialisation. 


## Commit `8a33ec6` | May 23, 2019

A very basic scenario with `Resident` agents (similar to those used for Surf Coast Shire) evacuating the Castlemaine/Region is ready (see [video](https://cloudstor.aarnet.edu.au/plus/s/icWong3jdLUIPo6)).

* Population consists of Castlemaine Region, and also Castlemaine, else
  we are left with a *hole*, which looks odd.
* Agents are all of type `Resident`.
* Population has preferred evacuation location set to Elphinstone.
* Both initial/final thresholds are randomly picked from range `[0.0,0.5]``.
* Messaging sequence is:
    * `1110 hrs, ADVICE` to all SA1s west of Castlemaine;
    * `1200 hrs, WATCH_AND_ACT` to all SA1s west of Castlemaine;
    * `1230 hrs, EVACUATE_NOW (to Harcourt)` to 4 SA1s of Maldon;
    * `1300 hrs, EVACUATE_NOW (to Harcourt)` to 2 large SA1s N/S of Maldon.   

## Phoenix fire GeoJSON

File `20181109_mountalex_evac_ffdi100d_grid.json` was generated using:
```
ogr2ogr -f "GeoJson" -t_srs EPSG:28355 \
  20181109_mountalex_evac_ffdi100d_grid.json \
  ../../../../ees-data/mount-alexander-shire/phoenix-shapefiles/20181109/Evac_Phoenix_runs/20181109_mountalex_evac_ffdi100d/20181109_mountalex_evac_ffdi100d_grid.shp

```
