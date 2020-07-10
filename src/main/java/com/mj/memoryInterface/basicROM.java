package com.mj.memoryInterface;

import com.mj.Devices.MemoryRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class basicROM implements MemoryDriver {
	private   int RTCstartAddress;
	private  int RTCEndAddress;
	PBus sysbus;
	private byte[] ROM = new  byte[0xffff-0xf000 + 1];
	public basicROM(PBus bus) {
		RTCstartAddress = 0xa000;
		RTCEndAddress =  RTCstartAddress  +21;
		byte lowerStartAddress = (byte)(RTCstartAddress&0x00ff);
		byte UpperStartAddress = (byte)((RTCstartAddress >> 8) &0x00ff);
		int MARKER;
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
		
		int i =   0;
		
		ROM[i++] = OpCodes.PHA.code(); // Store Acc
		ROM[i++] = OpCodes.TXA.code();
		ROM[i++] = OpCodes.PHA.code();
		ROM[i++] = OpCodes.LDX_IMM.code();  // Load length into X
		ROM[i++] = (byte)(20) ;

 		MARKER=i; // For branch location
 
		ROM[i++] =  OpCodes.LDA_ABSX.code(); // Clock is read backwards 
		ROM[i++] =  (byte)lowerStartAddress;
		ROM[i++] = (byte)UpperStartAddress;
		ROM[i++] =  OpCodes.STA_ABX.code(); // Store it in local memory
		ROM[i++] =  (byte)(0x00);
		ROM[i++] =   (byte)(0x40);
		ROM[i++] =  OpCodes.DEX.code();  // dec x to point to next location
		ROM[i++] =  OpCodes.BNE.code(); // branch for next			 
		ROM[i++] =  (byte) (MARKER - i +1);
		ROM[i++] =  OpCodes.LDA_ABSX.code(); // Clock is read backwards 
		ROM[i++] =  lowerStartAddress;
		ROM[i++] =  UpperStartAddress;
		
		ROM[i++] =  OpCodes.STA_ABX.code(); // Store it in local memory
		ROM[i++] = (byte)(0x00);
		ROM[i++] =   (byte)(0x40);
		ROM[i++] = OpCodes.PLA.code();
		ROM[i++]= OpCodes.TAX.code();
		ROM[i++] = OpCodes.PLA.code();
		
		ROM[i++] = OpCodes.RTI.code();
		
	}
	public DEVTYPE getDeviceType() {
		// TODO Auto-generated method stub
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		// TODO Auto-generated method stub
		return BussId.MEMROY;
	}

	public MemoryRange getAddressRange() {
		// TODO Auto-generated method stub
		return new MemoryRange(0xf000, 0xffff);
	}

	public byte read(int address) throws illegalAddressException {
		// TODO Auto-generated method stub
		return ROM[address - 0xf000];
	}

	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		throw new ROException();
	}
	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysbus.raisepowerOnReset();
		
	}
	@Override
	public void load(String fileName, int startAddress) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		
	}

}
