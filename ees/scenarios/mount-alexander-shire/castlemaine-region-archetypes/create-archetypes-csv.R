getRandomCoordsWithinRadius <- function (latslons, max_radius) {
  lats<-latslons[,1]
  lons<-latslons[,2]
  r <- sqrt(runif(1))*max_radius
  t <- 2*pi*runif(1)*max_radius
  dx <- r*cos(t); dy <- r*sin(t)
  EarthRadius<-6371 # km
  OneDegree<-EarthRadius * 2 * pi / 360 * 1000 # 1° latitude in meters
  random_lats <- lats + dy / OneDegree
  random_lons <- lons + dx / ( OneDegree * cos(lats * pi / 180) )
  cbind(random_lats,random_lons)
}

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

# Remove all Unknown types for now
filter<-df$Archetype!="Unknown.Type"
df<-df[filter,]

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
# 0. For Experienced Independents remove dependents (responsibility of other household members typically)
filter<-df$BDIAgentType=="io.github.agentsoz.ees.agents.archetype.ExperiencedIndependent"
df[filter,]$HasDependents<-0
# 1. get a list of dependents locations in random order
filter<-df$BDIAgentType=="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
locs<-sample(df[filter,]$Geographical.Coordinate)
# 2. Get people who have dependents and are not dependent themselves (to assign dependents to);
#    other than ExperiencedIndependent who should not have dependents
filter<-df$HasDependents==1 & df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.DependentEvacuator" & df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.ExperiencedIndependent"
candidates<-which(filter)
# 3. Assign as many dependents as possible
len<-min(sum(filter), length(locs))
candidates<-candidates[1:len]
df$HasDependentsAtLocation<-""
df[candidates,]$HasDependentsAtLocation<-locs
# 4. Assign random locations to remaining archetypes with dependents
filter<-df$HasDependents==1 & df$HasDependentsAtLocation==""
latslons<-as.numeric(unlist(strsplit(gsub('\\[|\\]| ','',sample(df[filter,]$Geographical.Coordinate)),",")))
latslons<-cbind(latslons[seq(1, length(latslons), 2)], latslons[seq(2, length(latslons), 2)])
rand_dests<-getRandomCoordsWithinRadius(latslons,5000)
df[filter,]$HasDependentsAtLocation<-sprintf("[%f,%f]", rand_dests[,1], rand_dests[,2])

# Remove all the dependent evacuators now (have been accounted for in pickups as best we can)
filter<-df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
df<-df[filter,]

# Ensure that the initial threshold is never greater than the final threshold
filter<-df$ResponseThresholdInitial > df$ResponseThresholdFinal
df[filter,]$ResponseThresholdFinal<-df[filter,]$ResponseThresholdInitial

# Change logicals to Java boolean
filter<-df$WillGoHomeAfterVisitingDependents==1
df[filter,]$WillGoHomeAfterVisitingDependents<-"true"
filter<-df$WillGoHomeAfterVisitingDependents==0
df[filter,]$WillGoHomeAfterVisitingDependents<-"false"
filter<-df$WillGoHomeBeforeLeaving==1
df[filter,]$WillGoHomeBeforeLeaving<-"true"
filter<-df$WillGoHomeBeforeLeaving==0
df[filter,]$WillGoHomeBeforeLeaving<-"false"
filter<-df$WillStay==1
df[filter,]$WillStay<-"true"
filter<-df$WillStay==0
df[filter,]$WillStay<-"false"
filter<-df$HasDependents==1
df[filter,]$HasDependents<-"true"
filter<-df$HasDependents==0
df[filter,]$HasDependents<-"false"

# Reorder by archetypes, needed for correct assignment of Jill BDI agents types
df<-df[order(df$BDIAgentType),]

# Assign ID and put it as first column
df$Id<-seq(0,nrow(df)-1)
df<-df[,c(ncol(df),1:(ncol(df)-1))]

# Write out the table
con <- gzfile('./population-archetypes.csv.gz')
write.csv(df, con, row.names=FALSE, quote=TRUE)
