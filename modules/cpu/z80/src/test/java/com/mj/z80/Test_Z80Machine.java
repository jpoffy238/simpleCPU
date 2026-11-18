package com.mj.z80;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.memoryInterface.basicMemory;

/** Proves out the two-bus design (see Z80Machine's class comment) before any
 * CPU exists to drive it: RAM on the memory bus round-trips normally, and
 * the memory bus and port bus are genuinely separate address spaces - a
 * device registered on one is invisible to the other, even at the exact
 * same address. */
public class Test_Z80Machine {
	@Test
	public void ramOnTheMemoryBusRoundTrips() throws Exception {
		Z80Machine machine = new Z80Machine();

		machine.memoryBus().write(0x1234, (byte) 0x42);

		assertEquals(0x42, machine.memoryBus().read(0x1234) & 0xff);
	}

	@Test
	public void thePortBusStartsWithNoDevicesRegisteredOnIt() {
		Z80Machine machine = new Z80Machine();

		assertThrows(DeviceUnavailable.class, () -> machine.portBus().read(0x00));
	}

	@Test
	public void aDeviceOnThePortBusIsInvisibleToTheMemoryBusAtTheSameAddress() throws Exception {
		Z80Machine machine = new Z80Machine();
		// A narrow 1-byte device at port $10 - nothing like this exists on the
		// memory bus's own address $0010 (that's just ordinary RAM there).
		machine.portBus().registerDevice(new basicMemory(machine.portBus(), new AddressRange(0x10, 0x10), null, 0));

		machine.portBus().write(0x10, (byte) 0x99);

		assertEquals(0x99, machine.portBus().read(0x10) & 0xff);
		assertEquals(0x00, machine.memoryBus().read(0x10) & 0xff,
				"the memory bus's own RAM at the same numeric address should be untouched");
	}
}
