assign_archetypes_attributes <- function(pp) {
  # Assign the attributes based on the read probabilities
  # Here attribute names have special meaning:
  # <name>.Logical is treated as the probability of a TRUE value
  # <name>.Numeric is treated as the mean, with an assumed sigma=0.1
  # All assinged values are clipped to the interval (0.0,1.0).
  # The '.Logical' and '.Numeric' prefix are removed before writing out.
  
  # Read the probabilities to assign
  probsfile<-'../../../../archetypes-modelling/data/archetypes-attributes.csv'
  probs_orig<-read.csv(probsfile,header = T, sep=',', stringsAsFactors=F, strip.white = T, row.names=1)
  archetypes<-colnames(probs_orig)
  attributes<-rownames(probs_orig)
  filterLogical<-grepl(".Logical",attributes)
  filterNumeric<-grepl(".Numeric",attributes)
  filterLiteral<-grepl(".Literal",attributes)
  
  df<-pp
  df[,attributes]<-0
  
  for (archetype in archetypes) {
    filter<-pp$Archetype==archetype 
    len<-sum(filter)
    probs<-probs_orig[attributes,archetype]
    vals<-matrix(rep(0,len*length(attributes)), ncol=length(attributes), nrow=len); colnames(vals)<-attributes
    
    # assign all logical attributes
    lvals<-t(matrix(rep(probs[filterLogical],len), nrow=sum(filterLogical), ncol=len))
    lvals<-apply(lvals,2,function(x) {rbinom(length(x),1,x[1])})
    vals[,filterLogical]<-lvals
    
    # assign all numerical attributes
    nvals<-t(matrix(rep(probs[filterNumeric],len), nrow=sum(filterNumeric), ncol=len))
    nvals<-apply(nvals,2,function(x) {rnorm(length(x),mean=x,sd=ifelse(x<1,0.1,x*0.2))})
    nvals<-round(nvals,digits=2)
    vals[,filterNumeric]<-nvals
    
    # assign all literal attributes
    cvals<-t(matrix(rep(probs[filterLiteral],len), nrow=sum(filterLiteral), ncol=len))
    cvals<-apply(cvals,2,function(x) {x})
    vals[,filterLiteral]<-cvals
    
    
    # replace the archetype cell values with the calculated ones
    df[filter,attributes]<-vals
  }
  
  #remove the .Logical and .Numeric prefix from the colnames
  cnames<-colnames(df)
  cnames<-gsub(".Logical","",cnames)
  cnames<-gsub(".Numeric","",cnames)
  cnames<-gsub(".Literal","",cnames)
  colnames(df)<-cnames
  pp<-df
  
  # Ensure that the initial threshold is never greater than the final threshold
  filter<-df$ResponseThresholdInitial > df$ResponseThresholdFinal
  df[filter,]$ResponseThresholdFinal<-df[filter,]$ResponseThresholdInitial
  
  return(pp)
}

assign_home_coordinates<-function(pp, popn, addresses) {
  # assign the home coordinates by randomly sampling (with replacement) from the addresses in the towns
  dd<-addresses[0,]
  for (i in seq(1,nrow(popn))) {
    town<-popn[i,"Town"]
    filter<-addresses$LOCALITY==town
    df<-addresses[filter,] # get the town's addresses
    df <- df[sample(1:nrow(df),replace=TRUE,popn[i,"CarsMobilised"]),]
    dd<-rbind(dd, df)
  }
  pp$EZI_ADD<-dd$EZI_ADD
  pp$Geographical.Coordinate<-dd$COORDINATES
  return(pp)
}

assign_archetypes<-function(pp) {
  # assign the archetypes using the same distribution as strahan's original paper
  # The 23% Unknonw types are not included in the vector below.
  # Those will therefore be assigned one of the other archetypes in the realtive proportion.
  archetypes<-c("Considered.Evacuator", "Community.Guided", "Threat.Denier", "Worried.Waverer", "Responsibility.Denier", "Dependent.Evacuator", "Experienced.Independent")
  archetypes_dist<-c(11.9,13,9.8,9.3,9.4,6.5,17.1) # archetyipal makeup in strahan's paper
  archetypesClass<-paste0("io.github.agentsoz.ees.agents.archetype.", gsub("\\.", "", archetypes))
  pp$Archetype<-NULL; pp$BDIAgentType<-NULL
  probs<-archetypes_dist
  probs<-probs/sum(probs) # normalise to 1
  probs<-cumsum(probs) # cumulative sum to 1
  for (i in seq(1,nrow(pp))) {
    roll<-runif(1)
    select<-match(TRUE,probs>roll) # pick the first col that is higher than the dice roll
    pp[i,"Archetype"]<-archetypes[select]
    pp[i,"BDIAgentType"]<-archetypesClass[select]
  }
  # Reorder by archetypes, needed for correct assignment of Jill BDI agents types
  pp<-pp[order(pp$BDIAgentType),]
  return(pp)
}

