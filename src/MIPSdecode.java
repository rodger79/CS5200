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
	
	//Define registers
	private static long[] registers = new long[32];

	public static void main(String[] args) throws IOException  {

		//read Instructions file		
		readInstructions("MachineInstructions.txt");
		
		//check memory
		instructionMemory.print("IMDump.txt");
		dataMemory.print("DMDumpBefore.txt");
		
		//decode instructions remove
	
		int syscall = 0;
		while (syscall == 0){
			
			//use program counter to get next instruction
			long instruction = instructionMemory.load(instructionMemory.instructionPC());
			
			//increment program counter
			instructionMemory.incPC();
			
			//decode instruction
			int opcode = (int)(instruction >> 26);
			long jumpLoc = (instruction &  0x03ffffff) << 2;
			long rs = (instruction >> 21 ) &  0x1f;
			long rt = (instruction >> 16 ) &  0x1f;
			int imm = (int) (instruction & 0xffff);
			long fct = instruction & 0x1f;

			syscall = decodeLong(opcode,jumpLoc,rs,rt,imm, fct);
		
			dataMemory.print("DMDumpAfter.txt");
		}
	}
	//parse the opcodes and instruction data
	//takes instruction in binary string format and returns assembly instruction
	public static int decodeLong(int opcode, long jumpLoc, long rs, long rt, int imm, long fct){
			int retval = 0;
			switch (opcode) {
				case 0: //syscall
					if (fct == 12) 
						retval = 1;
					else
						retval = -1; //error condition
					break;
				case 2: //jump
					instructionMemory.setPC(jumpLoc);
					break;	
				case 4: //beq
					if (registers[(int) rt] == registers[(int) rs])
						instructionMemory.setPC(instructionMemory.instructionPC() + fct*4);
					break;	
				case 8: //addi
					registers[(int) rt] = registers[(int) rs] + imm;
					break;	
				case 13: //ori
					registers[(int) rt] = registers[(int) rs] | imm;
					break;
				case 15: //lui
					registers[(int) rt] = imm << 16;
					break;	
				case 35: //lw
					registers[(int) rt]  = dataMemory.load(registers[(int) rs]);
					break;	
				case 43: //sw
					dataMemory.store(registers[(int) rs], registers[(int) rt]);
					break;	
				default:
					//error condition
					retval= -1;
					break;
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
    		   
    		   if (i == 0)  instructionMemory.setPC(key); //initialize program counter
    		   i++;
    	    }
    	    inputStream.close();
    	   
	   } catch (FileNotFoundException e1) {
		   e1.printStackTrace();
	   }

	}
	
	
}
