/****************************************************************
 * Rodger Byrd
 * Dynamic Scheduling Program, part 4
 * 11/7/2017
 * MIPS instruction decoding and implementation
 */

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;


public class MIPSdecode {
	
	//Instantiate Instruction Memory
	private static MIPSmemory instructionMemory = new MIPSmemory(0,0);
	//Instantiate Data Memory
	private static MIPSmemory dataMemory = new MIPSmemory(1,9);
	
	//program counters
	//Issue
	private static long programCounter = 0;
	//Exec
	//private static long execPC = 0;
	//cycle counter
	private static int cycle = 0;
	
	private static boolean programCont = true;
	
	private static String[] strInstructions = { "","","j","","beq","","","","addi","","","","","ori","","lui",
												"","","","","","","","","","","","","","","","","","","","lw",
												"","","","","","","","sw"};
	

	
	//Define registers
	private static long[] registers = new long[32];
	private static String[] strRegisters = {"$0","$at","$v0","$v1","$a0","$a1","$a2","$a3","$t0","$t1","$t2","$t3"};
	private static int[] AVR = new int[12];
	private static physicalRegister[] physicalRegisters = new physicalRegister[1024];
	
	//create array of reservation stations
	//private static reservationStation[] reservationStations = new reservationStation[53];
	private static ArrayList<reservationStation> reservationStations =new ArrayList<reservationStation>();  
	
	
	
	public static void main(String[] args) throws IOException  {
		
		//initialize physical registers
		for (int i=0; i < physicalRegisters.length;i++ ){
			physicalRegisters[i] = new physicalRegister();
			physicalRegisters[i].available = true;
			physicalRegisters[i].data = 0L;
			physicalRegisters[i].valid = false;
		}
		
		//initialize AVR registers
		AVR[0] = 0;
		physicalRegisters[0].available = false;
		physicalRegisters[0].valid = true;

		for (int i=0; i < AVR.length;i++ ){
			physicalRegisters[AVR[i]].data = 0xffffffff;;
		}
		

		//read Instructions file		
		readInstructions("MachineInstructions.txt");
		
		//check memory
		instructionMemory.print("IMDump.txt");
		dataMemory.print("DMDumpBefore.txt");
		
		
		//Issue instructions to RSs
		//Will insert into exec loop for program4
		boolean issue = true;
		boolean exec = true;
		
		
		while (programCont){
			System.out.println("issue");
			while (issue){
				
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
	
				issue = issueInstruction(opcode,jumpLoc,(int)rs,(int)rt,imm, fct);
				
				exec = true;
			} 
			printRS("Reservations.txt");
			System.out.println("exec instructions");
			while (exec){
				
				//update any available memory writes
				memoryUpdate();
				issue = true;
				
				//go through reservation stations to see if any can execute

				//for (reservationStation RE : reservationStations){
				for (int i = 0; i < reservationStations.size(); i++){
					reservationStation RE = new reservationStation();
					RE = reservationStations.get(i);
					exec = false;
					if ((RE.aReady || !RE.aRequired) && (RE.bReady || !RE.bRequired)){
						cycle++;
						exec = executeInstruction(RE);
						System.out.println(RE.instruction);
						reservationStations.remove(i);
					}

				}
				
				//exec = false; //remove to test
			}
			//programCont = false; //remove and use for syscall
		} 
		
		dataMemory.print("DMDumpAfter.txt");
	}
	public static void memoryUpdate(){
		//stub
	}
	//execute instructions
	public static boolean executeInstruction(reservationStation RE){
		boolean retval = true;
		switch (RE.opcode){
			case 0: //syscall
				retval = false;
				programCont = false;
				break;
			case 2: //jump
				//shouldn't happen
				break;	
			case 4: //beq
				if (RE.aData == RE.bData){
					//update issue program counter
					programCounter += RE.fct*4;
				}else
					//do nothing
				
				break;	
			case 8: //addi
				physicalRegisters[RE.aPRNum].data = physicalRegisters[RE.bPRNum].data + RE.imm;
				updateReady(RE.aPRNum);
				break;	
			case 13: //ori
				physicalRegisters[RE.aPRNum].data = physicalRegisters[RE.bPRNum].data | RE.imm;
				updateReady(RE.aPRNum);
				break;
			case 15: //lui
				physicalRegisters[RE.aPRNum].data = RE.imm << 16;
				updateReady(RE.aPRNum);
				break;	
			case 35: //lw
				physicalRegisters[RE.aPRNum].data = dataMemory.load(physicalRegisters[RE.bPRNum].data);
				updateReady(RE.aPRNum);
				break;	
			case 43: //sw
				dataMemory.store(physicalRegisters[RE.aPRNum].data , physicalRegisters[RE.bPRNum].data);
				break;	
			default:
				//error condition
				break;
		}
		
		return retval;
	}
	public static void updateReady(int physicalRegisterIndex){
		
		for (reservationStation RE : reservationStations){
			if (RE.aPRNum == physicalRegisterIndex){
				RE.aReady = true;
			} else if (RE.bPRNum == physicalRegisterIndex){
				RE.bReady = true;
			} else {/*do nothing*/}
			
		}
		
	}
	//issue instructions
	public static boolean issueInstruction(int opcode, long jumpLoc, int rs, int rt, int imm, long fct){
		//Init variables
		boolean issue = true;
		String instruction = "invalid inst";
		
		reservationStation RS = new reservationStation();
		RS.available = false;
		
		
		switch (opcode) {
			case 0: //syscall
				if (fct == 12){ 
					instruction =("Syscall");
					issue = false;
					RS.opcode = opcode;
					RS.fct = fct;
					
					RS.aPRNum = AVR[2];
					RS.aRequired = true;
					
					RS.instruction = instruction;
					reservationStations.add(RS);
				} else
					System.out.println("unrecognized instruction");
					//error condition
				break;
			case 2: //jump
				//no need to issue 
				//instruction = (strInstructions[opcode] + "\t" + String.format("0x%08X", jumpLoc) );
				//programCounter = (programCounter & 0xf0000000) | jumpLoc;
				programCounter = jumpLoc;
				break;	
				
			case 4: //beq
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rs] + ", " + strRegisters[(int)rt] + ", " + imm );
				RS.opcode = opcode;
				
