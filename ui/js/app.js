//
// GLOBALS
//
global = {};
global.townships = [ {
	name : "Maldon",
	latlon : [ -36.997609, 144.068722 ],
	osmArea : [ -36.8093, 144.2759, -37.0853, 143.9257 ],
	defaultMapZoom : 10,
	fires : [ {
		name : "20160420_MtAlexShire_FDI75_Iso",
		url : "media/20160420_MtAlexShire_FDI75_Iso.json"
	}, {
		name : "20160420_MtAlexShire_FDI50_Iso",
		url : "media/20160420_MtAlexShire_FDI50_Iso.json"
	} ]
},

{
	name : "Fryerstown",
	latlon : [ -37.140291, 144.249982 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	defaultMapZoom : 11,
	fires : []
} ];

global.existing_scenarios = [ {
	name : "Maldon-Bushfire-Jan-1944",
	township : "Maldon"
}, {
	name : "Maldon-Bushfire-Mar-1803",
	township : "Maldon"
}, {
	name : "Fryerstown-Bushfire-Jan-2017",
	township : "Fryerstown"
} ];

// Executes when the page is fully loaded
window.onload = function(e) {
	// Setup the visibility of pages
	$("#outer-start").show();
	$("#outer").hide();

	// Populate the scenario creation dropdowns
	addOptionsToDropdowns(
			document.getElementsByClassName("dropdown-townships"),
			global.townships);
	addOptionsToDropdowns(
			document.getElementsByClassName("dropdown-scenarios"),
			global.existing_scenarios);
}

// Handles the user selection for brand new scenario creation
$("#new-scenario-dropdown").on(
		"change",
		function() {
			$('#newsim').modal('hide');
			global.scenario_creation_arg = $(this).find(':selected').text();
			global.scenario_creation_new = true;
			$("#outer-start").hide();
			$("#outer").show();
			var township = getTownship(global.scenario_creation_arg);
			setScenarioTitle(township.name + " Simulation");
			addOptionsToDropdowns(document
					.getElementsByClassName("dropdown-fires"), township.fires);

			panMapTo(township.latlon, township.defaultMapZoom);
			drawMarker(township.latlon, township.name);
			drawSimulationAreaOnMap(township.osmArea);
		});

// Handles the user selection for new scenario creation based on existing
$("#existing-scenario-dropdown").on("change", function() {
	$('#newsim').modal('hide');
	var scenario = $(this).find(':selected').text();
	global.scenario_creation_arg = scenario;
	global.scenario_creation_new = false;
});

// Handles the user selection for the fire
$("body").on("change", "select", function(e) {
	if (e.target.id.localeCompare('existing-fires-dropdown') == 0) {
		var township = getTownship(global.scenario_creation_arg);
		var text = $(this).find(':selected').text();
		if (text.localeCompare('Select') == 0) {
			global.scenario_fire = null;
		} else {
			var fire = getFire(township, text);
			global.scenario_fire = fire;
		}
		drawFire();
	}
});

// Back confirmation dialog
$("#dialog-confirm").dialog({
	resizable : false,
	height : "auto",
	width : "auto",
	autoOpen : false,
	buttons : {
		"Continue" : function() {
			$(this).dialog("close");
			$("#outer").hide();
			$("#outer-start").show();
		},
		Cancel : function() {
			$(this).dialog("close");
		}
	}
});

// Back button
$("#nav-back").click(function(event) {
	$("#dialog-confirm").dialog("open");
	event.preventDefault();
});

// Save button
$("#nav-save").click(function(event) {
	save();
	timedPrompt('info', "Simulation saved");
});

$('#show-fire').change(function() {
	drawFire();
});

// Create simulation button
$("#nav-create-sim").click(function(event) {
	// Disable all buttons
	$('.btn').prop('disabled', true);
	// Start creating the simulation
	createSimulation();
	// Show the progress bar and update it every few ms
	// TODO: This should happen based on progress feedback from server instead
	$('.progress').show();
	var i = 0;
	var counterBack = setInterval(function() {
		i++;
		if (i <= 100) {
			$('.progress-bar').css('width', i + '%').attr('aria-valuenow', i);
			$('.progress-bar').text(i + '%');
		} else {
			// Clear the timer
			clearInterval(counterBack);
			// Hide and reset the progress bar
			$('.progress').hide();
			$('.progress-bar').css('width', '0%').attr('aria-valuenow', 0);
			// Disable all buttons
			$('.btn').prop('disabled', false);
			// Show success message
			timedPrompt('info', "Simulation created");
		}
	}, 100);
});

// Accordian
$("#accordion").accordion({
	collapsible : true,
	active : false
});

// Control group
$(".controlgroup").controlgroup();

// Appends the names to the array of dropdowns
// names: an array of structures, each with key name
function addOptionsToDropdowns(dropdowns, names) {
	for (var d = 0; d < dropdowns.length; d++) {
		var dropdown = dropdowns[d];
		for (var i = 0; i < names.length; i++) {
			var opt = names[i].name;
			var el = document.createElement("option");
			el.value = opt;
			el.innerHTML = opt;
			dropdown.appendChild(el);
		}
	}
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

function getScenario(name) {
	for (var i = 0; i < global.existing_scenarios.length; i++) {
		var scenario = global.existing_scenarios[i].name;
		if (scenario.localeCompare(name) == 0) {
			return global.existing_scenarios[i];
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

function setScenarioTitle(title) {
	$('#scenario-title').text(title);
}

// Saves the simulation config on the server
function save() {

}

// Shows the msg for a fixed amount of time in a standard info popup
// type: one of 'info', 'warn', or 'error'
function timedPrompt(type, msg) {
	var o = $("#info-overlay")
	o.text(msg);
	o.fadeIn("fast");
	// fadeout after 1 sec
	setTimeout(function() {
		$("#info-overlay").fadeOut(1000);
	}, 1000);

}

// Create simulation
function createSimulation() {
}
