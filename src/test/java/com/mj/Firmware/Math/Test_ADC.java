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
	
	// @Test
	public void Test_ADC_Basic07() {
///			  CLC      ; 1 + -1 = 0, returns C = 1
//		  LDA #$01
//		  ADC #$FF
		int i = 0x1000;
//		; Demonstrate that the V flag works as described
//		;
//		; Returns with ERROR = 0 if the test passes, ERROR = 1 if the test fails
//		;
//		; Five (additional) memory locations are used: ERROR, S1, S2, U1, and U2
//		; which can be located anywhere convenient in RAM
//		;
//		TEST CLD       ; Clear decimal mode (just in case) for test
//		     LDA #1
//		     STA ERROR ; Store 1 in ERROR until test passes
//		     LDA #$80
//		     STA S1    ; Initalize S1 and S2 to -128 ($80)
//		     STA S2
//		     LDA #0
//		     STA U1    ; Initialize U1 and U2 to 0
//		     STA U2
//		     LDY #1    ; Initialize Y (used to set and clear the carry flag) to 1
//		LOOP JSR ADD   ; Test ADC
//		     CPX #1
//		     BEQ DONE  ; End if V and unsigned result do not agree (X = 1)
//		     JSR SUB   ; Test SBC
//		     CPX #1
//		     BEQ DONE  ; End if V and unsigned result do not agree (X = 1)
//		     INC S1
//		     INC U1
//		     BNE LOOP  ; Loop until all 256 possibilities of S1 and U1 are tested
//		     INC S2
//		     INC U2
//		     BNE LOOP  ; Loop until all 256 possibilities of S2 and U2 are tested
//		     DEY
//		     BPL LOOP  ; Loop until both possiblities of the carry flag are tested
//		     LDA #0
//		     STA ERROR ; All tests pass, so store 0 in ERROR
//		DONE RTS
//		;
//		; Test ADC
//		;
//		; X is initialized to 0
//		; X is incremented when V = 1
//		; X is incremented when the unsigned result predicts an overflow
//		; Therefore, if the V flag and the unsigned result agree, X will be
//		; incremented zero or two times (returning X = 0 or X = 2), and if they do
//		; not agree X will be incremented once (returning X = 1)
//		;
//		ADD  CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
//		     LDA S1   ; Test twos complement addition
//		     ADC S2
//		     LDX #0   ; Initialize X to 0
//		     BVC ADD1
//		     INX      ; Increment X if V = 1
//		ADD1 CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
//		     LDA U1   ; Test unsigned addition
//		     ADC U2
//		     BCS ADD3 ; Carry is set if U1 + U2 >= 256
//		     BMI ADD2 ; U1 + U2 < 256, A >= 128 if U1 + U2 >= 128
//		     INX      ; Increment X if U1 + U2 < 128
//		ADD2 RTS
//		ADD3 BPL ADD4 ; U1 + U2 >= 256, A <= 127 if U1 + U2 <= 383 ($17F)
//		     INX      ; Increment X if U1 + U2 > 383
//		ADD4 RTS
		int ERROR = 0x3000;
		int S1 = ERROR+2;
		int S2 = S1 + 2;
		int U1 = S2 + 2;
		int U2 = U1 +2;
		
		try {
			int TEST = i;
//			TEST CLD       ; Clear decimal mode (just in case) for test
//		     LDA #1
//		     STA ERROR ; Store 1 in ERROR until test passes
//		     LDA #$80
//		     STA S1    ; Initalize S1 and S2 to -128 ($80)
//		     STA S2
//		     LDA #0
//		     STA U1    ; Initialize U1 and U2 to 0
//		     STA U2
//		     LDY #1    ; Initialize Y (used to set and clear the carry flag) to 1
			c.bus.write(i++,  OpCodes.CLD.code()) ;
			c.bus.write(i++,  OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0x01);
			c.bus.write(i++,  OpCodes.STA_ABS.code());
			c.bus.write(i++, (byte)(ERROR & 0x00ff));
			c.bus.write(i++, (byte)((ERROR >> 8) & 0x00ff));
			c.bus.write(i++,  OpCodes.LDA_IMM.code());
			c.bus.write(i++,  (byte)0x80);
			c.bus.write(i++,  OpCodes.STA_ABS.code());
			c.bus.write(i++, (byte)(S1 & 0x00ff));
			c.bus.write(i++, (byte)((S1 >> 8) & 0x00ff));
			c.bus.write(i++,  OpCodes.STA_ABS.code());
			c.bus.write(i++, (byte)(S2 & 0x00ff));
			c.bus.write(i++, (byte)((S2 >> 8) & 0x00ff));
			c.bus.write(i++,  OpCodes.LDY_IMM.code());
			c.bus.write(i++, (byte)(0x01));
			c.bus.write(i++, OpCodes.HLT.code());
		int LOOP = i;
		
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
