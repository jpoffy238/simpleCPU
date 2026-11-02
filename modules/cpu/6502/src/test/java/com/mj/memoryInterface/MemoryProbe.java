package com.mj.memoryInterface;

/** Test-only access to the raw backing array of a memory device (same-package access). */
public final class MemoryProbe {
	private MemoryProbe() {
	}

	public static byte[] raw(AbstractMemoryLayer device) {
		return device.memory;
	}
}
