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
		return Addressing.absolute(c);
	}

	protected int getAbsoluteAddressY(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.absoluteY(c);
	}

	protected int getAbsoluteAddressX(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.absoluteX(c);
	}

	protected int getZeroPageAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.zeroPage(c);
	}

	protected int getZeroPageXAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.zeroPageX(c);
	}

	protected int getZeroPageYAddress(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.zeroPageY(c);
	}

	protected int getIndirect(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.indirect(c);
	}

	/** Indexed indirect, (zp,X). See {@link Addressing}. */
	protected int getIndexX(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.indexedIndirect(c);
	}

	/** Indirect indexed, (zp),Y. See {@link Addressing}. */
	protected int getIndexY(CPU c) throws illegalAddressException, DeviceUnavailable {
		return Addressing.indirectIndexed(c);
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
