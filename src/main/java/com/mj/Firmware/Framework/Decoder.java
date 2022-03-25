package com.mj.Firmware.Framework;

import com.mj.exceptions.illegalOpCodeException;

public interface Decoder {
	public machineState decode( byte instruction) throws illegalOpCodeException;
	public void listCounts();


}
