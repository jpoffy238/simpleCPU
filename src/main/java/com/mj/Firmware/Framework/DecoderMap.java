package com.mj.Firmware.Framework;

import java.util.HashMap;

import java.util.Map;

public class DecoderMap {
 private static  Map<Integer, machineState> decoder = new HashMap<Integer, machineState>();
 
 private static machineState[] decoderB = new machineState[256];
 
 private static boolean init = false;

 public static machineState[] getMap() {
	 if (init == false) {
		 for (int i = 0; i <256; i++) {
			 decoderB[i] = null;
		 }
		 init=true;
	 }
	 return decoderB;
 }
}
