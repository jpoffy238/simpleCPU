package Firmeware.Framework;

import java.util.HashMap;
import java.util.Map;

import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;

public abstract class Instruction implements machineState {
	
	private byte opCode;
	
	public final String KEY_OPCODE = "opcode";
	public final String KEY_MNEMONIC = "Mnemonic";
	public final String KEY_INSTRUCTION_SIZE = "InstructionSize";
	public final String KEY_CYCLES = "Cycles";
	public final String KEY_FLAGS_EFFECTED = "Flags";
	public final String KEY_WEB ="Web Reference";
	public final String KEY_DESCRIPTION = "Description";
	public final String KEY_ADDRESSING_MODE = "AddressingMode";
	public final String VALUE_ADDM_IMM = "Immediate";
	public final String VALUE_ADDM_IMP = "Implied";
	public final String VALUE_ADDM_ZP = "Zero Page";
	public final String VALUE_ADDM_ZPX = "Zero Page,X";
	public final String VALUE_ADDM_ZPY = "Zero Page,Y";
	public final String VALUE_ADDM_ABS = "Absolute";
	public final String VALUE_ADDM_ABX = "Absolute,X";
	public final String VALUE_ADDM_ABY = "Absolute,Y";

	public final String VALUE_ADDM_IDX = "Indirect,X";
	public final String VALUE_ADDM_IDY = "Indirect,Y";
	public final String VALUE_ADDM_REL = "Relative offset";
	public final String VALUE_ADDM_STK = "From Stack";
	
			/* Immediate     LDA #$44      $A9  2   2
* 	Zero Page     LDA $44       $A5  2   3
Zero Page,X   LDA $44,X     $B5  2   4
Absolute      LDA $4400     $AD  3   4
Absolute,X    LDA $4400,X   $BD  3   4+
Absolute,Y    LDA $4400,Y   $B9  3   4+
Indirect,X    LDA ($44,X)   $A1  2   6
Indirect,Y    LDA ($44),Y   $B1  2   5+
*/
	private Map<String, String>  OpCodeProperties = new HashMap<String, String>();
	public Instruction(byte op)  {
		opCode = op;
	}
	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable{
		// TODO Auto-generated method stub
	}
	public byte getOpCode() {
		return opCode;
	}
public void  setProperty (String propertyName, String value) {
	OpCodeProperties.put(propertyName, value);
}
public String getProperty(String propertyName ) {
	return OpCodeProperties.get(propertyName);
}
protected int getAbsoluteAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int  opperand_lower = c.memory.read(c.pc); 
	System.out.println(String.format("ADDR: %04x Lower Operand = %02x" ,c.pc,  opperand_lower));
	
	int opperand_upper = (int)(c.memory.read(c.pc+1) & 0x00ff);
	System.out.println(String.format("ADDR: %04x Upper Operand = %02x" ,(c.pc+1), opperand_upper));
	
	int loadAddress = (((opperand_upper << 8) & 0xff00) + opperand_lower) & 0x0000ffff;
	System.out.println (String.format("Final Address %04x ", loadAddress));
	return loadAddress;
}
protected int getAbsoluteAddressY(CPU c) throws illegalAddressException, DeviceUnavailable {
	int loadAddress = getAbsoluteAddress(c);
	loadAddress += c.y.get();
	return loadAddress;
}
protected int getAbsoluteAddressX(CPU c) throws illegalAddressException, DeviceUnavailable {
	int loadAddress = getAbsoluteAddress(c);
	loadAddress += c.x.get();
	return loadAddress;
}
protected int getZeroPageAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int operand = c.memory.read(c.pc);
	int address  = (int)(operand) & 0x00ff;
	return address;
}
protected int getZeroPageXAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int operand = c.memory.read(c.pc);
	int address  = (int)(operand + c.x.get()) & 0x00ff;
	return address;
}
protected int getZeroPageYAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int operand = c.memory.read(c.pc);
	int address  = (int)(operand + c.y.get()) & 0x00ff;
	return address;
}
protected int getIndirect(CPU c) throws illegalAddressException, DeviceUnavailable {
	int lower = c.memory.read(c.pc);
	int upper = c.memory.read(c.pc+1);
	int lookupAddress = (upper&0x00ff) << 8  + (lower + 0x00ff);
	lower = c.memory.read(lookupAddress) ;
	upper = c.memory.read(lookupAddress+1) ;
	lookupAddress = (upper&0x00ff) << 8  + (lower + 0x00ff);
	return lookupAddress;
}
protected int getIndexX (CPU c) throws illegalAddressException, DeviceUnavailable {
	int address;
	int operand = c.memory.read(c.pc);
	int pageZero = (operand + c.x.get()) & 0x00ff;
	int lower = c.memory.read(pageZero);
	int upper = c.memory.read(pageZero+1);
	address =   (upper&0x00ff) << 8  + (lower + 0x00ff);
	return address;
}protected int getIndexY (CPU c) throws illegalAddressException, DeviceUnavailable {
	int address;
	int operand = c.memory.read(c.pc);
	int pageZero = (operand + c.y.get()) & 0x00ff;
	int lower = c.memory.read(pageZero);
	int upper = c.memory.read(pageZero+1);
	address =   (upper&0x00ff) << 8  + (lower + 0x00ff);
	return address;
}
protected void handleZException (CPU c) {
	c.ZFLAG.set();
	c.NFLAG.clear();
}
protected void handleNException(CPU c) {
	c.NFLAG.set();
	c.ZFLAG.clear();
}
protected byte psr(CPU c) {
	int status = 0;
	if (c.CFLAG.isSet()) {
		status = 1;
	}
	status <<= 1;
	if (c.ZFLAG.isSet()) {
		status+= 1;
	} 
	status <<= 1;
	if (c.IFLAG.isSet()) {
		status += 1;
	}
	status <<= 1;
	if (c.DFLAG.isSet()) {
		status += 1;
	}
	status <<= 1;
	if (c.BFLAG.isSet()) {
		status += 1;
	}
	status <<= 1;
	if (c.OFLAG.isSet()) {
		status += 1;
	}
	status <<= 1;
	if (c.NFLAG.isSet()) {
		status += 1;
	}
		
	return (byte)(status & 0xff);
}
protected void setFlags(CPU c, byte sr) {
	int tmpsr = (int)(sr & 0xff);
	int mask = 1;
	
	if ((tmpsr & mask) == 0) {
		c.NFLAG.clear();
	} else {
		c.NFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.OFLAG.clear();
	} else {
		c.OFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.BFLAG.clear();
	} else {
		c.BFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.DFLAG.clear();
	} else {
		c.DFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.IFLAG.clear();
	} else {
		c.IFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.ZFLAG.clear();
	} else {
		c.ZFLAG.set();
	}
	mask <<= 1;
	if ((tmpsr & mask) == 0) {
		c.CFLAG.clear();
	} else {
		c.CFLAG.set();
	}
}
}
