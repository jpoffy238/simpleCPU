package com.mj.Firmware.ExecutionFlow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.illegalOpCodeException;

public class HLT extends Instruction {
	final  Logger logger = LogManager.getLogger("HLT");
	public HLT() {
		super((byte) 0x3f);
		
	}
	public void exeute(CPU c) throws illegalOpCodeException {
		// TODO Auto-generated method stub
		logger.debug("HLT instruction : STOP");
	
		throw new illegalOpCodeException();
		
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
