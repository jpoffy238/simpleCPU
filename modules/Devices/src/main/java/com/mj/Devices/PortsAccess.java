package com.mj.Devices;

public interface PortsAccess {
	public void write(byte port, byte data);
	public byte read(byte port);
	public void registerDevice(byte port, Object driver);
	public void unregisterDevice (byte port);
}
