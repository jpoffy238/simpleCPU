package Registers;

import exceptions.cflagException;
import exceptions.oflagException;

public interface generalPurpose {
	public  int get();
	public void set (int  value);
	public void inc() throws cflagException;
	public void dec() throws oflagException;
	public char name();
	public void reset();
}
