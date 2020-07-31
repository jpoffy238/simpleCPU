package com.mj.Firmware.logic.ASL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class Test_ASL {
	private  CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ASL.class);

	@BeforeEach
	public  void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_ASL_ABS() {
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
	public void Test_ASL_ABS_2() {
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
	public void Test_ASL_ZP() {
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
	public void Test_ALS_ABSX()  {
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.LDX_IMM.code());
			c.bus.write(i++, (byte) (0x01));
			c.bus.write(i++, OpCodes.ASL_ABSX.code());
			c.bus.write(i++, (byte) (0x00));
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
	
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
		int result;
		try {
			result = (c.bus.read(0x2001) & 0x00ff);
			
			logger.debug (String.format("Value before test = %04x",  result));
			assert(result == 0xaa);
		} catch (illegalAddressException | DeviceUnavailable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		logger.debug("Starting CPU");
		c.run();

		 result = -1;
		try {
			result = (c.bus.read(0x2001) & 0x00ff);
			
		} catch (illegalAddressException | DeviceUnavailable e) {	
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		logger.debug("What result  is loaded with " + String.format(" %02x ",(result)));

		assert ((result & 0xff) == 0x54);
		assert (!c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		assert(c.CFLAG.isSet());

	}
	
}
