package MachineState;

import MachineCycle.PBus;
import Registers.generalPurpose;
import cpu001.CPU;
import memoryInterface.MemoryDriver;

public class CPUBuss implements PBus {
	private CPU cpu;
	
	public CPUBuss ( CPU c) {
		cpu = c;
	}
	
	public void access(BussId srcBus, BussId destBus, int srcAddress, int destAddress) {
		// TODO Auto-generated method stub
		int data;
		
		switch (srcBus) {
		case MEMROY : 		MemoryDriver m = cpu.memory;
				data = m.read(srcAddress);
				break;
		
		case REGISTER :
		////	generalPurpose r = cpu.registerFile.get(srcAddress);
			data = 5;
		}
			
	}

}
