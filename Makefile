all: dist run

VERSION=ees-2.0.2-SNAPSHOT

define RUNCMD
java -cp ./${VERSION}.jar io.github.agentsoz.ees.Main \
	--config scenarios/mount-alexander-shire/maldon-600/scenario_main.xml \
	--logfile scenarios/mount-alexander-shire/maldon-600/scenario.log \
	--loglevel INFO \
	--seed 12345 \
	--safeline-output-file-pattern "scenarios/mount-alexander-shire/maldon-600/safeline.%d%.out" \
	--matsim-output-directory scenarios/mount-alexander-shire/maldon-600/matsim_output \
	--jillconfig "--config={ \
		agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:600}], \
		logLevel: WARN, \
		logFile: \"scenarios/mount-alexander-shire/maldon-600/residents.log\", \
		programOutputFile: \"scenarios/mount-alexander-shire/maldon-600/residents.out\", \
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

dist-quick:
	mvn clean install ${QUICK}


target/${VERSION}/${VERSION}.jar :
	cd target && unzip -o ${VERSION}-dist.zip

ui-install: target/${VERSION}/${VERSION}.jar
	cd ui && $(MAKE) install

ui-start : ui-install
	cd ui && $(MAKE) run

run: target/${VERSION}/${VERSION}.jar
	cd target/${VERSION} && ${RUNCMD}
