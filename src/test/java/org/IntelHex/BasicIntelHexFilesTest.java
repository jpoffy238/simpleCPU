package org.IntelHex;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;

import org.IntelHex.common.IntelHexRecord;
import org.junit.jupiter.api.Test;

class BasicIntelHexFilesTest {

	@Test
	void test() {
		BasicIntelHexFiles hexfile = new BasicIntelHexFiles();
				try {
					ArrayList<IntelHexRecord > records = hexfile.read("/home/jpoffen/git/simpleCPU/src/main/asm/ADC_Test.hex") ;
					assert(true);
				
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					fail("Error");
				}
		

}
}
