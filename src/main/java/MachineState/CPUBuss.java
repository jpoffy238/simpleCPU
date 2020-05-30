package MachineState;

import MachineCycle.PBus;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import memoryInterface.MemoryDriver;

public class CPUBuss implements PBus {
	private CPU cpu;
	
	public CPUBuss ( CPU c) {
		cpu = c;
	}
	
	public void access(BussId srcBus, BussId destBus, int srcAddress, int destAddress) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		
		int data;
		switch (srcBus) {
		case MEMROY : 		MemoryDriver m = cpu.memory;
				data = m.read(srcAddress);
				break;
		
		case REGISTER :
		////	generalPurpose r = cpu.registerFile.get(srcAddress);
			data = 5;
		default:
			throw new DeviceUnavailable();
		}
			
	}

}
