package com.mj.Firmware.logic.CMP;

import com.mj.Firmware.logic.ASL.Test_ASL;
import com.mj.cpu001.CPU;
import com.mj.util.CPU_CreateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;

public class Test_CMP_ABS {
    private CPU c;

    private static final Logger logger = LogManager.getLogger(Test_ASL.class);

    @BeforeEach
    public  void setup() {
        c = CPU_CreateUtil.getCPU();
        logger.debug("In Setup -- Current cpu state: " + CPU.currentThread().getState());
    }
}
