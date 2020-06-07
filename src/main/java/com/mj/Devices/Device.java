package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;

public interface Device {
	public void write(int data) throws DeviceUnavailable;
	public int read()throws DeviceUnavailable;
	public int status() throws DeviceUnavailable;
	public void status (int data) throws DeviceUnavailable;

}
