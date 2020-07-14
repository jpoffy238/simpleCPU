package com.mj.Firmware.Framework;

import com.mj.exceptions.illegalOpCodeException;

public class cpu001decoder implements Decoder {
	
	static OpCodes nop = OpCodes.NOP;
	public machineState decode(byte instruction) throws illegalOpCodeException {

		machineState m = DecoderMap.getMap().get(new Integer((int) (instruction & 0x00ff)));

		if (null == m) {
			throw new illegalOpCodeException();
		}
		return m;
	}

}
