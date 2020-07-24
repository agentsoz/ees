#!/usr/bin/env python
import sys
import os
import subprocess
import gzip
import shutil
import argparse

#----------------------------------------------------------------------------
# Globals and Defaults
#----------------------------------------------------------------------------
OGR2OGR = 'ogr2ogr'
   
#----------------------------------------------------------------------------
# Check if a rogram is installed
#----------------------------------------------------------------------------
def which(program):
    import os
    def is_exe(fpath):
        return os.path.isfile(fpath) and os.access(fpath, os.X_OK)

    fpath, fname = os.path.split(program)
    if fpath:
        if is_exe(program):
            return program
    else:
        for path in os.environ["PATH"].split(os.pathsep):
            path = path.strip('"')
            exe_file = os.path.join(path, program)
            if is_exe(exe_file):
                return exe_file

    return None

#----------------------------------------------------------------------------
# START
#----------------------------------------------------------------------------

# check that ogr2ogr is installed
if which(OGR2OGR) == None :
   print '\n',OGR2OGR,' is either not installed or not accessible; aborting.\n'
   sys.exit(1)
   
# Parse input arguments
def valid_file(arg):
    if not os.path.exists(arg):
        msg = "The file %s does not exist!" % arg
        raise argparse.ArgumentTypeError(msg)
    else:
        return arg

parser = argparse.ArgumentParser(description='Converting a Shape file to a JSON')

parser.add_argument('-infile',
                    help='path to the input shape file',
                    required=True,
                    type=valid_file)
parser.add_argument('-outfile',
                    help='path to the output json file (default is /vsistdout)',
                    required=False,
                    default='/vsistdout')
args = parser.parse_args()

# process shape files and generate ouput json
gzfile = args.outfile + '.gz'

#print 'writing ', args.outfile
# delete the file if it exists
#try:
#   os.remove(args.outfile)
#except OSError:
#   pass
subprocess.call([OGR2OGR, '-f', 'GeoJSON', args.outfile, args.infile])
#with open(args.outfile, 'rb') as f_in, gzip.open(gzfile, 'wb') as f_out:
#   shutil.copyfileobj(f_in, f_out)
#   os.remove(args.outfile)

