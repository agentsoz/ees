# Lithgow NSW Scenario

## Inputs

Details on how the scenario inputs were created are [here](https://github.com/agentsoz/ees-data/tree/f216aed4c878c253387bddc847cc5cb77b452df1/nsw/blue-mountains-lithgow#readme).

![Data](https://raw.githubusercontent.com/agentsoz/ees-data/f216aed4c878c253387bddc847cc5cb77b452df1/nsw/blue-mountains-lithgow/data.png)

**TIP**: To get a sorted list of zones to paste into the messages file, do:
```
python -m json.tool lithgow_surrounds_zones_epsg4326.geojson |\
    grep "SA1_CODE21" |\
    awk -F'"' '{print $4}' |\
    sort |\
    awk '{print "        \""$0"\" : null,"}'

```
