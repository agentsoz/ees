## Commit `23aba19` | 12 Jul, 2019

* Changed messaging to
`ADVICE@1110`, `WATCH_AND_ACT@1200`, `EMERGENCY_WARNING@1230`, `EVACUATE_NOW@1300`
* Added a time lag of `15 mins +- 20%` when acting on initial response (picking up dependents etc.)
* Added a time lag in minutes when leaving finally as:
  * Threat Denier: 10 mins
  * Responsibility Denier: 10 mins
  * Community Guided: 15 mins
  * Worried Waverer: 20 mins
  * Dependent Evacuator: N/A
  * Considered Evacuator: 10 mins
  * Experienced Independent: 5 mins
* Adjusted message values such that reaction to messages is not too strong when compared to reaction to fire/smoke.

The [latest video is here](https://cloudstor.aarnet.edu.au/plus/s/UYB8i5Lrn4RS2c5). Some notes about the colours in the video:

Cars | Meaning |
--- | --- |
Blue | Worried Waverers |
Yellow | Considered Evacuators |
Green | Community Guided |
Pink | Responsibility Deniers |
Orange | Threat Deniers |
Red | Experienced Independents|

Dots | Meaning |
--- | --- |
Blue | People at home |
Pink | People deliberating/preparing (lag time before driving)
Purple | People at Invac/Evac place |
White | People at Dependents place |


## Commit `0cf4242` | 10 Jul, 2019

Debugged some behaviours related to dependents.

The [latest video is here](https://cloudstor.aarnet.edu.au/plus/s/7HbvH2OjqYKKPZw). Some notes about the colours in the video:

Cars | Meaning |
--- | --- |
Blue | Worried Waverers |
Yellow | Considered Evacuators |
Green | Community Guided |
Pink | Responsibility Deniers |
Orange | Threat Deniers |
Red | Experienced Independents|

Dots | Meaning |
--- | --- |
Blue | People at home |
Purple | People at Invac/Evac place |
White | People at Dependents place |




## Commit `4ba94ba` | 2 Jul, 2019


`CastlemaineRegionArchetypesIT` is now showing sensible outputs with all the
archetypes implemented to a reasonable degree.
* All behaviour attributes for all archetypes are included in the population.
* Dependent Evacuators are assigned randomly to others in the population (but not to Experienced Independents).
* Unknown Type persons are removed.
* Situation awareness involves sensing embers and fire, as well as all emergency messages (including Emergency Warnings).
* Archetypes correctly use their threshold values as well as barometer change values when assessing the situation.
* People with dependents go to attend to them in the initial phase as per the agreed behaviour (not fully debugged yet so might have some issues still).
* People evacuate to preferred location, and if stuck fall back to Invac location, or home, in that order. Each location is tries three times before giving up and falling back to the next option.

An initial check of randomly selected individuals in the simulation showed many interested traces, such as:
* An Experienced Independent chose to defend but evacuated at the last minute when the fire came very close, while some others stayed and defended.
* A Community Guided person left for the Elphington (evac location), got stuck in congestion, did a u-turn and decided to head towards Castlemaine (invac location), got stuck in congestion on that route too, and eventually turned back and headed home (successfully due to lighter traffic).

A [video of the run is here](https://cloudstor.aarnet.edu.au/plus/s/3aav6UK89gbRBfz). Some notes about the colours in the video:

Symbol | Meaning |
---    | --- |
Blue cars | Worried Waverers |
Yellow cars | Considered Evacuators |
Green cars | Community Guided |
Pink cars | Responsibility Deniers |
Orange cars | Threat Deniers |
Red cars | Experienced Independents|
White fixed dots | People at home |




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
