package intermediate;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import apps.TFTPCommons;

/**
 * This class has one function:
 * Forward packets from server to client and vice versa.
 */

public class ErrorSimulatorThread implements Runnable{
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	private InetAddress serverAddress;
	private int serverPort;
	private InetAddress clientAddress;
	private int clientPort;
	private boolean firstContact = true; 
	
	
	public ErrorSimulatorThread(DatagramPacket receivePacket, String destinationIP, int destinationPort) {
	
		try {
			sendReceiveSocket = new DatagramSocket();
			serverAddress = InetAddress.getByName(destinationIP);
		} catch (SocketException e) {
		// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		clientAddress = receivePacket.getAddress();
		this.receivePacket = receivePacket;
		this.receiveData = receivePacket.getData();
		clientPort = receivePacket.getPort();
		serverPort = destinationPort;
		
	}
	
	public void run() {
		
		while(true) {
			if(IntermediateControl.verboseMode) {
				printRequest();
			}
			formRequest();
			
			if(IntermediateControl.verboseMode) {
				reprintRequest();
			}
			sendRequest();
			receiveResponse();
			if(IntermediateControl.verboseMode) {
				printResponse();
			}
			formResponse();
			if(IntermediateControl.verboseMode) {
				reprintResponse();
			}
			sendResponse();
			
			receiveRequest();
		}
	}

	
	private void printRequest() {
		TFTPCommons.printMessage(false, receiveData, receivePacket.getLength());
	}
	
	
	private void formRequest() {
		// To create the request, we have the sendData point to the receiveData
		sendData = receiveData;
		
		// And now we build the packet
		sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), serverAddress,
				serverPort);
	}
	
	
	private void reprintRequest() {
		TFTPCommons.printMessage(true, sendData, sendPacket.getLength());
	}
	
	
	private void sendRequest() {
		// Here, we're going to create a new socket (the notional sendSocket)
		// Send the response packet, and then close the socket.
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}
	
	
	private void receiveResponse() {
		// In this function, we create a new packet to store and incoming response,
		// and store the incoming response.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[TFTPCommons.max_buffer];
		
		// Create a packet for the incoming packet.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		
		// Receive a message from the reception socket.
		// Surrounded with try-catch because receiving a message might fail.
		try {
			sendReceiveSocket.receive(receivePacket);
		} catch (IOException e) {
			// Print a stack trace, close the socket, and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
		if(firstContact) {
			serverAddress = receivePacket.getAddress();
			serverPort = receivePacket.getPort();
			firstContact = false;
		}
		
	}
	
	
	private void printResponse() {
		TFTPCommons.printMessage(false, receiveData, receivePacket.getLength());
	}
	
	
	private void formResponse() {
		// To create the response, we have the sendData point to the receiveData
		sendData = receiveData;
		
		// And now we build the packet, with the address and port we retrieved earlier
		sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), clientAddress,
				clientPort);
	}
	
	
	private void reprintResponse() {
		TFTPCommons.printMessage(true, sendData, sendPacket.getLength());
	}
	
	private void sendResponse() {
		// Here, we're going to use the sendReceiveSocket to send the 
		// response packet, and then close the socket.
		// If I were not bound by the specification, I'd create a new socket,
		// like the server does, so a rogue client couldn't attempt to disrupt
		// intermediate/server operations.
		
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}

	private void receiveRequest() {
		// In this function, we create a new packet to store and incoming request,
		// and store the incoming request. We also retrieve the port and IP of the requester.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[TFTPCommons.max_buffer];
		
		// Create a packet for the incoming packet.
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
		
		// Receive a message from the reception socket.
		// Surrounded with try-catch because receiving a message might fail.
		try {
			sendReceiveSocket.receive(receivePacket);
			
			//if received create a thread 	
			
		} catch(IOException ste) {
			ste.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
			
		}
		
		
	}	
}
