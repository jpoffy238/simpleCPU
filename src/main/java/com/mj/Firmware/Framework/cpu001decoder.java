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
		Map<Integer, machineState> map = DecoderMap.getMap();
		for ( machineState m :map.values() ) {
			byte i = m.getOpCode();
			Class<? extends machineState> inst = m.getClass();
			String name = inst.getCanonicalName();
			
			long excount = m.getExecutionCount();
			if (excount > 0 ) {
				System.out.println(name  + " : " + excount);;
			}
		}
	}
}
