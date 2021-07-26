# Lithgow NSW Scenario

## Inputs

Details on how the scenario inputs were created are [here](https://github.com/agentsoz/ees-data/tree/master/nsw/blue-mountains-lithgow#readme).

![Data](https://raw.githubusercontent.com/agentsoz/ees-data/master/nsw/blue-mountains-lithgow/data.png)

**TIP**: To get a sorted list of zones to paste into the messages file, do:
```
python -m json.tool lithgow_surrounds_zones_epsg4326.geojson |\
    grep "SA1_CODE21" |\
    awk -F'"' '{print $4}' |\
    sort |\
    awk '{print "        \""$0"\" : null,"}'

```
