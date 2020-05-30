package MachineState;

import java.util.HashMap;
import java.util.Map;

import Firmeware.ExecutionFlow.HLT;
import Firmeware.ExecutionFlow.JMP_ABS;
import Firmeware.ExecutionFlow.BEQ;
import Firmeware.ExecutionFlow.NOP;
import Firmeware.Framework.machineState;
import Firmeware.Load.LDA_IMM;
import Firmeware.Load.LDB;
import Firmeware.Logic.CMP;
import Firmeware.Math.ADDB;
import Firmeware.Stack.PLA;
import Firmeware.Stack.PUSHA;
import cpu001.CPU;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.illegalOpCodeException;
import memoryInterface.MemoryDriver;

public class cpu001decoder implements Decoder {
	public static Map<Integer, machineState> decoder = new HashMap<Integer, machineState>();
	
	public static void AddInstruction(Integer opcode, machineState m) {
		System.out.println("Adding : " + m.getClass().getCanonicalName());
		if (decoder.containsKey(opcode) ) {
			machineState tmp = decoder.get(opcode);
			System.err.println("Error duplicate opcode" + opcode.toString());
			System.err.println("Failed to add  : " + m.getClass().getCanonicalName());
			System.err.println("Failed dulicate: " + tmp.getClass().getCanonicalName() );
			System.exit(8);
		}
		decoder.put(opcode, m);
	}
	public byte fetchInstruction(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		MemoryDriver  m = c.memory;
		int value;
	
			value = (int)(m.read(c.pc) &0xff);
	
		
		c.pc = (++c.pc);
		return (byte)value;
	}

	public machineState decode( byte instruction) throws illegalOpCodeException {
	
		machineState m = decoder.get(new Integer((int)(instruction&0xff)));
		
		if (null == m) {
			throw new illegalOpCodeException();
		}
		return m;
	}

}
