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
		zoom : 6
	});
    var latLng = new google.maps.LatLng(latlon[0], latlon[1]);
	global.map.setZoom(zoom);
    global.map.panTo(latLng);
}