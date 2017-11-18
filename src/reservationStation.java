/****************************************************************
 * Rodger Byrd
 * Dynamic Scheduling Program, part 4
 * 11/7/2017
 * reservation station definition
 */
public class reservationStation {
	public int opcode;
	public long fct;
	//public long jumpLoc;
	public int aPRNum;
	public long aData;
	public boolean aReady;
	public boolean aRequired;
	public boolean aReused;
	public int bPRNum;
	public long bData;
	public boolean bReady;
	public boolean bRequired;
	public boolean bReused;
	public int memIndex;
	public int imm;
	public boolean immRequired;
	public boolean hasExecuted;
	public String instruction;
	public boolean available;

}
