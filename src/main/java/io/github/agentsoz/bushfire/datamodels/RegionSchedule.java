package io.github.agentsoz.bushfire.datamodels;

import io.github.agentsoz.bushfire.Config;

/*
 * #%L
 * BDI-ABM Integration Package
 * %%
 * Copyright (C) 2014 - 2015 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

public class RegionSchedule {
   
   //private Logger logger = Logger.getLogger( "Bushfire" );
   
   private String region;
   public  String getRegion() { return region; }
   public  Region getRegionRef() { return Config.getRegion( region ); }
   public  void   setRegion( String region ) { this.region = region; }
   
   private String       reliefCentre;
   public  String       getReliefCentre() { return reliefCentre; }
   public  ReliefCentre getReliefCentreRef() { return Config.getReliefCentre( reliefCentre ); }
   public  void         setReliefCentre( String reliefCentre ) { this.reliefCentre = reliefCentre; }
   
   private String route;
   public  String getRoute() { return route; }
   public  void   setRoute( String route ) { this.route = route; }
   
   private double evacTime;
   public  double getEvacTime() { return evacTime; }
   public  void   setEvacTime( double evacTime ) { this.evacTime = evacTime; }
   
   public boolean haveEvacuated = false;

   public RegionSchedule() {}
}