package Firmeware;

import cpu001.CPU;

public interface machineState {
		public void exeute(CPU c);
		public void setFlags(CPU u);
}

/*  Machine States
 * State      Operation      Description
 * 0             Fetch             Fetch byte from IP register
 * 1             Decode         -------------------------
 * 2             Execute        ---------------------
 * 3 
 */
/*  Exmple Load A from [IP]
 * 0             Fetch  Operand (0x01)     
 *  1            DECODE      
 *  2            Read memory [IP] Write to A
 *  3            set flag (if A is 0 then set ZFLAG)
 *  4            increment PC
 *  
 *  Type of States   Memory Access /READ/WRITE R by a REGISTER
 *                             
 *                              Operation between two Registers 
 *                              DECODE
 *                              SET FLAGS
 *                              UPDTE PC
 *                              
 *           
 */
