package Firmeware.Framework;


import java.util.Map;
import Firmeware.Framework.*;
import Firmeware.ExecutionFlow.BEQ;
import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.JSR;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.ExecutionFlow.RTS;
import Firmeware.Load.Accumulator.*;
import Firmeware.store.Accumulator.*;
import Firmeware.Load.IndexY.*;
import Firmeware.store.IndexX.*;
import Firmeware.store.IndexY.*;
import Firmeware.Transfers.*;
import Firmeware.incdec.*;
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
	LDA_ABS(new LDA_ABS()),
	LDA_ABSX(new LDA_ABSX()),
	LDA_ABSY(new LDA_ABSY()),
	LDA_INDX(new LDA_INDX()),
	LDA_INDY(new LDA_INDY()),
	LDA_ZP(new LDA_ZP()),
	LDA_ZX(new LDA_ZX()),
	
	LDX_ABS(new LDX_ABS()),
	LDX_ABSY(new LDX_ABSY()),
	LDX_IMM(new LDX_IMM()),
	LDX_ZP(new LDX_ZP()),
	LDX_ZPY(new LDX_ZPY()),
	
	LDY_ABS(new LDY_ABS()),
	LDY_ABSX(new LDY_ABSX()),
	LDY_IMM(new LDY_IMM()),
	LDY_ZP(new LDY_ZP()),
	LDY_ZPX(new LDY_ZPX()),
	
	STA_ABS(new STA_ABS()),
	STA_ABX(new STA_ABX()),
	STA_ABY(new STA_ABY()),
	STA_INX(new STA_INX()),
	STA_INY(new STA_INY()),
	STA_ZP(new STA_ZP()),
	STA_ZPX(new STA_ZPX()),

	STX_ABS(new STX_ABS()),
	STX_ZP(new STX_ZP()),
	STX_ZPY(new STX_ZPY()),
	
	STY_ABS(new STY_ABS()),
	STY_ZP(new STY_ZP()),
	STY_ZPX(new STY_ZPX()),
	
	TAX(new TAX()),
	TAY(new TAY()),
	TSX(new TSX()),
	TXA(new TXA()),
	TXS(new TXS()),
	TYA(new TYA()),

	DEC_ABS(new DEC_ABS()),
	DEC_ABX(new DEC_ABX()),
	DEC_ZP(new DEC_ZP()),
	DEC_ZPX(new DEC_ZPX()),
	INC_ABS(new INC_ABS()),
	INC_ABX(new INC_ABX()),
	INC_ZP(new INC_ZP()),
	INC_ZPX(new INC_ZPX()),
	DEX(new DEX()),
	DEY(new DEY()),
	INX(new INX()),
	INY(new INY()),
	
	BEQ(new BEQ()),
	JMP_ABS(new JMP_ABS()),
	JSR(new JSR()),
	RTS(new RTS()),

	PLA(new PLA()),

	PHA(new PHA()),
	
	TEST(new TEST()),
	MIH(new MIH()),
	
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
