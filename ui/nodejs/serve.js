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


/*
 * -----------------------------------------------------------------------
 * START HERE
 * -----------------------------------------------------------------------
 */
var app = express()
var validMessages = [shared.MSG_SAVE];
var dataDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/user-data');
var distDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/bushfire-1.0.1-SNAPSHOT');
var dist = path.join(distDir, 'bushfire-1.0.1-SNAPSHOT.jar');
var templateDir  = path.join(path.dirname(fs.realpathSync(__filename)), '../../data/template');

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
    res.send(JSON.stringify({'msg' : shared.MSG_OK, 'data' : {}}));
})

// POST http://localhost:8080/api/users
// parameters sent with 
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
    	res.send({'msg' : shared.MSG_ERROR, 'data' : errors});
    	return;
    }

    if (req.body.msg == shared.MSG_SAVE) {
    	save(req.body.data, function (err, filename) {
    		if (err) {
    			global.log('ERROR: ' + err);
    			res.send({'msg' : shared.MSG_ERROR, 'data' : err});
    			return;
    		}
			global.log("Saved scenario '" + filename + "'");
	    	create(req.body.data, function (err, data) {
	    		if (err) {
	    			global.log('ERROR: ' + err);
	    			res.send({'msg' : shared.MSG_ERROR, 'data' : err});
	    			return;
	    		}
	    		// use data
	    	});
    	});
    }
    else if (req.body.msg == shared.MSG_CREATE) {
    	create(req.body.data, function (err, data) {
    		if (err) {
    			global.log('ERROR: ' + err);
    			res.send({'msg' : shared.MSG_ERROR, 'data' : err});
    			return;
    		}
    		// use data
    	});
    }
    res.send(JSON.stringify({'msg':'OK'}));
});

app.listen(global.SERVICE_ASSESS_PORT, function () {
  global.log('Bushfire UI server is listening on port ' + global.SERVICE_ASSESS_PORT)
})


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
	
	// Remove the scenario directory if it exists
	rm('-rf', path.join(userDir, scenario));
	
	var options = {
	  mode: 'text',
	  //pythonPath: 'path/to/python',
	  //pythonOptions: ['-u'],
	  scriptPath: distDir,
	  args: ['-c', config,
	         '-o', userDir,
	         '-t', templateDir,
	         '-n', scenario,
	         '-j', dist,
	         '-v'
	         ]
	};
	console.log('build_scenario.py ' + options.args);
	pythonShell.run('build_scenario.py', options, function (err, results) {
	  if (err && err.exitCode != 0) callback('Could not build simulation: ' + err);
	  // results is an array consisting of messages collected during execution
	  console.log('results: %j', results);
	});
	/*
	
	// cmds below work as of 11/11/16 @ 14:17
	rm -rf data/user-data/Maldon-Bushfire-Jan-1944/scenario; data/bushfire-1.0.1-SNAPSHOT/build_scenario.py  -c data/user-data/Maldon-Bushfire-Jan-1944/Maldon-Bushfire-Jan-1944.json -o data/user-data/Maldon-Bushfire-Jan-1944 -t data/template/ -n scenario -j data/bushfire-1.0.1-SNAPSHOT/bushfire-1.0.1-SNAPSHOT.jar 

	// Note num agents in cmd below must match what was produced by build-scenario!!
	java -cp data/bushfire-1.0.1-SNAPSHOT/bushfire-1.0.1-SNAPSHOT.jar io.github.agentsoz.bushfire.matsimjill.Main --config datuser-data/Maldon-Bushfire-Jan-1944/scenario/scenario_main.xml --logfile data/user-data/Maldon-Bushfire-Jan-1944/scenario/scenario.log --loglevel INFO --jillconfig "--config={agents:[{classname:io.github.agentsoz.bushfire.matsimjill.agents.Resident, args:null, count:700}],logLevel: WARN,logFile: \"data/user-data/Maldon-Bushfire-Jan-1944/scenario/jill.log\",programOutputFile: \"data/user-data/Maldon-Bushfire-Jan-1944/scenario/jill.out\"}"


	*/
	
}

