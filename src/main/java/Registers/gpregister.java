package Registers;

import exceptions.cflagException;
import exceptions.oflagException;

public class gpregister implements generalPurpose {
	int value;
	char name;
	public int get() {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (value & 0xff);
	}

	public void set(int  value) {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.value = value & 0xff;
	}

	public void inc() throws cflagException {
		// TODO Auto-generated method stub
		if (value > 255) {
			throw new cflagException();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.value++;
	}

	public void dec() throws oflagException {
		// TODO Auto-generated method stub
		if (value == 0) {
			throw new oflagException();
		}
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.value--;
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
