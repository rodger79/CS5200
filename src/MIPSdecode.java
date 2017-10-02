/****************************************************************
 * Rodger Byrd
 * Program 1
 * 9/24/2017
 * MIPS instruction decoding and implementation
 */

import java.io.*;

public class MIPSdecode {
	
	//Instantiate Instruction Memory
	private static MIPSmemory instructionMemory = new MIPSmemory(0,0);
	//Instantiate Data Memory
	private static MIPSmemory dataMemory = new MIPSmemory(1,9);
	//program counter
	private static long programCounter = 0;
			
	private static String[] strInstructions = { "","","j","","beq","","","","addi","","","","","ori","","lui",
												"","","","","","","","","","","","","","","","","","","","lw",
												"","","","","","","","sw"};
	
	private static String[] strRegisters = {"$0","$at","$v0","$v1","$a0","$a1","$a2","$a3","$t0","$t1","$t2","$t3"};
	
	//Define registers
	private static long[] registers = new long[32];
	
	private static physicalRegister[] physicalRegisters = new physicalRegister[1024];

	public static void main(String[] args) throws IOException  {
		
		//initialize registers
		for (int i=0; i < physicalRegisters.length;i++ ){
			physicalRegisters[i] = new physicalRegister();
			physicalRegisters[i].available = true;
			physicalRegisters[i].data = 0L;
			physicalRegisters[i].valid = false;
		}
		//System.out.println("phys reg size:" + physicalRegisters.length + " next avail: " + nextAvailablePE());

		//read Instructions file		
		readInstructions("MachineInstructions.txt");
		
		//check memory
		instructionMemory.print("IMDump.txt");
		dataMemory.print("DMDumpBefore.txt");
		

	
		int syscall = 0;
		while (syscall == 0){
			
			//use program counter to get next instruction
			long instruction = instructionMemory.load(programCounter);
			
			//increment program counter
			programCounter = programCounter + 4;
			
			//decode instruction
			int opcode = (int)(instruction >> 26);
			long jumpLoc = (instruction &  0x03ffffff) << 2;
			long rs = (instruction >> 21 ) &  0x1f;
			long rt = (instruction >> 16 ) &  0x1f;
			int imm = (int) (instruction & 0xffff);
			long fct = instruction & 0x1f;

			syscall = decodeLong(opcode,jumpLoc,rs,rt,imm, fct);
		
			
		}
		dataMemory.print("DMDumpAfter.txt");
	}
	//parse the opcodes and instruction data
	//takes instruction in binary string format and returns assembly instruction
	public static int decodeLong(int opcode, long jumpLoc, long rs, long rt, int imm, long fct){
			int retval = 0;
			String instruction = "invalid inst";
			switch (opcode) {
				case 0: //syscall
					if (fct == 12){ 
						retval = 1;
						instruction =("Syscall");
					} else
						retval = -1; //error condition
					break;
				case 2: //jump
					instruction = (strInstructions[opcode] + "\t" + String.format("0x%08X", jumpLoc) );
					programCounter = jumpLoc;
					break;	
				case 4: //beq
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rs] + ", " + strRegisters[(int)rt] + ", " + imm);
					if (registers[(int) rt] == registers[(int) rs])
						programCounter = programCounter + fct*4;
					break;	
				case 8: //addi
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + strRegisters[(int)rs] + ", " + imm);
					registers[(int) rt] = registers[(int) rs] + imm;
					break;	
				case 13: //ori
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + strRegisters[(int)rs] + ", " + imm);
					registers[(int) rt] = registers[(int) rs] | imm;
					break;
				case 15: //lui
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm);
					registers[(int) rt] = imm << 16;
					break;	
				case 35: //lw
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm + "(" + strRegisters[(int)rs] + ")");
					registers[(int) rt]  = dataMemory.load(registers[(int) rs]);
					break;	
				case 43: //sw
					instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm + "(" + strRegisters[(int)rs] + ")");
					dataMemory.store(registers[(int) rs], registers[(int) rt]);
					break;	
				default:
					//error condition
					retval= -1;
					break;
			}
			//System.out.println(instruction);
			return retval;
		}
	//find next available register
	public static int nextAvailablePE(){
		int retval = -1;
		for (int i=0; i < physicalRegisters.length;i++ ){
			if (physicalRegisters[i].available == true){
				retval = i;
				break;
			}
		}
		return retval;
	}
	//read in the text file to instruction array
	public static void readInstructions(String filename) throws IOException{

		File file = new File(filename);

        
       try {
    	   InputStream inputStream = new FileInputStream(file);
    	   Reader      inputStreamReader = new InputStreamReader(inputStream);
    	   BufferedReader br = new BufferedReader(inputStreamReader);
    	   
    	   int i = 0;
    	   String strLine;
    	   while ((strLine = br.readLine()) != null) {

    		   String[] parts = strLine.split("\t");
    		   
    		   long key = Long.decode(parts[0]);
    		   long value = Long.decode(parts[1]);	   
    		   instructionMemory.store(key,value);
    		   
    		   if (i == 0)  programCounter = key; //initialize program counter
    		   i++;
    	    }
    	    inputStream.close();
    	   
	   } catch (FileNotFoundException e1) {
		   e1.printStackTrace();
	   }

	}
	
	
}
