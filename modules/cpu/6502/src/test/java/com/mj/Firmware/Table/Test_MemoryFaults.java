package com.mj.Firmware.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Firmware.Framework.OpCodes;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.util.CPU_CreateUtil;

/** Table-driven instructions behave sanely at memory boundaries: ROM writes and unmapped reads. */
public class Test_MemoryFaults {
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		c = CPU_CreateUtil.getCPU();
		c.step();               // consume power-on reset
		c.pc = 0x1000;
	}

	private void program(OpCodes op, int... operands) throws Exception {
		c.bus.write(0x1000, op.code());
		for (int i = 0; i < operands.length; i++) c.bus.write(0x1001 + i, (byte) operands[i]);
	}

	private void a(int v) { try { c.a.set(v); } catch (Exception e) { } }

	@Test
	public void storeIntoRomRaisesROExceptionAndLeavesRomUnchanged() throws Exception {
		int before = c.bus.read(0xf100) & 0xff;
		a(before ^ 0xff);
		program(OpCodes.STA_ABS, 0x00, 0xf1);
		assertThrows(ROException.class, () -> c.step());
		assertEquals(before, c.bus.read(0xf100) & 0xff);
		assertEquals((before ^ 0xff) & 0xff, c.a.get(), "registers untouched by a failed store");
	}

	@Test
	public void readModifyWriteIntoRomRaisesROException() throws Exception {
		int before = c.bus.read(0xf100) & 0xff;
		program(OpCodes.INC_ABS, 0x00, 0xf1);
		assertThrows(ROException.class, () -> c.step());
		assertEquals(before, c.bus.read(0xf100) & 0xff);

		c.pc = 0x1000;
		program(OpCodes.ASL_ABS, 0x00, 0xf1);
		assertThrows(ROException.class, () -> c.step());
		assertEquals(before, c.bus.read(0xf100) & 0xff);
	}

	@Test
	public void readFromUnmappedAddressRaisesDeviceUnavailableAndDoesNotLoad() throws Exception {
		a(0x5a);
		program(OpCodes.LDA_ABS, 0x00, 0x91);          // $9100 has no device on the bus (the VIA occupies $9000-$900F)
		assertThrows(DeviceUnavailable.class, () -> c.step());
		assertEquals(0x5a, c.a.get(), "A is untouched when the read faults");
	}

	@Test
	public void indexedReadThatWrapsOntoMappedMemoryDoesNotFault() throws Exception {
		c.bus.write(0x0010, (byte) 0x77);
		try { c.y.set(0x20); } catch (Exception e) { }
		program(OpCodes.LDA_ABSY, 0xf0, 0xff);         // $FFF0 + $20 wraps to $0010, no fault
		c.step();
		assertEquals(0x77, c.a.get());
	}

	@Test
	public void faultingInstructionDoesNotAdvancePastItsOperand() throws Exception {
		program(OpCodes.STA_ABS, 0x00, 0xf1);
		int start = c.pc;
		assertThrows(ROException.class, () -> c.step());
		// the PC advance happens only after a successful access: it stays at the operand
		assertEquals(start + 1, c.pc);
	}
}
