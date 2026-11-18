package com.mj.z80.instructions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.memoryInterface.basicMemory;
import com.mj.z80.Flags;
import com.mj.z80.Register8;
import com.mj.z80.Z80CPU;
import com.mj.z80.Z80InstructionDecoder;
import com.mj.z80.Z80Machine;

/** IN A,(n) / OUT (n),A (base table) and IN r,(C) / OUT (C),r (ED table) -
 * see PortTable and PortRegisterTable. */
public class Test_Port {
	private Z80Machine machine;
	private Z80CPU cpu;

	@BeforeEach
	public void setup() {
		machine = new Z80Machine();
		// A full-range stand-in device, exactly like memoryBus's own RAM -
		// not realistic hardware, just something to read back from so these
		// tests can verify what got written to a given port.
		machine.portBus().registerDevice(new basicMemory(machine.portBus(), new AddressRange(0, 0xffff), null, 0));
		Z80InstructionDecoder decoder = new Z80InstructionDecoder();
		PortTable.populate(decoder);
		PortRegisterTable.populate(decoder);
		cpu = new Z80CPU(machine.memoryBus(), machine.portBus(), decoder);
	}

	private void runBase(int opcode, int immediate) throws Exception {
		machine.memoryBus().write(0x1000, (byte) opcode);
		machine.memoryBus().write(0x1001, (byte) immediate);
		cpu.pc = 0x1000;
		cpu.step();
	}

	private void runED(int edOpcode) throws Exception {
		machine.memoryBus().write(0x1000, (byte) 0xED);
		machine.memoryBus().write(0x1001, (byte) edOpcode);
		cpu.pc = 0x1000;
		cpu.step();
	}

	// ---- IN A,(n) / OUT (n),A ----

	@Test
	public void inAReadsFromThePortAddressedByAInTheUpperByteAndNImmediateInTheLower() throws Exception {
		cpu.af.setHi(0x12);
		machine.portBus().write(0x1234, (byte) 0x99);
		cpu.af.setLo(Flags.S | Flags.Z | Flags.PV | Flags.N | Flags.C); // sentinel - IN A,(n) affects no flag

		runBase(0xDB, 0x34);

		assertEquals(0x99, cpu.af.hi());
		assertEquals(Flags.S | Flags.Z | Flags.PV | Flags.N | Flags.C, cpu.af.lo(), "IN A,(n) must leave every flag untouched");
		assertEquals(0x1002, cpu.pc);
	}

	@Test
	public void outAWritesToThePortAddressedByAInTheUpperByteAndNImmediateInTheLower() throws Exception {
		cpu.af.setHi(0x12);
		cpu.af.setLo(Flags.S | Flags.Z); // sentinel - OUT (n),A affects no flag

		runBase(0xD3, 0x34);

		assertEquals(0x12, machine.portBus().read(0x1234) & 0xff);
		assertEquals(Flags.S | Flags.Z, cpu.af.lo());
	}

	// ---- IN r,(C) / OUT (C),r ----

	@Test
	public void inRegisterFromPortReadsTheFullBcAddressedPortAndSetsFlagsFromTheValue() throws Exception {
		cpu.bc.set16(0x1234);
		machine.portBus().write(0x1234, (byte) 0x80); // S set, Z clear, odd parity -> PV clear
		cpu.af.setLo(Flags.C); // sentinel - C must be preserved

		runED(0x50); // IN D,(C)  ($40 | D.code()<<3 == $40 | 2<<3 == $50)

		assertEquals(0x80, Register8.D.get(cpu));
		assertEquals(Flags.C | Flags.S, cpu.af.lo());
	}

	@Test
	public void inRegisterFromPortSetsZeroAndParityFlagsOnAZeroByte() throws Exception {
		cpu.bc.set16(0x1234);
		machine.portBus().write(0x1234, (byte) 0x00);

		runED(0x40); // IN B,(C)

		assertEquals(0x00, Register8.B.get(cpu));
		assertEquals(Flags.Z | Flags.PV, cpu.af.lo(), "0 has even parity (zero bits set), so PV is set, same convention as the logic ALU family");
	}

	@Test
	public void outPortFromRegisterWritesTheRegisterToTheFullBcAddressedPortAndAffectsNoFlag() throws Exception {
		cpu.bc.set16(0x1234);
		Register8.E.set(cpu, 0x55);
		cpu.af.setLo(Flags.S | Flags.C); // sentinel

		runED(0x59); // OUT (C),E  ($41 | E.code()<<3 == $41 | 3<<3 == $59)

		assertEquals(0x55, machine.portBus().read(0x1234) & 0xff);
		assertEquals(Flags.S | Flags.C, cpu.af.lo());
	}

	@Test
	public void everyRegisterDispatchesToTheSamePortAddress() throws Exception {
		for (Register8 r : Register8.values()) {
			cpu.bc.set16(0x2000);
			machine.portBus().write(0x2000, (byte) 0x42);

			runED(0x40 | (r.code() << 3)); // IN r,(C)

			assertEquals(0x42, r.get(cpu), "IN " + r + ",(C) should have loaded the port's value");

			r.set(cpu, 0x77);
			// r is itself part of BC for r==B/C, so the port address the
			// instruction actually uses may have just moved along with it -
			// read back from wherever BC now points, not the stale 0x2000.
			int port = cpu.bc.get16();
			runED(0x41 | (r.code() << 3)); // OUT (C),r

			assertEquals(0x77, machine.portBus().read(port) & 0xff, "OUT (C)," + r + " should have written the register's value");
		}
	}
}
