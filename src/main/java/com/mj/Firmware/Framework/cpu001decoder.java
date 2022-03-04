package com.mj.Firmware.Framework;

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

}
