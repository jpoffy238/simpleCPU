package Registers;

import exceptions.nflagException;
import exceptions.zflagException;

public class gpregister implements generalPurpose {
	int value;
	char name;
	public int get() {
		// TODO Auto-generated method stub
		
		return (value & 0xff);
	}

	public void set(int  value) throws zflagException, nflagException {
		// TODO Auto-generated method stub
		
		this.value = value & 0xff;
		if (0 == this.value ) {
			throw new zflagException();
		}
		if ( (0x80 & this.value )!= 0 ) {
			throw new nflagException();
		}
	
	}

	public void inc() throws zflagException, nflagException { 
		// TODO Auto-generated method stub
	value++;
	if (value == 0) {
		throw new zflagException();
	} 
	
	if ( value < 0 ) {
		throw new nflagException();
	}
		
	}

	public void dec() throws nflagException, zflagException  {
		
		value--;
		value&=0x00ff;
		
		if (value == 0) {
			throw new zflagException();
		} 
		if ( value < 0 ) {
			throw new nflagException();
		}
		
		}
	


	public char name() {
		// TODO Auto-generated method stub
		return name;
	}

	public void reset() {
		// TODO Auto-generated method stub
		value = 0;
		
	}

}
