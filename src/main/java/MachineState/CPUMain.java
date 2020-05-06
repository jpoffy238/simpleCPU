package MachineState;

import java.util.ArrayList;
import java.util.List;

import Registers.registerFlags;
import cpu001.CPU;

public class CPUMain {

	public static void main(String[] args) {
		CPU c = new CPU();
		
		// TODO Auto-generated method stub
		/*
		 * List<registerFlags> flags = new ArrayList<registerFlags>();
		 * flags.add(registerFlags.CFLAG); flags.add(registerFlags.ZFLAG);
		 * flags.add(registerFlags.OVFLAG);
		 * 
		 * testFlag(registerFlags.CFLAG);
		 * 
		 * testFlag(registerFlags.ZFLAG); testFlag(registerFlags.IDFLAG);
		 * testFlag(registerFlags.DCFLAG) ; testFlag(registerFlags.BFLAG);
		 * testFlag(registerFlags.OVFLAG); testFlag(registerFlags.NFLAG);
		 * 
		 * registerFlags.set(flags); System.out.println("MultiSet: " +
		 * registerFlags.CFLAG.dump()); registerFlags.clear(flags);
		 * System.out.println("MultiClear: " + registerFlags.CFLAG.dump());
		 */
		
		c.start();
		}
	
	public static void testFlag( registerFlags r) {
		System.out.println("Initial State: \t" + r.flagName+ ":" + r.dump());
		r.set();
		System.out.println("Set State: \t" + r.flagName+ ":" + r.dump());
		r.clear();
		System.out.println("Clear State: \t" + r.flagName+ ":" + r.dump());
		System.out.println("==========================");
	}
		
	}


