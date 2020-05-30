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

}
