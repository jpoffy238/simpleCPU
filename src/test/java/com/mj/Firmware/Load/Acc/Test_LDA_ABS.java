package com.mj.Firmware.Load.Acc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.Device;
import com.mj.Devices.FileDevice;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.MachineState.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.MemoryDriver;
import com.mj.memoryInterface.basicMemory;

public class Test_LDA_ABS {
	private static CPU c;
	private static MemoryDriver mem;
	private static final Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		mem = new basicMemory();;
		mem.setIOPage(0xfe00);
		
		try {
			mem.write(0xfffc, (byte) 0x00);
			mem.write(0xfffd, (byte) 0x10);
			mem.write(0xfffe, (byte) 0x00);
			mem.write(0xffff, (byte) 0x10);
			mem.write(0xfffa, (byte) 0x00);
			mem.write(0xfffb, (byte) 0x10);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c = new CPU(mem, new cpu001decoder());
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_LDA_ABS1() {
		int i = 0x1000;
		try {
			mem.write(i++, OpCodes.LDA_ABS.code());
			mem.write(i++, (byte) 0x00);
			mem.write(i++, (byte) (0x20));
			mem.write(i++, OpCodes.HLT.code());
			mem.write(0x1fff,(byte)0x00);
			mem.write(0x2000, (byte) 0x55);
			mem.write(0x2001,(byte)0xff);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.debug("Starting CPU");
		c.start();
		logger.debug("CPU state = " + c.getState());
		while (c.getState() != Thread.State.TERMINATED) {
			try {
				Thread.sleep(10);
			} catch (Exception e) {

			}
		}
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == mem.read(0x2000));
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


}
