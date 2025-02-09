
package com.mj.Registers;

import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

// This is a 
public class gpregister implements generalPurpose {
    int value;
    char name;

    // Constructor to initialize the register's value and name
    public gpregister(char name, int initialValue) {
        this.name = name;
        set(initialValue);
    }

    @Override
    public int get() {
        return (value & 0xff);
    }

    @Override
    public void set(int value) throws zflagException, nflagException {
        this.value = value & 0xff;
        if (this.value == 0) {
            throw new zflagException();
        }
        if ((0x80 & this.value) != 0) {
            throw new nflagException();
        }
    }

    @Override
    public void inc() throws zflagException, nflagException {
        value++;
        value &= 0xff;

        if (value == 0) {
            throw new zflagException();
        } 
        if ((value & 0x80) != 0) {
            throw new nflagException();
        }
    }

    @Override
    public void dec() throws zflagException, nflagException {
        value--;
        value &= 0xff;

        if (value == 0) {
            throw new zflagException();
        } 
        if ((value & 0x80) != 0) {
            throw new nflagException();
        }
    }

    @Override
    public char name() {
        return name;
    }

    @Override
    public void reset() {
        value = 0;
        // Resetting the name might not be necessary, but it's up to your design
        // this.name = 'R'; // Example: resetting to a default name
    }
}


