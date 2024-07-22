# Emergency Evacuation Simulator
 
`main`![passing?](https://github.com/agentsoz/ees/actions/workflows/ci.yml/badge.svg?branch=main)

## Dependencies

This program depends on the following projects:
* [BDI-ABM Integration](https://github.com/agentsoz/bdi-abm-integration)
* [Social Network Diffusion Model](https://github.com/agentsoz/diffusion-model)

## Setup

Initialise and update the git submodules as below. This only has to be done once, when you first clone this repository.

```
git submodule update --init --recursive
```

## How to build

```
./mvnw package
```

This will produce the EES release archive in `ees/target/ees-x.y.z-SNAPSHOT.zip`.

## How to run

To run the example scenario unzip the release archive and follow the instructions provided in the packaged README.md.

## Known Issues

* See [GitHub Issues](https://github.com/agentsoz/ees/issues).
