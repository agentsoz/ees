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
	destinations : [ {
		name : "Castlemaine"
	}, {
		name : "Harcourt"
	}, {
		name : "Lockwood South"
	}, {
		name : "Newstead"
	} ]
},

{
	name : "Fryerstown",
	latlon : [ -37.140291, 144.249982 ],
	osmArea : [ -37.0615, 144.3840, -37.2019, 144.1547 ],
	defaultMapZoom : 11,
	fires : [],
	safeLines : [],
	destinations : []
} ];

global.existing_scenarios = [ {
	name : "Maldon-Bushfire-Jan-1944",
	township : "Maldon"
}, {
	name : "Maldon-Bushfire-Mar-1803",
	township : "Maldon"
}, {
	name : "Fryerstown-Bushfire-Jan-2017",
	township : "Fryerstown"
} ];

