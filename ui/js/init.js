//
// GLOBALS
//

defaults = {};
defaults.safeLineStrokeColor = '#0000FF';
defaults.safeLineStrokeWeight = 3;
defaults.safeLineHighlightColor = '#FF00FF';
defaults.safeLineHighlightWeight = 4;

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
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Campbells Creek",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Fryerstown",
	latlon : [ -37.140291, 144.249982 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Harcourt",
	latlon : [ -37.004293, 144.257085 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Newstead",
	latlon : [ -37.117277, 144.059986 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Taradale",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : []
}, {
	name : "Vaughan",
	latlon : [ -37.004293, 144.257085 ], // FIXME: coords incorrect
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ], // FIXME: osm incorrect
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
