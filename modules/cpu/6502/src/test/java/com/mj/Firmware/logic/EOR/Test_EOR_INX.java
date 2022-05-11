package com.mj.Firmware.logic.EOR;

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

public class Test_EOR_INX {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_EOR_INX.class);

	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_exec() {
		/* Indexed Indirect
		 * 
		 * This mode is only used with the X register. Consider a situation where the
		 * instruction is LDA ($20,X), X contains $04, and memory at $24 contains 0024:
		 * 74 20, First, X is added to $20 to get $24. The target address will be
		 * fetched from $24 resulting in a target address of $2074. Register A will be
		 * loaded with the contents of memory at $2074.
		 * 
		 * If X + the immediate byte will wrap around to a zero-page address. So you
		 * could code that like targetAddress = (X + opcode[1]) & 0xFF .
		 * 
		 * Indexed Indirect instructions are 2 bytes - the second byte is the zero-page
		 * address - $20 in the example. Obviously the fetched address has to be stored
		 * in the zero page.
	*/	 
		int i = 0x1000;
		try {
			c.bus.write(i++,  OpCodes.LDX_IMM.code());
			c.bus.write(i++, (byte)(0x08));
			c.bus.write(i++, OpCodes.LDA_ABS.code());
			c.bus.write(i++, (byte) 0x00);
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.EOR_INX.code());
			c.bus.write(i++, (byte) (0x20));
			c.bus.write(i++, OpCodes.HLT.code());
			c.bus.write(0x1fff, (byte) 0x00);
			c.bus.write(0x2000, (byte) 0x55);
			c.bus.write(0x2001, (byte) 0xaa);
			i = 0x28;
			c.bus.write(i++,(byte)0x01);
			c.bus.write(i++,(byte)0x20);
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
	
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert (result == 0x00ff);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());

	}

}
