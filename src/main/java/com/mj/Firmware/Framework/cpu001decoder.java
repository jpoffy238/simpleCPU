package com.mj.Firmware.Framework;

import java.util.Map;

import com.mj.exceptions.illegalOpCodeException;

public class cpu001decoder implements Decoder {
	
	static OpCodes nop = OpCodes.NOP;
	public machineState decode(byte instruction) throws illegalOpCodeException {
		int i = (instruction & 0x00ff);
		
		machineState m = DecoderMap.getMap()[i];

		if (null == m) {
			throw new illegalOpCodeException();
		}
		return m;
	}
	
	
	public void listCounts() {
		long totalTime = 0;
		long totalInstructions =0;
		machineState[] m = DecoderMap.getMap();
		for ( int i = 0; i < 256; i++) {
			byte i = m[i].getOpCode();
			Class<? extends machineState> inst = m[i].getClass();
			String name = inst.getCanonicalName();
			
			long excount = m[i].getExecutionCount();
			totalInstructions += excount;
			totalTime +=  m[i].getTotalExecutionTime();
			if (excount > 0 ) {
				System.out.println(name + " : " + excount );;
				System.out.println(name + " :  Total Time : " + m[i].getTotalExecutionTime());
				System.out.println(name + " :  Average Exec Time : " + (float)m[i].getTotalExecutionTime()/(float)excount);
				
			}
		}
		System.out.println("Total Execution Time : " + totalTime);
		System.out.println("Total Instruction Count : " + totalInstructions);
		System.out.println("Average instruction execution time: " + (float) totalTime / (float)totalInstructions);
	}
}
