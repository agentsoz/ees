cd ./target/ees-2.0.2-SNAPSHOT/ && \
java -cp ./ees-2.0.2-SNAPSHOT.jar io.github.agentsoz.ees.Main \
        --config scenarios/surf-coast-shire/otways-300/scenario_main.xml \
        --logfile scenarios/surf-coast-shire/otways-300/scenario.log \
        --loglevel INFO \
        --seed 12345 \
        --safeline-output-file-pattern "scenarios/surf-coast-shire/otways-300/safeline.%d%.out" \
        --matsim-output-directory scenarios/surf-coast-shire/otways-300/matsim_output \
        --jillconfig "--config={ \
                agents:[{classname:io.github.agentsoz.ees.agents.Resident, args:null, count:300}], \
                logLevel: WARN, \
                logFile: \"scenarios/surf-coast-shire/otways-300/residents.log\", \
                programOutputFile: \"scenarios/surf-coast-shire/otways-300/residents.out\", \
                randomSeed: 12345 \
        }"
