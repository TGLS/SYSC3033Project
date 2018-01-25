package assignment.one;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class Intermediate {
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket receiveSocket;
	
	private final static int max_buffer = 120;
	private int sourcePort;
	private int destinationPort;
	private String destinationIP;
	
	
	public Intermediate(int sourcePort, String destinationIP, int destinationPort) {
		this.sourcePort = sourcePort;
		this.destinationIP = destinationIP;
		this.destinationPort = destinationPort;
		
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		try {
			System.out.println("Intermediate: Waiting for Packet");
			receiveSocket = new DatagramSocket(sourcePort);
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void loop() {
		// Do the following until the user terminates the program from shell.
		while (true) {
			receiveRequest();
			createThread();
		}
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
		} catch (IOException e) {
			// Print a stack trace, close the socket, and exit.
			e.printStackTrace();
			receiveSocket.close();
			System.exit(1);
		}
		
	}
	
	
	private void createThread() {
		//This function creates a thread to deal with the interaction 
		//between the client and server
		Thread errorSimThread = new Thread(new ErrorSimulatorThread(receivePacket,sourcePort,destinationIP,destinationPort),"errorSimThread");
		errorSimThread.start();
	}
	
	
	

	
}