gz<-'../../../../archetypes-modelling/population/archetypes/Bendigo-SA4/Castlemaine/persons_archetypes_attributes.csv.gz'
con<-gzfile(gz,'rt')
persons1<-read.csv(con,header=T,sep=',',stringsAsFactors = F,strip.white = T)
close(con)

gz<-'../../../../archetypes-modelling/population/archetypes/Bendigo-SA4/Castlemaine Region/persons_archetypes_attributes.csv.gz'
con<-gzfile(gz,'rt')
persons2<-read.csv(con,header=T,sep=',',stringsAsFactors = F,strip.white = T)
close(con)


persons<-rbind(persons1,persons2)

df<-persons
df$BDIAgentType<-df$Archetype
for(archetype in unique(df$Archetype)) {
  filter<-df$BDIAgentType==archetype
  type<-gsub("\\.", "", archetype)
  df[filter,]$BDIAgentType<-paste0("io.github.agentsoz.ees.agents.archetype.", type)
}

# Fix any type naming
filter<-df$BDIAgentType=="io.github.agentsoz.ees.agents.archetype.DependentEvacuators"
df[filter,]$BDIAgentType<-"io.github.agentsoz.ees.agents.archetype.DependentEvacuator"

# Add any other attributes
df$EvacLocationPreference<-"Elphinstone,262869,5890813"

con <- gzfile('./population-archetypes.csv.gz')
write.csv(df, con, row.names=FALSE, quote=TRUE)
