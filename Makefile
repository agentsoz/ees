define CMD_RUN
mvn exec:exec \
	-Dexec.executable=java \
	-Dexec.classpathScope=test \
	-Dexec.args="\
		-Xmx4g -Xms4g \
		-cp %classpath io.github.agentsoz.ees.Run \
		--config scenarios/mount-alexander-shire/maldon-600/ees.xml"
endef

all: dist

jill:
	cd ../jill && mvn clean install

diffusion:
	cd ../diffusion-model && mvn clean install

bdiabm:
	cd ../bdi-abm-integration/ && mvn clean install -N
	cd ../bdi-abm-integration/util/ && mvn clean install
	cd ../bdi-abm-integration/integrations/bdi-abm/ && mvn clean install
	cd ../bdi-abm-integration/integrations/abm-jill/ && mvn clean install
	cd ../bdi-abm-integration/integrations/bdi-matsim/ && mvn clean install

dep: jill diffusion bdiabm

dist: dep
	mvn clean install

run:
	${CMD_RUN}


#	java -jar target/${VERSION}.jar --config scenarios/mount-alexander-shire/maldon-600/ees.xml
