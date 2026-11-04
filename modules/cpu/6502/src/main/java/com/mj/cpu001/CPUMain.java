package com.mj.cpu001;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.PBus;
import com.mj.Devices.VIA6522;
import com.mj.Disk.FloppyController;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Firmware.Framework.Decoder;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class CPUMain {
	private static final Logger logger = LogManager.getLogger(CPUMain.class);
	public static void main(String[] args) {

		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus,
					new AddressRange(0, 32*1024) ,
					null, 0	));

		// Two ROM chips, loaded via basicROM's privileged constructor-time path (real
		// write protection, not just a convention) - BIOS.hex is the primitives ROM
		// (console/disk hardware access only), MONITOR.hex is the command interpreter,
		// which calls BIOS.hex's fixed jump table rather than touching hardware
		// registers itself. MONITOR.hex carries its own vector table ($FFFA-$FFFF plus
		// our project-specific fault vector at $FFF8), so there's no separate
		// setVectors()/setFaultVector() call needed here.
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf000, 0xf7ff), "BIOS.hex", 0));
		bus.registerDevice(new basicROM(bus, new AddressRange(0xf800, 0xffff), "MONITOR.hex", 0));

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);
		ConsoleDevice con = new ConsoleDevice(via, new AddressRange(0xec00, 0xec01));
		bus.registerDevice(con);

		FloppyController fdc = new FloppyController(via, new AddressRange(0x9010, 0x9014));
		bus.registerDevice(fdc);
		for (int drive = 0; drive < 4; drive++) {
			String imageName = "disk" + drive + ".img";
			try {
				// Created fresh (with a valid header) on first run if it doesn't exist yet.
				IBM3740 disk = new IBM3740();
				disk.setVirtualDiskFile(imageName);
				fdc.attachDrive(drive, disk);
			} catch (Exception e) {
				logger.error("Unable to mount " + imageName + " - drive " + drive + " will report not-ready", e);
			}
		}

		Decoder d =  new cpu001decoder();

		CPU c = new CPU(bus, d);
		logger.error(String.format("Number of Opcodes = %d ", OpCodes.getMap().size()));
		con.show();
		c.start();
		}



	}

