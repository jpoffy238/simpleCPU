package Devices;

import exceptions.DeviceUnavailable;

public interface Device {
	public void write(byte data) throws DeviceUnavailable;
	public byte read()throws DeviceUnavailable;
	public byte status() throws DeviceUnavailable;
	public void status (byte controlWord) throws DeviceUnavailable;

}
