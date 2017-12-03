

## An installation on Mac

### Prepare local http server

##### Install http server

There are probably many ways of doing this.  I (kn) did
```
sudo port install apache2
```

The commands
```
sudo port load apache2
sudo port unload apache2
sudo port reload apache2
```
then do what they should.  **Note that these commands go to launchctl, so once it is loaded it also survives showdowns and restarts.**  They also say that you should really use this command, and not apachectl or launchctl directly.

There is also a good howto at https://trac.macports.org/wiki/howto/Apache2 .  It does, however, have the root directoy wrong; for me it was `/opt/local/apache2` instead of whatever they say.

##### Find the http base directory

One can now type `localhost` into a browser and normally see a window that says "It works.".  Unfortunately, this can also just be in the cache, or come from some other http process already running on the machine.  Always opening a private browser window hedges against the first, but not the second.

I ended up finding the root in `/opt/local/apache2/htdocs`, which is different from what the above howto says.  In the end, I decided to put the stuff into my user directory; how to set this up is explained in the howto and that worked without problems; _except_ that the config file is in `/opt/local/apache2/conf`, other than the above howto says.  In Mac,
* this goes to `~/Sites/...`, and
* is accessed from the browser by `localhost/~<username>` .

##### Forward `/api/` to `localhost:50001`

Most of the website is static, and so it goes directly to the `js` or `nodejs` directories.  However, some of it is dynamically loaded, and for this it connects to a weblocation called `/api/` (in `util.js`), which needs to be rerouted to a (normally virtual) server.  This is what does this.

Two variants of the config for this:  Dhirendra had
```
server {
  listen 80 default_server;
  server_name default_server;
  root /var/www/html;
  index index.php index.html index.htm;
  location /api/ {
    proxy_pass      http://localhost:50001/;
    proxy_redirect off;
    proxy_set_header          Host            $host;
    proxy_set_header          X-Real-IP       $remote_addr;
    proxy_set_header          X-Forwarded-For $proxy_add_x_forwarded_for;
  }
}
```
For me (kn), this seemed to be the wrong file format, and we translated it into
```
<VirtualHost *:80>
    ServerAdmin webmaster@dummy-host.example.com
    DocumentRoot "/Users/kainagel/Sites/html"
    ServerName dummy-host.example.com
    ServerAlias www.dummy-host.example.com
    ErrorLog "logs/dummy-host.example.com-error_log"
    CustomLog "logs/dummy-host.example.com-access_log" common
    ProxyPass /api/ http://localhost:50001/
    ProxyPassReverse /api/ http://localhost:50001/
    ProxyPreserveHost On
</VirtualHost>
```
without knowing if this is minimal or even 100% correct.  This needed to go into `/opt/local/apache2/conf/extra/httpd-vhosts.conf`, the command
```
 /opt/local/apache2/bin/apachectl -t
 ```
 was useful to check the syntax, and
 ```
 sudo port reload apache2
 ```
 made this start. **Note that you need to find the correct version of `apachectl`, the above readme was wrong for me, and the default variant was the apple build-in variant which is also not the correct one.**

 This can be debugged from a javascript console in the browser.  As said, using a private browsing window helps avoiding problems caused by cache.

### Prepare simulation infrastructure

Dhrendra had
```
sudo yum install python-pip
sudo pip install --upgrade pip
sudo pip install utm
sudo yum install gdal gdal-devel gdal-python
sudo yum install R
# Then within 'sudo R'
#   install.packages("ggplot2")
```

I (kn) already had `pip` from macports and didn't want to install `yum`, so I did something like
```
sudo pip install utm
sudo port install gdal gdal py-gdal
```
and that seems to be sufficient.  You will only know after the next step.

### Run what serves the simulation to the website

I (kn) needed
```
sudo port install forever
```
Then
```
cd examples/bushfire
mvn clean install
cd target
unzip bushfire-2.0.2-SNAPSHOT-minimal.zip
cd ../ui
```
Here you first need to adapt the makefile.  I said
```
WEBDIR=~/Sites/html    # (*)
DATADIR=~/Sites/data
```
```
make
make run
```
(I think that we should change these instructions to something along the lines of
```
mvn clean install
mvn exec:java -Dexec.mainClass="..." -Dexec.vmargs="=Xmx8000m" -Dexec.args="..."
```
)


Then, it should say, in `~/.forever/serve.js.log`, something like
```
Fri Nov 10 2017 11:51:43 GMT+1100 (AEDT): Bushfire UI server is listening on port 50001
```

Now load `localhost/~<username>/html` in the (preferably private) browser, where the `html` comes from `(*)` above.

Things that can happen
* Emergency evacuation simulation window does not show up -- http server not running; or material not in right place for http server to find it
* Simulation cannot be saved -- this really should not happen, but if you think you can cheat and start directly from the index.html file, then this will happen.
* Simulation does not start -- you need to look into `~/.forever/serve.js.log` to find out the reason.  If a python script is not running, you can copy it into a terminal (at the right place in the file system), replace the commas by spaces, and run it directly.  I initially had, for example
```
Fri Nov 10 2017 12:08:49 GMT+1100 (AEDT): build_scenario.py -c,/Users/kainagel/Sites/data/user-data/2017-11-10-abc/2017-11-10-abc.json,-o,/Users/kainagel/Sites/data/user-data/2017-11-10-abc,-t,/Users/kainagel/Sites/data/bushfire-2.0.2-SNAPSHOT/scenarios/template,-d,/Users/kainagel/Sites/html,-n,scenario,-j,/Users/kainagel/Sites/data/bushfire-2.0.2-SNAPSHOT/bushfire-2.0.2-SNAPSHOT.jar,-v
Fri Nov 10 2017 12:08:49 GMT+1100 (AEDT): ERROR: Could not build simulation: Error: ImportError: No module named utm
Fri Nov 10 2017 12:08:49 GMT+1100 (AEDT): Sending reply {"msg":"error","data":"Could not build simulation: Error: ImportError: No module named utm"}
```
which meant that the utm python module was not there, and thus needed to be installed.

Currently, it has the problem (in the matsim log, the location if which can be found from the forever log)
```
java.lang.IllegalArgumentException: NetworkChangeEvent must contain at least one link.
```
We had this problem before and I have some idea what it is, so stopping here.
