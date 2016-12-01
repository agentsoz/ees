/*
 * -----------------------------------------------------------------------
 * Modules required
 * -----------------------------------------------------------------------
 */
var global = require('./global');
var shared = require('../js/shared');
var express = require('express');
var bodyParser = require('body-parser');
var validator = require('express-validator');
var pythonShell = require('python-shell');
var jsonfile = require('jsonfile');
var mkdirp = require('mkdirp');
var path = require('path');
var fs   = require('fs');
var shell = require('shelljs/global');
var xml2js = require('xml2js');

/*
 * -----------------------------------------------------------------------
 * START HERE
 * -----------------------------------------------------------------------
 */
var app = express()
var validMessages = 
	[shared.MSG_CHECK_EXISTS,
	 shared.MSG_SAVE, 
	 shared.MSG_CREATE, 
	 shared.MSG_CREATE_PROGRESS];
var dataDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/user-data');
var distDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/bushfire-1.0.1-SNAPSHOT');
var dist = path.join(distDir, 'bushfire-1.0.1-SNAPSHOT.jar');
var templateDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/template');
var appDataDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../html');
var logLevelMain = 'INFO';
var logLevelJill = 'WARN';

// Keeps track of the number of requests handled
var requestNum = 0;


app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true })); // support encoded bodies
app.use(validator());

app.get('/', function (req, res) {
	// Increment request counter
    requestNum++;
    global.log("\n--------------------------------------------------");
    global.log('#' + requestNum + ' GET ' + req.originalUrl)
    send({'msg' : shared.MSG_OK, 'data' : {}});
})

app.post('/', function(req, res) {
    // Increment request counter
    requestNum++;
    global.log("\n--------------------------------------------------");
    global.log('#' + requestNum + ' POST ' + req.originalUrl)
    global.log('POST BODY ' + JSON.stringify(req.body));
    
    req.checkBody(
    		"msg", 
    		"Message should be one of ["+validMessages.toString()+"]"
    ).isIn(validMessages);

    // Return if errors in input
    var errors = req.validationErrors();
    if (errors) {
    	global.log('ERROR in input: ' + JSON.stringify(errors));
    	send(res, {'msg' : shared.MSG_ERROR, 'data' : errors});
    	return;
    }
    if (req.body.msg == shared.MSG_CHECK_EXISTS) {
		var dir = path.join(dataDir, req.body.data.name);
		global.log("Checking if scenario '"+dir+"' exists");
		fs.stat(dir, function (err, stats){
			if (err) {
				// Does not exist
				global.log("Scenario '"+dir+"' does not exist");
				send(res, {'msg': shared.MSG_CHECK_EXISTS,
					'data' : shared.MSG_NO});
			} else {
				// Exists; could be a file or dir but we don't really care
				global.log("Scenario '"+dir+"' exists");
				send(res, {'msg': shared.MSG_CHECK_EXISTS,
					'data' : shared.MSG_YES});
			}
		});
		return;
    } 
    if (req.body.msg == shared.MSG_SAVE) {
		global.log("Saving scenario");
    	save(req.body.data, function (err, filename) {
    		if (err) {
    			global.log('ERROR: ' + err);
    			send(res, {'msg' : shared.MSG_ERROR, 'data' : err});
    			return;
    		}
			global.log("Saved scenario '" + filename + "'");
	    	send(res, {'msg': shared.MSG_OK});
    	});
    	return;
    }
    if (req.body.msg == shared.MSG_CREATE) {
		global.log("Creating scenario");
    	create(req.body.data, function (err, data) {
    		if (err) {
    			global.log('ERROR: ' + err);
    			send(res, {'msg' : shared.MSG_ERROR, 'data' : err});
    			return;
    		}
			global.log("Scenario creation completed. " + data);
	    	send(res, {'msg': shared.MSG_OK});
    	});
		return;
    }
    if (req.body.msg == shared.MSG_CREATE_PROGRESS) {
		global.log("Checking scenario creation progress");
		check_creation_progress(req.body.data, function (err, data) {
    		if (err) {
    			global.log('ERROR: ' + err);
    			send(res, {'msg' : shared.MSG_ERROR, 'data' : err});
    			return;
    		}
			global.log("Scenario creation progress is: " + data);
			send(res, {'msg' : shared.MSG_CREATE_PROGRESS, 'data' : data});
			return;
    	});
		return;
    } 
    send(res, {'msg': shared.MSG_ERROR, 'data' : 'Nothing to do'});
});

