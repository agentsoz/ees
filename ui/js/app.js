// GLOBALS


// Executes when the page is fully loaded
window.onload = function(e) {
	// Setup the visibility of pages
	$("#outer-start").show();
	$("#outer").hide();

	// Auto complete setup
	$( "#new-scenario-input" ).autocomplete({
		source: getTownshipsNames(), // list of auto-complete options
		select: handleNewScenarioInput // function to call on selection
	});
	$( "#new-scenario-input" ).attr('placeholder', 'Start typing ...');
	$( "#existing-scenario-input" ).autocomplete({
		source: getExistingScenariosNames(), // list of auto-complete options
		select: handleExistingScenarioInput // function to call on selection
	});
	$( "#existing-scenario-input" ).attr('placeholder', 'Start typing ...');
	// FIXME: Implement "based on existing scenario"
	$( "#existing-scenario-input" ).attr('placeholder', 'Feature not available yet');
	$( "#existing-scenario-input" ).attr('disabled', true);
	
}

// Remove focus from buttons after click
$(".btn").mouseup(function() {
	$(this).blur();
});

// FIXME: reset() is not complete
function reset() {
	// Reset global info about which scenario we are creating
	global.scenario_creation_arg = null;
	global.scenario_creation_new = null;
	global.save_as = null;

	// Remove all safe lines
	for (var i = 0; i < global.townships.length; i++) {
		var township = global.townships[i];
		township.safeLines.length = 0;
		township.vehiclesAreas.length = 0;
		$('.glyphicon-remove-sign').parent().remove();
	}
	// Reset the new scenario dropdown
	$("#new-scenario-dropdown").val('Select');

	// Reset the new scenario input
	$( "#new-scenario-input" ).val('');
	$( "#new-scenario-input" ).attr('placeholder', 'Start typing ...');
	$( "#existing-scenario-input" ).val('');
	$( "#existing-scenario-input" ).attr('placeholder', 'Start typing ...');

	// Reset the fire description
	$('#incident-description').hide();
	$('#incident-description').text('');
	
	global.evacTime = {
		hh : 12,
		mm : 0
	};
	global.evacPeakMins = 60;

	$("#max-speed-slider").slider("option", "value", 10);
	global.maxSpeed = 100;
	
}

// FIXME: handle new scenario based on existing
function handleExistingScenarioInput(event, ui) {
}

// Handles the user selection for brand new scenario creation
function handleNewScenarioInput(event, ui) {
			// Hide the popup modal
			$('#newsim').modal('hide');
			// Save the data for use throughout scenario creation
			//global.scenario_creation_arg = $(this).find(':selected').text();
			global.scenario_creation_arg = ui.item.value;
			global.scenario_creation_new = true;
			// Hide the start panel and expose the main one
			$("#outer-start").hide();
			$("#outer").show();
			// This is how we get the town name from the global data
			var township = getTownship(global.scenario_creation_arg);
			// Set the scenario title to include the town name
			setScenarioTitle(township.name + " Simulation");
			// Populate the fire dropdrop list for this town
			setOptionsForIncidentDropdown();
			//setOptionsForDropdowns($(".dropdown-fires"), township.fires, "Phoenix fire models");
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
			
		    return false;

}

