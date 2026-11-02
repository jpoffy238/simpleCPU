package com.mj.Firmware.Framework;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;

public abstract class Instruction implements machineState {

	protected final  Logger logger = LogManager.getLogger(Instruction.class);
	private byte opCode;
	private long executionCount;
	private long totalExecutionTime;
	protected AddressMode addressMode;
	public final String KEY_OPCODE = "opcode";
	public final String KEY_MNEMONIC = "Mnemonic";
	public final String KEY_INSTRUCTION_SIZE = "InstructionSize";
	public final String KEY_CYCLES = "Cycles";
	public final String KEY_FLAGS_EFFECTED = "Flags";
	public final String KEY_WEB = "Web Reference";
	public final String KEY_DESCRIPTION = "Description";
	public final String KEY_ADDRESSING_MODE = "AddressingMode";
	public final String VALUE_ADDM_IMM = "Immediate";
	public final String VALUE_ADDM_IMP = "Implied";
	public final String VALUE_ADDM_ZP = "Zero Page";
	public final String VALUE_ADDM_ZPX = "Zero Page,X";
	public final String VALUE_ADDM_ZPY = "Zero Page,Y";
	public final String VALUE_ADDM_ABS = "Absolute";
	public final String VALUE_ADDM_ABX = "Absolute,X";
	public final String VALUE_ADDM_ABY = "Absolute,Y";

	public final String VALUE_ADDM_IDX = "Indirect,X";
	public final String VALUE_ADDM_IDY = "Indirect,Y";
	public final String VALUE_ADDM_REL = "Relative offset";
	public final String VALUE_ADDM_STK = "From Stack";

	/*
	 * Immediate LDA #$44 $A9 2 2 Zero Page LDA $44 $A5 2 3 Zero Page,X LDA $44,X
	 * $B5 2 4 Absolute LDA $4400 $AD 3 4 Absolute,X LDA $4400,X $BD 3 4+ Absolute,Y
	 * LDA $4400,Y $B9 3 4+ Indirect,X LDA ($44,X) $A1 2 6 Indirect,Y LDA ($44),Y
	 * $B1 2 5+
	 */
	private Map<String, String> OpCodeProperties = new HashMap<String, String>();

	public Instruction(byte op) {
		opCode = op;
	}

	public void exeute(CPU c) throws illegalOpCodeException, illegalAddressException, DeviceUnavailable, ROException {
		throw new illegalOpCodeException();
	}
	public void incrementExecutionCount() {
		executionCount++;
	}
	public byte getOpCode() {
		return opCode;
	}

	public void setProperty(String propertyName, String value) {
		OpCodeProperties.put(propertyName, value);
	}

	public String getProperty(String propertyName) {
		return OpCodeProperties.get(propertyName);
	}

