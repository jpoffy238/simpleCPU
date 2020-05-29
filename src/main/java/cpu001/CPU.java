package cpu001;
import Firmeware.Framework.OpCodes;
import Firmeware.Framework.machineState;
import MachineState.Decoder;
import MachineState.cpu001decoder;
import Registers.generalPurpose;
import Registers.gpregister;
import Registers.registerFlags;
import memoryInterface.MemoryDriver;
import memoryInterface.basicMemory;

public class CPU extends Thread  {
	private OpCodes op = OpCodes.NOP;
	public registerFlags flags = registerFlags.CFLAG;
	public generalPurpose a = new gpregister();
	public generalPurpose b = new gpregister();
	
	public generalPurpose x = new gpregister();
	public generalPurpose y = new gpregister();
	
	public int pc;
	public generalPurpose sp = new gpregister();
	public  MemoryDriver  memory = new basicMemory();
	public  machineState state;
	public int clockState;
	public  Decoder decoder = new cpu001decoder();
	
	public  CPU()
	{
		sp.set(0x01ff);
		clockState=0;
	}
	
	public void reset() {
		registerFlags.reset();
		a.reset();
		b.reset();
		x.reset();
		y.reset();
		pc= 0;
		sp.reset();
		sp.set(0x01ff); // set to top of memory
		clockState=0;
		
			
		
	}
	
	public void dump() {
		System.out.println ("FLAGS: " + flags.dump());	
		System.out.println("A         : " + String.format("%8s", Integer.toHexString(a.get())));
		System.out.println("B         : " + String.format("%8s", Integer.toHexString(b.get())));
		System.out.println("X         : " + String.format("%8s", Integer.toHexString(x.get())));
		System.out.println("Y         : " + String.format("%8s", Integer.toHexString(y.get())));
		System.out.println("PC      : " + String.format("%8s", Integer.toHexString((int)pc)));
		System.out.println("SP      : " + String.format("%8s", Integer.toHexString((int)sp.get())));
	  
	}

	public void run() {
		reset();
		while (true) {
			clockState=0;
			byte opcode = decoder.fetchInstruction(this);
			clockState++;
			state = decoder.decode(opcode);
			clockState++;
			state.exeute(this);
		
		
			dump();
		}
		
	}
}
