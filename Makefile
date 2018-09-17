all: dist

VERSION=ees-2.0.2-SNAPSHOT
QUICK=-DskipTests

full:
	export QUICK= && make all

dist:
	cd ../bdi-abm-integration/ && mvn clean install -N ${QUICK}
	cd ../bdi-abm-integration/util/ && mvn clean install ${QUICK}
	cd ../bdi-abm-integration/integrations/bdi-abm/ && mvn clean install ${QUICK}
	cd ../bdi-abm-integration/integrations/abm-jill/ && mvn clean install ${QUICK}
	cd ../bdi-abm-integration/integrations/bdi-matsim/ && mvn clean install ${QUICK}
	mvn clean install ${QUICK}

dist-quick:
	mvn clean install ${QUICK}

run: target/${VERSION}.jar
	java -jar target/${VERSION}.jar --config scenarios/mount-alexander-shire/maldon-600/ees.xml
