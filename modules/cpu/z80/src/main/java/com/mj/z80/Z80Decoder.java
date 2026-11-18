package com.mj.z80;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.illegalOpCodeException;

/** Resolves whatever instruction PC currently points at, pulling as many
 * bytes from cpu as it needs (1-4, depending on the prefix cascade) to get
 * there - see Z80InstructionDecoder for why this can't be a pure, one-byte-
 * in function the way the 6502 side's Decoder.decode(byte) is: a $CB/$ED/
 * $DD/$FD prefix byte means "there's at least one more byte before this is
 * even identifiable," and decode() needs the ability to fetch that itself. */
public interface Z80Decoder {
	Z80Instruction decode(Z80CPU cpu) throws illegalOpCodeException, illegalAddressException, DeviceUnavailable;
}
