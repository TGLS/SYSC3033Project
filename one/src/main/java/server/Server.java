package server;

import java.util.Scanner;

public class Server {

	private String shutDown = "shutdown"; 
	private String verbose = "verbose";
	private Thread recieveThread;
	private Scanner s ;
	
	public Server(int sourcePort) {
		// This method will start the server receive thread and then proceed to 
		// Run the Server CLI: 
		
		//Initialize the control settings
		ServerControl.serverStop = false;
		ServerControl.verboseMode = false;
		
		//First start the server receive thread  
		recieveThread = new Thread(new ServerReceiveThread(sourcePort),"recieveThread");
		recieveThread.start(); 
		
		s = new Scanner(System.in);
		
		//Run The Servers Command Line interface
		serverInterface();
		//Shutdown after the interface closes
		shutdown();
		
	}
	
	private void serverInterface() {
		//This method runs the Servers Command Line interface 
		
		//Create a scanner to take input from the user
		
		
		//run the interface until the shutdown command is received
		while (true) {
		
			System.out.println("Please Enter Command >: ");
			String commandIn = s.nextLine();
			commandIn.replaceAll("\\s", "");
			
			//if shut down is entered, signal the shutdown.
			if(commandIn.equals(shutDown)) {
				ServerControl.serverStop = true;
				System.out.println("The Shut Down will now commence");
				break;
				
			//if verbose is entered, signal verbose mode.
			}else if(commandIn.equals(verbose)) {
				ServerControl.verboseMode = false;
				
			//Not a valid Command  
			}else {
				System.out.println("Not a Valid Command Please try again");
			}
	
			
		}
		
	}
	
	
	private void shutdown() {
		
		//close the scanner 
		s.close();
		
		//Wait for the receive thread to complete before exiting this 
		//ensures that all threads will be complete before we finish.
		try {
			recieveThread.join();
		} catch (InterruptedException e) {
			System.out.println("There was an error shutting down the threads");
			e.printStackTrace();
			System.exit(1);
		}
		
		//Notify the user of the completion
		System.out.println("All the threads have completed goodbye ... ");
		
		
		//terminate the program
		System.exit(0);
		
	}
	
	
}