package io.github.agentsoz.bushfire;

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

import io.github.agentsoz.bushfire.bdi.VisualiserResponseHolder;
import io.github.agentsoz.dataInterface.DataServer;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.agentsoz.bdimatsim.visualiser.VisualiserModule;
import io.github.agentsoz.bdimatsim.moduleInterface.data.SimpleMessage;

public class FireVisualiser extends VisualiserModule {
   
   public static boolean awaitingPromptResponse = false;
   public static int     promptResponse         = -1;
   private final Logger logger = LoggerFactory.getLogger("");

   public FireVisualiser(int portNumber) throws IOException {
   
      super( portNumber );
      DataServer.getServer( "Bushfire" ).subscribe( 
    		  this, 
    		  new String[] {
    				  DataTypes.LOCATION,
    				  DataTypes.ROUTE,
    				  DataTypes.REGION,
    				  DataTypes.MATSIM_AGENT_UPDATES,
    				  DataTypes.BDI_AGENT_UPDATES,
    				  DataTypes.FIRE,
    				  DataTypes.BDI_PLANS,
    				  DataTypes.BDI_TREE,
    				  DataTypes.BDI_TRACE,
    				  DataTypes.UI_PROMPT,
    				  DataTypes.EVAC_SCHEDULE,
    				  DataTypes.IMAGE,
    				  DataTypes.REGION_ASSIGNMENT});
   }
   
   @Override
   public void sendUpdates() {
      
      double        time = DataServer.getServer( "Bushfire" ).getTime();
      SimpleMessage msg  = new SimpleMessage();
      msg.name           = "time";
      msg.params         = new Object[] { time };
      addUpdate( msg );
      
      super.sendUpdates();
   }
   
   @Override
   public String receiveUpdates() {
      
      String message = super.receiveUpdates();
      if (message == null) {
          if (Config.getDieOnDisconnect()) {
             logger.info( "visualiser has disconnected, so will terminate (die_on_disconnect is enabled in config)");
             System.exit( 0 );
          }
      }
      // receive messages from Unity
      try {
         if (message.charAt( 0 ) == 'R') {
            
            if (awaitingPromptResponse) {  // simple response prompt

               switch ((int)message.charAt(1)) {
                  
                  case -1: case 1: break;
                  case  0: { /*PlanDecoder.controllerUserInput = true;*/ } break;
               }
               promptResponse         = -1;
               awaitingPromptResponse = false;
            }
            else {  // plan choice response
               VisualiserResponseHolder.userResponse = Integer.parseInt( message.substring( 1, 2 ) );
               if (message.length() > 2) { VisualiserResponseHolder.settingsResponse = message.substring( 2 ); }
            }
         }
      }
      catch (Exception e) {
         logger.warn("Unknown message from visualiser '"+message+"'");
      }
      return message;
   }
   
   // visualiser listens for facility and fire location updates
   // and adds them to the list of updates to send to Unity on the next timestep
   @SuppressWarnings("unchecked")
   @Override
   public boolean dataUpdate( double time, String dataType, Object data ) {
      
      switch (dataType) {
         
         case DataTypes.BDI_PLANS: {
            
            addUpdate( (SimpleMessage)data );
            return true;
         }
      
         case DataTypes.LOCATION:
         case DataTypes.ROUTE:
         case DataTypes.REGION:
         case DataTypes.EVAC_SCHEDULE:
         case DataTypes.BDI_TREE:
         case DataTypes.IMAGE: {

            SimpleMessage msg = new SimpleMessage();
            msg.name          = dataType;
            msg.params        = new Object[] { data };
            addUpdate( msg );
            return true;
         }
         case DataTypes.REGION_ASSIGNMENT: {

            SimpleMessage msg = new SimpleMessage();
            msg.name          = dataType;
            msg.agentID       = (String) ((Object[]) data)[ 0 ];
            msg.params        = new Object[] { (String) ((Object[]) data)[ 1 ] };
            addUpdate( msg );
            return true;
         }
      
         // unpack fire phases and locations, and add updates for individual fire locations
         case DataTypes.FIRE: {
         
            List< List< double[] > > firePhases;
            
            try {
               firePhases = (List< List< double[] > >)data;
   
               for (List< double[] > phase : firePhases) {
                  
                  SimpleMessage formatted = new SimpleMessage();
                  formatted.name          = FireModule.FIREPHASE;
                  StringBuilder sb        = new StringBuilder();
                  for (double[] coords : phase) { sb.append( coords[0] + "," + coords[1] + " " ); }
                  formatted.params        = new Object[] { sb.toString() };
                  addUpdate( formatted );
               }
               return true;
            }
            catch (ClassCastException e) {
               
               System.out.println("Fire data invalid");
               return false;
            }
         }
         default: {
            return super.dataUpdate( time, dataType, data );
         }
         
         case DataTypes.UI_PROMPT: {
            
            awaitingPromptResponse = true;
            addUpdate( (SimpleMessage)data );
            return true;
         }
      }
   }
}
