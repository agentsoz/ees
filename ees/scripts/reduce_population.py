#!/usr/bin/env python3
"""
Reduce and re-order a MATSim population XML.

Motivation (Aug 2026, S7 debugging with Dhirendra):
  - The original demand file has person ids sorted alphabetically as
    strings ("0", "1", "10", "100", "1000", ...) rather than numerically.
  - For faster IT-test / dev-loop runs we also want a smaller population.

This script keeps only persons with numeric id < --max-agents, and writes
them back out in ascending numeric id order (which also happens to fix the
ordering issue above, since ids in this dataset are a contiguous 0..N-1
range).

Usage:
  python3 reduce_population.py \
      --input demand-seasonal-40k-50it-epsg32754-7886c91.xml.gz \
      --output demand-seasonal-20k-50it-epsg32754-7886c91.xml.gz \
      --max-agents 20000
"""
import argparse
import gzip
import re
import sys

PERSON_START_RE = re.compile(r'<person id="(\d+)"')


def reduce_population(src, dst, keep_max):
    header_lines = []
    footer_line = None
    kept = {}
    state = "header"
    current_id = None
    current_lines = []
    total_persons = 0

    with gzip.open(src, "rt", encoding="utf-8") as f:
        for line in f:
            if state == "header":
                m = PERSON_START_RE.search(line)
                if m:
                    state = "body"
                    current_id = int(m.group(1))
                    current_lines = [line]
                else:
                    header_lines.append(line)
                continue

            if state == "body":
                current_lines.append(line)
                if "</person>" in line:
                    total_persons += 1
                    if current_id < keep_max:
                        kept[current_id] = current_lines
                    current_lines = []
                    current_id = None
                    state = "between"
                continue

            if state == "between":
                m = PERSON_START_RE.search(line)
                if m:
                    state = "body"
                    current_id = int(m.group(1))
                    current_lines = [line]
                elif "</population>" in line:
                    footer_line = line
                    state = "footer"
                continue

    print(f"Total persons seen: {total_persons}", file=sys.stderr)
    print(f"Kept (id < {keep_max}): {len(kept)}", file=sys.stderr)

    if footer_line is None:
        footer_line = "</population>\n"

    with gzip.open(dst, "wt", encoding="utf-8") as out:
        out.writelines(header_lines)
        for pid in sorted(kept.keys()):
            out.writelines(kept[pid])
            out.write("\n")
        out.write(footer_line)

    print(f"Wrote {dst}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--input", required=True, help="source population .xml.gz")
    parser.add_argument("--output", required=True, help="destination population .xml.gz")
    parser.add_argument("--max-agents", type=int, default=20000, help="keep persons with numeric id below this value (default: 20000)")
    args = parser.parse_args()
    reduce_population(args.input, args.output, args.max_agents)


if __name__ == "__main__":
    main()
