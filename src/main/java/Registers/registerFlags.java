package Registers;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public enum registerFlags {
		CFLAG  (0x01, 'C'),
		ZFLAG	(0x01<<1, 'Z'),
		IDFLAG (0x01<<2, 'I'),
		DCFLAG (0x01<<3, 'D'),
		BFLAG (0x01<<4, 'B'),
		OVFLAG  (0x01<<5, 'O'),
		NFLAG	 (0x01<<6, 'N'),
		EFLAG(0x01<<7, 'E');
	
		private final byte  mask;
		private ReadWriteLock lock = new ReentrantReadWriteLock();
		private static  byte flags = 0;
		public  final char flagName;
		private registerFlags(int mask,char flagName) {
			this.mask=(byte)mask;
			this.flagName=flagName;
		}
		public  final boolean isSet() {
			lock.readLock().lock();
			try {
			return (flags&mask) != 0;
			} finally {
				lock.readLock().unlock();
			}
		}
		public final void  set() {
			lock.writeLock().lock();
			try { 
			flags|=this.mask;
			} finally {
				lock.writeLock().unlock();
			}
		}
		public final static void set(List<registerFlags> flagList) {
			for (registerFlags i : flagList) {
				i.set();
			}
		}
		public final void clear() {
			lock.writeLock().lock();
			try { 
			flags^=this.mask;
			} finally {
				lock.writeLock().unlock();
			}
		}
		public final static void clear(List<registerFlags> flagList) {
			for (registerFlags i : flagList) {
				i.clear();
			}
			
		}
		public final static void reset() {
			flags = 0;
		}
		public final  String  dump() {
			
			
			return String.format("%8s", Integer.toBinaryString(flags)).replace(" ", "0");
		}
		
}
