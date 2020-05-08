package Registers;

import exceptions.cflagException;
import exceptions.oflagException;

public class gpregister implements generalPurpose {
	int value;
	char name;
	public int get() {
		// TODO Auto-generated method stub
		
		return (value & 0xff);
	}

	public void set(int  value) {
		// TODO Auto-generated method stub
		
		this.value = value & 0x00ff;
		if (this.value == 0) {
			registerFlags.ZFLAG.set();
		} else {
			registerFlags.ZFLAG.clear();
		} 
		if ((value & 0x40) != 0) {
			registerFlags.NFLAG.set();
		}
	}

	public void inc() { 
		// TODO Auto-generated method stub
	this.value++;
	if (value > 255) {
			registerFlags.CFLAG.set();
			value = 0;
		}		
	}

	public void dec()  {
		// TODO Auto-generated method stub
		if (value == 0) {
			registerFlags.CFLAG.set();
			registerFlags.NFLAG.set();
		}
		this.value--;
		this.value&=0x00ff;
		}
	


	public char name() {
		// TODO Auto-generated method stub
		return name;
	}

	public void reset() {
		// TODO Auto-generated method stub
		value = 0;
		registerFlags.reset();
		registerFlags.ZFLAG.set();
	}

}
