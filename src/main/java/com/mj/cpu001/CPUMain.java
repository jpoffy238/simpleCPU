package com.mj.cpu001;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.mj.Devices.Device;
import com.mj.Devices.FileDevice;
import com.mj.MachineState.cpu001decoder;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.MemoryDriver;
import com.mj.memoryInterface.basicMemory;

public class CPUMain {

	public static void main(String[] args) {
		
		try {
			InetAddress inetAddr = InetAddress.getByName("73.44.90.146");
			String hostname = inetAddr.getHostName();
			String canonicalHostname = inetAddr.getCanonicalHostName();
			System.out.println ( "My host name : " + hostname + ":" + canonicalHostname );
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

				MemoryDriver mem = new basicMemory();
			mem.setIOPage(0xfe00);
			Device fd = new FileDevice();
			mem.registerDevice(0xfe00, fd);
			try {
				mem.write(0xfffc, (byte)0x00);
				mem.write(0xfffd, (byte)0x10);
				mem.write(0xfffe,(byte)0x00);
				mem.write(0xffff,(byte)0x10);
				mem.write(0xfffa,(byte)0x00);
				mem.write(0xfffb, (byte)0x10);
			} catch (illegalAddressException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DeviceUnavailable e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		CPU c = new CPU( mem, new cpu001decoder());
		
		c.start();
		}
	
	
		
	}


