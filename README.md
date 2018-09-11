# Emergency Evacuation Simulator


## Dependencies

This program depends on the following libraries:

*  BDI-ABM-Integration (`../../integrations/bdi-abm`)
*  BDI-MATSIM-Integration (`../../integrations/bdi-matsim`)
*  ABM-JILL-Integration (`http://agentsoz.github.io/jill`)
*  UTIL `../../util`


## How to build

In the `examples/bushfire` directory, do:
```
make
```

This will produce the executable JAR in `target/ees-x.y.z-SNAPSHOT.jar`.

## How to run

A scenario is a simulation configuration, specified in a XML file. To run a different scenario, you specify a different configuration file at startup.

The package includes several scenarios:

*   Mount Alexander Shire VIC: `scenarios/mount-alexander-shire/*`.
*   Surf Coast Shire VIC: `scenarios/surf-coast-shire/*`.

## Known Issues

*   See https://github.com/agentsoz/bdi-abm-integration/issues.


## License

BDI-ABM Integration Library
Copyright (C) 2014, 2015 by its authors. See AUTHORS file.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

For contact information, see AUTHORS file.
