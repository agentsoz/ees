//
// GLOBALS
//
var TIMEOUT = 30000; // timeout (ms) for server side services

var LATLNG_Castlemaine 		= [-37.064737, 144.212304];
var LATLNG_Chewton			= [-37.081686, 144.255568];
var LATLNG_Campbells_Creek	= [-37.093009, 144.201293];
var LATLNG_Elphingstone		= [-37.096682, 144.346803];
var LATLNG_Fryerstown		= [-37.140291, 144.249982];
var LATLNG_Guildford		= [-37.149457, 144.166034];
var LATLNG_Harcourt			= [-37.004293, 144.257085];
var LATLNG_Lockwood_South   = [-36.842702, 144.155577];
var LATLNG_Newstead 		= [-37.117277, 144.059986];
var LATLNG_Maldon			= [-36.997609, 144.068722]
var LATLNG_Malmsbury		= [-37.186710, 144.382265];
var LATLNG_Newstead 		= [-37.117277, 144.059986];
var LATLNG_Taradale 		= [-37.139258, 144.350221];
var LATLNG_Vaughan 			= [-37.158624, 144.214457];

defaults = {};
defaults.safeLineStrokeColor = '#0000FF';
defaults.safeLineStrokeWeight = 3;
defaults.safeLineHighlightColor = '#FF00FF';
defaults.safeLineHighlightWeight = 4;

global = {};
global.save_as = null;
global.townships = [ {
	name : "Castlemaine",
	latlon : LATLNG_Castlemaine,
	osmArea : [ -36.9754, 144.3614, -37.1579, 144.0452 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Castlemaine had 2918 occupied private dwellings, with on average 1.5 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Guildford",
		coordinates : { lat: LATLNG_Guildford[0], lng: LATLNG_Guildford[1] }
	}, {
		name : "Harcourt",
		coordinates : { lat: LATLNG_Harcourt[0], lng: LATLNG_Harcourt[1] }
	} ]
}, {
	name : "Campbells Creek",
	latlon : LATLNG_Campbells_Creek,
	osmArea : [ -36.9515, 144.4084, -37.2030, 144.0452 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Campbells Creek had 526 occupied private dwellings, with on average 2 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [{
		name : "Castlemaine",
		coordinates : { lat: LATLNG_Castlemaine[0], lng: LATLNG_Castlemaine[1] }
	}, {
		name : "Guildford",
		coordinates : { lat: LATLNG_Guildford[0], lng: LATLNG_Guildford[1] }
	} ]
}, {
	name : "Fryerstown",
	latlon : LATLNG_Fryerstown,
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Fryerstown had 120 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Chewton",
		coordinates : { lat: LATLNG_Chewton[0], lng: LATLNG_Chewton[1] }
	}, {
		name : "Guildford",
		coordinates : { lat: LATLNG_Guildford[0], lng: LATLNG_Guildford[1] }
	} ]
}, {
	name : "Harcourt",
	latlon : LATLNG_Harcourt,
	osmArea : [ -36.8988, 144.3452, -37.1108, 144.1564 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Harcourt had 330 occupied private dwellings, with on average 2.3 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Castlemaine",
		coordinates : { lat: LATLNG_Castlemaine[0], lng: LATLNG_Castlemaine[1] }
	}, {
		name : "Elphinstone",
		coordinates : { lat: LATLNG_Elphingstone[0], lng: LATLNG_Elphingstone[1] }
	} ]
}, {
	name : "Maldon",
	latlon : LATLNG_Maldon,
	osmArea : [ -36.8093, 144.2759, -37.0853, 143.9257 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Maldon had 635 occupied private dwellings, with on average 1.7 motor vehicles per dwelling",
	defaultMapZoom : 10,
	fires : [ {
		name : "20160420_MtAlexShire_FDI75_Iso",
		url : "app-data/mount-alexander-shire/maldon/20160420_MtAlexShire_FDI75_Iso.json",
		description : "Forest Fire Danger Index (FFDI) 75 with south west wind change; based on 11:00 point fire ignition and running until 18:00"
	}, {
		name : "20160420_MtAlexShire_FDI50_Iso",
		url : "app-data/mount-alexander-shire/maldon/20160420_MtAlexShire_FDI50_Iso.json",
		description : "Forest Fire Danger Index (FFDI) 50 with south west wind change; based on 11:00 point fire ignition and running until 18:00"
	} ],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Castlemaine",
		coordinates : { lat: LATLNG_Castlemaine[0], lng: LATLNG_Castlemaine[1] }
	}, {
		name : "Harcourt",
		coordinates : { lat: LATLNG_Harcourt[0], lng: LATLNG_Harcourt[1] }
	}, {
		name : "Lockwood South",
		coordinates : { lat: LATLNG_Lockwood_South[0], lng: LATLNG_Lockwood_South[1] }
	}, {
		name : "Newstead",
		coordinates : { lat: LATLNG_Newstead[0], lng: LATLNG_Newstead[1] }
	} ]
}, {
	name : "Newstead",
	latlon : LATLNG_Newstead,
	osmArea : [ -36.9696, 144.3425, -37.1860, 143.9463 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Newstead had 344 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Castlemaine",
		coordinates : { lat: LATLNG_Castlemaine[0], lng: LATLNG_Castlemaine[1] }
	}, {
		name : "Guildford",
		coordinates : { lat: LATLNG_Guildford[0], lng: LATLNG_Guildford[1] }
	}, {
		name : "Maldon",
		coordinates : { lat: LATLNG_Maldon[0], lng: LATLNG_Maldon[1] }
	}]
}, {
	name : "Taradale",
	latlon : LATLNG_Taradale,
	osmArea : [ -36.9919, 144.4771, -37.2571, 144.1530 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Taradale had 188 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Malmsbury",
		coordinates : { lat: LATLNG_Malmsbury[0], lng: LATLNG_Malmsbury[1] }
	}, {
		name : "Elphinstone",
		coordinates : { lat: LATLNG_Elphingstone[0], lng: LATLNG_Elphingstone[1] }
	} ]
}, {
	name : "Vaughan",
	latlon : LATLNG_Vaughan,
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	roadNetwork : "app-data/mount-alexander-shire/network.xml.gz", 
	census_data : "According to the 2011 census, Vaughan had 120 occupied private dwellings, with on average 1.9 motor vehicles per dwelling",
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	vehiclesAreas : [],
	destinations : [ {
		name : "Chewton",
		coordinates : { lat: LATLNG_Chewton[0], lng: LATLNG_Chewton[1] }
	}, {
		name : "Guildford",
		coordinates : { lat: LATLNG_Guildford[0], lng: LATLNG_Guildford[1] }
	} ]
}
];

global.existing_scenarios = [];

global.evacTime = {
	hh : 12,
	mm : 0
};
global.evacPeakMins = 60;

global.maxSpeed = 100;

// Help info goes here
info = [ {

} ];