load_street_addresses<-function() {
  gz<-'../../../../ees-data/yarra-cardinia-bawbaw-shires/vicmap-addresses/yarra-cardinia-bawbaw-addresses.csv.gz'
  con<-gzfile(gz,'rt')
  df<-read.csv(con,header=T,sep='|',stringsAsFactors = F,strip.white = T)
  close(con)
  filter<-df$LOCALITY=="POWELLTOWN" | df$LOCALITY=="THREE BRIDGES" | df$LOCALITY=="GLADYSDALE" | 
    df$LOCALITY=="YARRA JUNCTION" | df$LOCALITY=="GENTLE ANNIE"
  df<-df[filter,]
  return(df)
}

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

reassign_dependant_evacuators<-function(pp) {
  df<-pp
  # remove the dependent evacuators and instead assign their coordinates randomly to others (other than EIs)
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
  rand_dests<-getRandomCoordsWithinRadius(latslons,2000) # somewhere within a few kms
  df[filter,]$HasDependentsAtLocation<-sprintf("[%f,%f]", rand_dests[,1], rand_dests[,2])
  
  # Remove all the dependent evacuators now (have been accounted for in pickups as best we can)
  filter<-df$BDIAgentType!="io.github.agentsoz.ees.agents.archetype.DependentEvacuator"
  df<-df[filter,]
  return(df)
}

logicals_to_java_boolean<-function(df) {
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
  return(df)
}

assign_ids<-function(df) {
  # Assign ID and put it as first column
  df$Id<-seq(0,nrow(df)-1)
  df<-df[,c(ncol(df),1:(ncol(df)-1))]
  return(df)
}

assign_evacuation_locations <- function(df) {
  evac_locs<-c("Little Yarra CFA Station,381640,5812770", "Yarra Junction,377890,5817590") # evac destinations
  split<-c(0.8, 0.2) # split of evac destinations
  df<-pp
  count<-nrow(df)
  split1<-round(count * split[1]) 
  split<-c(split1, count - split1)
  locs<-c(rep(evac_locs[1],split[1]), rep(evac_locs[2],split[2])) # create all the destination instances
  locs<-sample(locs,length(locs),replace = FALSE) # randomly sample all (shuffle)
  df$EvacLocationPreference<-locs
  df$InvacLocationPreference<-"Powelltown Oval,389980,5808490"
  return(df)
}

# STARTS HERE

# Scenario variables
popn<-data.frame(matrix(0, ncol = 0, nrow = 3))
popn$Town<-c('POWELLTOWN', 'THREE BRIDGES', 'GLADYSDALE')
popn$Persons<-c(217,199,444) # Dwellings (ABS)
popn$Cars<-c(round(96*1.9), round(80*2.5), round(170*2.6)) # Dwellings x Avg. Motor Vehicles per Dwelling (ABS)
popn$CarsMobilised<-round(popn$Cars * 0.8) # 80% of all vehicles that will be in use

# Load the addresses for the relevant suburbs
addresses<-load_street_addresses()

# Create the population (of size = CarsMobilised)
pp<-data.frame(matrix(0, ncol = 0, nrow = sum(popn$CarsMobilised)))
pp<-assign_home_coordinates(pp, popn, addresses)
pp<-assign_archetypes(pp)
pp<-assign_archetypes_attributes(pp)
pp<-reassign_dependant_evacuators(pp)
pp<-assign_evacuation_locations(pp)
pp<-logicals_to_java_boolean(pp)
pp<-assign_ids(pp)

# Write out the table
#con <- gzfile('./population-archetypes.csv.gz')
con <- c('./population-archetypes.csv')
write.csv(pp, con, row.names=FALSE, quote=TRUE)
