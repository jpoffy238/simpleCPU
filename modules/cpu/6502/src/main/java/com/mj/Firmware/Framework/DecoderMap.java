package com.mj.Firmware.Framework;


public class DecoderMap {
 private static   machineState[] decoder = new machineState[256];
 public static machineState[] getMap() {
	 return decoder;
 }
}
