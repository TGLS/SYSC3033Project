package apps;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class contains static functions used by multiple classes to facilitate TFTP.
 */

public class TFTPCommons {
	// To coordinate the size of the data buffer used by each component of the system,
	// We're moving the max_buffer constant here.
	public final static int max_buffer = 516;
	
	public static String extractRequestData(byte[] messageData, int dataLength, int dataType) throws UnsupportedEncodingException {
		// Return a blank string if packet doesn't include a file name
		if ((messageData[1] == 1) | (messageData[1] == 2)) {
			// Copy the byte array after the first two bytes
			byte[] buffer = new byte[dataLength];
			for (int n = 2; n < dataLength; n++) {
				buffer[n - 2] = messageData[n];
			}

			// Now we convert the byte array to text, and split at null
			// characters
			String rawText = null;
			rawText = new String(buffer, "UTF-8");
			
			String[] text = rawText.split("\u0000");

			// Return the mode if 1 and filename if 0.
			return text[dataType];
		}
		return "";
	}
	
	public static String extractFileName(byte[] messageData, int dataLength) throws UnsupportedEncodingException {
		return extractRequestData(messageData, dataLength, 0);
	}
	
	public static String extractModeType(byte[] messageData, int dataLength) throws UnsupportedEncodingException {
		return extractRequestData(messageData, dataLength, 1);
	}
	
