package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

/** Drives the real BIOS.hex monitor through a CPU + console, the same way a person
 * typing at the keyboard would, and checks the result through getRow()/the bus
 * rather than the CPU's internal state - this is testing the firmware, not the CPU. */
public class Test_BIOS {
	private PBus bus;
	private ConsoleDevice console;
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0));
		basicROM rom = new basicROM(bus, new AddressRange(0xff00, 0xffff), null, 0);
		rom.setVectors(0x1000, 0x1000, 0x1000);
		bus.registerDevice(rom);

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		console = new ConsoleDevice(via, new AddressRange(0xec00, 0xec01));
		bus.registerDevice(console);

		CPU_CreateUtil.load(bus, "BIOS.hex", 0);

		c = new CPU(bus, new cpu001decoder());
	}

	private void runBriefly() throws Exception {
		c.start();
		Thread.sleep(2000);
		c.interrupt();
	}

	@Test
	public void dumpPrintsHexdumpStyleOutput() throws Exception {
		c.bus.write(0x2000, (byte) 'H');
		c.bus.write(0x2001, (byte) 'I');

		console.type("D 2000\r");
		runBriefly();

		assertTrue(console.getRow(0).contains("D 2000"), "the typed command should be echoed");
		assertTrue(console.getRow(1).startsWith(
				"2000  48 49 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |HI..............|"),
				"first dump line, got: [" + console.getRow(1) + "]");
		assertTrue(console.getRow(16).startsWith("20F0"), "16 lines of 16 bytes covers $2000-$20FF");
	}

	@Test
	public void writeStoresBytesStartingAtTheGivenAddress() throws Exception {
		console.type("W 2000 48 49 4a\r");
		runBriefly();

		assertEquals(0x48, c.bus.read(0x2000) & 0xff);
		assertEquals(0x49, c.bus.read(0x2001) & 0xff);
		assertEquals(0x4a, c.bus.read(0x2002) & 0xff);
	}
}
