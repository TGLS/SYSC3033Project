package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.ArrayList;

public class Client {
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramSocket sendReceiveSocket;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	private InetAddress destinationAddress;
	private int destinationPort;
	private boolean verbose;
	private boolean firstContact; 
	
	private final static int max_buffer = 120;

	public Client(String destinationIP, int destinationPort, boolean verbose) {
		this.destinationPort = destinationPort;
		this.verbose = verbose;
		firstContact = true;
		try {
			destinationAddress = InetAddress.getByName(destinationIP);
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
	}
	
	public void loop() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
	        try {
	        	System.out.print("Enter file name: ");
				String fileName = br.readLine();
				System.out.print("Enter request type (read, write): ");
				String requestType = br.readLine();
				if (requestType.equals("read")) {
					send(true, fileName, "octet");
				} else if (requestType.equals("write")) {
					send(true, fileName, "octet");
				} else {
					System.out.println("Invalid request type.");
				}
			} catch (IOException e) {
				// Output an error and repeat.
				System.out.println("Input Error!");
			}
		}
	}
	
	public void send(boolean read, String fileName, String mode) {
		formRequest(read, fileName, mode);
		// No printing if it isn't verbose.
		if (verbose) {
			printRequest();
		}
		sendRequest();
		receiveResponse();
		
	
		// No printing if it isn't verbose.
		if (verbose) {
			printResponse();
		}
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
		if(firstContact) {
			destinationAddress = receivePacket.getAddress();
			destinationPort = receivePacket.getPort();
			firstContact = false;
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
	
	private void sendRequest() {
		// Here, we're going to create a new socket (the notional sendSocket)
		// Send the response packet, and then close the socket.
		try {
			if (verbose) {
				System.out.println("Trying to send a packet");
			}
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
			sendReceiveSocket.close();
			System.exit(1);
		}
	}
	
	private void formRequest(boolean read, String fileName, String mode) {
		// To create the request, we create an arrayList of bytes,
		// then add all the bytes to it. Then we convert it to a regular array,
		// then build a packet.
		ArrayList<Byte> buffer = new ArrayList<Byte>();
		byte[] stringBuffer;
		
		// All requests begin with a 0.
		buffer.add((byte) 0);
		
		// If the request is a read, the next byte's a 1,
		// otherwise it's a write, so the next byte's a 2
		if (read) {
			buffer.add((byte) 1);
		} else {
			buffer.add((byte) 2);
		}
		
		// Next we add the filename.
		stringBuffer = fileName.getBytes();
		for (byte b : stringBuffer) {
			buffer.add(b);
		}
		
		// Now we need a zero
		buffer.add((byte) 0);
		
		// Next we add the mode.
		stringBuffer = mode.getBytes();
		for (byte b : stringBuffer) {
			buffer.add(b);
		}
		
		// And the final zero
		buffer.add((byte) 0);
		
		// Now we make an array
		// We make an array of the same size as the buffer, and then we copy the old bytes over.
		// n is used to keep track of the index.
		sendData = new byte[buffer.size()];
		int n = 0;
		for (Byte b : buffer) {
			sendData[n] = b;
			n++;
		}
		
		
		// And now we build the packet
		sendPacket = new DatagramPacket(sendData, sendData.length, destinationAddress,
				destinationPort);
	}

	private void printRequest() {
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
}