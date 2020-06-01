package Devices;

import exceptions.DeviceUnavailable;

public interface Device {
	public void write(int data) throws DeviceUnavailable;
	public int read()throws DeviceUnavailable;
	public int status() throws DeviceUnavailable;
	public void status (int data) throws DeviceUnavailable;

}
