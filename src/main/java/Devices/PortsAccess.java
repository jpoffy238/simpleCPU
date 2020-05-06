package Devices;

public interface PortsAccess {
	public void write(byte port);
	public byte read(byte port);
	public void registerDevice(byte port, Object driver);
	public void unregisterDevice (byte port);
}
