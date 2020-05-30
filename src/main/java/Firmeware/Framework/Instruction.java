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
public int getAbsoluteAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	byte opperand_lower = c.memory.read(c.pc); 
	
	byte opperand_upper = c.memory.read(c.pc+1); 
	
	int loadAddress = opperand_upper << 8 + opperand_lower;
	return loadAddress;
}
public int getAbsoluteAddressY(CPU c) throws illegalAddressException, DeviceUnavailable {
	int loadAddress = getAbsoluteAddress(c);
	loadAddress += c.y.get();
	return loadAddress;
}
public int getAbsoluteAddressX(CPU c) throws illegalAddressException, DeviceUnavailable {
	int loadAddress = getAbsoluteAddress(c);
	loadAddress += c.x.get();
	return loadAddress;
}
public int getZeroPageXAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int operand = c.memory.read(c.pc);
	int address  = (int)(operand + c.x.get()) & 0x00ff;
	return address;
}
public int getZeroPageYAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
	int operand = c.memory.read(c.pc);
	int address  = (int)(operand + c.y.get()) & 0x00ff;
	return address;
}
public int getIndirect(CPU c) throws illegalAddressException, DeviceUnavailable {
	int lower = c.memory.read(c.pc);
	int upper = c.memory.read(c.pc+1);
	int lookupAddress = (upper&0x00ff) << 8  + (lower + 0x00ff);
	lower = c.memory.read(lookupAddress) ;
	upper = c.memory.read(lookupAddress+1) ;
	lookupAddress = (upper&0x00ff) << 8  + (lower + 0x00ff);
	return lookupAddress;
}
public int getIndexX (CPU c) throws illegalAddressException, DeviceUnavailable {
	int address;
	int operand = c.memory.read(c.pc);
	int pageZero = (operand + c.x.get()) & 0x00ff;
	int lower = c.memory.read(pageZero);
	int upper = c.memory.read(pageZero+1);
	address =   (upper&0x00ff) << 8  + (lower + 0x00ff);
	return address;
}public int getIndexY (CPU c) throws illegalAddressException, DeviceUnavailable {
	int address;
	int operand = c.memory.read(c.pc);
	int pageZero = (operand + c.y.get()) & 0x00ff;
	int lower = c.memory.read(pageZero);
	int upper = c.memory.read(pageZero+1);
	address =   (upper&0x00ff) << 8  + (lower + 0x00ff);
	return address;
}
public void handleZException (CPU c) {
	c.ZFLAG.set();
	c.NFLAG.clear();
}
public void handleNException(CPU c) {
	c.NFLAG.set();
	c.ZFLAG.clear();
}
}
