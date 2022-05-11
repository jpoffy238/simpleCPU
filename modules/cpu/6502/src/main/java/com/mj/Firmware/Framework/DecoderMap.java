package com.mj.Firmware.Framework;

import java.util.HashMap;
import java.util.Map;

public class DecoderMap {
 private static  Map<Integer, machineState> decoder = new HashMap<Integer, machineState>();
 public static Map<Integer, machineState> getMap() {
	 return decoder;
 }
}
