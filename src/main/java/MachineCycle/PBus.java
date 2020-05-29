package MachineCycle;

public interface PBus {
	 enum BussId  { MEMROY, REGISTER, DEVICE, COMPUTATION, NOP }
	 enum IO {READ_IO, WRITE_IO }
	
	
	public  void access(BussId srcBus, BussId destBus,  int srcAddress , int destAddress );
	
}
