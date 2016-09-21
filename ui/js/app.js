// Executes when the page is fully loaded
window.onload = function(e) {
	// Setup the visibility of pages
	$("#outer-start").show();
	$("#outer").hide();

	// Populate the scenario creation dropdowns
	setOptionsForDropdowns(document
			.getElementsByClassName("dropdown-townships"), global.townships);
	setOptionsForDropdowns(document
			.getElementsByClassName("dropdown-scenarios"),
			global.existing_scenarios);
}

//Remove focus from buttons after click
$(".btn").mouseup(function(){
    $(this).blur();
})

// FIXME: reset() is not complete
function reset() {
	for (var i = 0; i < global.townships.length; i++) {
		var township = global.townships[i];
		// Remove all safe lines
		township.safeLines.length = 0;
		$('.glyphicon-remove-sign').parent().remove();
	}
}
// Handles the user selection for brand new scenario creation
$("#new-scenario-dropdown").on(
		"change",
		function() {
			// Hide the popup modal
			$('#newsim').modal('hide');
			// Save the data for use throughout scenario creation
			global.scenario_creation_arg = $(this).find(':selected').text();
			global.scenario_creation_new = true;
			// Hide the start panel and expose the main one
			$("#outer-start").hide();
			$("#outer").show();
			// This is how we get the town name from the global data
			var township = getTownship(global.scenario_creation_arg);
			// Set the scenario title to include the town name
			setScenarioTitle(township.name + " Simulation");
			// Populate the fire dropdrop list for this town
			setOptionsForDropdowns(document
					.getElementsByClassName("dropdown-fires"), township.fires);
			// Populate the relief destinations list for this town
			setOptionsForDropdowns(document
					.getElementsByClassName("dropdown-destinations"),
					township.destinations);
			initMap();
			panMapTo(township.latlon, township.defaultMapZoom);
			drawMarker(township.latlon, township.name);
			drawSimulationAreaOnMap(township.osmArea);
			
			// Set the evac time
			setEvacTime(global.evacTime);
			setEvacPeak(global.evacPeakMins);
		});

// Handles the user selection for new scenario creation based on existing
$("#existing-scenario-dropdown").on("change", function() {
	$('#newsim').modal('hide');
	var scenario = $(this).find(':selected').text();
	global.scenario_creation_arg = scenario;
	global.scenario_creation_new = false;
});

// Global handler for select change events on dynamically generated dropdowns
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
	} else if (e.target.id.localeCompare('existing-destinations') == 0) {
		var text = $(this).find(':selected').text();
		if (text.localeCompare('Select') == 0) {
			$("#add-safeline").prop('disabled', true);
			cancelDraw(true);
		} else {
			$("#add-safeline").prop('disabled', false);
		}
	}
});

// Global handler for remove sign on destinations/safelines
$("body").on("click", ".remove-safeline", function(e) {
	// Get the destination town associated with the x
	var dest = $(this).attr('name');
	// Remove the destination from the list
	$(this).parent().remove();
	// Remove the polyline associated with this destination
	var township = getTownship(global.scenario_creation_arg);
	var poly = getPolyLine(township, dest, true);
	removeSafeLine(poly.line);
	// Fix the row colours
	$('#destinations-list li').removeClass('li-odd');
	$('#destinations-list li').removeClass('li-even');
	$('#destinations-list li:nth-child(odd)').addClass('li-odd');
	$('#destinations-list li:nth-child(even)').addClass('li-even');
    // Stop the event form propagating to parents
	e.stopPropagation();
	// Enable the destination again
	$('#existing-destinations option[value="' + dest + '"]').prop('disabled',false);

});

// Global handler for remove sign on destinations/safelines
$("body").on("click", ".glyphicon-info-sign", function(e) {
	var id = $(this).attr('id');
	$('#infomodal').modal('show');
	e.stopPropagation();
});

$("body").on("click", ".item-safeline", function(e) {
	var dest = $(this).text();
	var township = getTownship(global.scenario_creation_arg);
	var poly = getPolyLine(township, dest, false);
	var vertex = poly.line.getPath().getArray()[0];
	panMapTo([ vertex.lat(), vertex.lng() ], poly.zoom);
	var color = poly.line.strokeColor;
	var size = poly.line.strokeWeight;
	poly.line.setOptions({
		strokeColor : defaults.safeLineHighlightColor,
		strokeWeight : defaults.safeLineHighlightWeight
	});
	setTimeout(function() {
		poly.line.setOptions({
			strokeColor : color,
			strokeWeight : size
		});
	}, 800);
});

