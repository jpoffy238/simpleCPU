package MachineState;

import Firmeware.machineState;
import cpu001.CPU;

public interface Decoder {
	public byte  fetchInstruction(CPU c);
	public machineState decode(CPU c, byte instruction);
	


}
