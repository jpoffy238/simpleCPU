package com.mj.Firmware.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.mj.Devices.AddressRange;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.Firmware.Framework.machineState;
import com.mj.cpu001.CPU;
import com.mj.memoryInterface.MemoryProbe;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

/**
 * Runs a legacy instruction class and its table-driven replacement through the
 * same randomized scenarios and reports every observable difference: A, X, Y,
 * SP, PC, each flag, all of RAM and ROM, and any exception raised.
 */
public class EquivalenceHarness {

	/** Everything observable about the machine after executing one instruction. */
	public static final class Snapshot {
		public int a, x, y, sp, pc;
		public boolean c, z, i, d, v, n;
		public String exception = "";
		public byte[] ram, rom;

		@Override
		public String toString() {
			return String.format("A=%02x X=%02x Y=%02x SP=%02x PC=%04x C=%b Z=%b I=%b D=%b V=%b N=%b exc=%s", a, x, y, sp, pc,
					c, z, i, d, v, n, exception);
		}
	}

	/** One difference found in one scenario. */
	public static final class Diff {
		public final int scenario;
		public final String field;
		public final Snapshot before, legacy, table;

		Diff(int scenario, String field, Snapshot before, Snapshot legacy, Snapshot table) {
			this.scenario = scenario;
			this.field = field;
			this.before = before;
			this.legacy = legacy;
			this.table = table;
		}

		@Override
		public String toString() {
			return "scenario " + scenario + " field " + field + "\n  before: " + before + "\n  legacy: " + legacy
					+ "\n  table : " + table;
		}
	}

	/** Decides whether a difference is a documented, intentional bug fix. */
	@FunctionalInterface
	public interface KnownDifference {
		boolean explains(Diff d);
	}

	private static final class Rig {
		final basicMemory ram;
		final basicROM rom;
		final CPU cpu;

		Rig() throws Exception {
			PBus bus = new DeviceBus();
			ram = new basicMemory(bus, new AddressRange(0, 32 * 1024), null, 0);
			bus.registerDevice(ram);
			rom = new basicROM(bus, new AddressRange(0xf000, 0xffff), null, 0);
			bus.registerDevice(rom);
			cpu = new CPU(bus, new cpu001decoder());
			bus.clearpowerOnReset();         // we execute instructions directly
		}
	}

	private static int biasedByte(Random r) {
		int[] edge = { 0x00, 0x01, 0x7f, 0x80, 0xfe, 0xff };
		return r.nextInt(10) < 4 ? edge[r.nextInt(edge.length)] : r.nextInt(256);
	}

	private static int highByte(Random r) {
		int k = r.nextInt(100);
		if (k < 60) return r.nextInt(0x80);            // RAM
		if (k < 75) return 0xff;                         // wraps / vector area
		if (k < 90) return 0xf0 + r.nextInt(0x0f);      // ROM
		return r.nextInt(256);                           // anywhere, incl. unmapped
	}

	private static Snapshot capture(Rig rig, String exception) {
		CPU c = rig.cpu;
		Snapshot s = new Snapshot();
		s.a = c.a.get(); s.x = c.x.get(); s.y = c.y.get(); s.sp = c.sp; s.pc = c.pc;
		s.c = c.CFLAG.isSet(); s.z = c.ZFLAG.isSet(); s.i = c.IFLAG.isSet();
		s.d = c.DFLAG.isSet(); s.v = c.OFLAG.isSet(); s.n = c.NFLAG.isSet();
		s.exception = exception;
		s.ram = MemoryProbe.raw(rig.ram).clone();
		s.rom = MemoryProbe.raw(rig.rom).clone();
		return s;
	}

	private static void setFlag(com.mj.Registers.registerFlags f, boolean on) {
		if (on) f.set(); else f.clear();
	}

