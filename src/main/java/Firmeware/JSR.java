package Firmeware;

import cpu001.CPU;

public class JSR extends Instruction {
	public void exeute(CPU c) {
		// Jump to Subroutine  
		// Push the high order PC+2 to stack 
		// Push low order PC+2 to stack
		// JMP to Immediate address next two bytes of this instruction
			int pc = c.pc;
		    int upper = (((pc+2) & 0xff00 )>> 8); 
		    int  lower = (((pc+2) & 0x00ff));
		    int  sp = c.sp.get();
			sp--;
			
			c.memory.write(sp, (byte)(upper & 0xff));
			sp--;
			c.memory.write(sp, (byte)(lower & 0xff));
			
			c.sp.set(sp);		
			
		    lower = (c.memory.read(pc)  & 0xff);
		    pc++;
		    upper = (c.memory.read(pc) & 0xff);
			int newpc =( (upper & 0x00ff) << 8) + (int)(lower & 0x00ff);
			c.pc = newpc;
		}
	}

