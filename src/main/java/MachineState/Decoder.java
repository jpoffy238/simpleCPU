package MachineState;

import Firmeware.Framework.machineState;
import cpu001.CPU;

public interface Decoder {
	public byte  fetchInstruction(CPU c) ;
	public machineState decode( byte instruction);
	


}
