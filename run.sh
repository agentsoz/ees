
java -Xmx7000m -cp target/ees-2.0.2-SNAPSHOT.jar io.github.agentsoz.ees.Main \
--config ./scenarios/otways-50/scenario_main.xml \
--logfile ./scenarios/otways-50/scenario.log \
--loglevel INFO \
--seed 12345 \
--safeline-output-file-pattern "./scenarios/otways-50/safeline.%d%.out" \
--matsim-output-directory ./scenarios/otways-50/matsim_output \
--setup blockage \
--jillconfig "--config={ agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:50}], logLevel: INFO, logFile: \"./scenarios/otways-50/jill.log\", programOutputFile: \"./scenarios/otways-50/jill.out\", randomSeed: 12345}"
	