// Global handler for key up events
$(document).keyup(function(e) {
	// ESC
	if (e.which == 27) {
		cancelDraw(true);
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
	reset();
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

$("#add-safeline").click(function(event) {
	var township = getTownship(global.scenario_creation_arg);
	var dest = $("#existing-destinations option:selected").text();
	// cancel any active draws first to be safe
	cancelDraw(true);
	drawSafeLine(township, dest, function() {
		// Add the destination to the list
		addDestinationSafeLine();
		// Disable the destination in the dropdown
		$("#existing-destinations option:selected").prop('disabled',true);
		// Reset the dropdown
		$("#existing-destinations").val('Select');
	});
});

function addDestinationSafeLine() {
	var dest = $("#existing-destinations").find(':selected').text();
	var li = '';
	li += '<li class="list-group-item item-safeline">';
	li += dest;
	li += '<span class="glyphicon glyphicon-remove-sign pull-right remove-safeline"';
	li += ' style="z-index: 1;"';
	li += ' name="' + dest + '"';
	li += ' aria-hidden="true"></span>';
	li += '</li>';
	$("#destinations-list").append(li);
	$('#destinations-list li').removeClass('li-odd');
	$('#destinations-list li').removeClass('li-even');
	$('#destinations-list li:nth-child(odd)').addClass('li-odd');
	$('#destinations-list li:nth-child(even)').addClass('li-even');
}

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
			timedPrompt('info', "Simulation created", function() {
				var township = getTownship(global.scenario_creation_arg);
				url = encodeURI("view.html?sim="+ township.name);
				window.location.href=url;
			});
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

// Timepicker
$('#evac-timepicker').timepicker({
	template : false,
	showInputs : false,
	showMeridian : false,
	minuteStep : 15,
	snapToStep : true,
	defaultTime : false
});
$('#evac-timepicker').timepicker().on('changeTime.timepicker', function(e) {
	global.evacTime = {hh: e.time.hours, mm: e.time.minutes};
	//console.log('Evac time is ' + JSON.stringify(global.evac_time));
});

// Time slider
$("#evac-timeslider").slider({
	value : 0,
	min : 0,
	max : 6,
	step : 1,
    slide: function( e, ui ) {
        $( "#amount" ).val( "$" + ui.value );
        global.evacPeakMins = ui.value * 30;
        //console.log('Peak time is ' + global.evacPeakMins);
      }

}).each(
		function() {

			// Add labels to slider whose values
			// are specified by min, max

			// Get the options for this slider (specified above)
			var opt = $(this).data().uiSlider.options;

			// Get the number of possible values
			var vals = opt.max - opt.min;

			// Position the labels
			for (var i = 0; i <= vals; i++) {
				var el;
				// Create a new element and position it with percentages
				if (i % 2 == 0) {
					el = $('<label>' + ((i + opt.min) * 30) + '</label>').css(
							'left', (i / vals * 100) + '%');
				} else {
					el = $('<label>|</label>').css('left',
							(i / vals * 100) + '%');
				}
				// Add the element inside the slider
				$("#evac-timeslider").append(el);

			}

		});
$('#evac-timeslider').timepicker().on('changeTime.timepicker', function(e) {
	// Store as date in (yy,mm,dd,hh,mm,ss) format
	global.evacTime = new Date(00,00,00,e.time.hours,e.time.minutes,00);
});


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


function setScenarioTitle(title) {
	$('#scenario-title').text(title);
}

// Saves the simulation config on the server
function save() {

}

// Shows the msg for a fixed amount of time in a standard info popup
// type: one of 'info', 'warn', or 'error'
function timedPrompt(type, msg, callback) {
	var o = $("#info-overlay")
	o.text(msg);
	o.fadeIn("fast");
	// fadeout after 1 sec
	setTimeout(function() {
		$("#info-overlay").fadeOut(1000);
		if (callback) callback();
	}, 1000);
}

// Create simulation
function createSimulation() {
}

function setEvacTime(evacTime) {
	if (evacTime) {
		$('#evac-timepicker').timepicker('setTime', evacTime.hh + ':' + evacTime.mm);
	} else {
		$('#evac-timepicker').timepicker('setTime', '');
	}
}

function setEvacPeak(mins) {
	var opt = $('#evac-timeslider').data().uiSlider.options;
	var index = 0;
	if (mins && mins > 0) {
		var size = opt.max-opt.min;
		var sizeMins = size*30;
		index = (mins/sizeMins)*size;
	}
	$('#evac-timeslider').slider( 'value', index );
}

//----------------------------------------------------------------------------
// VEHICLES
//----------------------------------------------------------------------------

// Vehicles: handle changes to input field with number of vehicles
$("#num-vehicles").change(function(){
	var val = $("#num-vehicles").val();
	if (val > 0) {
		$('#add-vehicles-area').prop('disabled', false);
	} else {
		$('#add-vehicles-area').prop('disabled', true);
	}
});
// Vehicles: handle clicks on 'draw' button
$("#add-vehicles-area").click(function(event) {
	var township = getTownship(global.scenario_creation_arg);
	var label = $("#num-vehicles").val();
	// cancel any active draws first to be safe
	cancelDraw(true);
	drawVehiclesArea(township, label, function(area) {
		// Add the destination to the list
		addVehiclesArea(area);
		// Reset the count
		$("#num-vehicles").val(0);
		$('#add-vehicles-area').prop('disabled', true);
	});
});
// Vehicles: add new entry (associated with a drawn rectangle on map) to list
function addVehiclesArea(areakm2) {
	var ncars = $("#num-vehicles").val();
	var li = '';
	var veh = 'vehicle';
	if (ncars > 1) {
		veh += 's';
	}
	li += '<li class="list-group-item item-vehicles-area"';
	li += ' name="' + ncars + '">';
	li += ncars + ' '+ veh +' in '+Number(areakm2/1000000).toFixed(2)+'km<sup>2</sup>';
	li += '<span class="glyphicon glyphicon-remove-sign pull-right remove-vehicles-area"';
	li += ' style="z-index: 1;"';
	li += ' name="' + ncars + '"';
	li += ' aria-hidden="true"></span>';
	li += '</li>';
	$("#vehicles-list").append(li);
	$('#vehicles-list li').removeClass('li-odd');
	$('#vehicles-list li').removeClass('li-even');
	$('#vehicles-list li:nth-child(odd)').addClass('li-odd');
	$('#vehicles-list li:nth-child(even)').addClass('li-even');
}
// Vehicles: handle clicks to list item and highlight area on map
$("body").on("click", ".item-vehicles-area", function(e) {
	var dest = $(this).attr('name');
	var township = getTownship(global.scenario_creation_arg);
	var poly = getPolyArea(township, dest, false);
	var vertex = poly.area.getBounds().getCenter();
	panMapTo([ vertex.lat(), vertex.lng() ], poly.zoom);
	var color = poly.area.strokeColor;
	var size = poly.area.strokeWeight;
	poly.area.setOptions({
		strokeColor : defaults.safeLineHighlightColor,
		strokeWeight : defaults.safeLineHighlightWeight
	});
	setTimeout(function() {
		poly.area.setOptions({
			strokeColor : color,
			strokeWeight : size
		});
	}, 800);
});
// Vechicles: remove item (and associated google map rectangle) from list
$("body").on("click", ".remove-vehicles-area", function(e) {
	// Get the destination town associated with the x
	var dest = $(this).attr('name');
	// Remove the destination from the list
	$(this).parent().remove();
	// Remove the polyline associated with this destination
	var township = getTownship(global.scenario_creation_arg);
	var poly = getPolyArea(township, dest, true);
	removeSafeLine(poly.area);
	// Fix the row colours
	$('#vehicles-list li').removeClass('li-odd');
	$('#vehicles-list li').removeClass('li-even');
	$('#vehicles-list li:nth-child(odd)').addClass('li-odd');
	$('#vehicles-list li:nth-child(even)').addClass('li-even');
    // Stop the event from propagating to parents
	e.stopPropagation();
});

