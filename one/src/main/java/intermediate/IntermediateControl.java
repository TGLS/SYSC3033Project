package intermediate;

/**
 * This short class has one function:
 * Send setting information to the ErroSimulator and ErrorSimRecieve Threads from the main Server Thread. 
 */
public class IntermediateControl {
	static Boolean IntermediateStop = false;
	static Boolean verboseMode = false; 
	static String mode = "0"; // default to the basic 
	static String packetType = ""; 
	static int packetNumber =0;
	static int delay =1000;
	
	public IntermediateControl(){}
}
