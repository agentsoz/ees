#!/usr/bin/env python
import sys
import os
import argparse
import fileinput
import json
import subprocess
from pprint import pprint
from shutil import copyfile
from osgeo import ogr
from osgeo import osr
#----------------------------------------------------------------------------
# Globals and Defaults
#----------------------------------------------------------------------------

nAgents = 300 # hardwired, fix me
proportionRelatives = 0.3 # hardwired, fix me
maxMtrsToRelatives = 1000 # hardwired, fix me

#----------------------------------------------------------------------------
# Util functions for argparse
#----------------------------------------------------------------------------
def is_valid_file(arg):
    if not os.path.exists(arg):
        msg = "%s does not exist" % arg
        raise argparse.ArgumentTypeError(msg)
    else:
        return arg
def is_valid_dir(arg):
    if not os.path.isdir(arg):
        msg = "%s does not exist" % arg
        raise argparse.ArgumentTypeError(msg)
    else:
        return arg
def new_dir(arg):
    if os.path.exists(arg):
        msg = "%s already exists; will not overwrite" % arg
        raise argparse.ArgumentTypeError(msg)
    else:
	os.makedirs(os.path.dirname(filename), exist_ok=True)
     	return arg    	
def parse_args():
	parser = argparse.ArgumentParser(description='Builds a bushfire simulation scenario using web UI produced JSON')
	parser.add_argument('-c','--config',
                    help='path to JSON file (created by bushfire scenario builder UI)',
                    required=True,
                    type=is_valid_file)
	parser.add_argument('-j','--jar',
                    help='path to Bushfire Simulation JAR file',
                    required=True,
                    type=is_valid_file)
	parser.add_argument('-o', '--outdir',
                    help='output directory to use for creating subdir with scenario files',
                    required=True,
                    type=is_valid_dir)
	parser.add_argument('-t', '--templatesdir',
                    help='path to the scenario templates directory',
                    required=True,
                    type=is_valid_dir)
	parser.add_argument('-d', '--datadir',
                    help='path to the application data directory (i.e., townships data)',
                    required=True,
                    type=is_valid_dir)
	parser.add_argument('-n', '--name',
                    help='scenario name used for naming files and directories',
                    required=True)
	parser.add_argument('-v', '--verbose',
                    help='be verbose',
                    action='store_true',
                    required=False)
	return parser.parse_args()


#----------------------------------------------------------------------------
# START
#----------------------------------------------------------------------------

# parse command line arguments
args = parse_args()

# set verbosity
if args.verbose is not None:
    def log(*args):
        # Print each argument separately so caller doesn't need to
        # stuff everything to be printed into a single string
        for arg in args:
           print arg,
        print
else:   
    log = lambda *a: None      # do-nothing function


# values
prefix = args.name
outdir = os.path.join(args.outdir, prefix)

t_main = os.path.join(args.templatesdir, "t_main.xml")
t_main_xsd = os.path.join(args.templatesdir, "main.xsd")
t_matsim = os.path.join(args.templatesdir, "t_matsim.xml")
t_matsim_network_change = os.path.join(args.templatesdir, "t_matsim_network_change_events.xml")
t_matsim_plans = os.path.join(args.templatesdir, "t_matsim_plans.xml")
t_geography = os.path.join(args.templatesdir, "t_geography.xml")
t_geography_xsd = os.path.join(args.templatesdir, "geography.xsd")

o_main = os.path.join(outdir, "%s_main.xml" % prefix)
o_matsim = os.path.join(outdir, "%s_matsim_main.xml" % prefix)
o_matsim_network = os.path.join(outdir, "%s_matsim_network.xml" % prefix)
o_matsim_network_change = os.path.join(outdir, "%s_matsim_network_change_events.xml" % prefix)
o_matsim_plans = os.path.join(outdir, "%s_matsim_plans.xml" % prefix)
o_matsim_outdir = os.path.join(outdir, "%s_matsim_output" % prefix)
o_fire = os.path.join(outdir, "%s_fire.json" % prefix)
o_geography = os.path.join(outdir, "%s_geography.xml" % prefix)

# check before proceeding
if os.path.exists(outdir):
    sys.exit("\nERROR: '%s' already exists; will not proceed" % outdir)
if not os.path.exists(t_main):
    sys.exit("\nERROR: Could not find template file '%s'" % t_main)

# create the output dir
log("creating output dir '%s'" % outdir)
os.makedirs(outdir)

# load the json input
log("loading input JSON config file '%s'" % args.config)
with open(args.config) as data_file:    
    data = json.load(data_file)
pprint(data) if args.verbose else ''

