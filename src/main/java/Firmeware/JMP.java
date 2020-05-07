package Firmeware;

import cpu001.CPU;

public class JMP extends Instruction {

	public JMP() {
	
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		int lower  = c.memory.read(c.pc++);
		int upper = c.memory.read(c.pc++);
		c.pc = (upper & 0x00ff ) << 8 + (lower & 0xff);
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
