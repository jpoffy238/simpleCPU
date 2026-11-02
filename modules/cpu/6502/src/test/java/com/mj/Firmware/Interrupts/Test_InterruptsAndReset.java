package com.mj.Firmware.Interrupts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

/** Reset, NMI, IRQ and BRK take their addresses from the vectors in ROM. */
public class Test_InterruptsAndReset {
	private static final int NMI = 0x2222, RESET = 0x3456, IRQ = 0x4000;

	private PBus bus;

	private CPU cpu(int nmi, int reset, int irq) throws Exception {
		bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0));
		basicROM rom = new basicROM(bus, new AddressRange(0xf000, 0xffff), null, 0);
		rom.setVectors(nmi, reset, irq);
		bus.registerDevice(rom);
		return new CPU(bus, new cpu001decoder());
	}

	/** CPU that has completed its power-on reset and sits at $1000 with a NOP there. */
	private CPU running() throws Exception {
		CPU c = cpu(NMI, RESET, IRQ);
		c.step();                         // power-on reset
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.NOP.code());
		return c;
	}

	private int stack(CPU c, int offsetFromSp) throws Exception {
		return c.bus.read(0x0100 | ((c.sp + offsetFromSp) & 0xff)) & 0xff;
	}

	@Test
	public void resetLoadsPcFromVectorAndInitialisesState() throws Exception {
		CPU c = cpu(NMI, RESET, IRQ);
		c.step();
		assertEquals(RESET, c.pc);
		assertEquals(0xfd, c.sp);
		assertTrue(c.IFLAG.isSet(), "interrupts are disabled after reset");
	}

	@Test
	public void resetVectorIsNotHardCoded() throws Exception {
		CPU c = cpu(0, 0x1234, 0);
		c.step();
		assertEquals(0x1234, c.pc);
	}

	@Test
	public void cpuResetReloadsPcFromVector() throws Exception {
		CPU c = running();
		c.reset();
		assertEquals(RESET, c.pc);
	}

	@Test
	public void irqJumpsThroughVectorPushingStateWithBreakClear() throws Exception {
		CPU c = running();
		c.IFLAG.clear();
		c.CFLAG.set();
		c.bus.raiseInterupt();
		c.step();
		assertEquals(IRQ, c.pc);
		assertTrue(c.IFLAG.isSet());
		assertEquals(0xfa, c.sp);
		assertEquals(0x21, stack(c, 1));         // status: unused + C, B clear, I clear (old)
		assertEquals(0x00, stack(c, 2));         // PCL
		assertEquals(0x10, stack(c, 3));         // PCH
		assertFalse(c.bus.IsInterruptRaised());
	}

	@Test
	public void irqIsIgnoredWhileInterruptDisableIsSet() throws Exception {
		CPU c = running();
		c.IFLAG.set();
		c.bus.raiseInterupt();
		c.step();                                // executes the NOP instead
		assertEquals(0x1001, c.pc);
		assertTrue(c.bus.IsInterruptRaised(), "request stays pending");
	}

	@Test
	public void maskedIrqIsServicedOnceInterruptsAreEnabled() throws Exception {
		CPU c = running();
		c.IFLAG.set();
		c.bus.raiseInterupt();
		c.step();
		c.IFLAG.clear();
		c.step();
		assertEquals(IRQ, c.pc);
	}

	@Test
	public void nmiIgnoresInterruptDisableAndUsesItsVector() throws Exception {
		CPU c = running();
		c.IFLAG.set();
		c.bus.raiseNMInterupt();
		c.step();
		assertEquals(NMI, c.pc);
		assertEquals(0x24, stack(c, 1));         // unused + I (was set), B clear
		assertFalse(c.bus.IsNMInterruptRaised());
	}

	@Test
	public void resetBeatsNmiAndIrq() throws Exception {
		CPU c = running();
		c.IFLAG.clear();
		c.bus.raiseNMInterupt();
		c.bus.raiseInterupt();
		c.bus.raisepowerOnReset();
		c.step();
		assertEquals(RESET, c.pc);
		assertEquals(0xfd, c.sp, "reset does not push");
	}

	@Test
	public void nmiBeatsIrq() throws Exception {
		CPU c = running();
		c.IFLAG.clear();
		c.bus.raiseNMInterupt();
		c.bus.raiseInterupt();
		c.step();
		assertEquals(NMI, c.pc);
		assertTrue(c.bus.IsInterruptRaised(), "the IRQ is still pending (and now masked)");
	}

	@Test
	public void vectorOfZeroIsAValidTarget() throws Exception {
		CPU c = cpu(0, RESET, 0x0000);
		c.step();
		c.pc = 0x1000;
		c.bus.write(0x1000, OpCodes.NOP.code());
		c.IFLAG.clear();
		c.bus.raiseInterupt();
		c.step();
		assertEquals(0x0000, c.pc);
		assertEquals(0xfa, c.sp, "state was pushed");
	}

	@Test
	public void brkUsesTheIrqVectorWithBreakSet() throws Exception {
		CPU c = running();
		c.bus.write(0x1000, OpCodes.BRK.code());
		c.step();
		assertEquals(IRQ, c.pc);
		assertEquals(0x30, stack(c, 1) & 0x30);  // unused and B set
		assertEquals(0x02, stack(c, 2));         // return address = BRK + 2
		assertEquals(0x10, stack(c, 3));
		assertTrue(c.IFLAG.isSet());
	}

	@Test
	public void rtiReturnsFromNmi() throws Exception {
		CPU c = running();
		c.IFLAG.clear();
		c.CFLAG.set();
		c.bus.write(NMI, OpCodes.RTI.code());
		c.bus.raiseNMInterupt();
		c.step();
		assertEquals(NMI, c.pc);
		c.CFLAG.clear();
		c.step();                                // RTI
		assertEquals(0x1000, c.pc, "returns to the interrupted instruction");
		assertTrue(c.CFLAG.isSet());
		assertFalse(c.IFLAG.isSet());
		assertEquals(0xfd, c.sp);
	}
}
