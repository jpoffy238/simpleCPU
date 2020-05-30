package MachineState;

import Firmeware.Framework.machineState;
import exceptions.illegalOpCodeException;

public interface Decoder {
	public machineState decode( byte instruction) throws illegalOpCodeException;



}
