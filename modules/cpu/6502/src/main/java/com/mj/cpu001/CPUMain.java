package com.mj.cpu001;

import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.DeviceBus;
import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.RTC;
import com.mj.Devices.VIA6522;
import com.mj.Disk.FloppyController;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Firmware.Framework.Decoder;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;
import com.mj.Video.VideoController;

public class CPUMain {
	private static final Logger logger = LogManager.getLogger(CPUMain.class);
	public static void main(String[] args) {
		// Optional first arg: a folder holding disk0.img/disk1.img/.../disk3.img.
		// Defaults to the current working directory, same as before this was configurable.
		String diskDir = args.length > 0 ? args[0] : ".";

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

		// BDOS: a third ROM, this one cc65-compiled rather than hand-assembled, so it
		// loads as a raw binary (loadBinary()) rather than through the constructor's
		// Intel-hex path - see AbstractMemoryLayer.loadBinary() and bdos.cfg for why.
		// $8000-$80FF is BDOS's own working RAM (its directory-scan position, open-
		// file state, sector buffer) - plain basicMemory, not basicROM, since BDOS's
		// C code writes to it at runtime; see bdos.cfg's file comment for the full
		// ROM/RAM split.
		bus.registerDevice(new basicMemory(bus, new AddressRange(0x8000, 0x81ff), null, 0));
		basicROM bdos = new basicROM(bus, new AddressRange(0xa000, 0xb7ff), null, 0);
		try {
			bdos.loadBinary("bdos.bin");
		} catch (java.io.IOException e) {
			logger.error("Unable to load bdos.bin - BDOS-dependent commands (B, and anything CCP.COM calls) will fault", e);
		}
		bus.registerDevice(bdos);

		VIA6522 via = new VIA6522(bus, new AddressRange(0x9000, 0x900f));
		bus.registerDevice(via);

		// Video: a CRTC-style display controller (see VideoController's class comment) -
		// the sole console now (ConsoleDevice drove this exclusively once, then both in
		// parallel while Video was being validated against it; this is the cutover,
		// now that it's trusted). The character RAM it scans out is plain basicMemory,
		// registered on the bus in its own right (the CPU pokes ASCII bytes straight
		// into it at ordinary addresses, no register call needed) but also handed
		// directly to VideoController's constructor, since nothing else gives its
		// renderer a way to read those bytes back for display. Sits right after BDOS's
		// ROM in the free address space.
		basicMemory videoRam = new basicMemory(bus, new AddressRange(0xb800, 0xbfff), null, 0);
		bus.registerDevice(videoRam);
		// Graphics VRAM for MODE_GRAPHICS (see VideoController's class comment) -
		// 320x200 1bpp needs 8000 bytes; $C000-$DFFF (8192) sat completely unused in
		// the memory map until now, between text video RAM ($B800-$BFFF) below and
		// BIOS/MONITOR ROM ($F000+) above - no existing region needed to move.
		basicMemory graphicsRam = new basicMemory(bus, new AddressRange(0xc000, 0xdfff), null, 0);
		bus.registerDevice(graphicsRam);
		VideoController video = new VideoController(new AddressRange(0x9020, 0x9024), videoRam, graphicsRam);
		bus.registerDevice(video);

		// Real-time clock (RTC.java) - a plain 4-byte read-only Unix-epoch-seconds
		// register block, right after Video's own range in the free $90xx MMIO
		// space (not $A000, RTC's own test-scratch-bus address - that range is
		// BDOS ROM here). Its ticking Thread is never started: nothing on this
		// bus has ever called bus.startDevices() (VIA/Floppy/Video don't need a
		// Thread either), and RTC.read() self-refreshes on its own whenever
		// nobody started it - see RTC.java's own comment - so BIOS_GET_TIME
		// still gets a genuinely live value through plain polling.
		RTC rtc = new RTC(() -> via.requestInterrupt(VIA6522.FLAG_T1), new AddressRange(0x9025, 0x9028));
		bus.registerDevice(rtc);

		FloppyController fdc = new FloppyController(() -> via.requestInterrupt(VIA6522.FLAG_CB1), new AddressRange(0x9010, 0x9014));
		bus.registerDevice(fdc);
		for (int drive = 0; drive < 4; drive++) {
			String imageName = Paths.get(diskDir, "disk" + drive + ".img").toString();
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
		video.show();
		c.start();
		}



	}

