SCENARIO=mount-alexander-shire/bradford-newstead-fire-crossing-400
java -Xmx7000m -cp target/ees-2.0.2-SNAPSHOT.jar io.github.agentsoz.ees.Main \
--config ./scenarios/$SCENARIO/scenario_main.xml \
--logfile ./scenarios/$SCENARIO/scenario.log \
--loglevel INFO \
--seed 12345 \
--safeline-output-file-pattern "./scenarios/$SCENARIO/safeline.%d%.out" \
--matsim-output-directory ./scenarios/$SCENARIO/matsim_output \
--jillconfig "--config={ agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:140}], logLevel: INFO, logFile: \"./scenarios/$SCENARIO/jill.log\", programOutputFile: \"./scenarios/$SCENARIO/jill.out\", randomSeed: 12345}"
	