	protected int getInterruptHandlerAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return c.readVector(CPU.VECTOR_IRQ);
	}

	protected int getNMInterruptHandlerAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return c.readVector(CPU.VECTOR_NMI);
	}

	protected int getResetHandlerAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return c.readVector(CPU.VECTOR_RESET);
	}

	protected int getAbsoluteAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		int opperand_lower = c.bus.read(c.pc) & 0xff;
		logger.debug(String.format("ADDR: %04x Lower Operand = %02x", c.pc, opperand_lower));

		int opperand_upper = (int) (c.bus.read(c.pc + 1) & 0xff);
		logger.debug(String.format("ADDR: %04x Upper Operand = %02x", (c.pc + 1), opperand_upper));

		int loadAddress = (((opperand_upper << 8) ) + opperand_lower) & 0x0ffff;
		logger.debug(String.format("Final Address %04x ", loadAddress));
		return loadAddress;
	}

	protected int getAbsoluteAddressY(CPU c) throws illegalAddressException, DeviceUnavailable {
		int loadAddress = getAbsoluteAddress(c);
		loadAddress += c.y.get() & 0xff;
		return loadAddress & 0xffff;
	}

	protected int getAbsoluteAddressX(CPU c) throws illegalAddressException, DeviceUnavailable {
		int loadAddress = getAbsoluteAddress(c);
		loadAddress += c.x.get() & 0xff;
		return loadAddress & 0xffff;
	}

	protected int getZeroPageAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		int operand = c.bus.read(c.pc);
		int address = (int) (operand) & 0x00ff;
		return address;
	}

	protected int getZeroPageXAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		int operand = c.bus.read(c.pc);
		int address = (int) (operand + c.x.get()) & 0x00ff;
		return address;
	}

	protected int getZeroPageYAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		int operand = c.bus.read(c.pc);
		int address = (int) (operand + c.y.get()) & 0x00ff;
		return address;
	}

	protected int getIndirect(CPU c) throws illegalAddressException, DeviceUnavailable {
		int lower = c.bus.read(c.pc) & 0xff;
		int upper = c.bus.read((c.pc + 1) & 0xffff) & 0xff;
		int PCAddress = (upper << 8) + lower;
		// NMOS 6502 quirk: the pointer's high byte is fetched from the same page,
		// so JMP ($30FF) reads $30FF and $3000 (not $3100).
		int highAddress = (PCAddress & 0xff00) | ((PCAddress + 1) & 0x00ff);
		lower = c.bus.read(PCAddress) & 0xff;
		upper = c.bus.read(highAddress) & 0xff;
		int lookupAddress = (upper << 8) + lower;
		logger.debug(String.format("IndirectAddress PC[%04x] -> To Address [%04x]",PCAddress , lookupAddress));

		return lookupAddress;
	}

	protected int getIndexX(CPU c) throws illegalAddressException, DeviceUnavailable {
		/*
		 * Indexed Indirect
		 * 
		 * This mode is only used with the X register. Consider a situation where the
		 * instruction is LDA ($20,X), X contains $04, and memory at $24 contains 0024:
		 * 74 20, First, X is added to $20 to get $24. The target address will be
		 * fetched from $24 resulting in a target address of $2074. Register A will be
		 * loaded with the contents of memory at $2074.
		 * 
		 * If X + the immediate byte will wrap around to a zero-page address. So you
		 * could code that like targetAddress = (X + opcode[1]) & 0xFF .
		 * 
		 * Indexed Indirect instructions are 2 bytes - the second byte is the zero-page
		 * address - $20 in the example. Obviously the fetched address has to be stored
		 * in the zero page.
		 */
		int address;
		int operand = c.bus.read(c.pc) & 0xff;;
		int pageZero = (operand + c.x.get()) & 0x00ff;
		int lower = c.bus.read(pageZero)   & 0xff;
		int upper = c.bus.read((pageZero + 1) & 0xff ) & 0xff;
		address = (upper & 0x00ff) << 8;
		address +=  (lower);
		return address;
	}

	protected int getIndexY(CPU c) throws illegalAddressException, DeviceUnavailable {
		/*
		 * Indirect Indexed
		 * 
		 * This mode is only used with the Y register. It differs in the order that Y is
		 * applied to the indirectly fetched address. An example instruction that uses
		 * indirect index addressing is LDA ($86),Y . To calculate the target address,
		 * the CPU will first fetch the address stored at zero page location $86. That
		 * address will be added to register Y to get the final target address.
		 * 
		 * For LDA ($86),Y, if the address stored at $86 is $4028 (memory is 0086: 28
		 * 40, remember little endian) and register Y contains $10, then the final
		 * target address would be $4038. Register A will be loaded with the contents of
		 * memory at $4038.
		 * 
		 * Indirect Indexed instructions are 2 bytes - the second byte is the zero-page
		 * address - $20 in the example. (So the fetched address has to be stored in the
		 * zero page.)
		 * 
		 * While indexed indirect addressing will only generate a zero-page address,
		 * this mode's target address is not wrapped - it can be anywhere in the 16-bit
		 * address space.
		 */
		int operand = c.bus.read(c.pc);
		int pageZero = (operand) & 0x00ff;
		int lower = c.bus.read(pageZero) & 0xff;
		int upper = c.bus.read((pageZero + 1) & 0x00ff) & 0xff;   // pointer wraps within zero page
		int address = (upper << 8) + lower;
		address += (c.y.get() & 0x00ff);
		return address & 0xffff;
	}

	/**
	 * Common relative-branch handling. The PC points at the offset byte on entry.
	 * Wraps the PC to 16 bits and records whether a taken branch crossed a page.
	 */
	protected void branch(CPU c, boolean taken) throws illegalAddressException, DeviceUnavailable {
		byte offset = (byte) c.bus.read(c.pc);
		int next = (c.pc + 1) & 0xffff;          // address of the following instruction
		if (taken) {
			int target = (next + offset) & 0xffff;
			branchPageCrossed = (target & 0xff00) != (next & 0xff00);
			c.pc = target;
		} else {
			branchPageCrossed = false;
			c.pc = next;
		}
	}

	/** True if the most recently executed taken branch crossed a page boundary. */
	protected boolean branchPageCrossed;

	protected void handleZException(CPU c) {
		c.ZFLAG.set();
		c.NFLAG.clear();
	}

	protected void handleNException(CPU c) {
		c.NFLAG.set();
		c.ZFLAG.clear();
	}

	// 6502 processor status register layout:  N V 1 B D I Z C  (bit 7 .. bit 0)
	public static final int SR_C = 0x01;
	public static final int SR_Z = 0x02;
	public static final int SR_I = 0x04;
	public static final int SR_D = 0x08;
	public static final int SR_B = 0x10;   // exists only in pushed copies of the status
	public static final int SR_UNUSED = 0x20;   // always reads as 1
	public static final int SR_V = 0x40;
	public static final int SR_N = 0x80;

	/** Status byte as pushed by PHP / BRK (B bit set). */
	protected byte psr(CPU c) {
		return psr(c, true);
	}

	/**
	 * Build the status byte for pushing on the stack.
	 * 
	 * @param breakFlag true for PHP/BRK (software), false for IRQ/NMI (hardware)
	 */
	protected byte psr(CPU c, boolean breakFlag) {
		int status = SR_UNUSED;
		if (c.CFLAG.isSet()) status |= SR_C;
		if (c.ZFLAG.isSet()) status |= SR_Z;
		if (c.IFLAG.isSet()) status |= SR_I;
		if (c.DFLAG.isSet()) status |= SR_D;
		if (breakFlag)       status |= SR_B;
		if (c.OFLAG.isSet()) status |= SR_V;
		if (c.NFLAG.isSet()) status |= SR_N;
		return (byte) status;
	}

	/** Load the flags from a status byte (PLP / RTI). B and the unused bit are ignored. */
	protected void setFlags(CPU c, byte sr) {
		int v = sr & 0xff;
		setFlag(c.CFLAG, (v & SR_C) != 0);
		setFlag(c.ZFLAG, (v & SR_Z) != 0);
		setFlag(c.IFLAG, (v & SR_I) != 0);
		setFlag(c.DFLAG, (v & SR_D) != 0);
		setFlag(c.OFLAG, (v & SR_V) != 0);
		setFlag(c.NFLAG, (v & SR_N) != 0);
	}

	private void setFlag(com.mj.Registers.registerFlags f, boolean on) {
		if (on) {
			f.set();
		} else {
			f.clear();
		}
	}

	/**
	 * Arithmetic shift left of a memory/register value: C takes bit 7, N and Z
	 * reflect the 8-bit result. Returns the shifted byte value.
	 */
	protected int aslAndSetFlags(CPU c, int value) {
		value &= 0xff;
		int result = (value << 1) & 0xff;
		if ((value & 0x80) != 0) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
		}
		if (result == 0) {
			c.ZFLAG.set();
		} else {
			c.ZFLAG.clear();
		}
		if ((result & 0x80) != 0) {
			c.NFLAG.set();
		} else {
			c.NFLAG.clear();
		}
		return result;
	}

	protected void setNZ(CPU c, int result) {
		if (result == 0) {
			c.ZFLAG.set();
		} else {
			c.ZFLAG.clear();
		}
		if ((result & 0x80) != 0) {
			c.NFLAG.set();
		} else {
			c.NFLAG.clear();
		}
	}

	/** ROL: rotate left through carry. C = old bit 7, bit 0 = old C. */
	protected int rolAndSetFlags(CPU c, int value) {
		value &= 0xff;
		int result = ((value << 1) | (c.CFLAG.isSet() ? 1 : 0)) & 0xff;
		if ((value & 0x80) != 0) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
		}
		setNZ(c, result);
		return result;
	}

	/** ROR: rotate right through carry. C = old bit 0, bit 7 = old C. */
	protected int rorAndSetFlags(CPU c, int value) {
		value &= 0xff;
		int result = (value >> 1) | (c.CFLAG.isSet() ? 0x80 : 0);
		if ((value & 0x01) != 0) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
		}
		setNZ(c, result);
		return result;
	}

	/** LSR: logical shift right. C = old bit 0, bit 7 = 0 (so N is always clear). */
	protected int lsrAndSetFlags(CPU c, int value) {
		value &= 0xff;
		int result = value >> 1;
		if ((value & 0x01) != 0) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
		}
		setNZ(c, result);
		return result;
	}

	public long getExecutionCount() {
		return executionCount;
	}

	public void setExecutionCount(long executionCount) {
		this.executionCount = executionCount;
	}
	
	public long getTotalExecutionTime() {
		return totalExecutionTime;
	}
	public void setExecutionTime(long time) {
		totalExecutionTime += time;
	}

}
