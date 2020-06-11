package com.mj.cpu001;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.machineState;
import com.mj.MachineState.Decoder;
import com.mj.Registers.generalPurpose;
import com.mj.Registers.gpregister;
import com.mj.Registers.registerFlags;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;
import com.mj.memoryInterface.MemoryDriver;

public class CPU extends Thread {
	public CPU(MemoryDriver memdrv, Decoder dcd) {
		memory = memdrv;
		decoder = dcd;
	}
    private static final Logger logger = LogManager.getLogger(CPU.class);

	private Boolean interruptFired = false;
	private Boolean NMInterruptFired = false;
	private Boolean powerOnResetFired = true;

	public Boolean getNMInterruptFired() {
		boolean returnValue =  NMInterruptFired;
		NMInterruptFired = false;
		return returnValue;
				
	}

	public void setNMInterruptFired() {
		NMInterruptFired = true;
	}

	public Boolean getPowerOnResetFired() {
		boolean returnValue = powerOnResetFired;
		powerOnResetFired = false;
		return returnValue;
	}

	public void setPowerOnResetFired() {
		powerOnResetFired = true;
	}

	public Boolean getInterruptFired() {
		boolean returnvalue = interruptFired;
		interruptFired = false;
		return returnvalue;
	}

	public void setInterruptFired() {
		interruptFired = true;
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
		pc = 0x1000;
		sp = (0x01ff);
		powerOnResetFired = true;
		
		logger.debug(dump());
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
		long instructionCount = 0;
       long beginTime = System.currentTimeMillis();
		long current  = System.currentTimeMillis();
		long previous = System.currentTimeMillis();
		long difftime;
		boolean RUN=true;
		while (RUN) {
			instructionCount++;
			current = System.currentTimeMillis();
			difftime = current - previous;
			previous = current;
			logger.debug("Instruction Count : " + instructionCount + "  Execution Time : " + difftime + "  Total Time: " + (current - beginTime));
			
			byte opcode = 0;
			try {
				opcode = 0;
				opcode = fetchInstruction();
				state = decoder.decode(opcode);
				String currentState = String.format("%-40s", "[" + state.getClass().getName() + "]");
				currentState += dump();
				logger.debug(currentState);
				
				state.exeute(this);
			} catch (illegalOpCodeException e) {
				// TODO Auto-generated catch block
				logger.error("[" + (int) (opcode & 0xff) + "] BAD OPCODE : " + dump());
				RUN=false;
			} catch (illegalAddressException e) {
				logger.error("Bad address: " + dump());

				RUN=false;
			} catch (DeviceUnavailable e) {
				logger.error("Device Unavailable : " + dump());
				RUN=false;
			}
			
		}
	}

	private byte fetchInstruction() throws illegalAddressException, DeviceUnavailable {
		// Interrupt handling
		// Interrupts are only checked when the CPU is in the fetch cycle.  If an interrupt is raised
		// this injects a special MIH instructions (non standard instruction) to read the specific
		// interrupt vector, push the current PC and PSR to the stack then set the pc to the 
		// interrupt routine.
		MemoryDriver m = memory;
		int value;
		if   ((!IFLAG.isSet() &&interruptFired) || NMInterruptFired || powerOnResetFired ) {
			// We have received an interrupt -- need to jump to the
			// indirect address between  $FFFA/B  and FFFC/D  and FFFE/F
			//  non-maskable interrupt handler ($FFFA/B), 
			// the power on reset location ($FFFC/D) and the 
			// BRK/interrupt request handler ($FFFE/F) respectively.
			// Also don't increment the PC still need to executethat instruction.
			value = OpCodes.MIH.code();
		} else {
			value = (int) (m.read(pc) & 0xff);
			pc++;
		}
		return (byte) value;
	}

}
