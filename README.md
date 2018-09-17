# Emergency Evacuation Simulator


## Dependencies

This program depends on the BDI-ABM integration framework: https://github.com/agentsoz/bdi-abm-integration.


## How to build

To build, first clone the BDI-ABM repository alongside this repository. Then do:
```
make
```

This will produce the executable JAR in `target/ees-x.y.z-SNAPSHOT.jar`.

## How to run

To run an example scenario do:
```concept
make run
```
A scenario is a simulation configuration, specified in a XML file. To run a different scenario, you specify a different configuration file at startup.

The package includes several scenarios:

*   Mount Alexander Shire VIC: `scenarios/mount-alexander-shire/*`.
*   Surf Coast Shire VIC: `scenarios/surf-coast-shire/*`.

## Known Issues

*   See https://github.com/agentsoz/ees/issues.


## License

Emergency Evacuation Simulator
Copyright (C) 2014-2018 by its authors. See AUTHORS file.

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
