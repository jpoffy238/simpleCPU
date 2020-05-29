package Firmeware.Framework;

import Firmeware.PRTW;
import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.JRZ;
import Firmeware.ExecutionFlow.JSR;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.ExecutionFlow.RTN;
import Firmeware.Load.LDA_IMM;
import Firmeware.Load.LDAZX;
import Firmeware.Load.LDB;
import Firmeware.Load.LDX;
import Firmeware.Logic.CMP;
import Firmeware.Math.ADCI;
import Firmeware.Math.ADDB;
import Firmeware.Stack.POPA;
import Firmeware.Stack.POPB;
import Firmeware.Stack.PUSHA;
import Firmeware.Stack.PUSHB;
import Firmeware.store.STAX;
import Firmeware.store.STAZ;
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
	
	NOP( new NOP()),
	LDA( new LDA_IMM()), // Immediate 
	LDAX( new LDAZX()), // load a from 0 page index x
//	LDX( new LDX()),
//	LDB( new LDB()),
//	PUSHA( new PUSHA()),
//	POPA( new POPA()),
//	CMP( new CMP()),
//	JMP_ABS( new JMP_ABS()),
//	JRZ( new JRZ()),
//	
//	JSR( new JSR()),
//	RTN( new RTN()),
//	ADDB( new ADDB()),
//	PUSHB(  new PUSHB()),
//	POPB( new POPB()),
//	PRTW( new PRTW()),
//	ADCI( new ADCI()),
//	STAZ( new STAZ()),
//	STAX( new STAX()),
HLT( new HLT());
	
	public final  Integer  instructionOpcode ;
	public final machineState microcode;
	
	private   OpCodes ( machineState m) {
		instructionOpcode = new Integer( (int)(m.getOpCode() & 0x00ff ));
		microcode = m;
		cpu001decoder.AddInstruction(instructionOpcode, microcode);
	}
	public byte code ( ) {
		return instructionOpcode.byteValue();
	}
}
