package cpu001;

import Firmeware.Framework.OpCodes;
import Firmeware.Framework.machineState;
import MachineState.Decoder;
import MachineState.cpu001decoder;
import Registers.generalPurpose;
import Registers.gpregister;
import Registers.registerFlags;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.illegalOpCodeException;
import memoryInterface.MemoryDriver;
import memoryInterface.basicMemory;

public class CPU extends Thread {
	private OpCodes op = OpCodes.NOP;
	public registerFlags CFLAG = new registerFlags("C");
	public registerFlags ZFLAG = new registerFlags("Z");
	public registerFlags IFLAG = new registerFlags("I");
	public registerFlags DFLAG = new registerFlags("D");
	public registerFlags BFLAG = new registerFlags("B");
	public registerFlags VFLAG = new registerFlags("V");
	public registerFlags NFLAG = new registerFlags("N");
	public generalPurpose a = new gpregister();
	public generalPurpose b = new gpregister();

	public generalPurpose x = new gpregister();
	public generalPurpose y = new gpregister();

	public int pc;
	public int sp;
	public MemoryDriver memory = new basicMemory();

	public int clockState;

	public CPU() {

		sp = 0x01ff;

		CFLAG.reset();
		ZFLAG.reset();
		IFLAG.reset();
		DFLAG.reset();
		NFLAG.reset();
		VFLAG.reset();
		BFLAG.reset();
		clockState = 0;
	}

	public void reset() {
		CFLAG.reset();
		ZFLAG.reset();
		IFLAG.reset();
		DFLAG.reset();
		NFLAG.reset();
		VFLAG.reset();
		BFLAG.reset();

		a.reset();
		b.reset();
		x.reset();
		y.reset();
		pc = 0;
		sp = (0x01ff);

		clockState = 0;

	}

	public String dump() {
		String flagStatus = CFLAG.toString() + NFLAG.toString() + ZFLAG.toString() + VFLAG.toString() + DFLAG.toString()
				+ IFLAG.toString() + BFLAG.toString() +

				"A         : " + String.format("%8s", Integer.toHexString(a.get())) + "B         : "
				+ String.format("%8s", Integer.toHexString(b.get())) + "X         : "
				+ String.format("%8s", Integer.toHexString(x.get())) + "Y         : "
				+ String.format("%8s", Integer.toHexString(y.get())) + "PC      : "
				+ String.format("%8s", Integer.toHexString((int) pc)) + "SP      : "
				+ String.format("%8s", Integer.toHexString((int) sp));
		return flagStatus;

	}

	public void run() {
		reset();
		Decoder decoder = new cpu001decoder();
		machineState state;
		while (true) {
			clockState = 0;
			byte opcode ;
			clockState++;
			try {
				opcode = decoder.fetchInstruction(this);
				state = decoder.decode(opcode);
				clockState++;
				state.exeute(this);
				String currentState = "[" + state.getClass().getName() + "] " + dump();
				System.out.println(currentState);
			} catch (illegalOpCodeException e) {
				// TODO Auto-generated catch block
				System.err.println("BAD OPCODE : " + dump());
				System.exit(8);
			} catch (illegalAddressException e) {
				System.err.println("Bad address: " + dump());
				System.exit(8);
			} catch (DeviceUnavailable e) {
				System.err.println("Device Unavailable : " + dump());
				System.exit(8);
			}
		}
	}
}
