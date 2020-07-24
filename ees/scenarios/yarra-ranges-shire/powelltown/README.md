# Powelltown Evacuation Simulation

The latest video (commit `9361c27`, 27-Sep-2019) is [here](https://cloudstor.aarnet.edu.au/plus/s/LXRx49X5MPgyrsf). Legend of colours in the video is below:

Symbol | Colour  | Description |
--- | --- | --- |
Triangle (moving) | Blue | Worried Waverer driver |
Triangle (moving) | Yellow | Considered Evacuator driver |
Triangle (moving) | Green | Community Guided driver|
Triangle (moving) | Pink | Responsibility Denier driver |
Triangle (moving) | Orange | Threat Denier driver |
Triangle (moving) | Red | Experienced Independent driver|
Circle (fixed) | Blue | Person at home |
Circle (fixed) | Pink | Person deliberating
Circle (fixed) | Purple | Person at Invac/Evac place |
Circle (fixed) | White | People at Dependants place |

## Fire

* ([Phoenix run](https://github.com/agentsoz/ees-data/tree/0457e6bf26ded3014aeafe0ae251d0b19d84dd09/yarra-cardinia-bawbaw-shires/phoenix-shapefiles/20190926)): Ignition point [Mount Lille Joe](https://www.google.com/maps/place/37°45'34.5%22S+145°40'17.0%22E/@-37.7584275,145.6105416,12.14z/data=!4m6!3m5!1s0x0:0x0!7e2!8m2!3d-37.7595948!4d145.6713947) `@1100 hrs`, generic `FDI25` conditions, moving south easterly towards Powelltown, changes direction to easterly around `@1800 hrs`.

## Emergency Messages

The following sequence of emergency messages are sent to the full region:

Simulation Time | Message Type | Message Content |
--- | --- | --- |
`1130 hrs` |  `ADVICE` | Bushfire near Mount Little Joe; stay up to date in case the situation changes. |
`1200 hrs` |  `WATCH_AND_ACT` | Bushfire near Mount Little Joe; not under control yet. |
`1230 hrs` | `EMERGENCY_WARNING` | Bushfire near Mount Little Joe is threatening homes and lives. You are in danger and need to act immediately to survive. |
`1300 hrs` | `EVACUATE_NOW` | Bushfire near Mount Little Joe is threatening homes and lives. It is strongly recommended that you evacuate now to safety. |

## Population

* The resident population of only the following towns is considered [Powelltown (`217` persons, `96` dwellings, `1.9` vehicles per dwelling)](https://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/SSC22103), [Gladysdale (`444` persons, `170` dwellings, `2.6` vehicles per dwelling)](https://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/SSC20998?opendocument), and [Three Bridges (`199` persons, `80` dwellings, `2.5` vehicles per dwelling)](https://quickstats.censusdata.abs.gov.au/census_services/getproduct/census/2016/quickstat/SSC22501?opendocument).

* The number of dwellings and average vehicles per dwelling is used to first calculation the number of vehicles in the area. Of these, it is assumed that `80%` are likely to be mobilised, giving a total of `660` vehicles (persons) as a starting figure.

* To these persons we first assign home addresses by randomly sampling (with replacement) from the [list of known street addresses for Powelltown, Gladysdale, and Three Bridges obtained from Vicmap](https://github.com/agentsoz/ees-data/tree/89784e51416fdebe93abff23ec332f5492a4035a/yarra-cardinia-bawbaw-shires/vicmap-addresses).

* Each person is then assigned an archetype using the distribution of archetypes obtained from the Strahan et al.'s original data for Perth and Adelaide Hills (`CE=11.9%, CG=13%, TD=9.8%, WW=9.3%, RD=9.4%, DE=6.5%, EI=17.1%`). This is a quick approximation in lieu of performing a demographic match (since we have not built a demographic-based population above). As well, each person is assigned behaviour attitudes as per the [archetypes attributes table](https://bitbucket.org/dhixsingh/archetypes-modelling/src/4706b1c72e96b4b3008509cb7f312e79d3e20592/data/archetypes-attributes.csv?at=master).

* Next dependant evacuators (`DE`), who are unable to drive, and depend on others to evacuate them, are reassigned as follows. First, the locations of `DE`s are assigned in random order to people who should have dependants (as per their attributes) and are not dependants themselves. No Experienced Independents are assigned  dependants, since they do not plan to leave and generally delegate responsibility of evacuating dependants to other household members. If the list of `DE` locations is exhausted, then any remaining persons who should have dependants are assigned a random location of dependants within `2km` of their home address. All `DE` persons are then removed from the population, since they are now all accounted for in the location of dependants of other persons.

* Each person is finally assigned an evacuation destination preference of either [Little Yarra CFA Station](https://www.google.com/maps/place/Little+Yarra+CFA/@-37.8221668,145.6508762,12.43z/data=!4m5!3m4!1s0x6b29d213d80919d5:0x8b03c38a16de481a!8m2!3d-37.8243456!4d145.6546237) (`~80%` of all evacuating persons will choose to go here) or the [Yarra Junction Oval](https://www.google.com/maps/place/Yarra+Junction+Football+%26+Netball+Club+Inc/@-37.7967957,145.6025538,13.03z/data=!4m5!3m4!1s0x6b29d30f616ced53:0x1116a0d8b7e551ae!8m2!3d-37.7810472!4d145.6132278) (the remaining `~20%` evacuating persons will choose to go here). All persons are also assigned a prefeered *invac* destination (for if they cannot reach the evacuation destination due to congestion) being the [Powelltown Oval](https://www.google.com/maps/place/Powelltown+Rd,+Powelltown+VIC+3797/@-37.8816848,145.7395095,12.94z/data=!4m5!3m4!1s0x6b29cc03867f5cad:0xb7b06b59c43da2ba!8m2!3d-37.8653076!4d145.7496017).

* The starting location of each person is their assigned home address. The final number of resident persons (vehicles) in the simulation (after removing `DE` persons as above) is `617`.
