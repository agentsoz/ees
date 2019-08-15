### Script to generate messages syntax from GeoJSON files

Do something like
```
python -m json.tool MountAlexander_SA1s/inner-west.geojson | \
  grep "SA1_MAIN16" | \
  awk -F'"' '{print $4}' | \
  sort -n | \
  awk '{print "        \""$1"\" : null,"}'
```

to get something like
```
"20202102701" : null,
"20202102707" : null,
"20202102708" : null,
"20202102709" : null,
"20202102717" : null,
"20202102718" : null,
"20202102720" : null,
"20202102721" : null,
"20202102722" : null,
"20202102723" : null,
"20202102724" : null,
"20202102726" : null,
"20202102727" : null,
```

that can be pasted directly into the [scenario messages JSON file](../scenario_messages.json).