	/** Load an identical, seeded scenario into a rig. */
	private static void prepare(Rig rig, long seed) throws Exception {
		Random r = new Random(seed);
		byte[] ram = MemoryProbe.raw(rig.ram);
		byte[] rom = MemoryProbe.raw(rig.rom);
		r.nextBytes(ram);
		r.nextBytes(rom);

		int pc;
		switch (r.nextInt(8)) {
		case 0: pc = 0x10ff; break;                        // two-byte operand straddles a page
		case 1: pc = 0x00ff; break;
		default: pc = 0x0200 + r.nextInt(0x7d00);
		}
		ram[pc] = (byte) biasedByte(r);
		ram[pc + 1] = (byte) (r.nextBoolean() ? highByte(r) : biasedByte(r));

		CPU c = rig.cpu;
		c.pc = pc;
		c.sp = r.nextInt(256);
		c.a.reset(); c.x.reset(); c.y.reset();
		try { c.a.set(biasedByte(r)); } catch (Exception e) { }
		try { c.x.set(biasedByte(r)); } catch (Exception e) { }
		try { c.y.set(biasedByte(r)); } catch (Exception e) { }
		setFlag(c.CFLAG, r.nextBoolean());
		setFlag(c.ZFLAG, r.nextBoolean());
		setFlag(c.IFLAG, r.nextBoolean());
		setFlag(c.DFLAG, r.nextBoolean());
		setFlag(c.OFLAG, r.nextBoolean());
		setFlag(c.NFLAG, r.nextBoolean());
	}

	private static Snapshot execute(Rig rig, machineState instruction) {
		String exception = "";
		try {
			instruction.exeute(rig.cpu);
		} catch (Exception e) {
			exception = e.getClass().getSimpleName();
		}
		return capture(rig, exception);
	}

	private static void compare(List<Diff> out, int scenario, Snapshot before, Snapshot l, Snapshot t) {
		if (l.a != t.a) out.add(new Diff(scenario, "A", before, l, t));
		if (l.x != t.x) out.add(new Diff(scenario, "X", before, l, t));
		if (l.y != t.y) out.add(new Diff(scenario, "Y", before, l, t));
		if (l.sp != t.sp) out.add(new Diff(scenario, "SP", before, l, t));
		if (l.pc != t.pc) out.add(new Diff(scenario, "PC", before, l, t));
		if (l.c != t.c) out.add(new Diff(scenario, "C", before, l, t));
		if (l.z != t.z) out.add(new Diff(scenario, "Z", before, l, t));
		if (l.i != t.i) out.add(new Diff(scenario, "I", before, l, t));
		if (l.d != t.d) out.add(new Diff(scenario, "D", before, l, t));
		if (l.v != t.v) out.add(new Diff(scenario, "V", before, l, t));
		if (l.n != t.n) out.add(new Diff(scenario, "N", before, l, t));
		if (!l.exception.equals(t.exception)) out.add(new Diff(scenario, "exception", before, l, t));
		if (!Arrays.equals(l.ram, t.ram)) out.add(new Diff(scenario, "RAM", before, l, t));
		if (!Arrays.equals(l.rom, t.rom)) out.add(new Diff(scenario, "ROM", before, l, t));
	}

	/**
	 * Compare the two implementations over {@code scenarios} random scenarios.
	 * 
	 * @return the differences that are NOT explained by {@code known}
	 */
	public static List<Diff> compare(Supplier<machineState> legacy, machineState table, int scenarios,
			KnownDifference known) throws Exception {
		return compare(legacy, table, scenarios, known, true);
	}

	/**
	 * @param checkOpcode false only where the legacy class was registered under the
	 *                    wrong opcode but its behavior matches the table row
	 */
	public static List<Diff> compare(Supplier<machineState> legacy, machineState table, int scenarios,
			KnownDifference known, boolean checkOpcode) throws Exception {
		if (checkOpcode && legacy.get().getOpCode() != table.getOpCode()) {
			throw new AssertionError("opcode mismatch");
		}
		Rig legacyRig = new Rig();
		Rig tableRig = new Rig();
		machineState legacyInstruction = legacy.get();
		List<Diff> unexplained = new ArrayList<>();
		for (int s = 0; s < scenarios; s++) {
			long seed = 1000003L * (s + 1) + (table.getOpCode() & 0xff);
			prepare(legacyRig, seed);
			prepare(tableRig, seed);
			Snapshot before = capture(tableRig, "");
			Snapshot l = execute(legacyRig, legacyInstruction);
			Snapshot t = execute(tableRig, table);
			List<Diff> diffs = new ArrayList<>();
			compare(diffs, s, before, l, t);
			for (Diff d : diffs) {
				if (known == null || !known.explains(d)) {
					unexplained.add(d);
				}
			}
		}
		return unexplained;
	}

	public static String summarize(List<Diff> diffs) {
		if (diffs.isEmpty()) return "no differences";
		StringBuilder sb = new StringBuilder(diffs.size() + " unexplained difference(s); first 3:\n");
		for (int i = 0; i < Math.min(3, diffs.size()); i++) sb.append(diffs.get(i)).append('\n');
		return sb.toString();
	}
}
