SHELL:=/bin/bash

VERSION:=ees-2.0.2-SNAPSHOT

BASEDIR=$(shell cd $(dir $(firstword $(MAKEFILE_LIST))) && pwd)/
JARDIR=${BASEDIR}../target/${VERSION}
JARARCHIVE=${BASEDIR}../target/${VERSION}-dist.zip

#############################################
### START USER CONFIG
#############################################

# To install elsewhere, do something like 'make INSTALL_DIR=~/Sites'
INSTALL_DIR:=/var/www

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

${JARDIR}:
	mkdir -p ${JARDIR}
	unzip ${JARARCHIVE} -d ${JARDIR}/..

config: ${JARARCHIVE} ${JARDIR}
	mkdir -p ${WEBDIR}

install: config
	# first copt the distribution to the data dir
	rsync -avz --delete --delete-excluded ${JARDIR} ${DATADIR}
	# next sync the ui to the web dir and set up links so ui can access data files
	rsync -avz --delete --delete-excluded ${DATADIR}/${VERSION}/ui/ ${WEBDIR}
	ln -s ${DATADIR}/user-data ${WEBDIR}/user-data
	ln -s ${DATADIR}/${VERSION}/app-data ${WEBDIR}/app-data
	ln -s ${DATADIR}/${VERSION}/scenarios/mount-alexander-shire/mount_alexander_shire_network.xml.gz ${WEBDIR}/app-data/network.xml.gz
	# now set up the user data dir where scenarios will be created by the ui
	mkdir -p ${DATADIR}/user-data
	rsync -avz --delete --delete-excluded ${DATADIR}/${VERSION}/scenarios/xsd ${DATADIR}/user-data
	cp ${DATADIR}/${VERSION}/scenarios/mount-alexander-shire/mount_alexander_shire_network.xml.gz ${DATADIR}/user-data/network.xml.gz

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
