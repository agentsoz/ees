// Google Maps Static API Key
GMAPS_STATIC_KEY='AIzaSyAEYAuIcELlK93kMgSCWOg3ppKONjHFQ6U';


// Globals
var urlParams;

// On window load
window.onload = function(e) {
	// Get the URL parameters
	getParams();

	send(shared.MSG_LIST_SCENARIOS, null,
		function(json) {
			if (json.msg == shared.MSG_ERROR) {
				timedPrompt('error', "Could not retrieve scenarios list. " + json.data[0].msg);
			} else {
				// Set the selection
				setOptionsForDropdowns(
					document.getElementsByClassName("dropdown-scenarios"),
					json.data);
				// Reset the page
				reset();
				// Select the simulation if specified in the URL
				if (urlParams.name) {
					$("#view-scenario-dropdown").val(urlParams.name);
					selectSimulation(urlParams.name);
				}
			}
		},
		function (str) {
			timedPrompt('error', "Could not retrieve scenarios list. " + str);
		}
	);
}

function getParams() {
	var match, pl = /\+/g, // Regex for replacing addition symbol with a space
	search = /([^&=]+)=?([^&]*)/g, decode = function(s) {
		return decodeURIComponent(s.replace(pl, " "));
	}, query = window.location.search.substring(1);

	urlParams = {};
	while (match = search.exec(query))
		urlParams[decode(match[1])] = decode(match[2]);
}

// Remove focus from buttons after click
$(".btn").mouseup(function() {
	$(this).blur();
})

// Print buttin functionality
$('#print-button').click(function() {
	window.print();
});

function reset() {
	// Set the title
	$('#scenario-title').text('Emergency Evacuation Simulator Results');
	// Hide parameters panel
	$("#scenario-params").hide();
	$("#scenario-graphs").hide();
	$(".scenario-view").hide();
	// Disable printing until a simulation is selected
	$('#print-button').prop('disabled', true);
	// Reset simulation selection
	$("#view-scenario-dropdown").val('Select');

}

// Handles the user selection for new scenario creation based on existing
$("#view-scenario-dropdown").on("change", function() {
	var scenario = $(this).find(':selected').text();
	if (scenario.localeCompare('Select') == 0) {
		reset();
		return;
	}
	selectSimulation(scenario);
});


function selectSimulation(scenarioName) {
	// Set the title
	$('#scenario-title').text(scenarioName);
	// Show scenario view elements
	$(".scenario-view").hide();
	$(".glyphicon-triangle-right").show();
	$(".glyphicon-triangle-bottom").hide();
	$(".scenario-view").fadeIn('slow');
	// Enable printing
	$('#print-button').prop('disabled', false);
	// Set the graphs menu
	var name = $("#view-scenario-dropdown option:selected").text();
	var scenario = getScenario(name);
	getScenarioFromServer(name,
		function(scenario) {
			// save the retrieved scenario
			global.existing_scenario_selected = scenario;
			$('.dropdown-graphs').selectpicker('destroy');
			setOptionsForGraphsSelection($(".dropdown-graphs"), scenario.data.graphs);
			$(".dropdown-graphs").selectpicker('refresh');
			// Set the scenario details
			setScenarioDetails(scenario);
			// Set the google earth link
			$('#playback-link').attr('href',scenario.data.playbackURL);
		},
		function(err) {
			timedPrompt('error', "Could not retrieve scenario '"+name+"'. " + err);
		}
	);
}

