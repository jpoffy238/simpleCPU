package com.mj.Firmware.Load.Acc;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.BasicIntelHexRecord;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class Test_LDA_ABS {
	private static CPU c;

	private static final Logger logger = LogManager.getLogger("Test_LDA_ABS");

	@BeforeAll
	public static void setup() {
		
		c = CPU_CreateUtil.getCPU();
	
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_LDA_ABS1() {
		int i = 0x1000;
		BasicIntelHexFiles testCode = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> code = new ArrayList<IntelHexRecord>();
		try {
			 code = testCode.read("/home/jpoffen/git/simpleCPU/src/main/asm/LDA_IMM_TEST.hex");
		} catch (IOException | IntelHexFileChecksumMisMatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int x = 0 ; x < code.size(); x++   ) {
			IntelHexRecord r = code.get(x);
			byte[] program = r.getData();
			try {
				for (int idx = 0; idx < program.length; idx++) {
					c.bus.write(i+idx,program[idx]);
				}
	//			c.bus.write(i++, OpCodes.LDA_ABS.code());
//				c.bus.write(i++, (byte) 0x00);
//				c.bus.write(i++, (byte) (0x20));
//				c.bus.write(i++, OpCodes.HLT.code());
//				c.bus.write(0x1fff,(byte)0x00);
//				c.bus.write(0x2000, (byte) 0x55);
//				c.bus.write(0x2001,(byte)0xff);
			} catch (illegalAddressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			} catch (DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			} catch (ROException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				assert(false);
			}
			
		}
	
		logger.debug("Starting CPU");
		c.run();
		
		int result = c.a.get();
		logger.debug("What A is loaded with : ", +result);
		try {
			assert (result == c.bus.read(0x2000));
			assert(!c.ZFLAG.isSet());
			assert(!c.NFLAG.isSet());
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert(false);
		}
		
	}


}
