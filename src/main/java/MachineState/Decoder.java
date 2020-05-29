package MachineState;

import Firmeware.Framework.machineState;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.illegalOpCodeException;

public interface Decoder {
	public byte  fetchInstruction(CPU c)  throws illegalAddressException, DeviceUnavailable;
	public machineState decode( byte instruction) throws illegalOpCodeException, illegalAddressException;
	


}