				RS.aPRNum = AVR[rs];
				if (physicalRegisters[RS.aPRNum].valid){ 
					RS.aReady = true;
					RS.aData = physicalRegisters[RS.aPRNum].data;
				}
				RS.aRequired = RS.bRequired = true;
				
				RS.bPRNum = AVR[rt];
				if (physicalRegisters[RS.bPRNum].valid){
					RS.bData = physicalRegisters[RS.bPRNum].data;
					RS.bReady = true;
				}
				
				RS.immRequired = true;
				RS.imm = imm;
				RS.instruction = instruction;
				reservationStations.add(RS);
				issue = false;  				//flag to hold issuing on branch
				break;	
			case 8: //addi
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + strRegisters[(int)rs] + ", " + imm);
				RS.opcode = opcode;
				//if RS == 0 set data and ready flag, else use actual register
				if (rs == 0){
					RS.aData = 0;
					RS.aReady = true;
				} else {
					RS.aPRNum = AVR[rs];
					if (physicalRegisters[RS.aPRNum].valid == true){
						RS.aData = physicalRegisters[RS.aPRNum].data;
						RS.aReady = true;
					}
				}
				RS.aRequired = true;
				
				RS.bPRNum = nextAvailablePE();
				AVR[rt] = RS.bPRNum;
				RS.bRequired = false;
				
				RS.immRequired = true;
				RS.imm = imm;
				if (rs == rt) RS.aReused = false;
				RS.instruction = instruction;
				reservationStations.add(RS);
				break;	
			case 13: //ori
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + strRegisters[(int)rs] + ", " + imm );
				RS.opcode = opcode;
				RS.aPRNum = AVR[rs];
				RS.aRequired = true;
				
				RS.bPRNum = nextAvailablePE();
				AVR[rt] = RS.bPRNum;
				RS.bRequired = false;
				
				RS.immRequired = true;
				RS.imm = imm;
				
				if (rs == rt) RS.aReused = false;
				
				RS.instruction = instruction;
				reservationStations.add(RS);
				break;
			case 15: //lui
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm );
				RS.opcode = opcode;
				
				RS.bPRNum = nextAvailablePE();
				AVR[rt] = RS.bPRNum;
				RS.bRequired = false;
				
				RS.immRequired = true;
				RS.imm = imm;
				
				RS.instruction = instruction;
				reservationStations.add(RS);
				break;	
			case 35: //lw
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm + "(" + strRegisters[(int)rs] + ")");
				RS.opcode = opcode;
				RS.aPRNum = AVR[rs];
				RS.aRequired = true;
				
				/* In notes but not example code */
				if (physicalRegisters[AVR[rs]].valid == true){
					RS.aData = physicalRegisters[AVR[rs]].data;		//if valid use immediately
					RS.aReady = true;
				} 
				
				RS.bPRNum = nextAvailablePE();
				AVR[rt] = RS.bPRNum;
				RS.bRequired = false;
				RS.immRequired = true;
				RS.imm = imm;
				RS.instruction = instruction;
				reservationStations.add(RS);
				break;	
			case 43: //sw
				instruction = (strInstructions[opcode] + "\t" + strRegisters[(int)rt] + ", " + imm + "(" + strRegisters[(int)rs] + ")");
				RS.opcode = opcode;
				RS.aPRNum = AVR[rt];		//had these backwards....
				RS.aRequired = true;
				RS.bPRNum = AVR[rs];
				RS.bRequired = true;		//not sure on this, added to match output
				RS.immRequired = true;
				RS.imm = imm;
				RS.instruction = instruction;
				reservationStations.add(RS);
				break;	
			default:
				//error condition
				break;
		}
		
		return issue;
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
			System.out.println(instruction);
			return retval;
	}
	
	public static void printRS(String filename) throws UnsupportedEncodingException, FileNotFoundException, IOException{
		
		try (FileWriter fw = new FileWriter(filename, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)){
	//	try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
			out.print("instruction\t\topcode\tfct\taPRNum\taData\taReady\taRequired\taReused\tbPRNum\tbData" + 
					"bReady\tbRequired\tbReused\t\tmemIndex\timmRequired\timm\n");

			for (reservationStation RE : reservationStations){
				if(RE.available == false){
		          out.print(RE.instruction + "\t" +
		        		  RE.opcode + "\t" +
		        		  RE.fct + "\t" +
		        		  RE.aPRNum + "\t" +
		        		  RE.aData + "\t" +
		        		  RE.aReady + "\t" +
		        		  RE.aRequired + "\t\t" +
		        		  RE.aReused + "\t" +
		        		  RE.bPRNum + "\t" +
		        		  RE.bData + "\t\t" +
		        		  RE.bReady + "\t\t" +
		        		  RE.bRequired + "\t\t" +
		        		  RE.bReused + "\t\t" +
		        		  RE.immRequired+ "\t\t" +
		        		  RE.imm + "\t" + "\n");
		        //  if (reservationStations[i].opcode == 4) writer.write("\n\nIssue\n");


				}
			}
		    }
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
		physicalRegisters[retval].available = false;
		return retval;
	}	
	/* not needed
	//find next available registration station
	public static int nextAvailableRS(){
		int retval = -1;
		for (int i=0; i < reservationStations.length;i++ ){
			if (reservationStations[i].available == true){
				retval = i;
				break;
			}
		}
		return retval;
	}
	*/
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
