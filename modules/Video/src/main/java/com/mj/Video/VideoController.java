package com.mj.Video;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.AddressRange;
import com.mj.Devices.Device;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** A CRTC-style display controller - register layout and semantics modeled on the
 * real Motorola 6845 (the chip real CP/M-era machines like the Kaypro and Osborne
 * actually used), trimmed to the registers that matter for rendering - same spirit
 * as FloppyController's WD179x trim (real registers, no physical sync/timing
 * generation). Unlike ConsoleDevice (a software terminal: the CPU sends one
 * character at a time through a putch-style register, and the device tracks its
 * own cursor/scrolling), this is genuinely memory-mapped: the CPU pokes ASCII bytes
 * directly into a separate character-RAM device at ordinary addresses, and this
 * controller only holds the registers that say how to interpret that RAM - where
 * the visible window starts (R12/R13, so scrolling is "change two registers," not
 * "copy 1920 bytes") and where the cursor is (R14/R15, a RAM offset, not a row/col).
 *
 * Four bytes of address space - the first two are exactly the real 6845's
 * addressing scheme; the last two are a keyboard interface bolted on alongside,
 * since Device only supports one AddressRange per device and a genuinely
 * separate keyboard-chip class would have no way to reach this controller's own
 * Screen panel to attach a KeyListener. Real CRTC-based machines (Kaypro,
 * Osborne) also had keyboard input as a physically separate chip from the CRTC -
 * bolting it on here is a pragmatic compromise around the interface constraint,
 * not a claim that one real chip did both:
 *   +0  Address Register (write-only) - selects which of R0-R17 the next access
 *       to +1 refers to.
 *   +1  Data Register (read/write) - gets/sets the selected register's value.
 *   +2  Keyboard Data (read) - dequeues the next keystroke, or 0 if none queued.
 *   +3  Keyboard Status (read) - bit 0 set if a keystroke is queued. No control
 *       bits (blocking/interrupt mode) like ConsoleDevice's status register -
 *       BIOS_GETCH only ever busy-polls this one bit, same as it does for
 *       ConsoleDevice today, so there's nothing else to model yet.
 *
 * All 18 real registers are readable/writable (real init code written against a
 * real 6845 would write every one of them in order, so even the irrelevant ones
 * need to not fault as "unknown"), but only these actually drive the simplified
 * rendering below:
 *   R1  Horizontal Displayed (columns)
 *   R6  Vertical Displayed (rows)
 *   R10 Cursor Start - used only as an on/off flag (CURSOR_MODE_OFF vs anything
 *       else), not the real 6845's blink-mode/start-scanline bit fields
 *   R12/R13 Display Start Address hi/lo
 *   R14/R15 Cursor Address hi/lo
 * Everything else (H Total, Sync Position/Width, V Total, V Total Adjust,
 * Interlace Mode, R9 Max Scanline, R11 Cursor End) is stored and reads back
 * correctly but isn't acted on yet; R16/R17 Light Pen are read-only and always 0
 * (no light pen to model). Scanline-accurate cursor shape/height and real
 * character-cell sizing from Max Scanline are natural follow-ups once something
 * actually needs them.
 *
 * No vertical-sync interrupt is modeled - raiseInterrupt() is a no-op. A real CRTC
 * continuously rescans its RAM from its own clock, independent of when the CPU last
 * wrote to it, so rendering here is driven by a periodic Swing Timer once show() is
 * called (started there, not in the constructor, same as ConsoleDevice.show()) -
 * not by individual register or character-RAM writes, since this controller never
 * even sees the latter (they go straight to the separate RAM device). */
public class VideoController implements Device {
	private static final int REG_ADDRESS = 0;
	private static final int REG_DATA = 1;
	private static final int REG_KEYBOARD_DATA = 2;
	private static final int REG_KEYBOARD_STATUS = 3;
	private static final int KEYBOARD_STATUS_DATA_AVAILABLE = 0x01;

	private static final int R_H_DISPLAYED = 1;
	private static final int R_V_DISPLAYED = 6;
	private static final int R_CURSOR_START = 10;
	private static final int R_START_ADDR_HI = 12;
	private static final int R_START_ADDR_LO = 13;
	private static final int R_CURSOR_ADDR_HI = 14;
	private static final int R_CURSOR_ADDR_LO = 15;
	private static final int REGISTER_COUNT = 18;

