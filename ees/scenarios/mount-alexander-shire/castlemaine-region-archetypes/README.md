## Commit `495a7f8` | 9 Sep, 2019

The [latest video is here](https://cloudstor.aarnet.edu.au/plus/s/phuCz9Q3suj9slb). Same colour scheme as below applies.

Note that the EES model (actually the underlying BDI-ABM action/percept model) does not support sending two messages (percepts) of the same type at the same time to an agent. Therefore the timing of the different messages should be unique. The [final messaging regime](./scenario_messages.json ) is as follows:

Message | Time sent | Zones sent to |
--- | --- | --- |
`ADVICE`  |  `1110hrs` | [western SA1s](MountAlexander_SA1s/west.geojson)
`WATCH_AND_ACT` | `1200hrs` | [western SA1s](MountAlexander_SA1s/west.geojson)
`EMERGENCY_WARNING` | `1230hrs` | [western SA1s](MountAlexander_SA1s/west.geojson)
`EVACUATE_NOW` | `1300hrs` | [western SA1s](MountAlexander_SA1s/west.geojson)
`ADVICE` | `1330hrs` | [inner SA1s](MountAlexander_SA1s/inner.geojson)
`EMERGENCY_WARNING` | `1500hrs` | [inner west SA1s](MountAlexander_SA1s/inner-west.geojson)
`WATCH_AND_ACT` | `1600hrs` | [inner SA1s](MountAlexander_SA1s/inner.geojson)

Desired response rates for the different message types are:
* For `ADVICE` messages, `1%` of those who received should respond.
* For `WATCH_AND_ACT` messages, `5%` of those who received should respond.
* For `EMERGENCY_WARNING` messages, `30%` of those who received should respond.
* For `EVACUATE_NOW` messages, `40%` of those who received should respond.

Model parameters are now calibrated to achieve close to the above response rates. This is done easily by using something like `qnorm(0.01,mean=0.3,sd=0.1)` in R which gives the parameter value at which 1% of values will be less than the values of the normal distribution with mean=0.3 and sd=0.1. Based on this the following parameter values were changed (in bold) against the initial threshold distribution:

Parameter | TD	| RD	| CG	| WW	| DE	| CE	| EI
--- | --- | --- | --- | --- | --- | --- | ---
ResponseThresholdInitial.Numeric | 0.6 | 0.4 | 0.3 | 0.3 | 0.3 | 0.3 | 0.4
ImpactFromMessageAdvice.Literal | 0 | 0 | **0.091** | **0.088** | 0 | **0.103** | 0
ImpactFromMessageWatchAndAct.Literal | 0 | 0 | **0.167** | **0.163** | 0 | **0.184** | 0
ImpactFromMessageEmergencyWarning.Literal | 0 | 0 | **0.326** | **0.313** | 0 | **0.337** | 0
ImpactFromMessageEvacuateNow.Literal | 0 | **0.327** | **0.346** | **0.311** | 0 | **0.358** | **0.325**

After changing the parameters as above, the response rates were verified individually by removing the fire/embers and all other messages, and sending a single message to the entire region, so that that full extent of the response could be attributed to that particular message. The individually tested response rates thus achieved were:

Message | Initial response rate | Calibrated response rate |
--- | --- | --- |
*Commit* | `d6b9a2e` | `5ab51eb`
`ADVICE` | `1767/10814 = 16.3%` | `110/10814 = 1.0%`
`WATCH_AND_ACT` | `1767/10814 = 16.3%` | `555/10814 = 5.1%`
`EMERGENCY_WARNING` | `3443/10814 = 31.8%` | `3408/10814 = 31.5%`
`EVACUATE_NOW` | `4867/10814 = 45%` | `4556/10814 = 42.1%`

Here are some convenience commands to verify the above.

To check how many people received a message, say `ADVICE @ 1110hrs`, do:
```
zless jill.out | grep "ADVICE" | grep "|11:10" | cut -f3 -d'|' | sort | wc -l
2739
```
To get the number of people who responded to that message do (note the matching time stamp):
```
zless jill.out | grep "responseThresholdInitialReached=true" | grep "|11:10" | cut -f3 -d'|' | sort | wc -l
420
```
To understand the makeup of people who responded to that message do:
```
zless jill.out  | grep "responseThresholdInitialReached=true" | grep "|11:10" | cut -f3 -d'|' | sort | uniq -c | sort -rn
260 ConsideredEvacuator
84 CommunityGuided
76 WorriedWaverer
```

## Commit `7106aca` | 6 Aug, 2019

Fixes `EMERGENCY_WARNING` not propagating. The [latest video is here](https://cloudstor.aarnet.edu.au/plus/s/HyT1yZLybuX7xEP). Same colour scheme as below applies.


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
