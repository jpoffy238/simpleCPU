package MachineState;

import java.util.HashMap;
import java.util.Map;

import Firmeware.Framework.machineState;

public class DecoderMap {
 private static  Map<Integer, machineState> decoder = new HashMap<Integer, machineState>();
 public static Map<Integer, machineState> getMap() {
	 return decoder;
 }
}
