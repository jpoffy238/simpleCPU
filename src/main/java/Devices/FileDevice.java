package Devices;

import exceptions.DeviceUnavailable;

public class FileDevice implements Device {

	public void write(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		System.out.print((char)data);
	}

	public int read() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public int status() throws DeviceUnavailable {
		// TODO Auto-generated method stub
		return 0;
	}

	public void status(byte controlWord) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		System.out.println("STatus control Word = " + controlWord);
	}

	

	public void status(int data) throws DeviceUnavailable {
		// TODO Auto-generated method stub
		System.out.println("STatus control Word = " + data);
	}

}
