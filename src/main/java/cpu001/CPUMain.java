package cpu001;

import MachineState.cpu001decoder;
import memoryInterface.basicMemory;

public class CPUMain {

	public static void main(String[] args) {
		CPU c = new CPU( new basicMemory(), new cpu001decoder());
		
		c.start();
		}
	
	
		
	}


