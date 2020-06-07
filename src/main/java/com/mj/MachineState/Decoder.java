package com.mj.MachineState;

import com.mj.Firmware.Framework.machineState;
import com.mj.exceptions.illegalOpCodeException;

public interface Decoder {
	public machineState decode( byte instruction) throws illegalOpCodeException;



}
