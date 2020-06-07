package com.mj.Registers;

import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public interface generalPurpose {
	public  int get();
	public void set (int  value) throws zflagException, nflagException ;
	public void inc()  throws  nflagException,  zflagException;
	public void dec()  throws zflagException, nflagException;
	public char name();
	public void reset() ;
}
