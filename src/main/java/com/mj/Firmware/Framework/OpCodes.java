package com.mj.Firmware.Framework;


import java.util.Map;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.ExecutionFlow.BCC;
import com.mj.Firmware.ExecutionFlow.BCS;
import com.mj.Firmware.ExecutionFlow.BEQ;
import com.mj.Firmware.ExecutionFlow.BNE;
import com.mj.Firmware.ExecutionFlow.BRK;
import com.mj.Firmware.ExecutionFlow.HLT;
import com.mj.Firmware.ExecutionFlow.JMP_ABS;
import com.mj.Firmware.ExecutionFlow.JSR;
import com.mj.Firmware.ExecutionFlow.NOP;
import com.mj.Firmware.ExecutionFlow.RTS;
import com.mj.Firmware.Load.Accumulator.LDA_ABS;
import com.mj.Firmware.Load.Accumulator.LDA_ABSX;
import com.mj.Firmware.Load.Accumulator.LDA_ABSY;
import com.mj.Firmware.Load.Accumulator.LDA_IMM;
import com.mj.Firmware.Load.Accumulator.LDA_INDX;
import com.mj.Firmware.Load.Accumulator.LDA_INDY;
import com.mj.Firmware.Load.Accumulator.LDA_ZP;
import com.mj.Firmware.Load.Accumulator.LDA_ZX;
import com.mj.Firmware.Load.IndexX.LDX_ABS;
import com.mj.Firmware.Load.IndexX.LDX_ABSY;
import com.mj.Firmware.Load.IndexX.LDX_IMM;
import com.mj.Firmware.Load.IndexX.LDX_ZP;
import com.mj.Firmware.Load.IndexX.LDX_ZPY;
import com.mj.Firmware.Load.IndexY.LDY_ABS;
import com.mj.Firmware.Load.IndexY.LDY_ABSX;
import com.mj.Firmware.Load.IndexY.LDY_IMM;
import com.mj.Firmware.Load.IndexY.LDY_ZP;
import com.mj.Firmware.Load.IndexY.LDY_ZPX;
import com.mj.Firmware.Logic.*;
import com.mj.Firmware.Logic.AND.AND_ABS;
import com.mj.Firmware.Logic.AND.AND_ABX;
import com.mj.Firmware.Logic.AND.AND_ABY;
import com.mj.Firmware.Logic.AND.AND_IMM;
import com.mj.Firmware.Logic.AND.AND_INX;
import com.mj.Firmware.Logic.AND.AND_INY;
import com.mj.Firmware.Logic.AND.AND_ZP;
import com.mj.Firmware.Logic.AND.AND_ZPX;
import com.mj.Firmware.Logic.ASL.ASL;
import com.mj.Firmware.Logic.ASL.ASL_ZP;
import com.mj.Firmware.Logic.ASL.ASL_ZPX;
import com.mj.Firmware.Logic.ASL.ASL_ABS;
import com.mj.Firmware.Logic.ASL.ASL_ABSX;
import com.mj.Firmware.Logic.EOR.EOR_ABS;
import com.mj.Firmware.Logic.EOR.EOR_ABSX;
import com.mj.Firmware.Logic.EOR.EOR_ABSY;
import com.mj.Firmware.Logic.EOR.EOR_IMM;
import com.mj.Firmware.Logic.EOR.EOR_ZP;
import com.mj.Firmware.Logic.EOR.EOR_ZPX;
import com.mj.Firmware.Logic.EOR.EOR_INX;
import com.mj.Firmware.Logic.EOR.EOR_INY;
import com.mj.Firmware.Logic.ROL.*;
import com.mj.Firmware.Stack.PHA;
import com.mj.Firmware.Stack.PLA;
import com.mj.Firmware.Transfers.TAX;
import com.mj.Firmware.Transfers.TAY;
import com.mj.Firmware.Transfers.TSX;
import com.mj.Firmware.Transfers.TXA;
import com.mj.Firmware.Transfers.TXS;
import com.mj.Firmware.Transfers.TYA;
import com.mj.Firmware.incdec.DEC_ABS;
import com.mj.Firmware.incdec.DEC_ABX;
import com.mj.Firmware.incdec.DEC_ZP;
import com.mj.Firmware.incdec.DEC_ZPX;
import com.mj.Firmware.incdec.DEX;
import com.mj.Firmware.incdec.DEY;
import com.mj.Firmware.incdec.INC_ABS;
import com.mj.Firmware.incdec.INC_ABX;
import com.mj.Firmware.incdec.INC_ZP;
import com.mj.Firmware.incdec.INC_ZPX;
import com.mj.Firmware.incdec.INX;
import com.mj.Firmware.incdec.INY;
import com.mj.Firmware.store.Accumulator.STA_ABS;
import com.mj.Firmware.store.Accumulator.STA_ABX;
import com.mj.Firmware.store.Accumulator.STA_ABY;
import com.mj.Firmware.store.Accumulator.STA_INX;
import com.mj.Firmware.store.Accumulator.STA_INY;
import com.mj.Firmware.store.Accumulator.STA_ZP;
import com.mj.Firmware.store.Accumulator.STA_ZPX;
import com.mj.Firmware.store.IndexX.STX_ABS;
import com.mj.Firmware.store.IndexX.STX_ZP;
import com.mj.Firmware.store.IndexX.STX_ZPY;
import com.mj.Firmware.store.IndexY.STY_ABS;
import com.mj.Firmware.store.IndexY.STY_ZP;
import com.mj.Firmware.store.IndexY.STY_ZPX;
import com.mj.Firmware.StatusFlags.*;
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
	STA_ZPX(new STA_ZPX()),// TODO Auto-generated catch block


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
	BCC(new BCC()),
	BCS(new BCS()),
	BIT_ZP(new BIT_ZP()),
	BIT_ABS(new BIT_ABS()),
	AND_IMM(new AND_IMM()),
	AND_ZP(new AND_ZP()),
	AND_ZPX(new AND_ZPX()),
	AND_ABS(new AND_ABS()),
	AND_ABX(new AND_ABX()),
	AND_ABY(new AND_ABY()),
	AND_INX(new AND_INX()),
	AND_INY(new AND_INY()),
	
	EOR_ABS(new EOR_ABS()),
	EOR_IMM(new EOR_IMM()),
	EOR_ZP(new EOR_ZP()),
	EOR_ZPX(new EOR_ZPX()),
	EOR_ABSX(new EOR_ABSX()),
	EOR_ABSY(new EOR_ABSY()),
	EOR_INX(new EOR_INX()),
	EOR_INY(new EOR_INY()),
	ASL(new ASL()),
	ASL_ZP(new ASL_ZP()),
	ASL_ZPX(new ASL_ZPX()),
	ASL_ABS(new ASL_ABS()),
	ASL_ABSX(new ASL_ABSX()),
	ROL_ABS(new ROL_ABS()),
	ROL_ACC(new ROL_ACC()),
	ROL_ZP(new ROL_ZP()),
	ROL_ZPX(new ROL_ZPX()),
	ROL_ABX(new ROL_ABX()),
	BNE(new BNE()),
	JMP_ABS(new JMP_ABS()),
	JSR(new JSR()),
	RTS(new RTS()),
	BRK(new BRK()),

	PLA(new PLA()),

	PHA(new PHA()),
	
	CLC(new CLC()),
	CLD(new CLD()),
	CLV(new CLV()),
	CLI(new CLI()),
	SEC(new SEC()),
	SED(new SED()),
	SEI(new SEI()),
	
	//TEST(new TEST()),
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
		final  Logger logger = LogManager.getLogger("OpCodes");
		
		String startup = String.format("%-40s",m.getClass().getCanonicalName() );
		startup += String.format("Opcode [%02x]" , opcode);
		
		logger.debug("Adding : " + startup);
		if (getMap().containsKey(opcode)) {
			machineState tmp = getMap().get(opcode);
			logger.error("Error duplicate opcode" + opcode.toString());
			logger.error("Failed to add  : " + m.getClass().getCanonicalName());
			logger.error("Failed dulicate: " + tmp.getClass().getCanonicalName());
			System.exit(8);
		}
		getMap().put(opcode, m);
	}
	public static Map<Integer, machineState>  getMap() {
		 
		return DecoderMap.getMap();
			 
	}
}
