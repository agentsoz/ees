### Script to generate messages syntax from GeoJSON files

Do something like
```
python -m json.tool SA1s-near-Powelltown.geojson | \
  grep "SA1_MAIN16" | \
  awk -F'"' '{print $4}' | \
  sort -n | \
  awk '{print "        \""$1"\" : null,"}'
```

to get something like
```
"20501107703" : null,
"20501107718" : null,
"21105128607" : null,
"21105128608" : null,
"21105128610" : null,
"21105128620" : null,
"21105128629" : null,
"21105128630" : null,
"21105128631" : null,
"21105128634" : null,
"21105128635" : null,
"21105128636" : null,
"21105128637" : null,
"21105128638" : null,
"21105128641" : null,
"21105128642" : null,
"21105128645" : null,
"21105128648" : null,
"21105128652" : null,
"21105128653" : null,
"21105128654" : null,
"21105128657" : null,
"21201128901" : null,
```

that can be pasted directly into the [scenario messages JSON file](./messages.json).