function setScenarioDetails(scenario) {
	var name = $("#view-scenario-dropdown option:selected").text();
	var township = getTownship(scenario.data.township);

	var url = 'https://maps.googleapis.com/maps/api/staticmap';
	url += '?size=640x350&scale=2&maptype=roadmap';
	//url += '&zoom=' + scenario.data.defaultMapZoom;
	url += '&zoom=' + (scenario.data.defaultMapZoom);
	//url += '&markers=color:blue|'+township.latlon[0]+','+township.latlon[1];
	url += '&center='+township.latlon[0]+','+township.latlon[1];
	url += '&key=' + GMAPS_STATIC_KEY;

	// draw OSM area
	var a = scenario.data.osmArea.rectangle;
	var bounds = '&path=color:red|weight:2';
	bounds += '|'+a[0]+','+a[1];
	bounds += '|'+a[2]+','+a[1];
	bounds += '|'+a[2]+','+a[3];
	bounds += '|'+a[0]+','+a[3];
	bounds += '|'+a[0]+','+a[1];
	//url += bounds;

	// draw safe lines
	for (var i = 0; i < scenario.data.safeLines.length; i++) {
		a = scenario.data.safeLines[i].coordinates;
		bounds = '&path=color:blue|weight:2';
		bounds += '|'+a[0].lat+','+a[0].lng;
		bounds += '|'+a[1].lat+','+a[1].lng;
		url += bounds;
	}

	// draw vehicles areas
	for (var i = 0; i < scenario.data.vehiclesAreas.length; i++) {
		a = scenario.data.vehiclesAreas[i].bounds;
		bounds = '&path=color:purple|weight:2';
		bounds += '|'+a.north+','+a.west;
		bounds += '|'+a.north+','+a.east;
		bounds += '|'+a.south+','+a.east;
		bounds += '|'+a.south+','+a.west;
		bounds += '|'+a.north+','+a.west;
		url += bounds;
	}

	url = encodeURI(url);

	var o = '<div id="scenario-data">';
	o += '<img class="img-responsive" src="'+url+'"/>'
	o += '<table class="table table-striped table-hover">';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Township: </label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(township.name, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Coordinates: </label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(township.latlon, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Road Network (OSM) Bounds:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(township.osmArea, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Census data:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(township.census_data, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Emergency incident:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.fire, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Evacuation destinations:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.destinations, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Evacuation start time:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.evacTime, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Evacuation peak (in mins after start):</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.evacPeakMins, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Maximum speed (as % of speed limit):</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.maxSpeed, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Safe lines:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.safeLines, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '<tr>';
	o += '<td class="col-xs-3"><label>Vehicles areas:</label></td>';
	o += '<td class="col-xs-9"><pre>'+JSON.stringify(scenario.data.vehiclesAreas, undefined, 2)+'</pre></td>';
	o += '</tr>';
	o += '</table>';
	o += '</div>';
	$('#scenario-details').find('#scenario-data').remove();
	$('#scenario-details').append(o);
}

$("body").on("change", ".dropdown-graphs", function(e) {
	var scenario = global.existing_scenario_selected;
	$(".graph-object").remove();
	$('.dropdown-graphs :selected').each(function(i, selected) {
		var sel = $(selected).text();
		var graph = getGraph(scenario.data, sel);
		var o = '<div class="graph-object col-sm-6 text-center">';
		//o += '<object width="100%" type="image/svg+xml" data="';
		//o += graph.url;
		//o += '"> Your browser does not support SVG </object>';
		o += '<img src="' + graph.url + '"/>'
		o += '</div>';
		$("#graphs").append(o);
	});
});

function jsonToHTMLTable(json) {
	var table = $('<table border=1>');
	var tblHeader = "<tr>";
	for ( var k in mydata[0])
		tblHeader += "<th>" + k + "</th>";
	tblHeader += "</tr>";
	$(tblHeader).appendTo(table);
	$.each(mydata, function(index, value) {
		var TableRow = "<tr>";
		$.each(value, function(key, val) {
			TableRow += "<td>" + val + "</td>";
		});
		TableRow += "</tr>";
		$(table).append(TableRow);
	});
	return ($(table));
};

function setOptionsForGraphsSelection(dropdowns, names) {
	for (var d = 0; d < dropdowns.length; d++) {
		var dropdown = dropdowns[d];
		if (dropdown.options)
			dropdown.options.length = 0;
		for (var i = 0; i < names.length; i++) {
			var opt = names[i].name;
			var el = document.createElement("option");
			el.value = opt;
			el.innerHTML = opt;
			dropdown.appendChild(el);
		}
	}
}

// function initMap() {
// var mapDiv = document.getElementById('map');
// global.map = new google.maps.Map(mapDiv, {
// center : {
// lat : -37.064558,
// lng : 144.218764
// },
// zoom : 6
// });
//
// // var kmlLayer = new google.maps.KmlLayer(
// // {
// // url : 'http://localhost/media/data/placemark.kml',
// // //url :
// 'http://googlemaps.github.io/kml-samples/kml/Placemark/placemark.kml',
// // //url : 'http://localhost/media/data/time_840.0.kml',
// // suppressInfoWindows : true,
// // map : global.map
// // });
//
// var myParser = new geoXML3.parser({map: global.map});
// myParser.parse('media/data/time_840.0.kml');
// setTimeout(function(){
// //if (myParser) myParser.hideDocument();
// myParser = new geoXML3.parser({map: global.map, zoom: false});
// myParser.parse('media/data/time_850.0.kml');
// }, 5000);
// }
