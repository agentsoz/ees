#!/bin/bash

# Using this script in the intermin till the ansible
# script is updated (Dhi, 10/Mar/2017)
# Things I had to do to get it running, beyond the enabled commands below
# sudo yum install python-pip
# sudo pip install --upgrade pip --proxy https://bproxy.rmit.edu.au:8080
# sudo pip install utm --proxy https://bproxy.rmit.edu.au:8080
# sudo yum install gdal gdal-devel gdal-python

BASEDIR=/var/www/html/
SERVER=e43361@rschnpeap65.eres.rmit.edu.au
SERVERDIR=/var/www/html/

rsync -avz ${BASEDIR} ${SERVER}:${SERVERDIR}
rsync -avz ${BASEDIR}/../data/bushfire-1.0.1-SNAPSHOT ${SERVER}:${SERVERDIR}/../data

# cd ${WEBDIR}/nodejs && forever stop serve.js  ; rm -f ~/.forever/serve.js.log  ; forever start -l serve.js.log serve.js
ssh ${SERVER} "cd ${SERVERDIR}/nodejs && forever stopall && forever cleanlogs && forever start -l serve.js.log serve.js"
