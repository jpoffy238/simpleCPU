package MachineState;

import java.util.HashMap;
import java.util.Map;

import Firmeware.ADDB;
import Firmeware.CMP;
import Firmeware.HLT;
import Firmeware.JMP;
import Firmeware.JRZ;
import Firmeware.LDA;
import Firmeware.LDB;
import Firmeware.NOP;
import Firmeware.POPA;
import Firmeware.PUSHA;
import Firmeware.machineState;
import cpu001.CPU;
import memoryInterface.MemoryDriver;

public class cpu001decoder implements Decoder {
	public static Map<Integer, machineState> decoder = new HashMap<Integer, machineState>();
	
	public static void AddInstruction(Integer opcode, machineState m) {
		System.out.println("Adding : " + m.getClass().getCanonicalName());
		decoder.put(opcode, m);
	}
	public byte fetchInstruction(CPU c) {
		// TODO Auto-generated method stub
		MemoryDriver  m = c.memory;
		int value = (int)(m.read(c.pc) &0xff);
		
		c.pc = (++c.pc);
		return (byte)value;
	}

	public machineState decode(CPU c, byte instruction) {
		// TODO Auto-generated method stub
	
		machineState m = decoder.get(new Integer((int)(instruction&0xff)));
		
		if (null == m) {
			System.out.println(" Illegal instruction - exit");
			m = new HLT();
		}
		System.out.println("Executing : " + m.getClass().getCanonicalName());
		return m;
	}

}