# replacements
main_replacements = {
    # main config template replacements 
    '${matsimfile_name}': o_matsim,
    '${firefile_name}' : o_fire,
    '${firefile_coordinates}' : data["fire"]["coordinate_system"],
    '${firefile_format}' : data["fire"]["format"],
    '${geographyfile_name}' : o_geography,
    '${inputNetworkFile}' : o_matsim_network,
    '${inputChangeEventsFile}' : o_matsim_network_change,
    '${inputPlansFile}' : o_matsim_plans,
    '${outputDirectory}' : o_matsim_outdir,
    '${bdiagents_number}' : "%s" % nAgents,
    '${trafficBehaviour_preEvacDetour_proportion}' : "%s" % proportionRelatives,
    '${trafficBehaviour_preEvacDetour_radiusInMtrs}' : "%s" % maxMtrsToRelatives
}
geography_replacements = {
    '${geographyfile_coordinate_system}' : data["coordinate_system"],
}

# write the main config file
log("writing %s" % o_main)
with open(t_main) as infile, open(o_main, 'w') as outfile:
    for line in infile:
        for src, target in main_replacements.iteritems():
            line = line.replace(src, target)
        outfile.write(line)
cmd = ["cp", t_main_xsd, outdir]
log(cmd)
subprocess.call(cmd)
        
# write the fire file
i_fire = os.path.join(args.datadir, data["fire"]["url"])
log("FIXME: writing fire file '%s' from '%s'" % (o_fire, i_fire))
copyfile(i_fire, o_fire)

# write the MATSim config file
log("writing %s" % o_matsim)
with open(t_matsim) as infile, open(o_matsim, 'w') as outfile:
    for line in infile:
        for src, target in main_replacements.iteritems():
            line = line.replace(src, target)
        outfile.write(line)

# write the MATSim network file
i_network = os.path.join(args.datadir, data["osmArea"]["url"])
log("FIXME: writing MATSim network file '%s' from '%s'" % (o_matsim_network, i_network))
copyfile(i_network, o_matsim_network)

# write the MATSim network file
log("FIXME: writing MATSim network change events file '%s' from '%s'" % (o_matsim_network_change, t_matsim_network_change))
copyfile(t_matsim_network_change, o_matsim_network_change)

# write the MATSim population plans file
#copyfile(t_matsim_plans, o_matsim_plans)

# write the population file
log("FIXME: hardwired %s agents" % nAgents)
### !!!NOTE: order should be LONGITUDE then LATITUDE!!!
xy1 = "%s,%s" % (data["osmArea"]["rectangle"][1], data["osmArea"]["rectangle"][0])
xy2 = "%s,%s" % (data["osmArea"]["rectangle"][3], data["osmArea"]["rectangle"][2])
popnPrefix = '' # popn names should be 1,2,.. so as to match Jill names!!
cmd = [
  "java", 
  "-cp", args.jar,
  "io.github.agentsoz.bushfire.GenerateInput",
  "-outdir", outdir,
  "-prefix", popnPrefix, 
  "-matsimpop", "%s/WGS84/RECT/%s&%s" % (nAgents, xy1, xy2),
  "-wkt", "EPSG:28355",
  "-verbose", "true" if args.verbose else "false"
]
log(cmd)
subprocess.call(cmd)
cmd = [
  "mv", 
  os.path.join(outdir, "%spopulation.xml" % popnPrefix), 
  o_matsim_plans
]
log(cmd)
subprocess.call(cmd)

# write the geography file
log("writing %s" % o_geography)
with open(t_geography) as infile, open(o_geography, 'w') as outfile:
    for line in infile:
        for src, target in geography_replacements.iteritems():
            line = line.replace(src, target)
        outfile.write(line)
cmd = ["cp", t_geography_xsd, outdir]
log(cmd)
subprocess.call(cmd)
     
# write destination locations
src = '<!--${location}-->'
target = ''
for dest in data["destinations"]:
    target = "%s<location>\n" % target
    target = "%s  <name>%s</name>\n" % (target, dest["name"])
    target = "%s  <coordinates>%s,%s</coordinates>\n" % (target, dest["coordinates"]["lat"], dest["coordinates"]["lng"])
    target = "%s</location>\n" % target
f = open(o_geography,'r')
filedata = f.read()
f.close()
newdata = filedata.replace(src, target)
f = open(o_geography,'w')
f.write(newdata)
f.close()
   

#java -cp APPJAR io.github.agentsoz.bushfire.GenerateInput    -outdir test    -prefix maldon    -matsimpop "700/EPSG:28355/RECT/234274,5895647&246377,5919215"
