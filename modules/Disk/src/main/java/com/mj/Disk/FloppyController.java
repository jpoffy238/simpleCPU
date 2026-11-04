package com.mj.Disk;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mj.Devices.AddressRange;
import com.mj.Devices.Device;
import com.mj.Devices.PBus.BussId;
import com.mj.Devices.PBus.DEVTYPE;
import com.mj.Devices.VIA6522;
import com.mj.Disk.exceptions.DiskReadError;
import com.mj.Disk.exceptions.DiskWriteError;
import com.mj.Disk.exceptions.GeometryException;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** A WD179x-style floppy disk controller, trimmed to Restore/Seek/Read Sector/Write
 * Sector: real register layout and interrupt-per-byte transfer, but only the
 * 128-byte sector payload moves - no gap/sync/CRC emulation.
 *
 * Registers, relative to the device's base address:
 *   +0  Status (read) / Command (write)
 *   +1  Track
 *   +2  Sector
 *   +3  Data
 *   +4  Drive Select (0-3)
 */
public class FloppyController implements Device {
	public static final int REG_STATUS_COMMAND = 0;
	public static final int REG_TRACK = 1;
	public static final int REG_SECTOR = 2;
	public static final int REG_DATA = 3;
	public static final int REG_DRIVE_SELECT = 4;

	private static final int STATUS_BUSY = 0x01;
	private static final int STATUS_DRQ = 0x02;
	private static final int STATUS_RECORD_NOT_FOUND = 0x04;
	private static final int STATUS_NOT_READY = 0x80;

	private static final int MAX_DRIVES = 4;

	private enum Transfer {
		NONE, READ, WRITE
	}

	private static final Logger logger = LogManager.getLogger(FloppyController.class);

	private final VIA6522 via;
	private final AddressRange range;
	private final FloppyDevice[] drives = new FloppyDevice[MAX_DRIVES];

	private int selectedDrive = 0;
	private int status = 0;
	private int trackRegister = 0;
	private int sectorRegister = 1;
	private int dataRegister = 0;

	private Transfer transfer = Transfer.NONE;
	private byte[] buffer;
	private int bufferIndex;

	public FloppyController(VIA6522 via, AddressRange range) {
		this.via = via;
		this.range = range;
	}

