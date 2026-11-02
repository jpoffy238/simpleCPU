package com.mj.Firmware.Framework;


import java.util.Map;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Table.InstructionTable;
import com.mj.Firmware.ExecutionFlow.BCC;
import com.mj.Firmware.ExecutionFlow.BCS;
import com.mj.Firmware.ExecutionFlow.BEQ;
import com.mj.Firmware.ExecutionFlow.BVC;
import com.mj.Firmware.ExecutionFlow.*;

import com.mj.Firmware.ExecutionFlow.BNE;
import com.mj.Firmware.ExecutionFlow.BRK;
import com.mj.Firmware.ExecutionFlow.HLT;
import com.mj.Firmware.ExecutionFlow.JMP_ABS;
import com.mj.Firmware.ExecutionFlow.JMP_IND;
import com.mj.Firmware.ExecutionFlow.JSR;
import com.mj.Firmware.ExecutionFlow.NOP;
import com.mj.Firmware.ExecutionFlow.RTS;
import com.mj.Firmware.ExecutionFlow.RTI;

import com.mj.Firmware.Logic.*;
import com.mj.Firmware.Logic.Shift.ASL.ASL;
import com.mj.Firmware.Logic.Shift.ASL.ASL_ABS;
import com.mj.Firmware.Logic.Shift.ASL.ASL_ABSX;
import com.mj.Firmware.Logic.Shift.ASL.ASL_ZP;
import com.mj.Firmware.Logic.Shift.ASL.ASL_ZPX;
import com.mj.Firmware.Logic.Shift.ROL.*;
import com.mj.Firmware.Logic.Shift.ROR.*;
import com.mj.Firmware.Stack.PHA;
import com.mj.Firmware.Stack.PLA;
import com.mj.Firmware.Stack.PHP;
import com.mj.Firmware.Stack.PLP;
import com.mj.Firmware.Transfers.TAX;
import com.mj.Firmware.Transfers.TAY;
import com.mj.Firmware.Transfers.TSX;
import com.mj.Firmware.Transfers.TXA;
import com.mj.Firmware.Transfers.TXS;
import com.mj.Firmware.Transfers.TYA;
import com.mj.Firmware.StatusFlags.*;
import com.mj.Firmware.Math.ADC.*;
import com.mj.Firmware.Logic.Shift.LSR.*;
import com.mj.Firmware.Math.SBC.*;
import com.mj.Firmware.Math.ADC.ADC_IMM;
import com.mj.Firmware.Logic.CMP.*;

/*
 * 
 */


public  enum OpCodes {
	
	ADC_IMM(new ADC_IMM()),
	ADC_ZP(new ADC_ZP()),
	ADC_ZPX(new ADC_ZPX()),
	ADC_ABS(new ADC_ABS()),
	ADC_ABSX(new ADC_ABSX()),
	ADC_ABSY(new ADC_ABSY()),
	ADC_INX(new ADC_INX()),
	ADC_INY(new ADC_INY()),
	SBC_ABS(new SBC_ABS()),
	SBC_ZPX(new SBC_ZPX()),
	SBC_ABSX(new SBC_ABSX()),
	SBC_ZP(new SBC_ZP()),
	SBC_INY(new SBC_INY()),
	SBC_INX(new SBC_INX()),
	SBC_IMM(new SBC_IMM()),
	SBC_ABSY(new SBC_ABSY()),
	
	MIH(new MIH()),  // Special instruction to handle interrupts
	
	NOP( new NOP()),
	LDA_IMM(InstructionTable.LDA_IMM), // Immediate 
	LDA_ABS(InstructionTable.LDA_ABS),
	LDA_ABSX(InstructionTable.LDA_ABSX),
	LDA_ABSY(InstructionTable.LDA_ABSY),
	LDA_INDX(InstructionTable.LDA_INDX),
	LDA_INDY(InstructionTable.LDA_INDY),
	LDA_ZP(InstructionTable.LDA_ZP),
	LDA_ZX(InstructionTable.LDA_ZX),
	
	LDX_ABS(InstructionTable.LDX_ABS),
	LDX_ABSY(InstructionTable.LDX_ABSY),
	LDX_IMM(InstructionTable.LDX_IMM),
	LDX_ZP(InstructionTable.LDX_ZP),
	LDX_ZPY(InstructionTable.LDX_ZPY),
	
	LDY_ABS(InstructionTable.LDY_ABS),
	LDY_ABSX(InstructionTable.LDY_ABSX),
	LDY_IMM(InstructionTable.LDY_IMM),
	LDY_ZP(InstructionTable.LDY_ZP),
	LDY_ZPX(InstructionTable.LDY_ZPX),
	
	STA_ABS(InstructionTable.STA_ABS),
	STA_ABX(InstructionTable.STA_ABX),
	STA_ABY(InstructionTable.STA_ABY),
	STA_INX(InstructionTable.STA_INX),
	STA_INY(InstructionTable.STA_INY),
	STA_ZP(InstructionTable.STA_ZP),
	STA_ZPX(InstructionTable.STA_ZPX),// TODO Auto-generated catch block


	STX_ABS(InstructionTable.STX_ABS),
	STX_ZP(InstructionTable.STX_ZP),
	STX_ZPY(InstructionTable.STX_ZPY),
	