	/** Real 6845 Cursor Start bit 5-6 field value meaning "cursor off entirely" -
	 * used here as the whole register's value for simplicity, rather than modeling
	 * every unused bit position. */
	private static final int CURSOR_MODE_OFF = 0x20;

	private final Logger logger = LogManager.getLogger(VideoController.class);

	private final AddressRange range;
	private final Device charRam;
	private final int charRamBase;
	private final int charRamSize;

	private final byte[] registers = new byte[REGISTER_COUNT];
	private int addressRegister = 0;
	private final Queue<Integer> input = new ConcurrentLinkedQueue<Integer>();

	private JFrame frame;
	private Screen screen;
	private Timer refreshTimer;

	public VideoController(AddressRange range, Device charRam) {
		this.range = range;
		this.charRam = charRam;
		this.charRamBase = charRam.getAddressRange().baseAddress();
		this.charRamSize = charRam.getAddressRange().size();
		reset();

		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		try {
			screen = new Screen();
			frame = new JFrame("Video");
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.add(screen);
			frame.pack();
		} catch (Throwable t) {
			logger.error("Unable to create video window, running headless", t);
			frame = null;
			screen = null;
		}
	}

	/** Makes the video window visible, gives it keyboard focus, and starts the
	 * periodic repaint that stands in for a real CRTC's continuous raster scan.
	 * Not called automatically - same "only an interactive entry point opts in"
	 * convention as ConsoleDevice.show(). */
	public void show() {
		if (frame == null) {
			return;
		}
		frame.setVisible(true);
		screen.requestFocusInWindow();
		if (refreshTimer == null) {
			refreshTimer = new Timer(33, e -> screen.repaint());
			refreshTimer.start();
		}
	}

	/** Feeds characters into the keyboard queue exactly as real keystrokes would -
	 * for tests to drive firmware that reads from it, since there's no window to
	 * type into under a headless run. Same role as ConsoleDevice.type(). */
	public void type(String text) {
		for (int i = 0; i < text.length(); i++) {
			handleKeyPress(text.charAt(i));
		}
	}

	private void handleKeyPress(char c) {
		input.add((int) c);
	}

	@Override
	public DEVTYPE getDeviceType() {
		return DEVTYPE.CHAR;
	}

	@Override
	public BussId getBusId() {
		return BussId.DEVICE;
	}