//Appends the names to the array of dropdowns
//names: an array of structures, each with key name
function setOptionsForIncidentDropdown() {
	var township = getTownship(global.scenario_creation_arg);
	dropdown = $(".dropdown-fires");
	names = township.fires;
	optgroup = "Phoenix fire models";
	
	//for (var d = 0; d < dropdowns.length; d++) {
	//	var dropdown = $(dropdowns[d]);
		dropdown.empty();
		//var el = document.createElement("option");
		//el.value = 'Select';
		//el.innerHTML = 'Select';
		//dropdown.appendChild(el);
   var option = $("<option></option>");
   option.val('Select');
   option.text('Select');
   dropdown.append(option);
   var option = $("<option></option>");
   option.val('No incident');
   option.text('No incident');
   dropdown.append(option);
   
		var group = dropdown;
		if (optgroup!=null) {
			group = $('<optgroup>');
       group.attr('label', optgroup);			
		}
		for (var i = 0; i < names.length; i++) {
			var opt = names[i].name;
			var el = document.createElement("option");
			el.value = opt;
			el.innerHTML = opt;
	        //var option = $("<option></option>");
	        //option.val(opt);
	        //option.text(opt);
			group.append(el);
		}
		if (optgroup!=null) {
			dropdown.append(group);
		}
	//}
}

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
		$('#incident-description').text('').hide();
		if (text.localeCompare('Select') == 0) {
			global.scenario_fire = null;
		} else if (text.localeCompare('No incident') == 0) {
			global.scenario_fire = null;
		} else {
			var fire = getFire(township, text);
			global.scenario_fire = fire;
			$('#incident-description').text(fire.description).fadeIn('slow');
		}
		drawFire();
	} else if (e.target.id.localeCompare('existing-destinations') == 0) {
		var text = $(this).find(':selected').text();
		if (text.localeCompare('Select') == 0) {
			$('.draw-line-step-1').hide();
			cancelDraw(true);
		} else {
			$('.draw-line-step-1').fadeIn('slow');
		}
	}
});

// Global handler for remove sign on destinations/safelines
$("body").on(
		"click",
		".remove-safeline",
		function(e) {
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
			$('#existing-destinations option[value="' + dest + '"]').prop(
					'disabled', false);
			if($('#destinations-list li').length==0) {
				$('#plus-safelines').hide();
				$('#existing-destinations').show();
			}
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
		$('#add-vehicles-area').text('Draw area');
		$('#add-safeline').text('Draw safe line');
	}
});

// Back button
$(".nav-back").click(function(event) {
	//$("#dialog-confirm").dialog("open");
	//reset();
	//event.preventDefault();
	BootstrapDialog.confirm({
        title: 'WARNING',
        message: 'Any unsaved changes will be permanently lost. Are you sure?',
        type: BootstrapDialog.TYPE_WARNING, // <-- Default value is BootstrapDialog.TYPE_PRIMARY
        btnOKClass: 'btn-warning', // <-- If you didn't specify it, dialog type will be used,
        callback: function(result) {
            // result will be true if button was click, while it will be false if users close the dialog directly.
            if(result) {
    			$("#outer").hide();
            	reset();
    			$("#outer-start").show();
            }
        }
    });
});

