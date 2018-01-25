package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


public class ServerReceiveThread implements Runnable{
	private DatagramSocket receiveSocket;
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	
	private final static int max_buffer = 120;
	
	public ServerReceiveThread(int sourcePort) {
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		try {
			receiveSocket = new DatagramSocket(sourcePort);
		} catch (SocketException e) {
		// Print a stack trace and exit.
		e.printStackTrace();
		System.exit(1);
		}
	}
	
	public void run() {
		
		
		while(true) {
			 //Wait to receive a request from the client 
			receiveRequest();
			//Create a thread to deal with the request
			createThread();	
	
		}
		
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
		} catch (IOException e) {
			// Print a stack trace, close the socket, and exit.
			e.printStackTrace();
			receiveSocket.close();
			System.exit(1);
		}
	}
	
	private void createThread() {
		//This function creates and runs a thread to respond to the Client 
		
		Thread repsonseThread = new Thread(new ServerResponseThread(receivePacket),"repsonseThread");
		repsonseThread.start();
	}
	

}
