package intermediate;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import apps.TFTPCommons;

public class ErrorSimDelayThread implements Runnable {
	int delay;
	DatagramPacket data;
	DatagramSocket SendSocket; 
	
	
	public ErrorSimDelayThread (int delay, DatagramPacket data, DatagramSocket Socket) {
		//take in the given variables and set local
		this.delay = delay;
		this.data = data; 
		this.SendSocket = Socket;
	}
	

	public void run() {
		// The purpose of this thread is to delay a 
		
		try {
			Thread.sleep(delay);
		}catch (InterruptedException e) {
			System.out.print(e);
		}
		
		TFTPCommons.printMessage(true, data.getData(), data.getLength());
		
		
		try {
			SendSocket.send(data);
		} catch (IOException e) {
			// Print a stack trace, close all sockets and exit.
			e.printStackTrace();
			SendSocket.close();
			System.exit(1);
		}
		
		IntermediateControl.canClose =true; 
		

	}

}
