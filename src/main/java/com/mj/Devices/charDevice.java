package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;

public interface charDevice extends Device {
	public void write(int data) throws DeviceUnavailable;
	public int read()throws DeviceUnavailable;
	public int status() throws DeviceUnavailable;
	public void control (int data) throws DeviceUnavailable;

}
