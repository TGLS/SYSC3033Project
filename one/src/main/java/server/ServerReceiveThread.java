package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
//import java.util.logging.Logger;


public class ServerReceiveThread implements Runnable{
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private ArrayList<Thread> activeThreads;
	//private static Logger logger; 
	
	private final static int max_buffer = 120;
	
	public ServerReceiveThread(int sourcePort) {
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		try {
			receiveSocket = new DatagramSocket(sourcePort);
			receiveSocket.setSoTimeout(1000);
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		}
		//Initialize a list to keep track of all the active threads 		
		activeThreads = new ArrayList<Thread>();
		
	}
	
	public void run() {
		
		//run until we get the shutdown message
		while(!ServerControl.serverStop) {
			 //Wait to receive a request from the client. 
			receiveRequest();
		}
		
		//Shutdown after the signal is received; 
		shutdown();
		
	}
	
	private void receiveRequest() {
		// In this function, we create a new packet to store and incoming request,
		// and store the incoming request.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[max_buffer];
			
		// Create a packet for the incoming packet.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
			
		// Receive a message from the reception socket.
		// Surrounded with try-catch because receiving a message might fail.
		try {
			receiveSocket.receive(receivePacket);
			
			//Create a thread to deal with the received request.
			createThread();	
			
		} catch (IOException e) {
		
			// This should be non blocking we want the receive to reset so that we
			// can properly shutdown the server 
			
		}
	}
	
	private void createThread() {
		//This function creates and runs a thread to respond to the Client 
		Thread responseThread = new Thread(new ServerResponseThread(receivePacket),"repsonseThread");
		responseThread.start();
		activeThreads.add(responseThread);
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
