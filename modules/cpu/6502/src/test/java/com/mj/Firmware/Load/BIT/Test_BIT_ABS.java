package com.mj.Firmware.Load.BIT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_BIT_ABS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_BIT_ABS.class);

	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0x01);
			c.bus.write(i++, OpCodes.BIT_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
	
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x2002, (byte) 0xc0);
			c.bus.write(0x2003, (byte) 0x00)
			;
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
		assert (! c.ZFLAG.isSet());
		assert (!c.NFLAG.isSet());
		assert(c.OFLAG.isSet());
	

		

	}
	@Test
	public void Test_exec1() {
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0x01);
			c.bus.write(i++, OpCodes.BIT_ABS.code());
			c.bus.write(i++, (byte) 0x01);
			c.bus.write(i++, (byte) (0x20));
			
			c.bus.write(i++, OpCodes.HLT.code());
	
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			c.bus.write(0x2002, (byte) 0xc0);
			c.bus.write(0x2003, (byte) 0x00)
			;
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
		assert ( c.ZFLAG.isSet());
		assert (c.NFLAG.isSet());
		assert(!c.OFLAG.isSet());
	
	}

}
