package com.mj.memoryInterface;

import com.mj.Devices.AddressRange;
import com.mj.Devices.PBus;
import com.mj.Devices.PBus.IOALLOW;
import com.mj.cpu001.CPU;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

public class basicROM extends AbstractMemoryLayer {

	
	public basicROM(PBus bus, AddressRange memsize, String OptinalFileToLoad, int startAddress) {
		super (bus, memsize, OptinalFileToLoad, startAddress);
		
		

		ioallow = IOALLOW.RO;


	}

	/**
	 * Program the three hardware vectors ($FFFA NMI, $FFFC RESET, $FFFE IRQ).
	 * This is the equivalent of burning them into the ROM image; it is meant for
	 * ROMs that are not loaded from a hex file, and must be called before the
	 * CPU starts. ROMs loaded from a hex file normally carry their own vectors.
	 */
	public void setVectors(int nmi, int reset, int irq) throws illegalAddressException {
		poke(CPU.VECTOR_NMI, nmi);
		poke(CPU.VECTOR_RESET, reset);
		poke(CPU.VECTOR_IRQ, irq);
	}

	/** Program the project-specific bus-fault vector ($FFF8) - see CPU.VECTOR_FAULT. */
	public void setFaultVector(int handler) throws illegalAddressException {
		poke(CPU.VECTOR_FAULT, handler);
	}

	private void poke(int vectorAddress, int target) throws illegalAddressException {
		memory[addressMapper(vectorAddress)] = (byte) (target & 0xff);
		memory[addressMapper(vectorAddress + 1)] = (byte) ((target >> 8) & 0xff);
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
