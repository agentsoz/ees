//
// GLOBALS
//
var TIMEOUT = 30000; // timeout (ms) for server side services

defaults = {};
defaults.safeLineStrokeColor = '#0000FF';
defaults.safeLineStrokeWeight = 3;
defaults.safeLineHighlightColor = '#FF00FF';
defaults.safeLineHighlightWeight = 4;

global = {};
global.save_as = null;
global.townships = [ {
	name : "Maldon",
	latlon : [ -36.997609, 144.068722 ],
	osmArea : [ -36.8093, 144.2759, -37.0853, 143.9257 ],
	census_data : "According to the 2011 census, Maldon had 635 occupied private dwellings, with on average 1.7 motor vehicles per dwelling",
	defaultMapZoom : 10,
	fires : [ {
		name : "20160420_MtAlexShire_FDI75_Iso",
		url : "app-data/maldon/20160420_MtAlexShire_FDI75_Iso.json",
		description : "Forest Fire Danger Index (FFDI) 75 with south west wind change; based on 11:00 point fire ignition and running until 18:00"
	}, {
		name : "20160420_MtAlexShire_FDI50_Iso",
		url : "app-data/maldon/20160420_MtAlexShire_FDI50_Iso.json",
		description : "Forest Fire Danger Index (FFDI) 50 with south west wind change; based on 11:00 point fire ignition and running until 18:00"
	} ],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Castlemaine",
		coordinates : { lat: -37.064737, lng: 144.212304 }
	}, {
		name : "Harcourt",
		coordinates : { lat: -37.004293, lng: 144.257085 }
	}, {
		name : "Lockwood South",
		coordinates : { lat: -36.842702, lng: 144.155577 }
	}, {
		name : "Newstead",
		coordinates : { lat: -37.117277, lng: 144.059986 }
	} ]
}, {
	name : "Castlemaine",
	latlon : [ -37.064737, 144.212304 ],
	osmArea : [ -36.9754, 144.3614, -37.1579, 144.0452 ],
	census_data : "According to the 2011 census, Castlemaine had 2918 occupied private dwellings, with on average 1.5 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Campbells Creek",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	census_data : "According to the 2011 census, Campbells Creek had 526 occupied private dwellings, with on average 2 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Fryerstown",
	latlon : [ -37.140291, 144.249982 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	census_data : "According to the 2011 census, Fryerstown had 120 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Harcourt",
	latlon : [ -37.004293, 144.257085 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	census_data : "According to the 2011 census, Harcourt had 330 occupied private dwellings, with on average 2.3 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Newstead",
	latlon : [ -37.117277, 144.059986 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	census_data : "According to the 2011 census, Newstead had 344 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Taradale",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	census_data : "According to the 2011 census, Taradale had 188 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Vaughan",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	census_data : "According to the 2011 census, Vaughan had 120 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}
];

global.existing_scenarios = [ {
	name : "Maldon-Bushfire-Jan-1944",
	township : "Maldon",
	graphs : [ {
		name : "Castlemaine Safe Line",
		url : "./media/castlemaine-safe-line.svg"
	}, {
		name : "Harcourt Safe Line",
		url : "./media/harcourt-safe-line.svg"
	} ]
}, {
	name : "Maldon-Bushfire-Mar-1803",
	township : "Maldon",
	graphs : []
}, {
	name : "Fryerstown-Bushfire-Jan-2017",
	township : "Fryerstown",
	graphs : []
} ];

global.evacTime = {
	hh : 12,
	mm : 0
};
global.evacPeakMins = 60;

global.maxSpeed = 100;

// Help info goes here
info = [ {

} ];