app.listen(global.SERVICE_ASSESS_PORT, function () {
  global.log('Bushfire UI server is listening on port ' + global.SERVICE_ASSESS_PORT)
})

function send(res, data) {
	global.log('Sending reply ' + JSON.stringify(data));
	res.send(data);
}

function save(data, callback) {
	// Create the top level user data directory if it does not already exist
	mkdirp(dataDir, function(err) { 
		if (err) return callback("Could not create data directory '" + dataDir + "' : " + err);
		// Check if user dir already exists, else create it
		var userDir = path.join(dataDir, data.name);
		mkdirp(userDir, function(err) { 
			if (err) return callback("Could not create scenario directory '" + userDir + "' : " + err);
			// Finally write the JSON to file
			var userFile = path.join(userDir, data.name + '.json');
			jsonfile.writeFile(userFile, data, {spaces: 2}, function (err) {
				if (err) return callback("Could not save scenario '" + userFile + "' : " + err);
				callback(null, userFile);
			});
		});
	});
}

function create(data, callback) {
	var userDir = path.join(dataDir, data.name);
	var config = path.join(userDir, data.name + '.json');
	var scenario = 'scenario';
	var scenarioPath = path.join(userDir, scenario);
	// Remove the scenario directory if it exists
	rm('-rf', scenarioPath);
	// build the scenario files from user data
	var options = {
	  mode: 'text',
	  scriptPath: distDir,
	  args: ['-c', config,
	         '-o', userDir,
	         '-t', templateDir,
	         '-d', appDataDir,
	         '-n', scenario,
	         '-j', dist,
	         '-v'
	         ]
	};
	global.log('build_scenario.py ' + options.args);
	// results is an array consisting of messages collected during execution
	var results = [];
	pythonShell.run('build_scenario.py', options, function (err, res1) {
		if (err && err.exitCode != 0) return callback('Could not build simulation: ' + err);
		if (res1) results.push(res1);
		
		var fileMain = path.join(scenarioPath, 'scenario_main.xml');
		var fileLog = path.join(scenarioPath, 'scenario.log');
		var fileJillLog = path.join(scenarioPath, 'jill.log');
		var fileJillOut = path.join(scenarioPath, 'jill.out');
		
    	// Read number of agents from config xml
		var parser = new xml2js.Parser();
		fs.readFile(fileMain, function(err, data) {
		    parser.parseString(data, function (err, res2) {
				if (err && err.exitCode != 0) return callback('Could not build simulation: ' + err);
				global.log(JSON.stringify(res2));
				console.log(require('util').inspect(res2, false, null));
		    	var nAgents = res2.simulation.bdiagents[0].trim();
				// Now run the simulation
				var cmd = 'java -cp ' + dist +
			       ' io.github.agentsoz.bushfire.matsimjill.Main' +
			       ' --config ' + fileMain +
			       ' --logfile ' + fileLog +
			       ' --loglevel ' + logLevelMain +
			       ' --jillconfig "--config={' + 
			       'agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:'+nAgents+'}],' +
			       'logLevel: ' + logLevelJill + ',' +
			       'logFile: \\"' + fileJillLog + '\\",' +
			       'programOutputFile: \\"' + fileJillOut + '\\"' +
			       '}"' +
			       '&'
			       ;
				global.log(cmd);
				exec(cmd, {async:true, silent:true});
				return callback(null, results);
		    });
		});
	});
}

function check_creation_progress(data, callback) {
	var logFile = path.join(dataDir, data.name, 'scenario', 'scenario_matsim_output','logfile.log');
	if (!test('-f', logFile)) {
		return callback(null, 0);
	} 
	var cmd = 'cat ' + logFile + ' | grep -i "New QSim\\|ITERATION 0\\|JVM\\|shutdown completed"';
	global.log(cmd);
	var stdout = exec(cmd, {silent:true}).stdout;

	var progress = 0;
	if(stdout.indexOf("ITERATION 0 BEGINS") > -1) {
		progress += 1;
	}
	var count = (stdout.match(/NEW QSim\) AT /g) || []).length;
	progress += count;
	if(stdout.indexOf("ITERATION 0 fires iteration end event") > -1) {
		progress += 1;
	}
	if(stdout.indexOf("shutdown completed") > -1) {
		progress += 1;
	}
	progress = Math.round((progress/27.0)*100);
	if (progress > 100) progress = 100;
	return callback(null, progress);
}