	public static void printMessage(Boolean send, byte[] messageData, int dataLength) {
		// Checksum is the sum of all the bytes, less overflows
		int checksum = 0;
		// First we convert the sent data into strings.
		// While we're doing this, we'll check array index bounds by try-catch.
		try {
			// Note whether the packet is being sent or received.
			if (send) {
				System.out.print("Sending ");
			} else {
				System.out.print("Recieving ");
			}
			// If the second byte is 1, the message is a Read Request.
			// If the second byte is 2, the message is a Write Request.
			// If the second byte is 3, the message is a Data.
			// If the second byte is 4, the message is an Acknowledgement.
			// If the second byte is 4, the message is an Error..
			// Anything else we say it's invalid.
			if (messageData[1] == 1) {
				System.out.println("Read Request");
			} else if (messageData[1] == 2) {
				System.out.println("Write Request");
			} else if (messageData[1] == 3) {
				System.out.println("Data");
			} else if (messageData[1] == 4) {
				System.out.println("Acknowledgment");
			} else if (messageData[1] == 5) {
				System.out.println("Error");
			} else {
				System.out.println("Invalid Message");
			}
			
			// For read and write requests, print the filename and mode
			if ((messageData[1] == 1) | (messageData[1] == 2)) {
				try {
					System.out.println("File Name: " + extractRequestData(messageData, dataLength, 0));
				} catch (UnsupportedEncodingException e) {
					System.out.println("Bad File Name.");
				}
				try {
					System.out.println("Mode: " + extractRequestData(messageData, dataLength, 1));
				} catch (UnsupportedEncodingException e) {
					System.out.println("Bad Mode.");
				}
			}
			
			// For Data and Acknowledgment Messages
			// Print the block number being sent.
			if ((messageData[1] == 3) | (messageData[1] == 4)) {
				int block_number = ((int)(messageData[2]) & 0xFF) * 256;
				block_number = block_number + ((int)(messageData[3]) & 0xFF);
				System.out.println("Block Number: " + block_number);
			}
			
			// For Error Messages, print error code and message (if any)
			if (messageData[1] == 5) {
				// Print error message
				printErrorMessage(messageData, dataLength);
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			// If we get an array exception, the message is badly formed
			System.out.println("Error reading message.");
		}

		System.out.println("");
		
		// Then we print it as hex and compute a checksum.
		// This is accomplished by converting each number to hex,
		// then printing it. Line breaks every 40 numbers.
		for (int n = 0; n < dataLength; n++) {
			String s = String.format("%02X", messageData[n]);
			if (n % 40 == 39) {
				System.out.println(s);
			} else if (n == dataLength - 1) {
				System.out.println(s);
			} else {
				System.out.print(s);
			}
			// Calculate the checksum while printing each byte
			checksum += (int)messageData[n] & 0xFF;

			// Because we validate data after we print the data,
			// we'll check if we're over the max_buffer limit here,
			// to avoid going over array index limitations.
			if (n >= TFTPCommons.max_buffer) {
				break;
			}
		}
		// Print the checksum
		System.out.println("");
		System.out.println("Checksum: " + String.format("%08X", checksum));
	}
	
	public static void printErrorMessage(byte[] messageData, int dataLength) {
		// Error code translation
		switch (messageData[3]) {
			case 0:
				System.out.println("Not defined, see error message (if any).");
				break;
			case 1:
				System.out.println("File not found.");
				break;
			case 2:
				System.out.println("Access violation.");
				break;
			case 3:
				System.out.println("Disk full or allocation exceeded.");
				break;
			case 4:
				System.out.println("Illegal TFTP operation.");
				break;
			case 5:
				System.out.println("Unknown transfer ID.");
				break;
			case 6:
				System.out.println("File already exists.");
				break;
			case 7:
				System.out.println("No such user.");
				break;
			default:
				System.out.println("Invalid Message!");
				break;
		}
		String message;
		// If there's a message, print it.
		if (messageData[4] == 0) {
			message = "No Message.";
		} else {
			// Copy the byte array after the first four bytes
			byte[] buffer = new byte[dataLength];
			for (int n = 4; n < dataLength; n++) {
				buffer[n - 4] = messageData[n];
			}

			// Now we convert the byte array to text, and split at null
			// characters
			String rawText;
			try {
				rawText = new String(buffer, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// Print a stack trace and exit.
				e.printStackTrace();
				return;
			}
			String[] text = rawText.split("\u0000");
			message = text[0];
		}
		System.out.println(message);
	}

	public static void incrementBlockCounter(byte[] blockCounter, byte[] previousBlockCounter) {
		// This is simple. We take in a 2 byte block counter, and we increment it.
		// We wrap around if necessary.
		
		// If we get a non-null previousBlockCounter, set it to the value of blockCounter
		if (previousBlockCounter != null) {
			previousBlockCounter[0] = blockCounter[0];
			previousBlockCounter[1] = blockCounter[1];
		}
		
		if (blockCounter[1] == -1) {
			blockCounter[1] = 0;
			if (blockCounter[0] == -1) {
				blockCounter[0] = 0;
			} else {
				blockCounter[0]++;
			}
		} else {
			blockCounter[1]++;
		}
	}
	
	public static boolean receiveFile(OutputStream stream, SendReceiveSocket sendReceiveSocket, Boolean verbose) throws SocketTimeoutException {
		return receiveFile(stream, sendReceiveSocket, verbose, null, -1);
	}

	public static boolean receiveFile(OutputStream stream, SendReceiveSocket sendReceiveSocket, Boolean verbose, InetAddress targetAddress, int targetPort) throws SocketTimeoutException {
		// First of all, let's create a block counter and some other important variables
		byte[] blockCounter = {0, 1};
		byte[] previousBlockCounter = {0, 0};
		byte[] receiveData;
		byte[] respondData; 
		DatagramPacket receivePacket;
		DatagramPacket respondPacket;
		
		// We are assuming that we are being given a FileOutputStream in Append mode.
		try {
			// Now let's start looping.
			while (true) {
				// First, receive a packet
				// Create a byte array for the incoming packet.
				receiveData = new byte[max_buffer];
					
				// Create a packet for the incoming packet.
				receivePacket = new DatagramPacket(receiveData, receiveData.length);
					
				// Receive a message from the socket.
				// Surrounded with try-catch because receiving a message might fail.
				sendReceiveSocket.receive(receivePacket, verbose);
				
				// Print it if we're being verbose
				if (verbose) {
					System.out.println(receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());
					printMessage(false, receiveData, receivePacket.getLength());
				}
				
				// Set our targetAddress if not yet set.
				if (targetAddress == null) {
					targetAddress = receivePacket.getAddress();
					targetPort = receivePacket.getPort();
				}
				
				// Check whether it has the right address/port
				if (!targetAddress.equals(receivePacket.getAddress()) || targetPort != receivePacket.getPort()) {
					// Send an invalid TID message to the sender.
					TFTPCommons.sendError(5,sendReceiveSocket, verbose,
							receivePacket.getAddress(), receivePacket.getPort());
				} else {
					// Check if it's an Error packet.
					if ((receiveData[0] == 0) & (receiveData[1] == 5)) {
						// Print the error message if we're not verbose
						if (!verbose) {
							printErrorMessage(receiveData, receivePacket.getLength());
						}
						
						// Stop; we won't be receiving any more packets after this.
						return false;
					}
					
					// For data packets,
					else if ((receiveData[0] == 0) & (receiveData[1] == 3)) {
						// If we get the previous data packet, respond the previous ACK
						if ((receiveData[2] == previousBlockCounter[0]) &
							(receiveData[3] == previousBlockCounter[1])) {
							// Then we respond with an acknowledge.
							respondData = new byte[] {0, 4, previousBlockCounter[0], previousBlockCounter[1]};
							respondPacket = new DatagramPacket(respondData, 4, targetAddress,
									targetPort);
						
							// Print response if we're being verbose
							if (verbose) {
								printMessage(true, respondData, respondPacket.getLength());
							}
						
							sendReceiveSocket.send(respondPacket);
						}
						
						// If its block counter matches
						if ((receiveData[2] == blockCounter[0]) &
							(receiveData[3] == blockCounter[1])) {
							// If it does, append the data block, if any
							if (receiveData.length > 4) {
								try {
									stream.write(Arrays.copyOfRange(receiveData, 4, receivePacket.getLength()));
								} catch (AccessDeniedException e) {
									// Send an Access Violation Error and break.
									TFTPCommons.sendError(2,sendReceiveSocket, verbose,
											targetAddress, targetPort);
									return false;
								} catch (IOException e) {
									// Out of disk space errors throw this.
									// Check if the disk is out of space
									if (new File("/").getUsableSpace() == 0) {
										// Send a Disk Full Error and break.
										TFTPCommons.sendError(3,sendReceiveSocket, verbose,
												targetAddress, targetPort);
										return false;
									} else {
										// Send a Access Violation and break.
										TFTPCommons.sendError(2,sendReceiveSocket, verbose,
												targetAddress, targetPort);
										return false;
									}
								}
							}

							// Then we respond with an acknowledge.
							respondData = new byte[] {0, 4, blockCounter[0], blockCounter[1]};
							respondPacket = new DatagramPacket(respondData, 4, targetAddress,
									targetPort);
							
							// Print response if we're being verbose
							if (verbose) {
								printMessage(true, respondData, respondPacket.getLength());
							}
							
							sendReceiveSocket.send(respondPacket);
							
							// Increment blockCounter
							incrementBlockCounter(blockCounter, previousBlockCounter);
							
							// Then check whether we're finished.
							if (receivePacket.getLength() < 516) {
								return true;
							}
						}
					} else {
						// Send an illegal operation message to the sender and quit.
						TFTPCommons.sendError(4,sendReceiveSocket, verbose,
								receivePacket.getAddress(), receivePacket.getPort());
						return false;
					}
				}
			}
		} catch (SocketTimeoutException e) {
			// Raise; the server won't care (beyond deleting the file), the client will.
			throw e;
		} catch (IOException e) {
			// Print an error and stop
			e.printStackTrace();
			return false;
		}
	}
	
	public static void sendFile(InputStream stream, SendReceiveSocket sendReceiveSocket, Boolean verbose, InetAddress targetAddress, int targetPort) throws SocketTimeoutException {
		// First of all, let's create a block counter and some other important variables
		byte[] blockCounter = {0, 1};
		ArrayList<Byte> respondDataBuilder;
		byte[] receiveData;
		byte[] respondData; 
		DatagramPacket receivePacket;
		DatagramPacket respondPacket;
		Boolean cont = true;
		
		// We're going to use a FileInputStream to read the file.
		try {
			// Now, let's start looping
			while (cont) {
				// Start building the message
				// Op code, block, then data
				respondDataBuilder = new ArrayList<Byte>();
				respondDataBuilder.add((byte) 0);
				respondDataBuilder.add((byte) 3);
				respondDataBuilder.add(blockCounter[0]);
				respondDataBuilder.add(blockCounter[1]);
				
				
				try {
					// Read in bytes from the file.
					// Break on EOF (-1) and when we reach max packet size.
					// On EOF, we stop the outer loop too.
					while (respondDataBuilder.size() < max_buffer) {
						int temp = stream.read();
						if (temp == -1) {
							cont = false;
							break;
						} else {
							respondDataBuilder.add((byte) temp);
						}
					}
				} catch (IOException e) {
					// Send an Access Violation Error and break.
					TFTPCommons.sendError(2,sendReceiveSocket, verbose,
							targetAddress, targetPort);
					return;
				}
				
				// Now we make an array
				// We make an array of the same size as the buffer, and then we copy the
				// old bytes over.
				// n is used to keep track of the index.
				respondData = new byte[respondDataBuilder.size()];
				int n = 0;
				for (Byte b : respondDataBuilder) {
					respondData[n] = b;
					n++;
				}
				
				// Then we create and send the packet
				respondPacket = new DatagramPacket(respondData, respondData.length, targetAddress, targetPort);
				// Print it if we're being verbose
				if (verbose) {
					printMessage(true, respondData, respondPacket.getLength());
				}
				
				sendReceiveSocket.send(respondPacket);
				
				// Now let's wait for the acknowledge
				while (true) {
					// Prepare to receive a packet
					// Create a byte array for the incoming packet.
					receiveData = new byte[max_buffer];
						
					// Create a packet for the incoming packet.
					receivePacket = new DatagramPacket(receiveData, receiveData.length);
						
					// Receive a message from the socket.
					sendReceiveSocket.receive(receivePacket, verbose);
					
					// Print it if we're being verbose
					if (verbose) {
						System.out.println(receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort());
						printMessage(false, receiveData, receivePacket.getLength());
					}
					
					// Check whether it has the right address/port
					if (!targetAddress.equals(receivePacket.getAddress()) || targetPort != receivePacket.getPort()) {
						// Send an invalid TID message to the sender.
						TFTPCommons.sendError(5,sendReceiveSocket, verbose,
								receivePacket.getAddress(), receivePacket.getPort());
					} else {
						// Check if it's an acknowledge packet
						// And whether if its block counter matches
						if ((receiveData[0] == 0) & (receiveData[1] == 4) & (receivePacket.getLength() == 4)) {
							if ((receiveData[2] == blockCounter[0]) &
								(receiveData[3] == blockCounter[1])) {
								// Increment blockCounter
								incrementBlockCounter(blockCounter, null);
								break;
							}
						}
						
						// Check if it's an Error packet.
						else if ((receiveData[0] == 0) & (receiveData[1] == 5)) {
							// Print the error message if we're not verbose
							if (!verbose) {
								printErrorMessage(receiveData, receivePacket.getLength());
							}
							
							// Stop; we won't be receiving any more packets after this.
							return;
						} else {
							// Send an illegal operation message to the sender and quit.
							TFTPCommons.sendError(4,sendReceiveSocket, verbose,
									receivePacket.getAddress(), receivePacket.getPort());
							return;
						}
					}
				}
			}
			
		} catch (SocketTimeoutException e) {
			// Raise; the server won't care, the client will.
			throw e;
		}  catch (AccessDeniedException e) {
			// Send an Access Violation Error and break.
			TFTPCommons.sendError(2,sendReceiveSocket, verbose,
					targetAddress, targetPort);
			return;
		}  catch (IOException e) {
			// Print an error and stop
			e.printStackTrace();
			return;
		}
	}

	public static void sendError(int errorCode, SendReceiveSocket sendReceiveSocket, Boolean verboseMode, InetAddress address,
			int port, String specialMessage) {
		// Construct the message.
		ArrayList<Byte> errorMessageBuilder = new ArrayList<Byte>();
		// Message op # first
		errorMessageBuilder.add((byte) 0);
		errorMessageBuilder.add((byte) 5);
		
		// Then the errorCode
		errorMessageBuilder.add((byte) 0);
		errorMessageBuilder.add((byte) errorCode);
		
		// Then the message, if any.
		byte[] stringBuffer = specialMessage.getBytes();
		for (byte b : stringBuffer) {
			errorMessageBuilder.add(b);
		}
		
		// Then the final null.
		errorMessageBuilder.add((byte) 0);
		
		// Now we make an array
		// We make an array of the same size as the buffer, and then we copy the
		// old bytes over.
		// n is used to keep track of the index.
		byte[] sendData = new byte[errorMessageBuilder.size()];
		int n = 0;
		for (Byte b : errorMessageBuilder) {
			sendData[n] = b;
			n++;
		}
		
		// And now we build the packet
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, port);
		
		// Print it if verbose.
		if (verboseMode) {
			printMessage(true, sendData, sendData.length);
		} else {
			// If not, print the brief version.
			printErrorMessage(sendData, sendData.length);
		}
		
		// Then send it.
		try {
			sendReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			// Print an error and stop.
			e.printStackTrace();
			return;
		}
	}
	
	public static void sendError(int errorCode, SendReceiveSocket sendReceiveSocket, Boolean verboseMode, InetAddress address,
			int port) {
		// This is a simpler version of sendError that eliminates the message argument
		sendError(errorCode, sendReceiveSocket, verboseMode, address, port, "");
	}
	
	public static OutputStream createFile(String fileName, Boolean force) throws IOException {
		// This function creates a blank file and a FileOutputStream in Append mode
		
		File file = new File(fileName);

		try {
			// If force, delete a file to prevent a fileAlreadyExists from being thrown.
			if (force) {
				file.delete();
			}
			if (file.createNewFile()) {
				// Return a FileOutputStream in append mode.
				return Files.newOutputStream(file.toPath());
			} else {
				// Throw a FileAlreadyExistsException to indicate the file exists.
				throw new FileAlreadyExistsException(fileName);
			}
		} catch (FileAlreadyExistsException e) {
			// We threw it, so throw it!
			throw e;
		} catch (NoSuchFileException e) {
			// Print a stack trace
			// This block shouldn't ever be accessed.
			e.printStackTrace();
		} catch (IOException e) {
			// Out of disk space errors throw this.
			// Check if the disk is out of space
			if (new File("/").getUsableSpace() == 0) {
				// If so, throw an OutOfDiskSpaceException
				throw new OutOfDiskSpaceException();
			} else {
				// Otherwise we treat it as an IOException.
				throw e;
			}
		}
		return null;
	}
}
