package com.mj.Firmware.Math;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

public class Test_ADC {
	private  CPU c;

	private static final Logger logger = LogManager.getLogger(Test_ADC.class);

	@BeforeEach
	public  void setup() {

		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory());
		bus.registerDevice(new basicROM(bus));

		c = new CPU(bus, new cpu001decoder());

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
			assert (false);assert(c.CFLAG.isSet());
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
}
