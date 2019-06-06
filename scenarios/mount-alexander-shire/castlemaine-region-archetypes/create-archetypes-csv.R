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

# Fix any type naming
filter<-df$Archetype=="Dependent.Evacuators"
df[filter,]$Archetype<-"Dependent.Evacuator"

# Add BDI agent class
df$BDIAgentType<-""
for(archetype in unique(df$Archetype)) {
  filter<-df$Archetype==archetype
  type<-gsub("\\.", "", archetype)
  df[filter,]$BDIAgentType<-paste0("io.github.agentsoz.ees.agents.archetype.", type)
}

# Add any other attributes
df$EvacLocationPreference<-"Elphinstone,262869,5890813"
df$InvacLocationPreference<-"Castlemaine,252140,5894312"

# Assign Dependent Evacuators (randomly) as dependents to be picked up by others
# 1. get a list of dependents locations in random order
filter<-df$BDIAgentType=="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
locs<-sample(df[filter,]$Geographical.Coordinate)
# 2. Get people who have dependents and are not dependent themselves (to assign dependents to)
filter<-df$HasDependents==1 & df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
candidates<-which(filter)
# 3. Assign as many dependents as possible
len<-min(sum(filter), length(locs))
candidates<-candidates[1:len]
df$HasDependentsAtLocation<-""
df[candidates,]$HasDependentsAtLocation<-locs

# Remove all the dependent evacuators now (have been accounted for in pickups as best we can)
filter<-df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
df<-df[filter,]

# Write out the table
con <- gzfile('./population-archetypes.csv.gz')
write.csv(df, con, row.names=FALSE, quote=TRUE)