// Save button
$(".nav-save").click(function(event) {
	var val = '';
	if (global.save_as != null) {
		val = global.save_as;
	}
	BootstrapDialog.show({
		title: 'Save working copy',
        message: 'Enter your initials and a short description, '+
        'e.g., <i>DS Maldon evacuation in low visibility</i> : '+
        '<input type="text" value="'+val+'" class="form-control">'+
        '<div style="color:rgb(110, 172, 44);" id="msg"></div>',
        onhide: function(dialogRef){
        	var action = dialogRef.getData('action');
        	// If not saving then continue closing
            if (action != 'save') {
            	return true;
            }
        	// User pressed save; if no value was entered then don't close
        	var input = dialogRef.getModalBody().find('input')
            var name = input.val().trim();
            if (name == '') {
                return false;
            }
            // If re-saving, then don't need to do any processing
            if (global.save_as) {
            	return true;
            }
            // Otherwise, rename to suitable slug name and check with user
        	var save_as = dialogRef.getData('save_as');
            if (!save_as) {
                var saveas = convertToSlug(name);
                var date = new Date();
                var d = date.getDate();
                var m =  date.getMonth() + 1;  // JS months are 0-11
                var y = date.getFullYear();
                var prefix = y + '-' + pad(m,2) + '-' + pad(d,2) + '-';
                var saveas = prefix + saveas;
                // Disable the input, and set the proposed name as its value
                input.prop('disabled', true);
                input.val(saveas);
            	dialogRef.setData('save_as', saveas);
                var msg = dialogRef.getModalBody().find('#msg');
                msg.html('Working copy will be saved as <i>'+saveas+'</i>. Please confirm.');
                return false;
            }
        },
        onhidden: function(dialogRef){
        	var action = dialogRef.getData('action');
        	var save_as = dialogRef.getData('save_as');
        	// Nothing to do here if not saving
            if (action != 'save' || !save_as) {
            	return;
            }
            // Just save if re-saving
            if (global.save_as) {
    			save(function (str) { timedPrompt('info', 'Saved ' + global.save_as); },
        				function (str) {timedPrompt('error', "The simulation could not be saved. " + str); });
            }
            
            // Check that the scenario does not already exist
            console.log("Checking if scenario '"+save_as+"' exists");
            send(shared.MSG_CHECK_EXISTS, {name: save_as},
            	function (json) {
            		if (json.data == shared.MSG_YES) {
                		// Scenario exists so confirm overwrite
            			console.log("Scenario '"+save_as+"' exists; will confirm with user now");
            			BootstrapDialog.confirm({
            				title: 'WARNING',
            				message: "Scenario '"+save_as+"' already exists and will be overwritten. Continue?",
            				type: BootstrapDialog.TYPE_WARNING,
            				btnOKClass: 'btn-warning',
            				callback: function(result) {
            					if(result) {
            						// Over-write accepted so continue saving
            						console.log("User accepted scenario overwrite");
            						global.save_as = save_as;
            						save(function (str) { timedPrompt('info', 'Saved ' + save_as); },
            							function (str) {timedPrompt('error', "The simulation could not be saved. " + str); });
            					} else {	
            						console.log("User cancelled scenario save");
            			        }
            				}
            			});
            		} else {
            			// Scenario does not exist so continue saving
            			console.log("Scenario '"+save_as+"' does not exist; calling save now");
            			save(function (str) { timedPrompt('info', 'Saved ' + save_as); },
            				function (str) {timedPrompt('error', "The simulation could not be saved. " + str); });
            		}
            	},
            	function (str) {
            		timedPrompt('error', "Could not check if scenario already exists. " + str); 
            	}
            );
        },
        buttons: [{
            label: 'Cancel',
            action: function(dialogRef) {
            	dialogRef.setData('action', 'close');
                dialogRef.close();
            }
        },
        {
            label: 'Save',
            hotkey: 13, // Enter key
            action: function(dialogRef) {
            	dialogRef.setData('action', 'save');
                dialogRef.close();
            }
        }]
    });
});

$('#show-fire').change(function() {
	drawFire();
});

$("#add-safeline").click(function(event) {
	var township = getTownship(global.scenario_creation_arg);
	var dest = $("#existing-destinations option:selected").text();
	// cancel any active draws first to be safe
	cancelDraw(true);
	if($('#add-safeline').text()=='Cancel draw') {
		$('#add-safeline').text('Draw safe line');
		return;
	}
	$('#add-safeline').text('Cancel draw');
	drawSafeLine(township, dest, function() {
		// Add the destination to the list
		addDestinationSafeLine();
		// Disable the destination in the dropdown
		$("#existing-destinations option:selected").prop('disabled', true);
		// Reset the dropdown
		$("#existing-destinations").val('Select');
		$('#add-safeline').text('Draw safe line');
		$('#plus-safelines').show();
		$('.draw-line-step-1').hide();
		$('#existing-destinations').hide();
		
	});
});

