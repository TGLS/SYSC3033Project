package server;

public class ServerControl {
	//This Class will be used to send a signal from the CLI 
	//To the server receive thread to signal a stop
	//any control settings that we need to transit can be 
	// put into this class 
	
	static boolean serverStop = false;
	static boolean verboseMode = false; 

	public ServerControl(){}
	
}
