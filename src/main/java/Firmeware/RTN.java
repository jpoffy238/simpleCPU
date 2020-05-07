package Firmeware;

import cpu001.CPU;

public class RTN extends Instruction {
	public void exeute(CPU c) {
		// return from  Subroutine  
		// pop the lower  order PC from  stack 
		// pop  high  order PC from  stack
		// 
		
		   
		    int  sp = c.sp.get();

			
			int lower = c.memory.read(sp);
			sp++;
			int upper = c.memory.read(sp);;
			sp++;
			c.sp.set(sp);		
		    
			int newpc =( (upper  &  0xff) << 8) + (int)(lower & 0x00ff);
			c.pc = newpc;
		}
}
