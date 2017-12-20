all: dist run

DIST=bushfire-2.0.2-SNAPSHOT

define RUNCMD
java -cp ./${DIST}.jar io.github.agentsoz.ees.Main \
	--config scenarios/maldon-2017-11-01/scenario_main.xml \
	--logfile scenarios/maldon-2017-11-01/scenario.log \
	--loglevel INFO \
	--seed 12345 \
	--safeline-output-file-pattern "scenarios/maldon-2017-11-01/safeline.%d%.out" \
	--matsim-output-directory scenarios/maldon-2017-11-01/matsim_output \
	--jillconfig "--config={ \
		agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:600}], \
		logLevel: WARN, \
		logFile: \"scenarios/maldon-2017-11-01/residents.log\", \
		programOutputFile: \"scenarios/maldon-2017-11-01/residents.out\", \
		randomSeed: 12345 \
	}"
endef

QUICK=-DskipTests

full:
	export QUICK= && make all

dist:
	cd ../../ && mvn clean install -N ${QUICK}
	cd ../../integrations/bdi-abm/ && mvn clean install ${QUICK}
	cd ../../integrations/abm-jill/ && mvn clean install ${QUICK}
	cd ../../integrations/bdi-matsim/ && mvn clean install ${QUICK}
	mvn clean install ${QUICK}

target/${DIST}/${DIST}.jar :
	cd target && unzip -o ${DIST}-minimal.zip

ui-install: target/${DIST}/${DIST}.jar
	cd ui && $(MAKE) install

ui-start : ui-install
	cd ui && $(MAKE) run

run: target/${DIST}/${DIST}.jar
	cd target/${DIST} && ${RUNCMD}
