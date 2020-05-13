package memoryInterface;

import Firmeware.OpCodes;

public class basicMemory implements MemoryDriver {
	public final static int  maxMemorySize = 4*1024;
	
	private byte[] memory ;
	
	public basicMemory() {
		memory = new byte[maxMemorySize];
		int i = 0;
		memory[i++] = OpCodes.ADCI.code();
		memory[i++] = 0x01;
		memory[i++] = OpCodes.ADCI.code();
		memory[i++] = 0x22;
		memory[i++] = OpCodes.ADCI.code();
		memory[i++] = 0x7f;
		memory[i++] = OpCodes.HLT.code();
	}
	
	public byte read(int address) {
		// TODO Auto-generated method stub
		int value = 0;
		if (address < maxMemorySize && address >= 0) {
			value = memory[address];
		}
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			return (byte)(value &0xff);
	}

	public void write(int address, byte data) {
		// TODO Auto-generated method stub
		if (address < maxMemorySize && address > 0) {
			 memory[address] = data;
		}
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
