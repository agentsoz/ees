//
// GLOBALS
//
global = {};
global.townships = 
	[
	 {
    	name: "Maldon",
    	latlon: [-36.997609, 144.068722]
     },
     {
    	name: "Fryerstown",
    	latlon: [-37.140291, 144.249982]
     }
    ];
global.existing_scenarios = 
	[
	 {
		 name: "Maldon-Bushfire-Jan-1944",
		 township: "Maldon"
	 },
	 {
		 name: "Maldon-Bushfire-Mar-1803",
		 township: "Maldon"
	 },
	 {
		 name: "Fryerstown-Bushfire-Jan-2017",
		 township: "Fryerstown"
	 }
	];

// Executes when the page is fully loaded
window.onload = function(e){ 
	// Setup the visibility of pages
    $( "#outer-start" ).show();
    $( "#outer" ).hide();
    
	// Populate the scenario creation dropdowns
	addNamesToNewScenarioDropdowns(
			document.getElementsByClassName("dropdown-townships"),
			global.townships);
	addNamesToNewScenarioDropdowns(
			document.getElementsByClassName("dropdown-scenarios"),
			global.existing_scenarios);

}

// Handles the user selection for brand new scenario creation
$("#new-scenario-dropdown").on("change", function() {
	$('#newsim').modal('hide');
    global.scenario_creation_arg = $(this).find(':selected').text();
    global.scenario_creation_new = true;
    $( "#outer-start" ).hide();
    $( "#outer" ).show();
    var township = getTownship(global.scenario_creation_arg);
    setScenarioTitle(township.name + " Simulation")
	panMapTo(township.latlon, 13);
});
//Handles the user selection for new scenario creation based on existing
$("#existing-scenario-dropdown").on("change", function() {
	$('#newsim').modal('hide');
    var scenario = $(this).find(':selected').text();
    global.scenario_creation_arg = scenario;
    global.scenario_creation_new = false;
});



function addNamesToNewScenarioDropdowns(dropdowns, names) {
	for (var d = 0; d < dropdowns.length; d++) {
		var dropdown = dropdowns[d];
		for(var i = 0; i < names.length; i++) {
		    var opt = names[i].name;
		    var el = document.createElement("option");
		    el.value=opt;
		    el.innerHTML=opt;
		    dropdown.appendChild(el);
		}
	}
}

function getTownship(name) {
	for(var i = 0; i < global.townships.length; i++) {
	    var township = global.townships[i].name;
	    if (township.localeCompare(name)==0) {
	    	return global.townships[i];
	    }
	}
	return null;
}

function getScenario(name) {
	for(var i = 0; i < global.existing_scenarios.length; i++) {
	    var scenario = global.existing_scenarios[i].name;
	    if (scenario.localeCompare(name)==0) {
	    	return global.existing_scenarios[i];
	    }
	}
	return null;
}

function setScenarioTitle(title) {
	$('#scenario-title').text(title);
}
