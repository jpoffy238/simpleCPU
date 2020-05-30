package Firmeware.Framework;


import java.util.Map;

import Firmeware.ExecutionFlow.BEQ;
import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.JSR;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.ExecutionFlow.RTS;
import Firmeware.Load.Accumulator.LDA_IMM;
import Firmeware.Load.Accumulator.LDA_ZX;
import Firmeware.Load.IndexX.LDX_ABS;
import Firmeware.Load.IndexX.LDX_IMM;
import Firmeware.Load.IndexX.LDX_ZP;
import Firmeware.Load.IndexX.LDX_ZPY;
import Firmeware.Load.IndexX.LDX_ABSY;
import Firmeware.Stack.PHA;
import Firmeware.Stack.PLA;
import Firmeware.store.Accumulator.STA_ZPX;
import Firmeware.store.Accumulator.STA_ZP;
import MachineState.DecoderMap;

/*
 * 
 */
public  enum OpCodes {
	
	NOP( new NOP()),
	LDA_IMM( new LDA_IMM()), // Immediate 
	LDAX( new LDA_ZX()), // load a from 0 page index x
	BEQ(new BEQ()),
	JMP_ABS(new JMP_ABS()),
	JSR(new JSR()),
	RTS(new RTS()),
	STAZ(new STA_ZP()), 
	STAX(new STA_ZPX()),
	PLA(new PLA()),
	LDXI(new LDX_IMM()),
	LDXZP(new LDX_ZP()),
	LDXZPY(new LDX_ZPY()),
	LDXABS(new LDX_ABS()),
	LDXABSY(new LDX_ABSY()),
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
		String startup = String.format("%-40s",m.getClass().getCanonicalName() );
		startup += String.format("Opcode [%02x]" , opcode);
		
		System.out.println("Adding : " + startup);
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
