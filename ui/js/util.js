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

function getGraph(scenario, name) {
	for (var i = 0; i < scenario.graphs.length; i++) {
		var graph = scenario.graphs[i].name;
		if (graph.localeCompare(name) == 0) {
			return scenario.graphs[i];
		}
	}
	return null;
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


