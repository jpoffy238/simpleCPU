package Firmeware;

import cpu001.CPU;

public class JMP extends Instruction {

	public JMP() {
	
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		int  m = c.memory.read(c.pc);
		
		c.pc = m&0xff;
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
