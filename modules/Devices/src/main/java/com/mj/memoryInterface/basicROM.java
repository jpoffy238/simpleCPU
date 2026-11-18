package com.mj.memoryInterface;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** A generic, CPU-agnostic ROM region - just "loaded bytes, write-protected".
 * Any architecture-specific concept (6502's fixed hardware-vector table, for
 * instance) deliberately does NOT live here; see com.mj.cpu001.CPU's
 * setVectors()/setFaultVector() for that, which poke an instance of this
 * class directly via its own public poke(), the same way any other caller
 * needing to write into an otherwise read-only ROM region would. */
public class basicROM extends AbstractMemoryLayer {

	
	public basicROM(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
		
		

		ioallow = IOALLOW.RO;


	}

	/** Pokes directly into the backing array - needed by callers (e.g.
	 * com.mj.cpu001.CPU's setVectors()/setFaultVector()) that must write into
	 * an otherwise read-only ROM region, the real hardware equivalent of
	 * burning bytes in at manufacture time rather than a runtime write()
	 * (which this class otherwise refuses once ioallow is RO). */
	public void poke(int address, byte data) throws illegalAddressException {
		memory[addressMapper(address)] = data;
	}

	@Override
	public void write(int address, byte data) throws illegalAddressException, ROException {
		// TODO Auto-generated method stub
		if (ioallow != IOALLOW.RO) {
		memory[addressMapper(address)] = data;
		}else {
		throw new ROException();
	}
	}

	public void raiseInterrupt() {
		// TODO Auto-generated method stub
		sysbus.raisepowerOnReset();

	}

}
