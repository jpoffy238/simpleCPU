package Firmeware;

import MachineState.cpu001decoder;

/*
 * decoder.put(new Integer(0x00), new NOP());
		decoder.put(new Integer(0x01), new LDA());
		decoder.put(new Integer(0x02), new LDB());
		decoder.put(new Integer(0x03), new PUSHA());
		decoder.put(new Integer(0x04), new POPA());
		decoder.put(new Integer(0x05), new CMP());
		decoder.put(new Integer(0x06), new JMP());
		decoder.put(new Integer(0x07), new JRZ());
		decoder.put(new Integer(0x08), new ADDB());
		decoder.put(new Integer((int)0x00ff), new HLT());
 */
public  enum OpCodes {
	
	NOP(getOpcode(), new NOP()),
	LDA(getOpcode(), new LDA()), // Immediate 
	LDAX(getOpcode(), new LDAZX()), // load a from 0 page index x
	LDX(getOpcode(), new LDX()),
	LDB(getOpcode(), new LDB()),
	PUSHA(getOpcode(), new PUSHA()),
	POPA(getOpcode(), new POPA()),
	CMP(getOpcode(), new CMP()),
	JMP(getOpcode(), new JMP()),
	JRZ(getOpcode(), new JRZ()),
	
	JSR(getOpcode(), new JSR()),
	RTN(getOpcode(), new RTN()),
	ADDB(getOpcode(), new ADDB()),
	PUSHB( getOpcode(), new PUSHB()),
	POPB(getOpcode(), new POPB()),
	PRTW(getOpcode(), new PRTW()),
	ADCI(getOpcode(), new ADCI()),
	HLT(255, new HLT());
	
	public final  Integer  instructionOpcode ;
	public final machineState microcode;
	private static int i;
	private static int getOpcode() {
		return i++;
	}
	private OpCodes (int code, machineState m) {
		instructionOpcode = new Integer(code);
		microcode = m;
		cpu001decoder.AddInstruction(instructionOpcode, microcode);
	}
	public byte code ( ) {
		return instructionOpcode.byteValue();
	}
}
