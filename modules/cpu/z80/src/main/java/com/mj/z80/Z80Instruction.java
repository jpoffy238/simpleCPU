package com.mj.z80;

import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.ROException;
import com.mj.exceptions.illegalAddressException;

/** One resolved, executable instruction - the Z80 counterpart of the 6502
 * side's machineState, minus its execution-count/time profiling (nothing
 * calls this in a loop yet to make that worth adding - see Z80CPU.step()).
 * A single Z80Instruction instance is shared across every call that
 * dispatches to it (same singleton-table-row convention as the 6502 side),
 * so it must not hold any per-call state of its own; anything that varies
 * per call (e.g. DD-CB/FD-CB's displacement byte, consumed by the decoder
 * before the instruction itself can be identified - see
 * Z80InstructionDecoder) is threaded through Z80CPU instead. */
public interface Z80Instruction {
	void execute(Z80CPU cpu) throws illegalAddressException, DeviceUnavailable, ROException;
}
