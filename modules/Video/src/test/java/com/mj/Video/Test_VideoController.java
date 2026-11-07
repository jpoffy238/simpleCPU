package com.mj.Video;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.Device;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** Exercises VideoController's register protocol and its reading of a separate
 * character-RAM device - a minimal in-test FakeMemory stands in for the real
 * basicMemory (modules/cpu/6502) a real boot would wire up, since Video has no
 * reason to depend on that module just to test against a byte array. */
public class Test_VideoController {
	private static final int R_H_DISPLAYED = 1;
	private static final int R_V_DISPLAYED = 6;
	private static final int R_CURSOR_START = 10;
	private static final int R_START_ADDR_HI = 12;
	private static final int R_START_ADDR_LO = 13;

	private FakeMemory charRam;
	private VideoController video;

	@BeforeEach
	public void setup() {
		charRam = new FakeMemory(new AddressRange(0xb800, 0xbfff));
		video = new VideoController(new AddressRange(0x9020, 0x9023), charRam);
	}

	private void setRegister(int index, int value) throws Exception {
		video.write(0x9020, (byte) index);
		video.write(0x9021, (byte) value);
	}

	private int getRegister(int index) throws Exception {
		video.write(0x9020, (byte) index);
		return video.read(0x9021) & 0xff;
	}

	@Test
	public void addressRegisterReadsBackWhatWasWritten() throws Exception {
		video.write(0x9020, (byte) 7);

		assertEquals(7, video.read(0x9020) & 0xff);
	}

	@Test
	public void dataRegisterRoundTripsPerIndex() throws Exception {
		setRegister(R_H_DISPLAYED, 80);
		setRegister(R_V_DISPLAYED, 24);

		assertEquals(80, getRegister(R_H_DISPLAYED));
		assertEquals(24, getRegister(R_V_DISPLAYED));
	}

	@Test
	public void writingOneRegisterDoesNotDisturbAnother() throws Exception {
		setRegister(R_H_DISPLAYED, 80);
		setRegister(R_V_DISPLAYED, 99);

		assertEquals(80, getRegister(R_H_DISPLAYED), "H Displayed should be untouched by writing V Displayed");
	}

	@Test
	public void outOfRangeRegisterIndexIsIgnoredRatherThanFaulting() throws Exception {
		video.write(0x9020, (byte) 200);
		video.write(0x9021, (byte) 42);

		assertEquals(0, video.read(0x9021) & 0xff);
	}

	@Test
	public void resetClearsEveryRegister() throws Exception {
		setRegister(R_H_DISPLAYED, 80);

		video.reset();

		assertEquals(0, getRegister(R_H_DISPLAYED));
	}

	@Test
	public void getCharReadsTheCharacterRamAtDisplayStartZero() throws Exception {
		setRegister(R_H_DISPLAYED, 4);
		setRegister(R_V_DISPLAYED, 2);
		charRam.write(0xb800, (byte) 'H');
		charRam.write(0xb801, (byte) 'I');

		assertEquals('H', video.getChar(0, 0));
		assertEquals('I', video.getChar(0, 1));
	}

	@Test
	public void getRowReturnsTheWholeVisibleRow() throws Exception {
		setRegister(R_H_DISPLAYED, 4);
		setRegister(R_V_DISPLAYED, 2);
		charRam.write(0xb800, (byte) 'H');
		charRam.write(0xb801, (byte) 'I');
		charRam.write(0xb802, (byte) ' ');
		charRam.write(0xb803, (byte) ' ');

		assertEquals("HI  ", video.getRow(0));
	}

	@Test
	public void displayStartAddressScrollsWithoutTouchingRam() throws Exception {
		// Real CRTC scrolling: change two registers, not the buffer - row 1's
		// content becomes visible at row 0 purely by moving the start address
		// past row 0's own 4 bytes.
		setRegister(R_H_DISPLAYED, 4);
		setRegister(R_V_DISPLAYED, 2);
		charRam.write(0xb800, (byte) 'A');
		charRam.write(0xb804, (byte) 'B');

		setRegister(R_START_ADDR_HI, 0);
		setRegister(R_START_ADDR_LO, 4);

		assertEquals('B', video.getChar(0, 0), "row 0 should now show what was row 1's data");
	}

