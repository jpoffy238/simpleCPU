package com.mj.z80;

import java.io.IOException;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Disk.EightInch.IBM3740;
import com.mj.Disk.FloppyController;
import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.IntelHex.IntelHexLoader;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.Video.VideoController;

/** A real Z80 machine - the two-bus scaffolding from Z80Machine, plus
 * VideoController and FloppyController actually wired up, the same way
 * CPUMain wires the 6502 side's own devices. A separate class from
 * Z80Machine, not a constructor option on it: Z80Machine's own no-arg
 * constructor fills the entire 64K memory map with plain RAM, which
 * every instruction test still depends on, and that RAM would collide
 * with the video RAM carved out below - this class builds its own two
 * fresh buses from scratch instead of reusing or extending that one.
 *
 * Memory bus: RAM everywhere except the video RAM carve-outs, which
 * match the 6502 side's own addresses exactly ($B800-$BFFF text,
 * $C000-$DFFF graphics) - no reason for them to differ, and it keeps the
 * two CPUs' memory maps easy to compare. No ROM/firmware exists for the
 * Z80 side yet, so what would be ROM space on the 6502 side is just more
 * RAM here for now.
 *
 * Port bus: VideoController's small register set (CRTC Address/Data,
 * keyboard data/status, Mode Control) at $00-$04; FloppyController
 * wholesale (it has no bulk-RAM piece the way Video does - sector data
 * already moves one byte at a time through its own Data register) at
 * $10-$14. FloppyController no longer needs a VIA to interrupt through
 * (see InterruptLine) - it pulls the port bus's own single interrupt
 * line directly, which is period-correct for a Z80 system (a VIA is
 * 6502-era hardware) and simpler than the 6502 side's VIA-routed
 * version.
 *
 * Deliberately does not attach any disk images itself - attachDrive()
 * below is for that, so a real boot entry point (once one exists) can
 * point it at real files ("z80-disk0.img", "z80-disk1.img", ... - a
 * "z80-" prefix so they can't collide with the 6502 side's own
 * disk0.img..disk3.img if both ever run from the same working
 * directory) while a test can point it at a temp directory instead,
 * the same way every other disk-touching test in this repo already
 * avoids writing real image files into the working directory.
 * FloppyController itself now supports up to 8 drives (a real 3-line
 * drive-select field - see its own class comment); only as many as
 * attachDrive() is actually called for report ready, the rest just
 * report not-ready, same as an empty drive bay. */
public class Z80VideoDiskMachine {
	private final PBus memoryBus;
	private final PBus portBus;
	private final VideoController video;
	private final FloppyController floppyController;

	public Z80VideoDiskMachine() {
		memoryBus = new DeviceBus();
		portBus = new DeviceBus();

		memoryBus.registerDevice(new basicMemory(memoryBus, new AddressRange(0x0000, 0xb7ff), null, 0));
		memoryBus.registerDevice(new basicMemory(memoryBus, new AddressRange(0xe000, 0xffff), null, 0));

		basicMemory videoRam = new basicMemory(memoryBus, new AddressRange(0xb800, 0xbfff), null, 0);
		memoryBus.registerDevice(videoRam);
		basicMemory graphicsRam = new basicMemory(memoryBus, new AddressRange(0xc000, 0xdfff), null, 0);
		memoryBus.registerDevice(graphicsRam);

		video = new VideoController(new AddressRange(0x00, 0x04), videoRam, graphicsRam);
		portBus.registerDevice(video);

		floppyController = new FloppyController(portBus::raiseInterupt, new AddressRange(0x10, 0x14));
		portBus.registerDevice(floppyController);
	}

	public PBus memoryBus() {
		return memoryBus;
	}

	public PBus portBus() {
		return portBus;
	}

	public VideoController video() {
		return video;
	}

	public FloppyController floppyController() {
		return floppyController;
	}

	/** Attaches a disk image file to one of FloppyController's drive
	 * slots, creating it fresh (with a valid header) on first use if it
	 * doesn't exist yet - see this class's own comment for the
	 * "z80-diskN.img" naming convention a real boot entry point should
	 * follow. */
	public void attachDrive(int driveNumber, String imageFileName) throws DiskReadError, DiskWriteError {
		IBM3740 disk = new IBM3740();
		disk.setVirtualDiskFile(imageFileName);
		floppyController.attachDrive(driveNumber, disk);
	}

	/** Loads a z80-asm-assembled .z80 file (see Z80BinaryFile) onto the
	 * memory bus at its own embedded start address - same convenience
	 * Z80Machine's own loadIntoMemory() provides. */
	public void loadIntoMemory(String fileName)
			throws IOException, illegalAddressException, ROException, DeviceUnavailable {
		Z80BinaryFile.load(memoryBus, fileName);
	}

	/** Loads a plain Intel HEX file (SDCC's own .ihx output, same format
	 * cc65/crasm's .hex already use on the 6502 side) onto the memory bus -
	 * shares IntelHexLoader (modules/Devices) rather than duplicating the
	 * record-walking loop, since both toolchains just emit standard Intel
	 * HEX. startAddress is the caller's own entry point, matching whatever
	 * --code-loc the C program was linked with (Demo.asm's own convention,
	 * $1000, for the only one that exists so far - see
	 * src/main/c/Makefile) - Intel HEX records carry their own absolute
	 * addresses, so this is a plain relocation offset, not a lookup;
	 * CPU_CreateUtil.load()'s every real 6502 caller passes 0 here too. */
	public void loadIntelHexIntoMemory(String fileName, int startAddress)
			throws illegalAddressException, ROException, IOException, DeviceUnavailable {
		IntelHexLoader.load(memoryBus, fileName, startAddress);
	}
}
