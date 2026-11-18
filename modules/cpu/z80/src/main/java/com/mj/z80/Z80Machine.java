package com.mj.z80;

import java.io.IOException;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;

/** Wires up a Z80 machine's two independent address spaces - the ordinary
 * 16-bit memory bus every LD/arithmetic/etc. instruction uses, and a
 * separate port bus that only IN/OUT ever touch. Two distinct DeviceBus
 * instances rather than one bus with a memory/IO discriminator - the real
 * hardware's /IORQ-vs-/MREQ pin distinction is just "which method the CPU's
 * decoder calls" here, with no need to overlay port numbers onto the same
 * address range memory uses (that overlay, on real hardware, was about
 * conserving address-bus pins, not something a method-call bus needs).
 *
 * Both com.mj.Devices (the bus/device framework) and com.mj.memoryInterface
 * (basicMemory/basicROM) are shared, CPU-agnostic infrastructure the 6502
 * module already depends on - nothing here is Z80-specific yet beyond the
 * two-bus split itself; that arrives with the fetch/decode loop and the
 * actual port-mapped devices (FloppyController-equivalent, VideoController's
 * register set - see the Video split noted in project memory).
 *
 * RAM fills the whole 64K memory map for now - no ROM/ports have been
 * carved out yet, since that depends on decisions (BIOS layout, boot
 * sequence) that come later, once there's a CPU to actually run one. */
public class Z80Machine {
	public static final int MEMORY_SIZE = 64 * 1024;

	private final PBus memoryBus;
	private final PBus portBus;

	public Z80Machine() {
		memoryBus = new DeviceBus();
		portBus = new DeviceBus();
		memoryBus.registerDevice(new basicMemory(memoryBus, new AddressRange(0, MEMORY_SIZE - 1), null, 0));
	}

	public PBus memoryBus() {
		return memoryBus;
	}

	public PBus portBus() {
		return portBus;
	}

	/** Loads a z80-asm-assembled .z80 file (see Z80BinaryFile) onto the
	 * memory bus at its own embedded start address - the "a way to load
	 * code into memory" half of the bus/memory bootstrap step, now that
	 * there's a real toolchain to produce code to load. */
	public void loadIntoMemory(String fileName)
			throws IOException, illegalAddressException, ROException, DeviceUnavailable {
		Z80BinaryFile.load(memoryBus, fileName);
	}
}
