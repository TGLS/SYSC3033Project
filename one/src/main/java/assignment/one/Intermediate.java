package assignment.one;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Intermediate {
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private DatagramSocket receiveSocket;
	private byte[] sendData;
	private InetAddress serverAddress;
	private int serverPort;
	private InetAddress clientAddress;
	private int clientPort;
	
	private final static int max_buffer = 120;
	
	public Intermediate(int sourcePort, int destinationPort) {
		this.serverPort = destinationPort;
		try {
			serverAddress = InetAddress.getLocalHost();
			sendReceiveSocket = new DatagramSocket();
		} catch (UnknownHostException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			System.exit(1);
		}
		
		// Create a DatagramSocket for reception with the port number you were given.
		// Surrounded with try-catch because creating a new socket might fail.
		try {
			receiveSocket = new DatagramSocket(sourcePort);
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}
	
	public void loop() {
		// Do the following until the user terminates the program from shell.
		while (true) {
			receiveRequest();
			printRequest();
			formRequest();
			reprintRequest();
			sendRequest();
			receiveResponse();
			printResponse();
			formResponse();
			reprintResponse();
			sendResponse();
		}
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
			receiveSocket.close();
			System.exit(1);
		}
	}
	
	private void reprintResponse() {
		// This one's simple.
		// We're going to print out the hex values of the four bytes in sendData.
		// No check sum here. It wouldn't be meaningful anyway.
		System.out.println("Response: ");
		System.out.print(String.format("%02X", sendData[0]));
		System.out.print(String.format("%02X", sendData[1]));
		System.out.print(String.format("%02X", sendData[2]));
		System.out.println(String.format("%02X", sendData[3]));
	}
	
	private void receiveResponse() {
		// In this function, we create a new packet to store and incoming response,
		// and store the incoming response.
		
		// Create a byte array for the incoming packet.
		receiveData = new byte[4];
		
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
	}
	
	private void printResponse() {
		// This one's simple.
		// We're going to print out the hex values of the four bytes in receiveData.
		// No check sum here. It wouldn't be meaningful anyway.
		System.out.println("Response: ");
		System.out.print(String.format("%02X", receiveData[0]));
		System.out.print(String.format("%02X", receiveData[1]));
		System.out.print(String.format("%02X", receiveData[2]));
		System.out.println(String.format("%02X", receiveData[3]));
	}
	
	private void formRequest() {
		// To create the request, we have the sendData point to the receiveData
		sendData = receiveData;
		
		// And now we build the packet
		sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), serverAddress,
				serverPort);
	}
	
	private void formResponse() {
		// To create the response, we have the sendData point to the receiveData
		sendData = receiveData;
		
		// And now we build the packet, with the address and port we retrieved earlier
		sendPacket = new DatagramPacket(sendData, receivePacket.getLength(), clientAddress,
				clientPort);
	}
	
	private void printRequest() {
		//Checksum is the sum of all the bytes, less overflows
		int checksum = 0;
		// First we convert the received data into strings.
		// While we're doing this, we'll check array index bounds by try-catch.
		try {
			// If the second byte is 1, we are reading.
			// If the second byte is 2, we are writing.
			// Anything else we say it's invalid.
			if (receiveData[1] == 1) {
				System.out.println("Read Request");
			} else if (receiveData[1] == 2) {
				System.out.println("Write Request");
			} else {
				System.out.println("Invalid Request");
			}
			
			// Next, we copy the byte array after the first two bytes
			byte[] buffer = new byte[receivePacket.getLength()]; 
			for (int n = 2; n < receivePacket.getLength(); n++) {
				buffer[n - 2] = receiveData[n];
			}
			
			// Now we convert the byte array to text, and split at null characters
			String rawText = new String(buffer, "UTF-8");
			String[] text = rawText.split("\u0000");
			
			// Then we print the file name and mode, with labels
			System.out.println("File Name: " + text[0]);
			System.out.println("Mode: " + text[1]);
			
		}  catch (ArrayIndexOutOfBoundsException e) {
			// If we get an array exception, the request is probably
			// has a blank file name somewhere, or is an invalid request.
			// We'll just make a note.
			System.out.println("Error reading request.");
    	} catch (UnsupportedEncodingException e) {
			// If we have an unsupported character, we'll just make a note.
    		// This doesn't necessarily mean the request is invalid.
    		System.out.println("Cannot render filename or buffer.");
		}
		
		// Then we print it as hex and compute a checksum.
		// This is accomplished by converting each number to hex,
		// then printing it. Line breaks every 40 numbers.
		for (int n = 0; n < receivePacket.getLength(); n++) {
			String s = String.format("%02X", receiveData[n]);
			if (n % 40 == 39) {
				System.out.println(s);
			} else if (n == receivePacket.getLength() - 1) {
				System.out.println(s);
			} else {
				System.out.print(s);
			}
			// Calculate the checksum while printing each byte
			checksum += receiveData[n];
			
			// Because we validate data after we print the data,
			// we'll check if we're over the max_buffer limit here,
			// to avoid going over array index limitations.
			if (n >= max_buffer) {
				break;
			}
		}
		// Print the checksum
		System.out.println("");
		System.out.println("Checksum: " + String.format("%08X", checksum));
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
		
		clientAddress = receivePacket.getAddress();
		clientPort = receivePacket.getPort();
	}
	
	private void reprintRequest() {
		//Checksum is the sum of all the bytes, less overflows
		int checksum = 0;
		// First we convert the sent data into strings.
		// While we're doing this, we'll check array index bounds by try-catch.
		try {
			// If the second byte is 1, we are reading.
			// If the second byte is 2, we are writing.
			// Anything else we say it's invalid.
			if (sendData[1] == 1) {
				System.out.println("Read Request");
			} else if (sendData[1] == 2) {
				System.out.println("Write Request");
			} else {
				System.out.println("Invalid Request");
			}
			
			// Next, we copy the byte array after the first two bytes
			byte[] buffer = new byte[sendPacket.getLength()]; 
			for (int n = 2; n < sendPacket.getLength(); n++) {
				buffer[n - 2] = sendData[n];
			}
			
			// Now we convert the byte array to text, and split at null characters
			String rawText = new String(buffer, "UTF-8");
			String[] text = rawText.split("\u0000");
			
			// Then we print the file name and mode, with labels
			System.out.println("File Name: " + text[0]);
			System.out.println("Mode: " + text[1]);
			
		}  catch (ArrayIndexOutOfBoundsException e) {
			// If we get an array exception, the request is probably
			// has a blank file name somewhere, or is an invalid request.
			// We'll just make a note.
			System.out.println("Error reading request.");
    	} catch (UnsupportedEncodingException e) {
			// If we have an unsupported character, we'll just make a note.
    		// This doesn't necessarily mean the request is invalid.
    		System.out.println("Cannot render filename or buffer.");
		}
		
		System.out.println("");
		
		// Then we print it as hex and compute a checksum.
		// This is accomplished by converting each number to hex,
		// then printing it. Line breaks every 40 numbers.
		for (int n = 0; n < sendPacket.getLength(); n++) {
			String s = String.format("%02X", sendData[n]);
			if (n % 40 == 39) {
				System.out.println(s);
			} else if (n == sendPacket.getLength() - 1) {
				System.out.println(s);
			} else {
				System.out.print(s);
			}
			// Calculate the checksum while printing each byte
			checksum += sendData[n];
			
			// Because we validate data after we print the data,
			// we'll check if we're over the max_buffer limit here,
			// to avoid going over array index limitations.
			if (n >= max_buffer) {
				break;
			}
		}
		// Print the checksum
		System.out.println("");
		System.out.println("Checksum: " + String.format("%08X", checksum));
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
}
