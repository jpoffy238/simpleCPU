package com.mj.Firmware.Framework;



import com.mj.exceptions.illegalOpCodeException;

public class cpu001decoder implements Decoder {
	

	static machineState[] decoder = DecoderMap.getMap();
	
	public machineState decode(byte instruction) throws illegalOpCodeException {
		
		
		machineState m = decoder[m.getOpCode()] ;

		if (null == m) {
			throw new illegalOpCodeException();
		}
		return m;
	}
	
	public void listCounts() {
		long totalTime = 0;
		long totalInstructions =0;
		
		for ( machineState m :decoder ) {
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
