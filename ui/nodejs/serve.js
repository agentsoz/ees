/*
 * -----------------------------------------------------------------------
 * Modules required
 * -----------------------------------------------------------------------
 */
var globals = require('./global');
var express = require('express')
var bodyParser = require('body-parser');


/*
 * -----------------------------------------------------------------------
 * START HERE
 * -----------------------------------------------------------------------
 */
var app = express()

app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true })); // support encoded bodies

app.get('/', function (req, res) {
  var user_id = req.param('id');
  var token = req.param('token');
  var geo = req.param('geo');  
  res.send('GET ' + user_id + ' ' + token + ' ' + geo);
})

// POST http://localhost:8080/api/users
// parameters sent with 
app.post('/', function(req, res) {
    var user_id = req.body.id;
    var token = req.body.token;
    var geo = req.body.geo;
    res.send('POST' + user_id + ' ' + token + ' ' + geo);
});

app.listen(globals.SERVICE_ASSESS_PORT, function () {
  console.log('Example app listening on port 3000!')
})
