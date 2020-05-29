package Registers;

public class  registerFlags {
		private Boolean state;		
		private String flagName;
		public registerFlags (String name) {
			flagName = name;
			state = false;
		}
		public   boolean isSet() {
			return state;
		}
		public  void  set() {
			state = true;
		}
		public  void clear() {
			state = false;
			}
		public  void reset() {
			state = false;
		}

		public String toString () {
			return state ? new String(flagName).toUpperCase() : new String(flagName).toLowerCase();
		}
		
}
