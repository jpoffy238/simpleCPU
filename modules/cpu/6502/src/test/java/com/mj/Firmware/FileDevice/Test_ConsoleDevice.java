package com.mj.Firmware.FileDevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.illegalOpCodeException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

/** Drives the console through a real 6502 CPU - a handful of LDA/STA instructions
 * writing to the data register - and checks the resulting screen state directly,
 * since there's no display under a headless test run. */
public class Test_ConsoleDevice {
	private static final int CONSOLE_DATA = 0xe000;

	private PBus bus;
	private ConsoleDevice console;
	private CPU c;

	@BeforeEach
	public void setup() throws Exception {
		bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0));
		basicROM rom = new basicROM(bus, new AddressRange(0xf000, 0xffff), null, 0);
		rom.setVectors(0x1000, 0x1000, 0x1000);
		bus.registerDevice(rom);

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		console = new ConsoleDevice(via, new AddressRange(0xe000, 0xe001));
		bus.registerDevice(console);

		c = new CPU(bus, new cpu001decoder());
		c.step();     // power-on reset
		c.pc = 0x1000;
	}

	private void poke(int address, int... bytes) throws Exception {
		for (int i = 0; i < bytes.length; i++) {
			c.bus.write(address + i, (byte) bytes[i]);
		}
	}

	private void runToHalt() throws Exception {
		assertThrows(illegalOpCodeException.class, () -> {
			for (int i = 0; i < 20; i++) {
				c.step();
			}
		});
	}

	@Test
	public void printsCharactersInOrderOnTheFirstRow() throws Exception {
		poke(0x1000,
				OpCodes.LDA_IMM.code() & 0xff, 'H',
				OpCodes.STA_ABS.code() & 0xff, CONSOLE_DATA & 0xff, CONSOLE_DATA >> 8,
				OpCodes.LDA_IMM.code() & 0xff, 'I',
				OpCodes.STA_ABS.code() & 0xff, CONSOLE_DATA & 0xff, CONSOLE_DATA >> 8,
				OpCodes.HLT.code() & 0xff);

		runToHalt();

		assertEquals('H', console.getRow(0).charAt(0));
		assertEquals('I', console.getRow(0).charAt(1));
	}

	@Test
	public void newlineMovesToTheNextRow() throws Exception {
		poke(0x1000,
				OpCodes.LDA_IMM.code() & 0xff, 'A',
				OpCodes.STA_ABS.code() & 0xff, CONSOLE_DATA & 0xff, CONSOLE_DATA >> 8,
				OpCodes.LDA_IMM.code() & 0xff, '\n',
				OpCodes.STA_ABS.code() & 0xff, CONSOLE_DATA & 0xff, CONSOLE_DATA >> 8,
				OpCodes.LDA_IMM.code() & 0xff, 'B',
				OpCodes.STA_ABS.code() & 0xff, CONSOLE_DATA & 0xff, CONSOLE_DATA >> 8,
				OpCodes.HLT.code() & 0xff);

		runToHalt();

		assertEquals('A', console.getRow(0).charAt(0));
		assertEquals('B', console.getRow(1).charAt(0));
	}
}
