package com.mj.Firmware.Framework;

import java.util.Map;

import com.mj.exceptions.illegalOpCodeException;

public class cpu001decoder implements Decoder {
	
	static OpCodes nop = OpCodes.NOP;
	public machineState decode(byte instruction) throws illegalOpCodeException {
		int i = (instruction & 0x00ff);
		Integer in;
		in = Integer.valueOf(i);
		machineState m = DecoderMap.getMap().get(in);

		if (null == m) {
			throw new illegalOpCodeException();
		}
		return m;
	}
	
	public void listCounts() {
		long totalTime = 0;
		long totalInstructions =0;
		Map<Integer, machineState> map = DecoderMap.getMap();
		for ( machineState m :map.values() ) {
			byte i = m.getOpCode();
			Class<? extends machineState> inst = m.getClass();
			String name = inst.getCanonicalName();
			
			long excount = m.getExecutionCount();
			totalInstructions += excount;
			totalTime +=  m.getTotalExecutionTime();
			if (excount > 0 ) {
				System.out.println(name + " : " + excount );;
				System.out.println(name + " :  Total Time : " + m.getTotalExecutionTime());
				System.out.println(name + " :  Average Exec Time : " + (float)m.getTotalExecutionTime()/(float)excount);
				
			}
		}
		System.out.println("Total Execution Time : " + totalTime);
		System.out.println("Total Instruction Count : " + totalInstructions);
		System.out.println("Average instruction execution time: " + (float) totalTime / (float)totalInstructions);
	}
}