	@Override
	public AddressRange getAddressRange() {
		return range;
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		int local = range.addressMapper(address);
		if (local == REG_ADDRESS) {
			addressRegister = data & 0xff;
		} else if (local == REG_DATA) {
			if (addressRegister < REGISTER_COUNT) {
				registers[addressRegister] = data;
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("video register R%d = 0x%02X", addressRegister, data & 0xff));
				}
			}
		}
		// Keyboard Data/Status are read-only from the CPU's side - a write to
		// either is simply ignored, same as writing CONSOLE_DATA's read path would
		// be meaningless for ConsoleDevice.
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		int local = range.addressMapper(address);
		if (local == REG_ADDRESS) {
			return (byte) addressRegister;
		}
		if (local == REG_KEYBOARD_DATA) {
			Integer key = input.poll();
			return key == null ? 0 : (byte) (key.intValue() & 0xff);
		}
		if (local == REG_KEYBOARD_STATUS) {
			return input.isEmpty() ? 0 : (byte) KEYBOARD_STATUS_DATA_AVAILABLE;
		}
		if (addressRegister < REGISTER_COUNT) {
			return registers[addressRegister];
		}
		return 0;
	}

	@Override
	public void raiseInterrupt() {
		// No vertical-sync interrupt modeled - see the class comment.
	}

	@Override
	public void reset() {
		addressRegister = 0;
		for (int i = 0; i < REGISTER_COUNT; i++) {
			registers[i] = 0;
		}
		// Cursor Start defaults to "off" rather than 0 (which real hardware would
		// treat as "on, non-blinking, start scanline 0") - nothing initializes this
		// controller's registers at power-on yet (no BIOS_VIDEO exists), so without
		// this a cursor bar would appear at R14/R15's own default of (0,0) before
		// any software ever asked for one.
		registers[R_CURSOR_START] = (byte) CURSOR_MODE_OFF;
	}

	private int unsigned(int index) {
		return registers[index] & 0xff;
	}

	private int horizontalDisplayed() {
		return unsigned(R_H_DISPLAYED);
	}

	private int verticalDisplayed() {
		return unsigned(R_V_DISPLAYED);
	}

	private int displayStartAddress() {
		return (unsigned(R_START_ADDR_HI) << 8) | unsigned(R_START_ADDR_LO);
	}

	private int cursorAddress() {
		return (unsigned(R_CURSOR_ADDR_HI) << 8) | unsigned(R_CURSOR_ADDR_LO);
	}

	private boolean cursorEnabled() {
		return unsigned(R_CURSOR_START) != CURSOR_MODE_OFF;
	}

	/** The character at (row, col) of the currently visible window - lets tests
	 * verify rendering content without a real display, same role as
	 * ConsoleDevice.getRow(). Wraps around charRamSize, matching a real CRTC
	 * scanning a character buffer that's bigger than one screen's worth so
	 * Display Start Address can scroll without the CPU copying anything. */
	public char getChar(int row, int col) throws illegalAddressException, DeviceUnavailable {
		int offset = (displayStartAddress() + row * horizontalDisplayed() + col) % charRamSize;
		return (char) (charRam.read(charRamBase + offset) & 0xff);
	}

	/** The whole of row's currently visible columns, same role and signature as
	 * ConsoleDevice.getRow() so tests driving VideoController in its place can
	 * keep the same assertion style (.contains(), .startsWith(), etc.). */
	public String getRow(int row) throws illegalAddressException, DeviceUnavailable {
		int cols = horizontalDisplayed();
		StringBuilder sb = new StringBuilder(cols);
		for (int col = 0; col < cols; col++) {
			sb.append(getChar(row, col));
		}
		return sb.toString();
	}

	private class Screen extends JPanel {
		private static final long serialVersionUID = 1L;
		// java.awt.Color has no GOLD constant (that's a JavaFX/CSS name, not AWT) -
		// this is the standard web "Gold" RGB value (255, 215, 0).
		private static final Color GOLD = new Color(255, 215, 0);
		private final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 16);
		private final int charWidth;
		private final int charHeight;

		Screen() {
			BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			FontMetrics fm = probe.getGraphics().getFontMetrics(font);
			charWidth = fm.charWidth('M');
			charHeight = fm.getHeight();

			setFont(font);
			setBackground(Color.BLACK);
			setForeground(GOLD);
			setPreferredSize(new Dimension(charWidth * 80, charHeight * 24));
			setFocusable(true);
			addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(KeyEvent e) {
					handleKeyPress(e.getKeyChar());
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			int cols = horizontalDisplayed();
			int rows = verticalDisplayed();
			if (cols == 0 || rows == 0) {
				return;
			}
			setPreferredSize(new Dimension(charWidth * cols, charHeight * rows));
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					char c;
					try {
						c = getChar(row, col);
					} catch (illegalAddressException | DeviceUnavailable e) {
						c = ' ';
					}
					g.drawChars(new char[] { c }, 0, 1, col * charWidth, (row + 1) * charHeight);
				}
			}
			if (cursorEnabled()) {
				int offset = (cursorAddress() - displayStartAddress() + charRamSize) % charRamSize;
				int cursorRow = offset / cols;
				int cursorCol = offset % cols;
				if (cursorRow < rows) {
					// +1 here for the same reason the text loop above draws at
					// (row + 1) * charHeight - drawChars/fillRect position from the
					// top, but a row's glyphs are drawn at their baseline, (row + 1)
					// rows down. Using plain cursorRow (no +1) here was the actual
					// bug: it drew the cursor a full row above the text it should
					// sit under, not a register-update problem.
					g.fillRect(cursorCol * charWidth, (cursorRow + 1) * charHeight + 2, charWidth, 2);
				}
			}
		}
	}
}
