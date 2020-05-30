package memoryInterface;

import Firmeware.Framework.OpCodes;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public class basicMemory implements MemoryDriver {
	public final static int  maxMemorySize = 4*1024;
	
	private byte[] memory ;
	
	public basicMemory() {
		memory = new byte[maxMemorySize];
		int i = 0;
		memory[i++] = OpCodes.LDA.code();
		memory[i++] = 0x00;
		memory[i++] = OpCodes.LDA.code();        //   7
		memory[i++] = (byte) 0x0ff;   			       //   8
		memory[i++] = OpCodes.LDA.code();
		memory[i++] = 0x00;
		memory[i++] = OpCodes.HLT.code()	;                  //  15  
		
		  	memory[i++] = OpCodes.NOP.code()	;      //   0
				memory[i++] = OpCodes.LDA.code();		   //   1
				memory[i++] = 0x0a;   		                       //   2
				memory[i++] = OpCodes.LDB.code();		   //   3
				memory[i++] = 0x0c;			                       //   4
				memory[i++] = OpCodes.PHA.code();   //   6
				memory[i++] = OpCodes.LDA.code();        //   7
				memory[i++] = (byte) 0x0ff;   			       //   8
				memory[i++] = OpCodes.PHA.code();  	   //   9 	
		
				memory[i++] = OpCodes.PHA.code();     //  11
				memory[i++] = OpCodes.JSR.code();         // 12
				memory[i++] = 0x00;                                 //  13
				memory[i++] = 0x04;                                 // 14
				memory[i++] = OpCodes.HLT.code()	;                  //  15  
				i = 0x0400;
				memory[i++] = OpCodes.LDXI.code();      // 0x0400
				memory[i++] = 0x02;                               // 0x0401
				memory[i++] = OpCodes.LDA.code();
				memory[i++] = 0x00;
				memory[i++] = OpCodes.RTS.code();      // 0x0402
		 
			
	}
	
	public byte read(int address) throws DeviceUnavailable,illegalAddressException {
		// TODO Auto-generated method stub
		int value = 0;
		if (address < maxMemorySize && address >= 0) {
			value = (int)(memory[address] & 0xff);
		} else {
			throw new illegalAddressException();
		}
	
			return (byte)(value &0xff);
	}

	public void write(int address, byte data) throws illegalAddressException {
		// TODO Auto-generated method stub
		if (address < maxMemorySize && address > 0) {
			 memory[address] = data;
		} else {
			throw new illegalAddressException();
		}
	

	}

	public void blockWrite(int startAddress, int length, byte[] data) {
		// TODO Auto-generated method stub
		
	}

	public byte[] blockRead(int startAddress, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setIOPage(int address) {
		// TODO Auto-generated method stub
		
	}

	public int getIOPage() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void setIOPage(int address, int length) {
		// TODO Auto-generated method stub
		
	}

	public int getIOStartPage() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getLenIOPage() {
		// TODO Auto-generated method stub
		return 0;
	}

	
/*
 * 	memory[i++] = OpCodes.NOP.code()	;      //   0
		memory[i++] = OpCodes.LDA.code();		   //   1
		memory[i++] = 0x0a;   		                       //   2
		memory[i++] = OpCodes.LDB.code();		   //   3
		memory[i++] = 0x0c;			                       //   4
		memory[i++] = OpCodes.ADDB.code();     //   5
		memory[i++] = OpCodes.PUSHA.code();   //   6
		memory[i++] = OpCodes.LDA.code();        //   7
		memory[i++] = (byte) 0x0ff;   			       //   8
		memory[i++] = OpCodes.POPA.code();  	   //   9 	
		memory[i++] = OpCodes.PUSHB.code();   //  10
		memory[i++] = OpCodes.POPA.code();     //  11
		memory[i++] = OpCodes.JSR.code();         // 12
		memory[i++] = 0x00;                                 //  13
		memory[i++] = 0x04;                                 // 14
		memory[i++] = (byte) 0xff; 	                  //  15  
		i = 0x0400;
		memory[i++] = OpCodes.LDX.code();      // 0x0400
		memory[i++] = 0x02;                               // 0x0401
		memory[i++] = OpCodes.RTN.code();      // 0x0402
 */
	

}
