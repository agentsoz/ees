function initMap() {
	var mapDiv = document.getElementById('map');
	global.map = new google.maps.Map(mapDiv, {
		center : {
			lat : -37.064558,
			lng : 144.218764
		},
		zoom : 6
	});
}

function panMapTo(latlon, zoom) {
	var mapDiv = document.getElementById('map');
	global.map = new google.maps.Map(mapDiv, {
		center : {
			lat : -37.064558,
			lng : 144.218764
		},
		zoom : zoom
	});
	var latLng = new google.maps.LatLng(latlon[0], latlon[1]);
	global.map.setZoom(zoom);
	global.map.panTo(latLng);
}

function drawSimulationAreaOnMap(area) {
	var rectangle = new google.maps.Rectangle({
		strokeColor : '#FF0000',
		strokeOpacity : 0.5,
		strokeWeight : 2,
		fillColor : '#FF0000',
		fillOpacity : 0.03,
		zIndex : -1,
		clickable : false,
		map : global.map,
		bounds : {
			north : area[0],
			east : area[1],
			south : area[2],
			west : area[3]
		}
	});
}

function drawMarker(latlon, title) {
	var pos = new google.maps.LatLng(latlon[0], latlon[1]);
	var marker = new google.maps.Marker({
		position : pos,
		title : title
	});

	// To add the marker to the map, call setMap();
	marker.setMap(global.map);
}

function drawFire() {
	global.map.data.forEach(function(feature) {
		// If you want, check here for some constraints.
		global.map.data.remove(feature);
	});
	if (global.scenario_fire && $('#show-fire').is(":checked")) {
		// Now load the specified shape
		global.map.data.loadGeoJson(global.scenario_fire.url);
		global.map.data.setStyle({
			fillColor : 'red',
			strokeWeight : 1,
			strokeColor : 'red'
		});
	}
}

function drawSafeLine(safeLines, callback) {
	var firstClick = true;
	global.map.setOptions({
		draggableCursor : 'crosshair'
	});
	global.hClick = google.maps.event.addListener(global.map, 'click',
			function(e) {
				if (firstClick) {
					firstClick = false;
					global.poly = new google.maps.Polyline({
						map : global.map,
						clickable : false,
						zindex : 1,
						path : [],
						strokeColor : "#0000FF",
						strokeOpacity : 0.8,
						strokeWeight : 2
					});
					global.poly.getPath().setAt(0, e.latLng);
					global.map.setOptions({
						draggableCursor : 'crosshair'
					});

					global.hMove = google.maps.event.addListener(global.map,
							'mousemove', function(mouseMoveEvent) {
								global.poly.getPath().setAt(1,
										mouseMoveEvent.latLng);
							});
				} else {
					cancelDrawSafeLine();
					safeLines.push(global.poly);
					callback();
				}
			});
}

function cancelDrawSafeLine(removeActivePoly) {
	google.maps.event.removeListener(global.hMove);
	google.maps.event.removeListener(global.hClick);
	global.map.setOptions({
		draggableCursor : ''
	});
	if (removeActivePoly && global.poly) {
		global.poly.setMap(null);
	}
	global.poly = null;
}