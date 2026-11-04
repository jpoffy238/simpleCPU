package com.mj.Devices;

import java.io.Console;

public class Ports implements PortsAccess {

	public void write(byte port, byte data) {
		// TODO Auto-generated method stub
		char a = (char) data;
		Console con = System.console();
		con.printf("%c", a);
		}

	public byte read(byte port) {
		// TODO Auto-generated method stub
		return 0;
	}

	public void registerDevice(byte port, Object driver) {
		// TODO Auto-generated method stub

	}

	public void unregisterDevice(byte port) {
		// TODO Auto-generated method stub

	}

}
