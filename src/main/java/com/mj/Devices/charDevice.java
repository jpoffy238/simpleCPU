package com.mj.Devices;

import com.mj.exceptions.DeviceUnavailable;

public interface charDevice extends Device {
	
	public int status() throws DeviceUnavailable;
	public void control (int data) throws DeviceUnavailable;

}
