// Appends the names to the array of dropdowns
// names: an array of structures, each with key name
function setOptionsForDropdowns(dropdowns, names) {
	for (var d = 0; d < dropdowns.length; d++) {
		var dropdown = dropdowns[d];
		if (dropdown.options) dropdown.options.length = 0;
		var el = document.createElement("option");
		el.value = 'Select';
		el.innerHTML = 'Select';
		dropdown.appendChild(el);
		for (var i = 0; i < names.length; i++) {
			var opt = names[i].name;
			var el = document.createElement("option");
			el.value = opt;
			el.innerHTML = opt;
			dropdown.appendChild(el);
		}
	}
}

function getScenario(name) {
	for (var i = 0; i < global.existing_scenarios.length; i++) {
		var scenario = global.existing_scenarios[i].name;
		if (scenario.localeCompare(name) == 0) {
			return global.existing_scenarios[i];
		}
	}
	return null;
}

function getScenarioFromServer(scenarioName, callback, errfn) {
	send(shared.MSG_GET_SCENARIO, {name: scenarioName},
			function(json) {
				if (json.msg == shared.MSG_ERROR) {
					if (errfn) return errfn(json.data[0].msg);
				} else {
					if (callback) return callback(json);
				}
			},
			function (str) {
				console.log("Could not retrieve scenarios list: " + str);
				if (errfn) return errfn(str);
			}
		);
	return null;
}

function getExistingScenariosNames() {
	var val = [];
	for (var i = 0; i < global.existing_scenarios.length; i++) {
		val.push(global.existing_scenarios[i].name);
	}
	return val;
}

function getDestinationsNames(township) {
	var val = [];
	for (var i = 0; i < township.destinations.length; i++) {
		val.push(township.destinations[i].name);
	}
	return val;
}

function getDestination(township, name) {
	for (var i = 0; i < township.destinations.length; i++) {
		var dest = township.destinations[i].name;
		if (dest.localeCompare(name) == 0) {
			return township.destinations[i];
		}
	}
	return null;
}

function getGraph(scenario, name) {
	for (var i = 0; i < scenario.graphs.length; i++) {
		var graph = scenario.graphs[i].name;
		if (graph.localeCompare(name) == 0) {
			return scenario.graphs[i];
		}
	}
	return null;
}

function getTownshipsNames() {
	var townships = [];
	for (var i = 0; i < global.townships.length; i++) {
		townships.push(global.townships[i].name);
	}
	return townships;
}


function getTownship(name) {
	for (var i = 0; i < global.townships.length; i++) {
		var township = global.townships[i].name;
		if (township.localeCompare(name) == 0) {
			return global.townships[i];
		}
	}
	return null;
}

function getFire(township, firename) {
	for (var i = 0; i < township.fires.length; i++) {
		var fire = township.fires[i].name;
		if (fire.localeCompare(firename) == 0) {
			return township.fires[i];
		}
	}
	return null;
}

function getDestination(township, destname) {
	for (var i = 0; i < township.destinations.length; i++) {
		var dest = township.destinations[i].name;
		if (dest.localeCompare(destname) == 0) {
			return township.destinations[i];
		}
	}
	return null;
}

function getPolyLine(township, dest, remove) {
	for (var i = 0; i < township.safeLines.length; i++) {
		var town = township.safeLines[i].name;
		if (town.localeCompare(dest) == 0) {
			var line = township.safeLines[i];
			if (remove) {
				township.safeLines.splice(i, 1);
			}
			return line;
		}
	}
	return null;
}

function getPolyArea(township, dest, remove) {
	for (var i = 0; i < township.vehiclesAreas.length; i++) {
		var town = township.vehiclesAreas[i].name;
		if (town.localeCompare(dest) == 0) {
			var area = township.vehiclesAreas[i];
			if (remove) {
				township.vehiclesAreas.splice(i, 1);
			}
			return area;
		}
	}
	return null;
}

function pad(n, width, z) {
	  z = z || '0';
	  n = n + '';
	  return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
}

function convertToSlug(text)
{
  return text
      .toLowerCase()
      .replace(/ /g,'-')
      .replace(/[^\w-]+/g,'')
      ;
}


$(".collapse").on('show.bs.collapse', function(e) {
	$(e.target).parent().find(".glyphicon-triangle-right").hide();
	$(e.target).parent().find(".glyphicon-triangle-bottom").show();
});
$(".collapse").on('hide.bs.collapse', function(e) {
	$(e.target).parent().find(".glyphicon-triangle-right").show();
	$(e.target).parent().find(".glyphicon-triangle-bottom").hide();
});

function send(msg, data, callback, errfn) {
	var jmsg = JSON.stringify({'msg' : msg, 'data' : data});
	console.log('Sending: ' + jmsg);
	// Start the location-based assessment
	$.ajax({
		type : "POST",
		dataType : 'json', // data type of response we get from server
		contentType : 'application/json', // data type of request to server
		data : jmsg,
		timeout : TIMEOUT,
		url : "/api/", // <-- NOTE THE TRAILING '/' IS NEEDED
		success : function(obj) {
			var str = JSON.stringify(obj);
			console.log('Received: ' + str);
			var json = jQuery.parseJSON(str);
			if (json.msg == shared.MSG_ERROR) {
				if (errfn) return errfn(json.data[0].msg);
			} else {
				if (callback) return callback(json);
			}
		},
		error : function (xhr, ajaxOptions, thrownError) {
			console.log("Call to /api failed with error " + xhr.status + ":" + xhr.responseText);
			if (errfn) return errfn(xhr.responseText);
		}
	});
}

//Shows the msg for a fixed amount of time in a standard info popup
//type: one of 'info', 'warn', or 'error'
function timedPrompt(type, msg, cb) {
	var t = BootstrapDialog.TYPE_DEFAULT;
	var title = '';
	if (type == 'info') {
		t = BootstrapDialog.TYPE_INFO;
		title = 'Information';
	} else if (type == 'warn') {
		t = BootstrapDialog.TYPE_WARNING;
		title = 'WARNING';
	} else if (type == 'error') {
		t = BootstrapDialog.TYPE_DANGER;
		title = 'ERROR';
	}
	BootstrapDialog.show({
		title: title,
     message: msg,
     type: t,
     onhide: cb
 });
}
