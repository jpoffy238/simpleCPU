package cpu001;

import Firmeware.Framework.machineState;
import MachineState.Decoder;
import Registers.generalPurpose;
import Registers.gpregister;
import Registers.registerFlags;
import exceptions.DeviceUnavailable;
import exceptions.illegalAddressException;
import exceptions.illegalOpCodeException;
import memoryInterface.MemoryDriver;

public class CPU extends Thread {
	public CPU(MemoryDriver memdrv, Decoder dcd) {
		memory = memdrv;
		decoder = dcd;
	}

	public registerFlags CFLAG = new registerFlags("C");
	public registerFlags ZFLAG = new registerFlags("Z");
	public registerFlags IFLAG = new registerFlags("I");
	public registerFlags DFLAG = new registerFlags("D");
	public registerFlags BFLAG = new registerFlags("B");
	public registerFlags OFLAG = new registerFlags("O");
	public registerFlags NFLAG = new registerFlags("N");
	public generalPurpose a = new gpregister();
	public generalPurpose b = new gpregister();

	public generalPurpose x = new gpregister();
	public generalPurpose y = new gpregister();

	public int pc;
	public int sp;
	public MemoryDriver memory;
	Decoder decoder;
	public int clockState;
	machineState state;

	public CPU() {

		sp = 0x01ff;
		pc = 0xfffb;
		CFLAG.reset();
		ZFLAG.reset();
		IFLAG.reset();
		DFLAG.reset();
		NFLAG.reset();
		OFLAG.reset();
		BFLAG.reset();
		clockState = 0;
		try {
			memory.write(0xfffc, (byte) 0x00);
			memory.write(0xfffd, (byte) 0x10);
		} catch (illegalAddressException e) {
			System.err.println("ERROR ILLAddress: Unable to set poweron/reset just address");
			System.exit(8);
		} catch (DeviceUnavailable e) {
			System.err.println("ERROR DEVUN : Unable to set poweron/reset just address");
			System.exit(8);
		}
	}

	public void reset() {
		CFLAG.reset();
		ZFLAG.reset();
		IFLAG.reset();
		DFLAG.reset();
		NFLAG.reset();
		OFLAG.reset();
		BFLAG.reset();

		a.reset();
		b.reset();
		x.reset();
		y.reset();
		pc = 0xfffc;
		sp = (0x01ff);

		clockState = 0;
		state = new Firmeware.ExecutionFlow.JMP_ABS(); // power on reset - start location
		System.out.println (dump());
	}

	public String dump() {
		String flagStatus = "\t\t[" + CFLAG.toString() + NFLAG.toString() + ZFLAG.toString() + OFLAG.toString()
				+ DFLAG.toString() + IFLAG.toString() + BFLAG.toString() + "]\tA[ " + String.format("%02x", a.get())
				+ "]\tB[" + String.format("%02xs", b.get()) + "]\tX[" + String.format("%02x", x.get()) + "]\tY[ "
				+ String.format("%02x", y.get()) + "]\tPC[" + String.format("%04x", pc) + "]\tSP["
				+ String.format("%04x", sp) + "]";
		return flagStatus;

	}

	public void run() {
		reset();
		try {
			state.exeute(this);
			System.out.println(dump());
		} catch (illegalAddressException e) {
			System.err.println("Bad address: " + dump());
			System.exit(8);
		} catch (DeviceUnavailable e) {
			System.err.println("Device Unavailable : " + dump());
			System.exit(8);
		}

		while (true) {
			clockState = 0;
			byte opcode = 0;
			clockState++;
			try {
				opcode = 0;
				opcode = fetchInstruction();
				state = decoder.decode(opcode);
				clockState++;
				state.exeute(this);
				String currentState = String.format("%-40s", "[" + state.getClass().getName() + "]");
				currentState += dump();
				System.out.println(currentState);
			} catch (illegalOpCodeException e) {
				// TODO Auto-generated catch block
				System.err.println("[" + (int) (opcode & 0xff) + "] BAD OPCODE : " + dump());
				System.exit(8);
			} catch (illegalAddressException e) {
				System.err.println("Bad address: " + dump());
				e.printStackTrace(System.err);
				
				System.exit(8);
			} catch (DeviceUnavailable e) {
				System.err.println("Device Unavailable : " + dump());
				System.exit(8);
			}
		}
	}

	public byte fetchInstruction() throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		MemoryDriver m = memory;
		int value;

		value = (int) (m.read(pc) & 0xff);

		pc++;
		return (byte) value;
	}
	
}
