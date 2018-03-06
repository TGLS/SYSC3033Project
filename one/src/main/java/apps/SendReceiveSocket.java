package apps;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class SendReceiveSocket {
	private DatagramSocket socket;
	private DatagramPacket previousPacket;
	
	public SendReceiveSocket() {
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(1000);
			previousPacket = null;
		} catch (SocketException e) {
			// Print a stack trace and exit.
			e.printStackTrace();
			socket.close();
			System.exit(1);
		}
	}

	public void receive(DatagramPacket receivePacket) throws IOException {
		// Until we receive a 
		while (true) {
			try {
				// Attempt to receive a packet.
				socket.receive(receivePacket);
				
				// After we successfully received a packet, break
				break;
			} catch (SocketTimeoutException e) {
				// Retransmit the last sent packet, unless it's an ACK or we haven't sent a packet yet.
				if (previousPacket != null) {
					socket.send(previousPacket);
				}
			}
		}
	}

	public void close() {
		// Close the socket
		socket.close();
	}

	public void send(DatagramPacket sendPacket) throws IOException {
		// Send the datagram packet
		socket.send(sendPacket);
		
		// Check whether the datagram packet is an ACK or not.
		if ((sendPacket.getData()[0] == 0) | (sendPacket.getData()[1] == 4)) {
			// Clear the previousPacket
			previousPacket = null;
		} else {
			// Keep a reference of the previous packet handy.
			previousPacket = sendPacket;
		}
	}
}
