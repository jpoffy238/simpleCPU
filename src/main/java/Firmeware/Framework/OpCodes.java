package Firmeware.Framework;

import Firmeware.ExecutionFlow.BEQ;
import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.JSR;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.ExecutionFlow.RTS;
import Firmeware.Load.LDAZX;
import Firmeware.Load.LDA_IMM;
import Firmeware.Stack.PLA;
import Firmeware.store.STAX;
import Firmeware.store.STAZ;
import MachineState.cpu001decoder;

/*
 * 
 */
public  enum OpCodes {
	
	NOP( new NOP()),
	LDA( new LDA_IMM()), // Immediate 
	LDAX( new LDAZX()), // load a from 0 page index x
	BEQ(new BEQ()),
	JMP_ABS(new JMP_ABS()),
	JSR(new JSR()),
	RTS(new RTS()),
	STAZ(new STAZ()), 
	STAX(new STAX()),
	PLA(new PLA()),
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
