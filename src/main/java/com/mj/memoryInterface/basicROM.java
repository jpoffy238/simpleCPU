package com.mj.memoryInterface;

import java.io.IOException;
import java.util.ArrayList;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class basicROM extends AbstractMemoryLayer {

	PBus sysbus;
	AddressRange romMemoryMapping = new AddressRange(0xf000, 0xffff);
	private byte[] ROM = new  byte[0xffff-0xf000 + 1];
	public basicROM(PBus bus) {
		
	
		sysbus = bus;
		/* The only other reserved locations in the memory map are the very last 6 bytes of memory 
		 * $FFFA to $FFFF which must be programmed with the addresses of the non-maskable interrupt handler ($FFFA/B), 
		 * the power on reset location ($FFFC/D) and the 
		 * BRK/interrupt request handler ($FFFE/F) respectively.
		 * 
		 */
		
		ROM[0xfffa - 0xf000] = (byte)(0x00);  // Non maskable interrupt handler
		ROM[0xfffb - 0xf000] =  (byte)(0x10);
		ROM[0xfffc - 0xf000] =  (byte)(0x00); // Power on reset
		ROM[0xfffd - 0xf000] =  (byte)(0x10);
		ROM[0xfffe - 0xf000] =  (byte)(0x00); // BRK/interrupt handler
		ROM[0xffff - 0xf000] = (byte)(0xf0);
		load("/home/jpoffen/git/simpleCPU/src/main/asm/RTCInt.hex");
//		int i =   0;
//		
//		ROM[i++] = OpCodes.PHA.code(); // Store Acc
//		ROM[i++] = OpCodes.TXA.code();
//		ROM[i++] = OpCodes.PHA.code();
//		ROM[i++] = OpCodes.LDX_IMM.code();  // Load length into X
//		ROM[i++] = (byte)(20) ;
//
// 		MARKER=i; // For branch location
// 
//		ROM[i++] =  OpCodes.LDA_ABSX.code(); // Clock is read backwards 
//		ROM[i++] =  (byte)lowerStartAddress;
//		ROM[i++] = (byte)UpperStartAddress;
//		ROM[i++] =  OpCodes.STA_ABX.code(); // Store it in local memory
//		ROM[i++] =  (byte)(0x00);
//		ROM[i++] =   (byte)(0x40);
//		ROM[i++] =  OpCodes.DEX.code();  // dec x to point to next location
//		ROM[i++] =  OpCodes.BNE.code(); // branch for next			 
//		ROM[i++] =  (byte) (MARKER - i +1);
//		ROM[i++] =  OpCodes.LDA_ABSX.code(); // Clock is read backwards 
//		ROM[i++] =  lowerStartAddress;
//		ROM[i++] =  UpperStartAddress;
//		
//		ROM[i++] =  OpCodes.STA_ABX.code(); // Store it in local memory
//		ROM[i++] = (byte)(0x00);
//		ROM[i++] =   (byte)(0x40);
//		ROM[i++] = OpCodes.PLA.code();
//		ROM[i++]= OpCodes.TAX.code();
//		ROM[i++] = OpCodes.PLA.code();
//		
//		ROM[i++] = OpCodes.RTI.code();
//		
	}
	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return BussId.MEMROY;
	}

	public AddressRange getAddressRange() {
		// TODO Auto-generated method stub
		return romMemoryMapping;
	}

	public byte read(int address) throws illegalAddressException {
		// TODO Auto-generated method stub
		return ROM[address - 0xf000];
	}
@Override
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		throw new ROException();
	}
	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysbus.raisepowerOnReset();
		
	}
	
		
	


	
}
