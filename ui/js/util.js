// Appends the names to the array of dropdowns
// names: an array of structures, each with key name
function setOptionsForDropdowns(dropdowns, names) {
	for (var d = 0; d < dropdowns.length; d++) {
		var dropdown = dropdowns[d];
		dropdown.options.length = 0;
		var el = document.createElement("option");
		el.value = 'Select';
		el.innerHTML = 'Select';
		dropdown.appendChild(el);
		for (var i = 0; i < names.length; i++) {
			var opt = names[i].name;
			var el = document.createElement("option");
			el.value = opt;
			el.innerHTML = opt;
			dropdown.appendChild(el);
		}
	}
}
