/*
 * ---------------------------------------------------------------------------
 * Strings and constants common to both the JS clients and the NODEJS services
 * should be added here. 
 * Based on http://caolan.org/posts/writing_for_node_and_the_browser/
 * ---------------------------------------------------------------------------
 */
(function(exports){

	exports.MSG_OK = 'ok';
	exports.MSG_ERROR = 'error';
	exports.MSG_SAVE = 'save';
	exports.MSG_CREATE = 'create';
	exports.MSG_CREATE_PROGRESS = 'create_progress';

})(typeof exports === 'undefined'? this['shared']={}: exports);