$("#plus-safelines").click(function(event) {
	$('#existing-destinations').show();
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
$(".nav-create-sim").click(function(event) {
	if (global.save_as == null) {
		$('.progress-bar-modal.in').modal('hide');
		timedPrompt('warn', 'Please save the scenario first'); 
		return;
	}
	$('.progress-bar-modal').modal('show');
	// Start creating the simulation
	send(shared.MSG_CREATE, {name: global.save_as},
		// Success function
		function (data) {
			var limit = 200; 
			var progress = 0;
			$('.progress').show();
			var timeout = setInterval( function() {
				// force checks to terminate when limit is reached (should have completed by now)
				if (limit-- <= 0) return clearInterval(timeout);
				send(shared.MSG_CREATE_PROGRESS, {name: global.save_as},
						// success
						function (json) {
							progress = Number(json.data);
							$('.progress-bar').css('width', progress + '%').attr('aria-valuenow', progress);
							$('.progress-bar').text(progress + '%');
							if (progress >= 100) {
								clearTimeout(timeout);
								// Hide and reset the progress bar
								$('.progress').hide();
								$('.progress-bar').css('width', '0%').attr('aria-valuenow', 0);
								// Close the modal
								$('.progress-bar-modal').modal('hide');
								// Show success message
								timedPrompt('info', "Simulation created", function() {
									// FIXME: Redirect to results page
									var township = getTownship(global.scenario_creation_arg);
									url = encodeURI("view.html?sim=" + township.name);
									window.location.href = url;
								}, 3000);
							}
						},
						function (err) {
							clearInterval(timeout);
							timedPrompt('error', "Create progress unknown. " + str); 
						}
					);
			},	2000);
		},
		function (str) {
			timedPrompt('error', "Could not create simulation. " + str); 
			$('.progress-bar-modal.in').modal('hide');
		}
	);
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
	global.evacTime = {
		hh : e.time.hours,
		mm : e.time.minutes
	};
	// console.log('Evac time is ' + JSON.stringify(global.evac_time));
});

// Time slider
$("#evac-timeslider").slider({
	value : 0,
	min : 0,
	max : 6,
	step : 1,
	slide : function(e, ui) {
		global.evacPeakMins = ui.value * 30;
		// console.log('Peak time is ' + global.evacPeakMins);
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
	global.evacTime = new Date(00, 00, 00, e.time.hours, e.time.minutes, 00);
});

function setScenarioTitle(title) {
	$('#scenario-title').text(title);
}

// Saves the simulation config on the server
function save(callback, errfn) {
	var msg = {};
	// Get the township for this scenario
	var township = getTownship(global.scenario_creation_arg);
	// Scenario name (used for naming directories and files)
	msg.name = global.save_as;
	msg.township = township.name;
	// Global coordinate system used
	msg.coordinate_system = "latlong";
	// Road network area and associated MATSim Network file
	msg.osmArea = { // FIXME: network file hard-wired to maldon
			"rectangle" : township.osmArea, 
			"url" :  "app-data/maldon/network.xml"};
	// Fire shape file in Geo JSON format
	msg.fire = {
		    "name" : (typeof global.scenario_fire === 'undefined') ? "" : global.scenario_fire.name,
		    "url" : (typeof global.scenario_fire === 'undefined') ? "" : global.scenario_fire.url,
		    "coordinate_system" : "latlong",
		    "format" : "geojson"
		  };
	// Selected destinations and safe lines
	msg.destinations = [];
	msg.safeLines = [];
	for (var i = 0; i < township.safeLines.length; i++) {
		var line = township.safeLines[i].line.getPath().getArray();
		msg.safeLines.push({
			name: township.safeLines[i].name,
			coordinates: 
				[{lat: line[0].lat(), lng: line[0].lng()},
				 {lat: line[0].lat(), lng: line[0].lng()}]
		});
		var dest = getDestination(township, township.safeLines[i].name);
		if (dest != null) {
			msg.destinations.push({
				name: dest.name,
				coordinates: dest.coordinates
			});
		}
	}
	// Add all the vehicles areas
	msg.vehiclesAreas = [];
	for (var i = 0; i < township.vehiclesAreas.length; i++) {
		var sw = township.vehiclesAreas[0].area.getBounds().getSouthWest();
		var ne = township.vehiclesAreas[0].area.getBounds().getNorthEast();
		msg.vehiclesAreas.push({
			name: township.vehiclesAreas[i].name,
			bounds: {
				south: sw.lat(), west: sw.lng(),
				north: ne.lat(), east: ne.lng()}
		});
	}
	// Evacuation start time and departure peak (offset from evac start)
	msg.evacTime = global.evacTime;
	msg.evacPeakMins = global.evacPeakMins;
	// Max effective speed of cars as a percentage of speed limits
	msg.maxSpeed = global.maxSpeed;

	send(shared.MSG_SAVE, msg,
		function(obj) {
			var str = JSON.stringify(obj);
			var json = jQuery.parseJSON(str);
			if (json.msg == shared.MSG_ERROR) {
				if (errfn) return errfn(json.data[0].msg);
			} else {
				if (callback) return callback(json);
			}
		},
		function (str) {
			timedPrompt('error', "Could not create simulation. " + str); 
			$('.progress-bar-modal.in').modal('hide');
		}
	);

}

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
		error : function(req, error) {
			console.log("Save call to /api failed with error: " + error);
			if (errfn) return errfn(error);
		}
	});
}

