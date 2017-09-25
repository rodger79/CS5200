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
    		   //instructions[i] = strLine; //remove later deprecated version of HW1

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
	
	//lookup for register names
	public static String registerLookup (int input){
		String result = "";
		switch (input) {
		case 0:
			result = "$0";
			break;
		case 1:
			result = "$at";
			break;
		case 2:
			result = "$v0";
			break;
		case 3:
			result = "$v1";
			break;
		case 4:
			result = "$a0";
			break;
		case 5:
			result = "$a1";
			break;
		case 6:
			result = "$a2";
			break;
		case 7:
			result = "$a3";
			break;
		case 8:
			result = "$t0";
			break;
		case 9:
			result = "$t1";
			break;
		case 10:
			result = "$t2";
			break;
		case 11:
			result = "$t3";
			break;
		case 12:
			result = "$t4";
			break;
		case 13:
			result = "$t5";
			break;
		case 14:
			result = "$t6";
			break;
		case 15:
			result = "$t7";
			break;
		case 16:
			result = "$s0";
			break;
		case 17:
			result = "$s1";
			break;
		case 18:
			result = "$s2";
			break;
		case 19:
			result = "$s3";
			break;
		case 20:
			result = "$s4";
			break;
		case 21:
			result = "$s5";
			break;
		case 22:
			result = "$s6";
			break;
		case 23:
			result = "$s7";
			break;
		case 24:
			result = "$t8";
			break;
		case 25:
			result = "$t9";
			break;
		case 26:
			result = "$k0";
			break;
		case 27:
			result = "$k1";
			break;
		case 28:
			result = "$gp";
			break;
		case 29:
			result = "$sp";
			break;
		case 30:
			result = "$fp";
			break;
		case 31:
			result = "$ra";
			break;
		default:
			result = "error";
			break;
		}
		return result;
	}
	
	//quick conversion for hex to string
	public static String hextoString (char input){
		String result = "";
		switch (input) {
			case '0': 
				result = "0000"; 
				break;
			case '1':
				result = "0001"; 
				break;
			case '2':
				result = "0010"; 
				break;
			case '3':
				result = "0011"; 
				break;
			case '4':
				result = "0100"; 
				break;
			case '5':
				result = "0101"; 
				break;
			case '6':
				result = "0110"; 
				break;
			case '7':
				result = "0111"; 
				break;
			case '8':
				result = "1000"; 
				break;
			case '9':
				result = "1001"; 
				break;
			case 'a':
				result = "1010"; 
				break;
			case 'b':
				result = "1011"; 
				break;
			case 'c':
				result = "1100"; 
				break;
			case 'd':
				result = "1101"; 
				break;
			case 'e':
				result = "1110"; 
				break;
			case 'f':
				result = "1111"; 
				break;
			default:
				result = "XXXX"; 
				break;
				
		}
		return result;
		
	}
	
}
