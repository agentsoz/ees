SHELL:=/bin/bash
BASEDIR=$(shell cd $(dir $(firstword $(MAKEFILE_LIST))) && pwd)/
JAR=${BASEDIR}../target/bushfire-2.0.2-SNAPSHOT

#############################################
### START USER CONFIG
#############################################

# To install elsewhere, do something like 'make INSTALL_DIR=~/Sites'
INSTALL_DIR:=~/Sites/html

#############################################
### END USER CONFIG
#############################################

# where to install the web app
WEBDIR=${INSTALL_DIR}/html
# where bushfire simulation data will live
DATADIR=${INSTALL_DIR}/data

MINIFY_CMD:=java -jar ${BASEDIR}/etc/minify/yuicompressor-2.4.8.jar
TEMPLATESDIR:=${BASEDIR}../scenarios/template
SCRIPTSDIR:=${BASEDIR}../scripts

all: install

config:
	mkdir -p ${WEBDIR}

install: config
	rsync -a -F --delete --delete-excluded ${BASEDIR} ${WEBDIR}
	ln -s ${DATADIR}/user-data ${WEBDIR}/user-data
	rsync -avz --delete --delete-excluded ${JAR} ${DATADIR}

minify : config
	#
	# Minify our JS files
	#
	cd ${WEBDIR}/js && ${MINIFY_CMD} -o ".js$$:.js" *.js
 	#
	# Minify our CSS files
	#
	cd ${WEBDIR}/css && ${MINIFY_CMD} -o ".css$$:.css" *.css

run:
	cd ${WEBDIR}/nodejs && forever stop serve.js  ; rm -f ~/.forever/serve.js.log  ; forever start -l serve.js.log serve.js

clean:
	rm -rf ${WEBDIR}/*
