// Google Maps Static API Key 
GMAPS_STATIC_KEY='AIzaSyAEYAuIcELlK93kMgSCWOg3ppKONjHFQ6U';


// Globals
var urlParams;

// On window load
window.onload = function(e) {
	// Get the URL parameters
	getParams();

	// Set the selection
	setOptionsForDropdowns(document
			.getElementsByClassName("dropdown-scenarios"),
			global.existing_scenarios);

	// Reset the page
	reset();

	// Select the simulation if specified in the URL
	if (urlParams.name) {
		$("#view-scenario-dropdown").val(urlParams.name);
		selectSimulation(urlParams.name);
	}

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
	$('#scenario-title').text('Bushfire Evacuation Simulation Results');
	// Hide parameters panel
	$("#scenario-params").hide();
	$("#scenario-graphs").hide();
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

$(".collapse").on('show.bs.collapse', function(e) {
	$(e.target).parent().find(".glyphicon-triangle-right").hide();
	$(e.target).parent().find(".glyphicon-triangle-bottom").show();
});
$(".collapse").on('hide.bs.collapse', function(e) {
	$(e.target).parent().find(".glyphicon-triangle-right").show();
	$(e.target).parent().find(".glyphicon-triangle-bottom").hide();
});

function selectSimulation(scenario) {
	// Set the title
	$('#scenario-title').text(scenario);
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
	$('.dropdown-graphs').selectpicker('destroy');
	setOptionsForGraphsSelection($(".dropdown-graphs"), scenario.graphs);
	$(".dropdown-graphs").selectpicker('refresh');
	// Set the scenario details
	setScenarioDetails();
}

function setScenarioDetails() {
	var name = $("#view-scenario-dropdown option:selected").text();
	var scenario = getScenario(name);
	var township = getTownship(scenario.township);

	var url = 'https://maps.googleapis.com/maps/api/staticmap';
	url += '?size=500x350&scale=2&maptype=roadmap';
	url += '&zoom=' + township.defaultMapZoom;
	url += '&markers=color:blue|'+township.latlon[0]+','+township.latlon[1];
	url += '&key=' + GMAPS_STATIC_KEY;
	
	var a = township.osmArea;
	var bounds = '&path=color:red|weight:2';
	bounds += '|'+a[0]+','+a[1];
	bounds += '|'+a[2]+','+a[1];
	bounds += '|'+a[2]+','+a[3];
	bounds += '|'+a[0]+','+a[3];
	bounds += '|'+a[0]+','+a[1];
	url += bounds;

	url = encodeURI(url);
	
	var o = '<div id="scenario-data">';
	o += '<h3>'+township.name+'</h3>';
	o += '<div><label>Location: </label> '+township.latlon+'</div>';
	o += '<div><label>Road Network (OSM) Bounds: </label> '+township.osmArea+'</div>';
	o += '<img class="img-responsive" src="'+url+'"/>'
	o += '</div>';
	$('#scenario-details').find('#scenario-data').remove();
	$('#scenario-details').append(o);
}

$("body").on("change", ".dropdown-graphs", function(e) {
	var name = $("#view-scenario-dropdown option:selected").text();
	var scenario = getScenario(name);
	$(".graph-object").remove();
	$('.dropdown-graphs :selected').each(function(i, selected) {
		var sel = $(selected).text();
		var graph = getGraph(scenario, sel);
		var o = '<div class="graph-object col-sm-6 text-center">';
		o += '<object width="100%" type="image/svg+xml" data="';
		o += graph.url;
		o += '"> Your browser does not support SVG </object>';
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
