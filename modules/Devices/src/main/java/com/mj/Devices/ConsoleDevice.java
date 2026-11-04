package com.mj.Devices;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class ConsoleDevice implements charDevice {
	// Console Device usage:
	// Console device uses 2 bytes of memory address space:
	// o The even byte (LSb=0) is used to read from OR write to
	// the console device. The read operation can be set to
	// - Block until a key is pressed
	// - NoBlock returns a Null 0x00 character there is
	// no input available
	// - Raise Interrupt.
	// The odd memory location (LSb=1) is the control/status
	// for the console device. Bit definition:
	// Read/Write Operations:
	// Bit 0: Data Available Bit
	// - If 1 then data is available and can be read
	// - If 0 then data is not available
	// Bit 1: Mode Bit
	// - If 1 then Data read will block
	// - If 0 then Data read will not block
	// Bit 2: Interrupt Mode
	// - If 1 then device will raise the interrupt.
	// - If 0 then device will not raise interrupt when data is available.
	// Bit 3: Unused
	// Bit 4-7: (Upper nibble) Number of Characters Available for reading (0-15)
	//
	//
	private static final int STATUS_DATA_AVAILABLE = 0x01;
	private static final int STATUS_MODE_BLOCK = 0x02;
	private static final int STATUS_INTERRUPT_MODE = 0x04;
	private static final int COLUMNS = 80;
	private static final int ROWS = 24;

	final Logger logger = LogManager.getLogger(ConsoleDevice.class);
	Queue<Integer> input = new ConcurrentLinkedQueue<Integer>();
	volatile int status = 0;
	VIA6522 via;
	AddressRange range;

	private final Object gridLock = new Object();
	private final char[][] grid = new char[ROWS][COLUMNS];
	private int cursorRow = 0;
	private int cursorCol = 0;

	private JFrame frame;
	private Screen screen;

	public ConsoleDevice(VIA6522 via, AddressRange range) {
		this.via = via;
		this.range = range;
		for (char[] row : grid) {
			Arrays.fill(row, ' ');
		}

		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		try {
			screen = new Screen();
			frame = new JFrame("Console");
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.add(screen);
			frame.pack();
		} catch (Throwable t) {
			logger.error("Unable to create console window, running headless", t);
			frame = null;
			screen = null;
		}
	}

	/** What's currently on screen at the given row - lets tests verify the effect of a
	 * write() without needing an actual display. */
	public String getRow(int row) {
		synchronized (gridLock) {
			return new String(grid[row]);
		}
	}

	/** Makes the console window visible. Not called automatically - CPU_CreateUtil's
	 * test fixture constructs a ConsoleDevice per test and must never pop up a window;
	 * only an interactive entry point (CPUMain) should call this. */
	public void show() {
		if (frame != null) {
			frame.setVisible(true);
			screen.requestFocusInWindow();
		}
	}

	/** Feeds characters into the input queue exactly as real keystrokes would - for
	 * tests to drive firmware that reads from the console, since there's no window
	 * to type into under a headless run. */
	public void type(String text) {
		for (int i = 0; i < text.length(); i++) {
			handleKeyPress(text.charAt(i));
		}
	}

	private void handleKeyPress(char c) {
		input.add((int) c);
		if ((status & STATUS_INTERRUPT_MODE) != 0) {
			raiseInterrupt();
		}
	}

	public void write(int data) throws DeviceUnavailable {
		char c = (char) (data & 0xff);
		synchronized (gridLock) {
			if (c == '\r') {
				cursorCol = 0;
			} else if (c == '\n') {
				newline();
			} else if (c == '\b') {
				// Pure cursor-left; firmware erases visually by following this with a
				// space (overwrite) and a second backspace (reposition) - same as any
				// real dumb terminal.
				if (cursorCol > 0) {
					cursorCol--;
				}
			} else {
				grid[cursorRow][cursorCol] = c;
				cursorCol++;
				if (cursorCol >= COLUMNS) {
					cursorCol = 0;
					newline();
				}
			}
		}
		if (screen != null) {
			SwingUtilities.invokeLater(screen::repaint);
		}
	}

	private void newline() {
		cursorRow++;
		cursorCol = 0;
		if (cursorRow >= ROWS) {
			for (int r = 1; r < ROWS; r++) {
				grid[r - 1] = grid[r];
			}
			grid[ROWS - 1] = new char[COLUMNS];
			Arrays.fill(grid[ROWS - 1], ' ');
			cursorRow = ROWS - 1;
		}
	}

	public void status(int data) throws DeviceUnavailable {
		status = data;
		logger.debug("STatus control Word = " + data);
	}

	public DEVTYPE getDeviceType() {
		return DEVTYPE.CHAR;
	}

	public BussId getBusId() {
		return BussId.DEVICE;
	}

	public AddressRange getAddressRange() {
		return range;
	}

	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		if ((address & 0x0001) == 0) {
			write(data);
		} else {
			logger.debug("Control   : " + data);
			control(data);
		}

	}

	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		byte returnValue = 0;
		if ((address & 0x0001) == 0) {
			returnValue = (byte) read(); // get character from console
		} else {

			returnValue = (byte) status(); // get status flags of the console
		}
		return returnValue;
	}

	public void raiseInterrupt() {
		via.requestInterrupt(VIA6522.FLAG_CA1);
	}

	@Override
	public int read() throws DeviceUnavailable {
		if (input.isEmpty()) {
			return 0;
		} else {
			Integer i = input.remove();
			return (byte) (i.byteValue() & 0x00ff);
		}

	}

	@Override
	public int status() throws DeviceUnavailable {
		// Bit 0 and bits 4-7 reflect live hardware state (is there input queued, and how
		// much) - they're computed here, not stored, so they can't go stale. Bits 1-2
		// (blocking mode, interrupt mode) are genuinely CPU-controlled, set via control().
		int computed = status & (STATUS_MODE_BLOCK | STATUS_INTERRUPT_MODE);
		int queued = input.size();
		if (queued > 0) {
			computed |= STATUS_DATA_AVAILABLE;
			computed |= Math.min(queued, 15) << 4;
		}
		return (byte) computed;
	}

	@Override
	public void control(int data) throws DeviceUnavailable {
		status = (byte) (data & 0x00ff);

	}

	@Override
	public void reset() {

	}

	/** The actual text grid. Key events feed `input` directly - no polling thread
	 * needed, since Swing delivers keystrokes as callbacks. */
	private class Screen extends JPanel {
		private static final long serialVersionUID = 1L;
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
			setForeground(Color.LIGHT_GRAY);
			setPreferredSize(new Dimension(charWidth * COLUMNS, charHeight * ROWS));
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
			synchronized (gridLock) {
				for (int row = 0; row < ROWS; row++) {
					g.drawChars(grid[row], 0, COLUMNS, 0, (row + 1) * charHeight);
				}
			}
		}
	}

}
