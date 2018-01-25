package assignment.one;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ClientConnectionThread implements Runnable {
	private DatagramSocket sendSocket;
	private DatagramPacket receivePacket;
	private byte[] receiveData;
	private DatagramPacket sendPacket;
	private byte[] sendData;
	
	boolean portAvailable = true;
	
	private final static int max_buffer = 120;

	
	public ClientConnectionThread(DatagramPacket receivePacket) {
		this.receivePacket = receivePacket;
		this.receiveData = receivePacket.getData();
	}
	
	public void run() {
		printRequest();
		if (!validateRequest()) {
			System.out.println("Invalid Message!");
			System.exit(1);
		}
		formResponse();
		printResponse();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sendResponse();
		
	}
	
	private void formResponse() {
		// The only important information when responding is the second byte
		// (first index). If it is 1, then we return the read signal (0,3,0,1).
		// If it is 2, then we return the write signal (0,4,0,0).
		// At this point, we've already validated the received data,
		// So we won't bother checking for out of bounds values.
		sendData = new byte[4];
		if (receiveData[1] == 1) {
			sendData[0] = 0;
			sendData[1] = 3;
			sendData[2] = 0;
			sendData[3] = 1;
		} else {
			sendData[0] = 0;
			sendData[1] = 4;
			sendData[2] = 0;
			sendData[3] = 0;
		}
		
		// Now that the data has been set up, let's form the packet.
		sendPacket = new DatagramPacket(sendData, 4, receivePacket.getAddress(),
				receivePacket.getPort());	
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
	
	private void printResponse() {
		// This one's simple.
		// We're going to print out the hex values of the four bytes in sendData.
		// No check sum here. It wouldn't be meaningful anyway.
		System.out.println("Response: ");
		System.out.print(String.format("%02X", sendData[0]));
		System.out.print(String.format("%02X", sendData[1]));
		System.out.print(String.format("%02X", sendData[2]));
		System.out.println(String.format("%02X", sendData[3]));
	}

	private synchronized void sendResponse() {
		// Wait for the port to become available
		while(!portAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				sendSocket.close();
				System.exit(1);
			}
		}
		
		// Here, we're going to create a new socket (the notional sendSocket)
		// Send the response packet, and then close the socket.
		try {
			sendSocket = new DatagramSocket();
			sendSocket.send(sendPacket);
		} catch (SocketException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendSocket.close();
			System.exit(1);
		} catch (IOException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			sendSocket.close();
			System.exit(1);
		}
		
		// I'd have put this in a finally block, but we exit on Exception,
		// so that won't work.
		sendSocket.close();
		
		// Allow for other ClientConnectionThreads to compete for the socket
		portAvailable = true;
		notify();
	}
	
	private boolean validateRequest() {
		// In this function, we determine whether or not the message we received
		// is valid.
		
		// Current Cell
		// This keeps track of what cell we are going to access next.
		// If it equals length, the message is invalid, likely because it was malformed.
		// It starts at 2 because the first two bytes are checked directly.
		int cell = 2;
		
		// If the length is over max_buffer bytes, the message is too long.
		if (receivePacket.getLength() > max_buffer) {
			return false;
		}
		
		// If the first two bytes aren't 0 1 or 0 2, then the message is malformed.
		if ((receiveData[0] != 0) | !((receiveData[1] == 1) | (receiveData[1] == 2))) {
			return false;
		}
		
		// Note which cell we're at now.
		int prev_cell = cell;
		
		// Fast-forward through the text to the first separator 0.
		while (true) {
			// If cell equals length, the message is invalid,
			// likely because it was malformed.
			if (cell == receivePacket.getLength()) {
				return false;
			}
			
			// If this is the first separator 0, break loop and go to next cell
			// Unless the first separator zero is the cell we started at,
			// then the message is invalid
			if (receiveData[cell] == 0) {
				if (cell == prev_cell) {
					return false;
				}
				cell++;
				break;
			} else {
				// Otherwise, go to next cell
				cell++;
			}
		}
		
		// Note which cell we're at now.
		prev_cell = cell;
				
		// Fast-forward through the text to the second separator 0.
		while (true) {
			// If cell equals length, the message is invalid,
			// likely because it was malformed.
			if (cell == receivePacket.getLength()) {
				return false;
			}
			
			// If this is the second separator 0, break loop and go to next cell.
			// Unless the first separator zero is the cell we started at,
			// then the message is invalid
			if (receiveData[cell] == 0) {
				if (cell == prev_cell) {
					return false;
				}
				cell++;
				break;
			} else {
				// Otherwise, go to next cell
				cell++;
			}
		}
		
		// At this point, we should have covered the whole message,
		// Unless it is malformed.
		if (cell == receivePacket.getLength()) {
			return true;
		} else {
			return false;
		}
	}
	
}
