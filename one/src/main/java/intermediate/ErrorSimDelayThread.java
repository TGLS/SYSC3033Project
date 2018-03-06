package intermediate;

import java.net.DatagramPacket;

public class ErrorSimDelayThread implements Runnable {
	int delay;
	DatagramPacket data;
	ErrorSimulatorThread errorSim; 
	
	
	public ErrorSimDelayThread (int delay, DatagramPacket data, ErrorSimulatorThread errorSim ) {
		//take in the given variables and set local
		this.delay = delay;
		this.data = data; 
		this.errorSim = errorSim;
	}
	

	public void run() {
		// The purpose of this thread is to delay a 
		
		try {
			Thread.sleep(delay);
		}catch (InterruptedException e) {
			System.out.print(e);
		}
		
		errorSim.sendPacket(data);
		

	}

}