// Shows the msg for a fixed amount of time in a standard info popup
// type: one of 'info', 'warn', or 'error'
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
        callback: cb
    });
}

function setEvacTime(evacTime) {
	if (evacTime) {
		$('#evac-timepicker').timepicker('setTime',
				evacTime.hh + ':' + evacTime.mm);
	} else {
		$('#evac-timepicker').timepicker('setTime', '');
	}
}

function setEvacPeak(mins) {
	var opt = $('#evac-timeslider').data().uiSlider.options;
	var index = 0;
	if (mins && mins > 0) {
		var size = opt.max - opt.min;
		var sizeMins = size * 30;
		index = (mins / sizeMins) * size;
	}
	$('#evac-timeslider').slider('value', index);
}

// ----------------------------------------------------------------------------
// VEHICLES
// ----------------------------------------------------------------------------

// Vehicles: handle changes to input field with number of vehicles
$("#num-vehicles").change(function() {
	var val = $("#num-vehicles").val();
	if (val > 0) {
		$('.draw-area-step-1').fadeIn('slow');
	} else {
		$('.draw-area-step-1').hide();
	}
});
// Vehicles: handle clicks on 'draw' button
$("#add-vehicles-area").click(function(event) {
	var township = getTownship(global.scenario_creation_arg);
	var label = $("#num-vehicles").val();
	// cancel any active draws first to be safe
	cancelDraw(true);
	if($('#add-vehicles-area').text()=='Cancel draw') {
		$('#add-vehicles-area').text('Draw area');
		return;
	}
	$('#add-vehicles-area').text('Cancel draw');
	drawVehiclesArea(township, label, function(area) {
		// Add the destination to the list
		addVehiclesArea(area);
		// Reset the count
		$("#num-vehicles").val('');
		$('#add-vehicles-area').text('Draw area');
		$('#plus-vehicles').show();
		$('.draw-area-step-1').hide();
		$('#num-vehicles').hide();
	});
});

$("#plus-vehicles").click(function(event) {
	$('#num-vehicles').show();
});


// Vehicles: add new entry (associated with a drawn rectangle on map) to list
function addVehiclesArea(aream2) {
	var hectares = Number(aream2 / 10000).toFixed(2);
	var km2 = Number(aream2 / 1000000).toFixed(2);
	
	var ncars = $("#num-vehicles").val();
	var li = '';
	var veh = 'vehicle';
	if (ncars > 1) {
		veh += 's';
	}
	li += '<li class="list-group-item item-vehicles-area"';
	li += ' name="' + ncars + '">';
	//li += ncars + ' ' + veh + ' in ' + km2 + 'km<sup>2</sup>';
	li += ncars + ' ' + veh + ' in ' + hectares + ' hectares';
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
	if($('#vehicles-list li').length==0) {
		$('#plus-vehicles').hide();
		$('#num-vehicles').show();
	}
	// Stop the event from propagating to parents
	e.stopPropagation();
});


//----------------------------------------------------------------------------
// Effective speed on roads
//----------------------------------------------------------------------------
$("#max-speed-slider").slider({
	value : 10,
	min : 1,
	max : 10,
	step : 1,
	slide : function(e, ui) {
		global.maxSpeed = ui.value * 10;
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
				if (i % 2 == 1) {
					el = $('<label>' + ((i + opt.min) * 10) + '%</label>').css(
							'left', (i / vals * 100) + '%');
				} else {
					el = $('<label>|</label>').css('left',
							(i / vals * 100) + '%');
				}
				// Add the element inside the slider
				$("#max-speed-slider").append(el);

			}

		});