	STY_ABS(InstructionTable.STY_ABS),
	STY_ZP(InstructionTable.STY_ZP),
	STY_ZPX(InstructionTable.STY_ZPX),
	
	TAX(new TAX()),
	TAY(new TAY()),
	TSX(new TSX()),
	TXA(new TXA()),
	TXS(new TXS()),
	TYA(new TYA()),

	DEC_ABS(InstructionTable.DEC_ABS),
	DEC_ABX(InstructionTable.DEC_ABX),
	DEC_ZP(InstructionTable.DEC_ZP),
	DEC_ZPX(InstructionTable.DEC_ZPX),
	INC_ABS(InstructionTable.INC_ABS),
	INC_ABX(InstructionTable.INC_ABX),
	INC_ZP(InstructionTable.INC_ZP),
	INC_ZPX(InstructionTable.INC_ZPX),
	DEX(InstructionTable.DEX),
	DEY(InstructionTable.DEY),
	INX(InstructionTable.INX),
	INY(InstructionTable.INY),
	
	BEQ(new BEQ()),
	BCC(new BCC()),
	BCS(new BCS()),
	BVC(new BVC()),
	BVS(new BVS()),
	BMI(new BMI()),
	BPL(new BPL()),
	BIT_ZP(new BIT_ZP()),
	BIT_ABS(new BIT_ABS()),
	AND_IMM(InstructionTable.AND_IMM),
	AND_ZP(InstructionTable.AND_ZP),
	AND_ZPX(InstructionTable.AND_ZPX),
	AND_ABS(InstructionTable.AND_ABS),
	AND_ABX(InstructionTable.AND_ABX),
	AND_ABY(InstructionTable.AND_ABY),
	AND_INX(InstructionTable.AND_INX),
	AND_INY(InstructionTable.AND_INY),
	
	EOR_ABS(InstructionTable.EOR_ABS),
	EOR_IMM(InstructionTable.EOR_IMM),
	EOR_ZP(InstructionTable.EOR_ZP),
	EOR_ZPX(InstructionTable.EOR_ZPX),
	EOR_ABSX(InstructionTable.EOR_ABSX),
	EOR_ABSY(InstructionTable.EOR_ABSY),
	EOR_INX(InstructionTable.EOR_INX),
	EOR_INY(InstructionTable.EOR_INY),
	ORA_ABS(InstructionTable.ORA_ABS),
	ORA_ABSX(InstructionTable.ORA_ABSX),
	ORA_ZPX(InstructionTable.ORA_ZPX),
	ORA_ZP(InstructionTable.ORA_ZP),
	ORA_INY(InstructionTable.ORA_INY),
	ORA_IMM(InstructionTable.ORA_IMM),
	ORA_INX(InstructionTable.ORA_INX),
	ORA_ABSY(InstructionTable.ORA_ABSY),
	ASL(new ASL()),
	ASL_ZP(new ASL_ZP()),
	ASL_ZPX(new ASL_ZPX()),
	ASL_ABS(new ASL_ABS()),
	ASL_ABSX(new ASL_ABSX()),
	LSR_ACC(new LSR_ACC()),
	LSR_ZP(new LSR_ZP()),
	LSR_ZPX(new LSR_ZPX()),
	LSR_ABS(new LSR_ABS()),
	LSR_ABX(new LSR_ABX()),
	ROL_ABS(new ROL_ABS()),
	ROL_ACC(new ROL_ACC()),
	ROL_ZP(new ROL_ZP()),
	ROL_ZPX(new ROL_ZPX()),
	ROL_ABX(new ROL_ABX()),
	
	
	ROR_ABS(new ROR_ABS()),
	ROR_ABX(new ROR_ABX()),
	ROR_ACC(new ROR_ACC()),
	ROR_ZP(new ROR_ZP()),
	ROR_ZPX(new ROR_ZPX()),
	
	BNE(new BNE()),
	JMP_ABS(new JMP_ABS()),
	JMP_IND(new JMP_IND()),
	JSR(new JSR()),
	RTS(new RTS()),
	RTI(new RTI()),
	
	BRK(new BRK()),

	PLA(new PLA()),

	PHA(new PHA()),
	PHP(new PHP()),
	PLP(new PLP()),
	
	CLC(new CLC()),
	CLD(new CLD()),
	CLV(new CLV()),
	CLI(new CLI()),
	SEC(new SEC()),
	SED(new SED()),
	SEI(new SEI()),
	CMP_IMM(new CMP_IMM()),
	CMP_ABS(new CMP_ABS()),
	CMP_ZP(new CMP_ZP()),
	CMP_ZPX(new CMP_ZPX()),
	CMP_ABX(new CMP_ABX()),
	CMP_ABY(new CMP_ABY()),
	CMP_INX(new CMP_INX()),
	CMP_INY(new CMP_INY()),
	CPX_ABS(new CPX_ABS()),
	CPX_IMM(new CPX_IMM()),
	CPX_ZP(new CPX_ZP()),
	CPY_IMM(new CPY_IMM()),
	CPY_ABS(new CPY_ABS()),
	CPY_ZP(new CPY_ZP()),
	//TEST(new TEST()),
	
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
