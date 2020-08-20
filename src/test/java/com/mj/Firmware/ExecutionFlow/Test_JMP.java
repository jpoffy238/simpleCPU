package com.mj.Firmware.ExecutionFlow;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;
import com.mj.util.CPU_CreateUtil;

public class Test_JMP {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger(Test_JMP.class);

	@BeforeAll
	public static void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_JMPABS() {
		try {
		CPU_CreateUtil.load(c.bus, "/home/jpoffen/git/simpleCPU/src/main/asm/JMP_ABS.hex", 0);
		
		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}

		

		logger.debug("Starting CPU");
		c.run();

		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
			assert (result == 11);
			assert (!c.ZFLAG.isSet());
			assert (!c.NFLAG.isSet());
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

	}

	@Test
	public void Test_JMPIND() {
		
		try {
			CPU_CreateUtil.load(c.bus, "/home/jpoffen/git/simpleCPU/src/main/asm/JMP_IND.hex", 0);
			
			
		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}

		logger.debug("Starting CPU");
		try {
			c.pc = 0x1000;
			c.step();
	
		assert(c.pc == 0x1001); // SEI;
		c.step();
		assert(c.a.get() == 10);// LDA #$0A 
		assert(c.pc == 0x1003);
		c.step();  // JMP (TAD)
		assert(c.pc == 0x100e) ;  // 1015 0D10               19  TAD DW ADDONE
		c.step(); // 100D 6901               12  ADDONE  ADC #$01
		assert(c.a.get() == 11);
		c.step(); // 100F 6C1710             13          JMP (TRT)
		assert(c.pc == 0x1006); 
		c.step(); // 1005 8D0020              7  RT      STA VALUE
		assert(c.pc == 0x1009);
		c.step();
		assert(c.pc == 0x100a);
		c.step();   //100A 6C1A10             10          JMP (RDH)
		assert(c.pc == 0x1015);
		//c.step();
		
		
		
		} catch (illegalAddressException | DeviceUnavailable | illegalOpCodeException | ROException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			fail("Something failed");
		}
	
	}
}
