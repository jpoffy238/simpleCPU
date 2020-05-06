package Firmeware;

import cpu001.CPU;

public class HLT extends Instruction {

	public HLT() {
		
	}
	public void exeute(CPU c) {
		// TODO Auto-generated method stub
		System.out.println("HLT instruction : STOP");
		c.dump();
		System.exit(0);
	}

	public void setFlags(CPU u) {
		// TODO Auto-generated method stub

	}

}
