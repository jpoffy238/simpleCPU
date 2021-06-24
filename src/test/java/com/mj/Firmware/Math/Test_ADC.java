package com.mj.Firmware.Math;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.util.CPU_CreateUtil;

public class Test_ADC {
	private  CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ADC.class);

	@BeforeEach
	public  void setup() {
		c = CPU_CreateUtil.getCPU();
		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_ADC_Basic() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) -1);
			
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == (byte)-1);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());

	}
	@Test
	public void Test_ADC_Basic02() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)1);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) -1);
			c.bus.write(i++, OpCodes.NOP.code());
		
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == (byte)0);
		assert (c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());

	}
	@Test
	public void Test_ADC_Basic03() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)127);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) 127);
			c.bus.write(i++, OpCodes.NOP.code());
		
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == (byte)(127+127));
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(c.OFLAG.isSet());
		assert(!c.CFLAG.isSet());

	}
	@Test
	public void Test_ADC_Basic04() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)200);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) 127);
			c.bus.write(i++, OpCodes.NOP.code());
		
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == ((byte)(200+127) & 0xff));
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(c.OFLAG.isSet());
		assert(c.CFLAG.isSet());

	}
//	CLC      ; 1 + 1 = 2, returns C = 0
//			  LDA #$01
//			  ADC #$01
//
//			  CLC      ; 1 + -1 = 0, returns C = 1
//			  LDA #$01
//			  ADC #$FF
//
//			  CLC      ; 127 + 1 = 128, returns C = 0
//			  LDA #$7F
//			  ADC #$01
//
//			  CLC      ; -128 + -1 = -129, returns C = 1
//			  LDA #$80
//			  ADC #$FF
	
	@Test
	public void Test_ADC_Basic05() {
//		CLC      ; 1 + 1 = 2, returns C = 0
//		  LDA #$01
//		  ADC #$01
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.CLC.code());
			c.bus.write(i++, OpCodes.CLV.code());
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)1);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) 1);
			c.bus.write(i++, OpCodes.NOP.code());
		
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == ((byte)2));
		assert (!c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		assert(!c.OFLAG.isSet());
		assert(!c.CFLAG.isSet());

	}
	@Test
	public void Test_ADC_Basic06() {
///			  CLC      ; 1 + -1 = 0, returns C = 1
//		  LDA #$01
//		  ADC #$FF
		int i = 0x1000;
		try {
			c.bus.write(i++, OpCodes.CLC.code());
			c.bus.write(i++, OpCodes.CLV.code());
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)1);
			c.bus.write(i++, OpCodes.ADC_IMM.code());
			c.bus.write(i++, (byte) 0xff);
			c.bus.write(i++, OpCodes.NOP.code());
		
			
			c.bus.write(i++, OpCodes.HLT.code());
		
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
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == ((byte)0));
		assert (c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		assert(c.CFLAG.isSet());
		assert(c.OFLAG.isSet());
		

	}	
	
	@Test
	public void Test_ADC_Basic07() {
		int i = 0x1000;
		BasicIntelHexFiles testCode = new BasicIntelHexFiles();
		ArrayList<IntelHexRecord> code = new ArrayList<IntelHexRecord>();
		try {
			 code = testCode.read("LDA_IMM_TEST.hex");
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
	}
	@Test
	public void Test_ADC_Advanced() {
		try {
			CPU_CreateUtil.load(c.bus, "ADC_Test.hex", 0);
			
			
		} catch (IOException | illegalAddressException | ROException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to load test program");

		}
		c.pc = 0x1000;
		c.run();
		try {
			int results = c.bus.read(0x108);
			logger.debug(String.format("Computed Value = %02x", results));
			assert(results == 0);
		} catch (illegalAddressException | DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Unable to read computed value");
		}
	}
	}	

