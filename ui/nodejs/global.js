module.exports = {	
		
	/*
	 * -----------------------------------------------------------------------
	 * SERVICE PORTS
	 * -----------------------------------------------------------------------
	 */
	SERVICE_ASSESS_PORT : 50001,

	/*
	 * -----------------------------------------------------------------------
	 * Function to record information to log
	 * -----------------------------------------------------------------------
	 */
	log : function(s) {
		console.log(new Date().toString() + ": " + s);
	},

	/*
	 * -----------------------------------------------------------------------
	 * Function to send back our reply as a JSON object
	 * -----------------------------------------------------------------------
	 */
	reply : function(response, obj, isJSON) {
	    // Send a JSON response after calculating the indicators
		response.writeHead(200, {"Content-Type": "application/json"});
		var jsonp = (isJSON === 0) ? obj : JSON.stringify(obj);
		response.write(jsonp);
		response.end();
		console.log(new Date().toString() + ": Sent response: "+jsonp);
	},
	
	/*
	 * -----------------------------------------------------------------------
	 * Returns a random number between min (inclusive) and max (exclusive)
	 * -----------------------------------------------------------------------
	 */
	getRandomArbitrary : function(min, max) {
	    return Math.random() * (max - min) + min;
	},

	/*
	 * -----------------------------------------------------------------------
	 * Returns a random integer between min (inclusive) and max (inclusive)
	 * Using Math.round() will give you a non-uniform distribution!
	 * -----------------------------------------------------------------------
	 */
	getRandomInt : function(min, max) {
	    return Math.floor(Math.random() * (max - min + 1)) + min;
	},
	
};
