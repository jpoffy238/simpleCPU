package com.mj.cpu001;


import java.io.IOException;

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
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.util.CPU_CreateUtil;

public class CPUMain {
	private static final Logger logger = LogManager.getLogger(CPUMain.class);
	public static void main(String[] args) {
		
		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus,
					new AddressRange(0, 32*1024) ,
					null, 0	));
		
		
		basicROM rom = new basicROM(bus, 
				new AddressRange(0xff00, 0xffff),
				null, 0);
		try {
			// NMI, RESET and IRQ all point at BIOS.hex's entry ($1000): nothing is mapped at the old
			// $f000 IRQ target (ROM here only covers $ff00-$ffff), and the BIOS never enables/uses
			// interrupts anyway, so a safe restart is the right fallback if one ever fires.
			rom.setVectors(0x1000, 0x1000, 0x1000);
			rom.setFaultVector(0x1800);   // BIOS.asm's FAULT handler
		} catch (illegalAddressException e) {
			e.printStackTrace();
		}
		bus.registerDevice(rom);
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
		//CPU_CreateUtil.load(bus, fileName, startAddress);
		try {
			CPU_CreateUtil.load(bus,  "BIOS.hex", 0);
		} catch (illegalAddressException | ROException | DeviceUnavailable | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		con.show();
		c.start();
		}
	
	
		
	}


