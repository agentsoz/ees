package io.github.agentsoz.nonmatsim;

import io.github.agentsoz.dataInterface.DataClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

import com.google.gson.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualiserModule implements DataClient {

	final Logger logger = LoggerFactory.getLogger("");

	private   Socket                 visualiserPipe;
	private   PrintWriter            out;
	protected BufferedReader         in;
	protected boolean                disconnected = false;
	private   Queue< SimpleMessage > updates      = new LinkedList< SimpleMessage >();
	private   Gson                   gson         = new GsonBuilder().serializeNulls()
			.disableHtmlEscaping()
			.create();

	public static int response = 0;

	// add an update to be sent to Unity on the next timestep
	public void addUpdate( SimpleMessage message ) {

		updates.add( message );
	}

	public VisualiserModule(int portNumber) throws IOException {
		ServerSocket sl = new ServerSocket( portNumber );
		logger.info( "Listening for visualiser connection on port {}", portNumber);
		visualiserPipe = sl.accept();
		logger.info( "Accepted visualiser connection on port {}", portNumber);
		out = new PrintWriter( new OutputStreamWriter( visualiserPipe.getOutputStream() ), true );
		in  = new BufferedReader( new InputStreamReader( visualiserPipe.getInputStream() ) );
		sl.close();
	}

	public String receiveUpdates() {
		String message = null;
		// receive messages from Unity
		try {
			message = in.readLine();
			if (message != null) {
				logger.debug("Received visualiser update: {}", message);
			}
		}
		catch (IOException e) {

			logger.error("while receiving visualiser updates: {}", e.getMessage());
			disconnected = true;
		}
		return message;
	}

	// send all queued updates to Unity
	public void sendUpdates() {
		if (disconnected) return;
		String msg = "";
		while (updates.size() > 0) { 
			msg += gson.toJson(updates.poll()) + "\n";
		}
		msg += "End";
		logger.debug("Sending update to visualiser:\n{}", msg);
		send(msg);
		receiveUpdates();
		return;
	}

	public void send( String data ) {
		out.println( data );
	}

	// visualiser listens for MATSim agent action updates and sends them to Unity.
	// MATSim is responsible for timestepping the whole simulation, which Unity is synced to -
	// so on receipt of these updates, all queued data is sent to Unity in a batch
	@Override
	public boolean dataUpdate( double time, String dataType, Object data ) {
		try { 
			if (dataType == "matsim_agent_updates") {
				for (SimpleMessage update : (SimpleMessage[])data) { 
					addUpdate( update ); 
				}
				sendUpdates();
				return true;
			}
			if (dataType == "bdi_agent_updates") {
				addUpdate( (SimpleMessage)data );
				return true;
			}
			return false;
		}
		catch (ClassCastException e) { 
			return false; 
		}
	}
}
