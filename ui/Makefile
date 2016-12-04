SHELL:=/bin/bash
BASEDIR=$(shell cd $(dir $(firstword $(MAKEFILE_LIST))) && pwd)/

#############################################
### START USER CONFIG
#############################################

# where to install the web app
WEBDIR=/var/www/html
# where bushfire simulation data will live
DATADIR=/var/www/data

#############################################
### END USER CONFIG
#############################################

MINIFY_CMD:=java -jar ./utils/yuicompressor-2.4.8.jar
TEMPLATESDIR:=${BASEDIR}../scenarios/template
SCRIPTSDIR:=${BASEDIR}../scripts

all: install

config:
	mkdir -p ${WEBDIR}
	
install: config
	rsync -a -F --delete --delete-excluded ${BASEDIR} ${WEBDIR}
	ln -s ${DATADIR}/user-data ${WEBDIR}/user-data
	rsync -avz ${BASEDIR}../target/bushfire-1.0.1-SNAPSHOT ${DATADIR}
	
minify : config
	#
	# Minify our JS files
	#
	${MINIFY_CMD} ${WEBDIR}/js/dwellingform.js -o ${WEBDIR}/js/dwellingform.js
	${MINIFY_CMD} ${WEBDIR}/js/householdform.js -o ${WEBDIR}/js/householdform.js
	${MINIFY_CMD} ${WEBDIR}/js/shared.js -o ${WEBDIR}/js/shared.js
	${MINIFY_CMD} ${WEBDIR}/js/hat.js -o ${WEBDIR}/js/hat.js
	${MINIFY_CMD} ${WEBDIR}/nodejs/assess.js -o ${WEBDIR}/nodejs/assess.js
	${MINIFY_CMD} ${WEBDIR}/nodejs/dwellingnode.js -o ${WEBDIR}/nodejs/dwellingnode.js
	${MINIFY_CMD} ${WEBDIR}/nodejs/global.js -o ${WEBDIR}/nodejs/global.js
	${MINIFY_CMD} ${WEBDIR}/nodejs/transitnode.js -o ${WEBDIR}/nodejs/transitnode.js
	${MINIFY_CMD} ${WEBDIR}/nodejs/walkability.js -o ${WEBDIR}/nodejs/walkability.js
 	#
	# Minify our CSS files
	#
	${MINIFY_CMD} ${WEBDIR}/css/hat.css -o ${WEBDIR}/css/hat.css

run:
	cd ${WEBDIR}/nodejs && forever stop serve.js  ; rm -f ~/.forever/serve.js.log  ; forever start -l serve.js.log serve.js

clean:
	rm -rf ${WEBDIR}/*

