package memoryInterface;

import Firmeware.OpCodes;

public class basicMemory implements MemoryDriver {
	public final static int  maxMemorySize = 4*1024;
	
	private byte[] memory ;
	
	public basicMemory() {
		memory = new byte[maxMemorySize];
		int i = 0;
		memory[i++] = OpCodes.NOP.code()	;
		memory[i++] = OpCodes.LDA.code();		
		memory[i++] = 0x0a;   		
		memory[i++] = OpCodes.LDB.code();			
		memory[i++] = 0x0c;			
		memory[i++] = OpCodes.ADDB.code();    
		memory[i++] = OpCodes.PUSHA.code();
		memory[i++] = OpCodes.LDA.code(); 
		memory[i++] = (byte) 0x0ff;   			
		memory[i++] = OpCodes.POPA.code();  		
		memory[i++] = OpCodes.PUSHB.code();
		memory[i++] = OpCodes.POPA.code();
		memory[i++] = OpCodes.JMP.code();
		memory[i++] = 8;
		memory[i++] = (byte) 0xff; 	 //  0a HLT
	}
	
	public byte read(int address) {
		// TODO Auto-generated method stub
		int value = 0;
		if (address < maxMemorySize && address > 0) {
			value = memory[address];
		}
		try {
			Thread.sleep(100);
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
			Thread.sleep(100);
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

	

	

}