	@Test
	public void displayStartAddressWrapsAroundTheEndOfCharRam() throws Exception {
		int ramSize = charRam.getAddressRange().size();
		setRegister(R_H_DISPLAYED, 4);
		setRegister(R_V_DISPLAYED, 2);
		charRam.write(0xb800, (byte) 'Z'); // offset 0, where a start address of
		                                   // (ramSize - 1) + 1 should wrap back to

		setRegister(R_START_ADDR_HI, ((ramSize - 1) >> 8) & 0xff);
		setRegister(R_START_ADDR_LO, (ramSize - 1) & 0xff);

		assertEquals('Z', video.getChar(0, 1), "offset ramSize-1 plus one column should wrap to offset 0");
	}

	@Test
	public void cursorStartsOffUntilSoftwareEnablesIt() throws Exception {
		// Real hardware leaves register contents undefined at power-on, but nothing
		// in this system initializes VideoController yet (no BIOS_VIDEO exists), so
		// defaulting Cursor Start to 0 (which would mean "on") would draw a stray
		// cursor bar at R14/R15's own default of (0,0) before any software ever
		// asked for one. 0x20 is CURSOR_MODE_OFF.
		assertEquals(0x20, getRegister(R_CURSOR_START));
	}

	@Test
	public void keyboardStatusReflectsWhetherAKeyIsQueued() throws Exception {
		assertEquals(0, video.read(0x9023) & 0xff, "nothing typed yet");

		video.type("A");

		assertEquals(1, video.read(0x9023) & 0xff, "a key is now queued");
	}

	@Test
	public void keyboardDataDequeuesInOrderThenReadsZero() throws Exception {
		video.type("AB");

		assertEquals('A', video.read(0x9022) & 0xff);
		assertEquals('B', video.read(0x9022) & 0xff);
		assertEquals(0, video.read(0x9022) & 0xff, "queue is empty now");
	}

	@Test
	public void keyboardStatusGoesBackToZeroOnceDrained() throws Exception {
		video.type("A");
		video.read(0x9022);

		assertEquals(0, video.read(0x9023) & 0xff);
	}

	@Test
	public void arrowKeysQueueTheirSyntheticCodesAlongsideTypedCharacters() throws Exception {
		// Arrow keys never reach keyTyped() (no character), so firmware reading
		// BIOS_GETCH needs them to come through as ordinary queued bytes outside
		// plain ASCII's 0-127 range - same queue, same dequeue order as typing.
		video.type("A");
		video.pressArrowKey(VideoController.KEY_UP);
		video.type("B");

		assertEquals('A', video.read(0x9022) & 0xff);
		assertEquals(VideoController.KEY_UP, video.read(0x9022) & 0xff);
		assertEquals('B', video.read(0x9022) & 0xff);
	}

	/** Minimal standalone Device backing a flat byte array over one AddressRange -
	 * exactly what a real basicMemory does, without pulling in that module. */
	private static class FakeMemory implements Device {
		private final AddressRange range;
		private final byte[] memory;

		FakeMemory(AddressRange range) {
			this.range = range;
			this.memory = new byte[range.size()];
		}

		@Override
		public DEVTYPE getDeviceType() {
			return DEVTYPE.CHAR;
		}

		@Override
		public BussId getBusId() {
			return BussId.MEMROY;
		}

		@Override
		public AddressRange getAddressRange() {
			return range;
		}

		@Override
		public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
			memory[range.addressMapper(address)] = data;
		}

		@Override
		public byte read(int address) throws illegalAddressException, DeviceUnavailable {
			return memory[range.addressMapper(address)];
		}

		@Override
		public void raiseInterrupt() {
		}

		@Override
		public void reset() {
		}
	}
}
