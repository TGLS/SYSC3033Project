package server;

import java.util.Scanner;

public class Server {
	private boolean running = true;
	
	public Server(int sourcePort) {
		// This method will start the server receive thread and then proceed to 
		// Run the Server CLI: 
		
		
		//First start the server 
		Thread recieveThread = new Thread(new ServerReceiveThread(sourcePort),"recieveThread");
		recieveThread.start(); 
		
		Scanner s = new Scanner(System.in);
		while (running) {
			// ************** THE CLI WILL GO HERE  **************
			
		
			System.out.println("Please Enter Command >: ");
			String commandIn = s.nextLine();
			
			System.out.println("You entered: " + commandIn);
	
		}
		s.close();
		
	}	
	
}