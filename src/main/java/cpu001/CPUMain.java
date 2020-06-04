package cpu001;

import Devices.Device;
import Devices.FileDevice;
import MachineState.cpu001decoder;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import memoryInterface.basicMemory;
import memoryInterface.MemoryDriver;

public class CPUMain {

	public static void main(String[] args) {
		MemoryDriver mem = new basicMemory();
			mem.setIOPage(0xfe00);
			Device fd = new FileDevice();
			mem.registerDevice(0xfe00, fd);
			try {
				mem.write(0xfffc, (byte)0x00);
				mem.write(0xfffd, (byte)0x10);
				mem.write(0xfffe,(byte)0x00);
				mem.write(0xffff,(byte)0x10);
				mem.write(0xfffa,(byte)0x00);
				mem.write(0xfffb, (byte)0x10);
			} catch (illegalAddressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		CPU c = new CPU( mem, new cpu001decoder());
		
		c.start();
		}
	
	
		
	}


