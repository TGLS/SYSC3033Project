package intermediate;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;

public class ErrorSimRecieveThread implements Runnable{

	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket receiveSocket;
	
	private final static int max_buffer = 120;
	private int destinationPort;
	private String destinationIP;
	private ArrayList<Thread> activeThreads;
	
	
	public ErrorSimRecieveThread(int sourcePort, String destinationIP, int destinationPort) {
		this.destinationIP = destinationIP;
		this.destinationPort = destinationPort;
		
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		try {
			receiveSocket = new DatagramSocket(sourcePort);
			receiveSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			//non blocking to catch shutdown
		}
		//Initialize a list to keep track of all the active threads 	
		activeThreads = new ArrayList<Thread>();
	}
	
	
	
	public void run() {
		
		while (!IntermediateControl.IntermediateStop) {
			receiveRequest();
		}
	
		//Shutdown after the signal is received; 
		shutdown();
		
	}
	
	
	
	private void receiveRequest() {
		// In this function, we create a new packet to store and incoming request,
		// and store the incoming request. We also retreive the port and ip of the requester.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[max_buffer];
		
		// Create a packet for the incoming packet.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		
		// Receive a message from the reception socket.
		// Surrounded with try-catch because receiving a message might fail.
		try {
			receiveSocket.receive(receivePacket);
			
			//if recieved create a thread 
			createThread();
			
		} catch (IOException e) {
			// non blocking so that we can recieve shutdown
		}
		//Create a thread to deal with the received request.
		
	}
	
	private void createThread() {
		//This function creates a thread to deal with the interaction 
		//between the client and server
		Thread errorSimThread = new Thread(new ErrorSimulatorThread(receivePacket,destinationIP,destinationPort),"errorSimThread");
		errorSimThread.start();
		 activeThreads.add(errorSimThread);
		
	}
	
	private void shutdown() {
		// To properly shutdown the threads
		
		for(Thread curThread: activeThreads) {
		// wait for all thread to finish, then exit 
			try {
				curThread.join();
			} catch (InterruptedException e) {
				System.out.println("There was an error shutting down the threads");
				e.printStackTrace();
			}		
		}
	}
	
}
