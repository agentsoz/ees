# Bushfire Modeling

The Bushfire program simulates the spread of a bushfire over the area
surrounding a town, and the evacuation of the town's inhabitants,
which are modeled as JACK agents. An evacuation controller, which determines
where, when and how the residents should evacuate, is also modeled as a
JACK agent.

After an initial broadcast ordering the evacuation, the residents will
make their way to the evacuation point (after first stopping at a school
to pick up children if they have any, and then a randomly-assigned house
to pick up relatives if they have any), guided by the MATSim engine.

This program can be run with a visualiser front-end which has been programmed
in the Unity game engine. This can display the movements of resident agents
and allows displaying and choosing BDI plans for the evacuation controller
agent.



## Dependencies

This program depends on the following libraries:

*  BDI-ABM-Integration (`/integrations/bdi-abm`) 
*  BDI-MATSIM-Integration (`/integrations/bdi-matsim`) 
*  ABM-JACK-Integration (`/integrations/abm-jack`)
*  ABM-JILL-Integration (`http://agentsoz.github.io/jill`)
*  UTIL
*  Visualiser (optional) <https://bitbucket.org/Kidney/fire-sim-unity/overview>

See the respective README files for information on how to build these 
libraries.



## How to Compile

### Command Line
  
1.  Build the bdi-abm-integration layer: In the source repository `/`, do 
    `mvn clean install -N`
2.  Build the BDI-ABM library: See `/integrations/bdi-abm/README.md`
    for instructions
3.  Build the UTIL library: See `/util/README.md`
    for instructions
4.  Build the ABM-JACK library: See `/integrations/abm-jack/README.md`
    for instructions
5.  Build the ABM-JILL library: See `/integrations/abm-jill/README.md`
    for instructions
6.  Build the Bushfire application: In `/examples/bushfire`, do
    `mvn clean install`

### Eclipse

Ensure that you have the corrent version of Eclipse installed. See 
`../../README.md` for details. Then import and build the following 
Eclipse projects:

*  `/integrations/bdi-abm` 
*  `/util` 
*  `/integrations/bdi-matsim` 
*  `/integrations/abm-jack` 
*  `/integrations/abm-jill` 
*  `/examples/bushfire` 


## How to Run

### Windows Binary

*	Extract `bushfire-0.0.1-SNAPSHOT.zip` file
*	Double click on run.bat file
*	If you want to run without gui, do:

        > cd bushfire-0.0.1-SNAPSHOT
        > run.bat -non-gui

### Script File

From the `/examples/bushfire` directory, and assuming you have already built the application, do:

        > cd target
        > unzip bushfire-0.0.1-SNAPSHOT-bin.zip
        > cd bushfire-0.0.1-SNAPSHOT
        > sh ./run.sh

### Eclipse

In Eclipse, run `agentsoz.bushfire.BushfireMain` as a Java application with the arguments:

    -c scenarios/halls_gap/halls_gap.xml


### Program options

*   `-gui` : Specify if using the interactive Visualiser. Causes the simulation
    to wait for a connection from the visualiser. The port used for the connection
    is specified in the `hals_gap.xml` configuration file. 

*   `-x <long>` and `-y <lat>`: Applies offsets to the location of the fire. 
     For example, in the Halls Gap scenario, specifing `-x 0.01 -y -0.005` 
     moves the fire approximately 1km east and 500m south closer to the town.
     This web site has a calculator to calculate distance based on the latitude.
     http://msi.nga.mil/MSISiteContent/StaticFiles/Calculators/degree.html


## Documentation

*   The `/examples/bushfire/doc` directory contains the following documents:
    *   UserGuide.doc
    *   DesignDescription.doc
    *   DeveloperGuide.doc

*   For background information about integrating BDI agents into MATSim, 
    see the top level `/README.md`

*   Documentation on the Visualiser can be found at:
    https://bitbucket.org/Kidney/fire-sim-unity/overview
    Refer to `target/bushfire-0.0.1-SNAPSHOT/gui/README.md` for downloading 
    installation instructions


## Scenarios

A scenario is a simulation configuration, specified in a XML file. 
To run a different scenario, you specify a different configuration file
at startup.

The package includes the following scenarios:

*   `scenarios/halls_gap/halls_gap.xml`: Evacuation simulation for
    Halls Gap VIC. The evacuation schedule is decided by an 
    Evacuation Controller agent. In GUI mode, the decisions of the
    controller agent can be influenced interactively by a user.
*   `scenarios/barwon_heads/barwon_heads.xml`: Evacuation simulation
    for Barwon Heads VIC. Does not use the Evacuation Controller.

Instructions for configuring scenarios are included in the User Guide
in ``/examples/bushfire/doc/UserGuide.doc`.


## Known Issues

*   Loading of MATSim dictionaries for the xml files takes some time 
    as it tries to get the information via the network. 
    Documentation here http://www.matsim.org/faq#n488 suggests
    that it may be possible to stop this however, this did not seem to work.

    The comments on the FAQ suggest that MATSim may sense the network 
    connection and always try to access the latest dictionaries.



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
