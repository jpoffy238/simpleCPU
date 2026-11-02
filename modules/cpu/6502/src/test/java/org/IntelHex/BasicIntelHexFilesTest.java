package org.IntelHex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexRecord;
import com.mj.IntelHex.common.IntelHexRecordType;

class BasicIntelHexFilesTest {

	/** Reads a program assembled from src/main/asm (found on the test classpath) and checks its structure. */
	@Test
	void readsAnAssembledProgram() throws Exception {
		ArrayList<IntelHexRecord> records = new BasicIntelHexFiles().read("ADC_Test.hex");

		assertFalse(records.isEmpty(), "no records read");
		IntelHexRecord first = records.get(0);
		assertEquals(IntelHexRecordType.DATA, first.getRecordType());
		assertEquals(0x1000, first.getAddress(), "ADC_Test.asm is assembled at $1000");

		IntelHexRecord last = records.get(records.size() - 1);
		assertEquals(IntelHexRecordType.EOF, last.getRecordType(), "the file must end with exactly one EOF record");
		long eofCount = records.stream().filter(r -> r.getRecordType() == IntelHexRecordType.EOF).count();
		assertEquals(1, eofCount);

		int bytes = records.stream().filter(r -> r.getRecordType() == IntelHexRecordType.DATA)
				.mapToInt(r -> r.getData().length).sum();
		assertTrue(bytes > 100, "expected the whole program, got " + bytes + " bytes");
	}

	@Test
	void missingFileIsReportedNotSwallowed() {
		org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
				() -> new BasicIntelHexFiles().read("no_such_program.hex"));
	}
}
