package org.IntelHex;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.mj.IntelHex.BasicIntelHexFiles;
import com.mj.IntelHex.common.IntelHexFileChecksumMisMatchException;
import com.mj.IntelHex.common.IntelHexRecord;

class BasicIntelHexFilesTest {

	@Test
	void test() {
		ArrayList<IntelHexRecord > records = null;
		BasicIntelHexFiles hexfile = new BasicIntelHexFiles();
				try {
					try {
						records = hexfile.read("/home/jpoffen/git/simpleCPU/src/main/asm/ADC_Test.hex") ;
					} catch (IntelHexFileChecksumMisMatchException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					assert(true);
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					fail("Error");
				}
		for(IntelHexRecord r: records) {
			System.out.println(r.toString());
		}

}
}
