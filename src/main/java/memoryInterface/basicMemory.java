package memoryInterface;

import java.util.HashMap;
import java.util.Map;

import Devices.Device;
import Firmeware.Framework.OpCodes;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class basicMemory implements MemoryDriver {
	public final static int maxMemorySize = 64 * 1024;;
	private int ioPage;
	private Map<Integer, Device> devices = new HashMap<Integer, Device>();
	private byte[] memory;

	public basicMemory() {
		memory = new byte[maxMemorySize];
		int i = 0x1000;

		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x40;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x20;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x10;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x08;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x04;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x02;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x01;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = 0x00;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0xc0;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0xa0;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x90;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x88;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x84;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x82;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x81;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x80;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0xff;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x00;
		memory[i++] = OpCodes.TEST.code();
		memory[i++] = (byte) 0x80;
		memory[i++] = OpCodes.HLT.code(); // 15

	}

	public byte read(int address) throws DeviceUnavailable, illegalAddressException {
		// TODO Auto-generated method stub
		int value = 0;
		if ((address & 0xfff0) == ioPage) {
			int pageAddress = address & 0x00ff;
			Device d = devices.get(new Integer(pageAddress));
			if (null == d) {
				throw new DeviceUnavailable();
			} else {
					if ((address & 0x01) == 1) {
						value = d.status();
					} else {
						value = d.read();
					}
			}
		} else {

			if (address < maxMemorySize && address >= 0) {
				value = (int) (memory[address] & 0xff);
			} else {
				throw new illegalAddressException();
			} 
		}

		return (byte) (value & 0xff);
	}

	public void write(int address, byte data) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		// For devices the odd address is control while even is data.
		if ((address & 0xfff0) == ioPage) {
			int pageAddress = address & 0x00ff;
			Device d = devices.get(new Integer(pageAddress));
			if (null == d) {
				throw new DeviceUnavailable();
			} else {
				if ((address & 0x01) == 1) {
					d.status(data);
				} else {
					d.write(data);
				}
			}
		} else {
			if (address <= maxMemorySize && address > 0) {
				memory[address] = data;
			} else {
				throw new illegalAddressException();
			}
		}
	}

	public void setIOPage(int address) {
		// TODO Auto-generated method stub
		ioPage = address & 0xfff0; // must be on 256 byte boundary
	}

	public int getIOPage() {
		// TODO Auto-generated method stub
		return ioPage;
	}

	public void registerDevice(int address, Device d) {
		// TODO Auto-generated method stub
		int pageAddress = address & 0x00ff;
		devices.put(new Integer(pageAddress), d);
	}

	public void unregisterDevice(int address) {
		// TODO Auto-generated method stub
		int pageAddress = address & 0x00ff;
		devices.remove(new Integer(pageAddress));
	}

}
