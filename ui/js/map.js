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
        strokeColor: '#FF0000',
        strokeOpacity: 0.5,
        strokeWeight: 2,
        fillColor: '#FF0000',
        fillOpacity: 0.05,
        map: global.map,
        bounds: {
          north: area[0],
          east: area[1],
          south: area[2],
          west: area[3]
        }
      });
}