	public void attachDrive(int driveNumber, FloppyDevice drive) {
		drives[driveNumber] = drive;
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

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException, DeviceUnavailable {
		int reg = range.addressMapper(address);
		int value = data & 0xff;
		switch (reg) {
		case REG_STATUS_COMMAND:
			issueCommand(value);
			break;
		case REG_TRACK:
			trackRegister = value;
			break;
		case REG_SECTOR:
			sectorRegister = value;
			break;
		case REG_DATA:
			dataRegister = value;
			writeData(value);
			break;
		case REG_DRIVE_SELECT:
			selectedDrive = value % MAX_DRIVES;
			break;
		default:
			throw new illegalAddressException(address, value);
		}
	}

	@Override
	public byte read(int address) throws illegalAddressException, DeviceUnavailable {
		int reg = range.addressMapper(address);
		switch (reg) {
		case REG_STATUS_COMMAND:
			return (byte) status;
		case REG_TRACK:
			return (byte) trackRegister;
		case REG_SECTOR:
			return (byte) sectorRegister;
		case REG_DATA:
			return (byte) readData();
		case REG_DRIVE_SELECT:
			return (byte) selectedDrive;
		default:
			throw new illegalAddressException(address, 0);
		}
	}

	private FloppyDevice currentDrive() {
		return drives[selectedDrive];
	}

	private void issueCommand(int command) {
		transfer = Transfer.NONE;
		FloppyDevice drive = currentDrive();
		if (drive == null) {
			status = STATUS_NOT_READY;
			return;
		}

		int type = command & 0xf0;
		switch (type) {
		case 0x00: // Restore: seek to track 0
			seekTo(drive, 0);
			break;
		case 0x10: // Seek: target track was staged in the Data register
			seekTo(drive, dataRegister);
			break;
		case 0x80: // Read Sector
			beginRead(drive);
			break;
		case 0xa0: // Write Sector
			beginWrite();
			break;
		case 0xf0: // Format Track: fill every sector on the current track with the byte
			// staged in the Data register (real WD179x Write Track, simplified - no
			// gap/sync/CRC bytes, one completion interrupt instead of a per-byte DRQ dance)
			formatTrack(drive);
			break;
		default:
			logger.debug(String.format("Unsupported floppy command %02x", command));
			status = STATUS_RECORD_NOT_FOUND;
			via.requestInterrupt(VIA6522.FLAG_CB1);
			break;
		}
	}

	private void seekTo(FloppyDevice drive, int targetTrack) {
		try {
			drive.setTrack(targetTrack);
			trackRegister = targetTrack;
			status = 0;
		} catch (GeometryException e) {
			status = STATUS_RECORD_NOT_FOUND;
		}
		via.requestInterrupt(VIA6522.FLAG_CB1);
	}

	private void formatTrack(FloppyDevice drive) {
		try {
			drive.setTrack(trackRegister);
			byte[] fillBuffer = new byte[drive.getGeometry().getSectorSize()];
			Arrays.fill(fillBuffer, (byte) dataRegister);
			int sectorCount = drive.getGeometry().getSectors();
			for (int sector = 1; sector <= sectorCount; sector++) {
				drive.setSector(sector);
				drive.write(fillBuffer);
			}
			status = 0;
		} catch (GeometryException | DiskWriteError e) {
			status = STATUS_RECORD_NOT_FOUND;
		}
		via.requestInterrupt(VIA6522.FLAG_CB1); // single completion interrupt, Type-I style
	}

	private void beginRead(FloppyDevice drive) {
		try {
			drive.setTrack(trackRegister);
			drive.setSector(sectorRegister);
			buffer = drive.read();
			bufferIndex = 0;
			transfer = Transfer.READ;
			status = STATUS_BUSY | STATUS_DRQ;
			via.requestInterrupt(VIA6522.FLAG_CB1); // first byte is ready
		} catch (GeometryException | DiskReadError e) {
			status = STATUS_RECORD_NOT_FOUND;
			via.requestInterrupt(VIA6522.FLAG_CB1);
		}
	}

	private void beginWrite() {
		buffer = new byte[currentDrive().getGeometry().getSectorSize()];
		bufferIndex = 0;
		transfer = Transfer.WRITE;
		status = STATUS_BUSY | STATUS_DRQ;
		// no interrupt here: firmware already knows to start writing once BUSY is set
	}

	private int readData() {
		if (transfer != Transfer.READ || bufferIndex >= buffer.length) {
			return 0;
		}
		int value = buffer[bufferIndex++] & 0xff;
		if (bufferIndex >= buffer.length) {
			status &= ~(STATUS_BUSY | STATUS_DRQ);
			transfer = Transfer.NONE;
		} else {
			via.requestInterrupt(VIA6522.FLAG_CB1); // next byte is ready
		}
		return value;
	}

	private void writeData(int value) {
		if (transfer != Transfer.WRITE || bufferIndex >= buffer.length) {
			return;
		}
		buffer[bufferIndex++] = (byte) value;
		if (bufferIndex >= buffer.length) {
			try {
				FloppyDevice drive = currentDrive();
				drive.setTrack(trackRegister);
				drive.setSector(sectorRegister);
				drive.write(buffer);
				status &= ~(STATUS_BUSY | STATUS_DRQ);
			} catch (GeometryException | DiskWriteError e) {
				status = STATUS_RECORD_NOT_FOUND;
			}
			transfer = Transfer.NONE;
		} else {
			via.requestInterrupt(VIA6522.FLAG_CB1); // ready for next byte
		}
	}

	public void raiseInterrupt() {
		via.requestInterrupt(VIA6522.FLAG_CB1);
	}

	public void reset() {
		selectedDrive = 0;
		status = 0;
		trackRegister = 0;
		sectorRegister = 1;
		dataRegister = 0;
		transfer = Transfer.NONE;
	}
}
