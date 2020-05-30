package Firmeware.Framework;


import java.util.Map;

import Firmeware.ExecutionFlow.BEQ;
import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.JSR;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.ExecutionFlow.RTS;
import Firmeware.Load.LDAZX;
import Firmeware.Load.LDA_IMM;
import Firmeware.Load.LDXABS;
import Firmeware.Load.LDXABSY;
import Firmeware.Load.LDXI;
import Firmeware.Load.LDXZP;
import Firmeware.Load.LDXZPY;
import Firmeware.Stack.PHA;
import Firmeware.Stack.PLA;
import Firmeware.store.STAX;
import Firmeware.store.STAZ;
import MachineState.DecoderMap;

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
	LDB(new Firmeware.Load.LDB()),
	LDXI(new LDXI()),
	LDXZP(new LDXZP()),
	LDXZPY(new LDXZPY()),
	LDXABS(new LDXABS()),
	LDXABSY(new LDXABSY()),
	PHA(new PHA()),
HLT( new HLT());
	
	public final  Integer  instructionOpcode ;
	public final machineState microcode;
	
	private   OpCodes ( machineState m) {
		instructionOpcode = new Integer( (int)(m.getOpCode() & 0x00ff ));
		microcode = m;
		//cpu001decoder.AddInstruction(instructionOpcode, microcode);
		AddInstruction(instructionOpcode, microcode);
	}
	public byte code ( ) {
		return instructionOpcode.byteValue();
	}
	private static  void AddInstruction(Integer opcode, machineState m) {
		
		System.out.println("Adding : " + m.getClass().getCanonicalName());
		if (getMap().containsKey(opcode)) {
			machineState tmp = getMap().get(opcode);
			System.err.println("Error duplicate opcode" + opcode.toString());
			System.err.println("Failed to add  : " + m.getClass().getCanonicalName());
			System.err.println("Failed dulicate: " + tmp.getClass().getCanonicalName());
			System.exit(8);
		}
		getMap().put(opcode, m);
	}
	public static Map<Integer, machineState>  getMap() {
		 
		return DecoderMap.getMap();
			 
	}
}
