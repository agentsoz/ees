#!/usr/bin/env python3
"""
Convert a MATSim network XML (nodes + links) into GeoJSON, for loading
directly into QGIS or any other GIS tool.

Usage:
  python3 network_to_geojson.py \
      --input surf-coast-shire-network-2021-epsg32754.xml.gz \
      --output-links network-links.geojson \
      --output-nodes network-nodes.geojson \
      --epsg 32754
"""
import argparse
import gzip
import json
import re
import sys

NODE_RE = re.compile(r'<node id="([^"]+)" x="([^"]+)" y="([^"]+)"')
LINK_RE = re.compile(
    r'<link id="([^"]+)" from="([^"]+)" to="([^"]+)" length="([^"]+)" '
    r'freespeed="([^"]+)" capacity="([^"]+)" permlanes="([^"]+)"(?: oneway="([^"]+)")?(?: modes="([^"]+)")?'
)


def convert(src, links_out, nodes_out, epsg):
    nodes = {}
    links = []

    with gzip.open(src, "rt", encoding="utf-8") as f:
        for line in f:
            nm = NODE_RE.search(line)
            if nm:
                nid, x, y = nm.groups()
                nodes[nid] = (float(x), float(y))
                continue
            lm = LINK_RE.search(line)
            if lm:
                lid, frm, to, length, freespeed, capacity, permlanes, oneway, modes = lm.groups()
                links.append({
                    "id": lid, "from": frm, "to": to, "length": float(length),
                    "freespeed": float(freespeed), "capacity": float(capacity),
                    "permlanes": float(permlanes), "modes": modes or "",
                })

    print(f"nodes={len(nodes)} links={len(links)}", file=sys.stderr)
    crs = {"type": "name", "properties": {"name": f"urn:ogc:def:crs:EPSG::{epsg}"}}

    node_features = [
        {"type": "Feature", "geometry": {"type": "Point", "coordinates": [x, y]}, "properties": {"id": nid}}
        for nid, (x, y) in nodes.items()
    ]
    with open(nodes_out, "w") as f:
        json.dump({"type": "FeatureCollection", "name": "network-nodes", "crs": crs, "features": node_features}, f)

    link_features = []
    skipped = 0
    for l in links:
        if l["from"] not in nodes or l["to"] not in nodes:
            skipped += 1
            continue
        x1, y1 = nodes[l["from"]]
        x2, y2 = nodes[l["to"]]
        link_features.append({
            "type": "Feature",
            "geometry": {"type": "LineString", "coordinates": [[x1, y1], [x2, y2]]},
            "properties": {
                "id": l["id"], "from": l["from"], "to": l["to"],
                "length_m": l["length"], "freespeed_mps": l["freespeed"],
                "capacity_vph": l["capacity"], "lanes": l["permlanes"], "modes": l["modes"],
            },
        })
    print(f"link features={len(link_features)} skipped(missing node)={skipped}", file=sys.stderr)

    with open(links_out, "w") as f:
        json.dump({"type": "FeatureCollection", "name": "network-links", "crs": crs, "features": link_features}, f)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", required=True, help="source MATSim network .xml.gz")
    parser.add_argument("--output-links", required=True, help="destination links GeoJSON")
    parser.add_argument("--output-nodes", required=True, help="destination nodes GeoJSON")
    parser.add_argument("--epsg", default="32754", help="EPSG code of the network's coordinates (default: 32754)")
    args = parser.parse_args()
    convert(args.input, args.output_links, args.output_nodes, args.epsg)


if __name__ == "__main__":
    main()
