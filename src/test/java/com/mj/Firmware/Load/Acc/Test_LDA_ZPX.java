package com.mj.Firmware.Load.Acc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.BasicBus;
import com.mj.Devices.CPUBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class Test_LDA_ZPX {
	private static CPU c;
	private static CPUBus mem;
	private static final Logger logger = LogManager.getLogger("Test_LDA_ZPX");

	@BeforeAll
	public static void setup() {
		mem = new BasicBus();;
		
		
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
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		c = new CPU(mem, new cpu001decoder());
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			mem.write(i++,  OpCodes.LDX_IMM.code());
			mem.write(i++,  (byte)0x10);
			mem.write(i++, OpCodes.LDA_ZX.code());
			mem.write(i++, (byte) (0x04));
			mem.write(i++, OpCodes.HLT.code());
			mem.write(0x0013,(byte)0x01);
			mem.write(0x0014, (byte) 0x55);
			mem.write(0x0015,(byte)0xff);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ROException e) {
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
			assert (result == mem.read(0x14));
			assert(result != mem.read(0x13));
			assert(result != mem.read(0x15));
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


}
