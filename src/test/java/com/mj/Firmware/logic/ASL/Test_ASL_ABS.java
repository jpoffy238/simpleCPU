package com.mj.Firmware.logic.ASL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.Firmware.Load.IndexX.LDX_IMM;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class Test_ASL_ABS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ASL_ABS.class);

	@BeforeAll
	public static void setup() {

		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));

		c = new CPU(bus, new cpu001decoder());

		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.ASL_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();
		
		int result = -1;
		try {
			result = c.bus.read(0x2000);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What A is loaded with : ", +result);

		assert ((result & 0xff) == 0xaa);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());

	}
	@Test
	public void Test_exec_01() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.ASL_ABS.code());
			c.bus.write(i++, (byte) 0x01);
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();
//		logger.debug("CPU state = " + c.getState());
//		while (c.getState() != Thread.State.TERMINATED) {
//			try {
//				Thread.sleep(10);
//			} catch (Exception e) {
//
//			}
//		}
		int result = -1;
		try {
			result = c.bus.read(0x2001);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What A is loaded with " + String.format(" %02x ",(result&0xff)));

		assert ((result & 0xff) == 0x54);
		assert (!c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		assert(c.CFLAG.isSet());

	}

	@Test
	public void Test_exec_ZP() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.ASL_ZP.code());
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x20, (byte)0x55);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();
//		logger.debug("CPU state = " + c.getState());
//		while (c.getState() != Thread.State.TERMINATED) {
//			try {
//				Thread.sleep(10);
//			} catch (Exception e) {
//
//			}
//		}
		int result = -1;
		try {
			result = c.bus.read(0x2001);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What A is loaded with " + String.format(" %02x ",(result&0xff)));

		assert ((result & 0xff) == 0xaa);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(!c.CFLAG.isSet());

	}
	@Test
	public void Test_exec_ABSX() {
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.LDX_IMM.code());
			c.bus.write(i++, (byte) (0x01));
			c.bus.write(i++, OpCodes.ASL_ABSX.code());
			c.bus.write(i++, (byte) (0x00));
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x20, (byte)0x55);
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();

		int result = -1;
		try {
			result = c.bus.read(0x2001);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What result  is loaded with " + String.format(" %02x ",(result&0xff)));

		assert ((result & 0xff) == 0x54);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(c.CFLAG.isSet());

	}
	
}
