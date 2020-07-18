package com.mj.Firmware.Math;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mj.Devices.AddressRange;
import com.mj.Devices.ConsoleDevice;
import com.mj.Devices.DeviceBus;
import com.mj.Devices.PBus;
import com.mj.Firmware.Framework.OpCodes;
import com.mj.Firmware.Framework.cpu001decoder;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;
import com.mj.memoryInterface.basicMemory;
import com.mj.memoryInterface.basicROM;

public class Test_SBC {
	private  CPU c;

	private static final Logger logger = LogManager.getLogger(Test_SBC.class);

	@BeforeEach
	public  void setup() {

		PBus bus = new DeviceBus();
		bus.registerDevice(new basicMemory(bus,
				new AddressRange(0, 32*1024) ,
				null, 0	));
	
	
	bus.registerDevice(new basicROM(bus, 
			new AddressRange(0xff00, 0xffff),
			null, 0));
	bus.registerDevice(new ConsoleDevice(bus));

		c = new CPU(bus, new cpu001decoder());

		logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
	}

	@Test
	public void Test_SBC_Basic01() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0x0a);
			c.bus.write(i++, OpCodes.SBC_IMM.code());
			c.bus.write(i++, (byte) 1);
			
			
			c.bus.write(i++, OpCodes.HLT.code());
		
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == (byte)9);
		assert (!c.ZFLAG.isSet());
		assert(!c.NFLAG.isSet());
		assert(!c.OFLAG.isSet());

	}
	@Test
	public void Test_SBC_Basic02() {
		int i = 0x1000;
		try {
			c.bus.write( i++, OpCodes.LDA_IMM.code());
			c.bus.write(i++, (byte)0x0a);
			c.bus.write(i++, OpCodes.SBC_IMM.code());
			c.bus.write(i++, (byte) 20);
			c.bus.write(i++,  OpCodes.NOP.code());
			
			c.bus.write(i++, OpCodes.HLT.code());
		
		} catch (illegalAddressException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (DeviceUnavailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		} catch (ROException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			assert (false);
		}

		logger.debug("Starting CPU");
		c.run();
		
		int result;
		result = c.a.get();
		logger.debug("What A is loaded with : ", +result);

		assert ((byte)(result & 0xff) == (byte)-10);
		assert (!c.ZFLAG.isSet());
		assert(c.NFLAG.isSet());
		assert(!c.OFLAG.isSet());
		assert(!c.CFLAG.isSet());

	}
	
	
}
