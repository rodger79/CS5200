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
	
	//create arraylist of reservation stations
	//private static reservationStation[] reservationStations = new reservationStation[53];
	private static ArrayList<reservationStation> reservationStations =new ArrayList<reservationStation>();  
	
	
	//create list of memory items in the delay queue
	private static ArrayList<delayedMemItem> memDelayList = new ArrayList<delayedMemItem>();
	
	//use delay on load 
	static boolean delayedLoad = true;
	
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
		readInstructions("MachineInstructions2.txt");
		
		//check memory
		instructionMemory.print("IMDump.txt");
		dataMemory.print("DMDumpBefore.txt");
		
		
		//Issue instructions to RSs
		//Will insert into exec loop for program4
		boolean issue = true;
		boolean exec = true;
		
		//debugging
		printToFile("DebugOutput2.txt", "RE.memIndex RE.instruction RE.aPRNum RE.aData"
				+ " RE.bPRNum RE.bData RE.imm\n");
		
		
		int programLoopCounter = 0;
		while (programCont){
		//	dataMemory.printConsole();
			System.out.println("issue");
			programLoopCounter++;

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
			//printRS("Reservations.txt");
			System.out.println("exec instructions");

			while (exec){
				

				printRS("Reservations.txt");


				for (int i = 0; i < reservationStations.size(); i++){
					reservationStation RE = new reservationStation();
					RE = reservationStations.get(i);
					//update any available memory writes
					if (delayedLoad) memoryUpdate();
					//System.out.println("reg data cycle = current cycle: " + cycle);
					//for (int j = 0; j < memDelayList.size(); j++) {
						//if (memDelayList.get(j).available == true)
						//	System.out.println(memDelayList.get(j).registerID + " " + memDelayList.get(j).data + " " + memDelayList.get(j).cycleAvailable + " " + cycle);
					//}
					cycle++;
					if ((RE.aReady || !RE.aRequired) && (RE.bReady || !RE.bRequired) && RE.available == true){
						

						//System.out.println("EXEC: " + RE.instruction + " cycle: " + cycle);
						exec = executeInstruction(i);
						String executedRE = RE.memIndex + " " + RE.instruction + "\t\t" +RE.aPRNum + "\t" + RE.aData /*Long.toHexString(RE.aData)*/ +"\t" +
								RE.bPRNum + "\t" +RE.bData/* Long.toHexString(RE.bData)*/ +"\t" +RE.imm;
						printToFile("DebugOutput.rtf", executedRE);
						//printRS("Reservations.txt");
						//reservationStations.remove(i);
						//i--;
						reservationStations.get(i).available = false;
					}

				}
				//go through reservation stations to see if any can execute
				int numReady = 0;
				for (int i = 0; i < reservationStations.size(); i++) {
					if (reservationStations.get(i).available == true) {
						numReady++;
					}
				}

				if (numReady == 0){
					//stop exec, start issuing
					exec = false;
					issue = true;
					
					dataMemory.printConsole();
					
					//free available physical registers
					//currently breaking the required mappings, not using now
					//think it should be !archVisRegisterInUse(i)
					for (int i = 0; i < physicalRegisters.length; i++) {
						if (!archVisRegisterInUse(i) ) {

							physicalRegisters[i].available = true;
							physicalRegisters[i].valid = false;
							physicalRegisters[i].data = 0xffffffff;

						}
					}
				} else {
					//if even 1 instruction executed, look again for more ready instructions	
				}

			}

			//prevent infinite loop
			if (programLoopCounter == 9){
				issue = false;
				exec = false;
				programCont = false;
				dataMemory.print("DMDumpDebug.txt");

				
			}
		} 
		
		dataMemory.print("DMDumpAfter.txt");
	}
	public static boolean archVisRegisterInUse(int index) {
		boolean retval = false;
		for (int i = 0; i < AVR.length; i++) {
			if (AVR[i] == index) retval = true;
		}
		return retval;
	}
	//execute instructions
	//need to change to update actual reservation station entry
	public static boolean executeInstruction(int index){
		boolean retval = true;
		
		switch (reservationStations.get(index).opcode){
			case 0: //syscall
				System.out.println("Syscall");
				retval = false;
				programCont = false;
				break;
			case 2: //jump
				//shouldn't happen
				break;	
			case 4: //beq
				System.out.println("beq: aData: " + reservationStations.get(index).aData + "bData: " + reservationStations.get(index).bData);
				if (reservationStations.get(index).aData == reservationStations.get(index).bData){
					//update issue program counter
					System.out.println("old pc: " + Long.toHexString(programCounter));
					programCounter += reservationStations.get(index).imm*4;
					System.out.println("new pc: " + Long.toHexString(programCounter));
				}else
					//do nothing
				break;	
			case 8: //addi
				physicalRegisters[reservationStations.get(index).bPRNum].data = reservationStations.get(index).aData + reservationStations.get(index).imm;
				physicalRegisters[reservationStations.get(index).bPRNum].valid = true;
				System.out.println("addi update: " + reservationStations.get(index).bPRNum + " "+ reservationStations.get(index).bData);
				updateReady(reservationStations.get(index).bPRNum, index);
				break;	
			case 13: //ori
				physicalRegisters[reservationStations.get(index).bPRNum].data = reservationStations.get(index).aData | reservationStations.get(index).imm;
				physicalRegisters[reservationStations.get(index).bPRNum].valid = true;
				System.out.println("ori update: " + reservationStations.get(index).bPRNum + " "+ reservationStations.get(index).bData);
				updateReady(reservationStations.get(index).bPRNum, index);
				break;
			case 15: //lui
				physicalRegisters[reservationStations.get(index).bPRNum].data = reservationStations.get(index).imm << 16;
				physicalRegisters[reservationStations.get(index).bPRNum].valid = true;
				System.out.println("lui update: " + reservationStations.get(index).bPRNum + " "+ reservationStations.get(index).bData);
				updateReady(reservationStations.get(index).bPRNum, index);
				break;	
			case 35: //lw
				//debug purposes, directly load vs delay
				if (delayedLoad) {

					//don't actually load it just call the load, the actual load will be handled after 3 cycles
					long tempdata = dataMemory.load(reservationStations.get(index).aData + reservationStations.get(index).imm);
					
					System.out.println("rega data: " + physicalRegisters[reservationStations.get(index).aPRNum].data);
					System.out.println("tempdata: " + Long.toHexString(tempdata));
					System.out.println(Long.toHexString(dataMemory.load(reservationStations.get(index).aData + reservationStations.get(index).imm)));
					
					if (tempdata == -1) {
						retval = false;
						programCont = false;
						System.out.println("errorload: bData: " + reservationStations.get(index).aData + " imm: " + reservationStations.get(index).imm );
					} else {
						delayedMemItem tempItem = new delayedMemItem();
						tempItem.cycleAvailable = cycle + 3;
						tempItem.data = tempdata;
						tempItem.registerID = reservationStations.get(index).bPRNum;
						tempItem.available = true;
						memDelayList.add(tempItem);
					}
				}
				else {
					//immediate load
					physicalRegisters[reservationStations.get(index).bPRNum].data = dataMemory.load(reservationStations.get(index).aData + reservationStations.get(index).imm);
					System.out.println("lw update: " + reservationStations.get(index).bPRNum + " "+ reservationStations.get(index).bData);
					updateReady(reservationStations.get(index).bPRNum, index);
				}
				

				break;	
			case 43: //sw
				//need to fix store, //seems to be working now
				long temp = reservationStations.get(index).bData + reservationStations.get(index).imm ;
				System.out.println("sw debug, id: " + Long.toHexString(temp) + " "/* +  temp */ + "val: " +reservationStations.get(index).aData );
				dataMemory.store(reservationStations.get(index).bData + reservationStations.get(index).imm  , reservationStations.get(index).aData);
				
				break;	
			default:
				//error condition
				break;
		}
		
		return retval;
	}
	//go through list of memory items in queue and update any ready
	public static void memoryUpdate(){
		//need to remove items from this list that have been loaded
		for (int i = 0; i < memDelayList.size(); i++) {
		//for (delayedMemItem mi : memDelayList){
			if (memDelayList.get(i).cycleAvailable == cycle && memDelayList.get(i).available ==true){
				
				System.out.println("data to update from delayed load: " + memDelayList.get(i).data);
				
				physicalRegisters[memDelayList.get(i).registerID].data = memDelayList.get(i).data;
				physicalRegisters[memDelayList.get(i).registerID].valid = true;
				System.out.println("phyReg val from delayed load: " + physicalRegisters[memDelayList.get(i).registerID].data );
				updateReady(memDelayList.get(i).registerID, -1);
				
				System.out.println("data available, memItemID: " + memDelayList.get(i).registerID + " mi.data: " +  
						memDelayList.get(i).data+ " mi.cycle: " +  memDelayList.get(i).cycleAvailable);
				memDelayList.get(i).available = false;
				//memDelayList.remove(i);
				//i--;
			}
		}
		
	}
	//used to update reservation stations when a physical register is updated
	public static void updateReady(int physicalRegisterIndex, int currentRE){
		
		for (int i = 0; i < reservationStations.size(); i++){

		if (reservationStations.get(i).aPRNum == physicalRegisterIndex){
			
			reservationStations.get(i).aReady = true;
			reservationStations.get(i).aData = physicalRegisters[physicalRegisterIndex].data;
			//System.out.println("updateRS a num/val: " + reservationStations.get(i).aPRNum + " " + reservationStations.get(i).aData );
			//physicalRegisters[physicalRegisterIndex].valid = true;
		} else if (reservationStations.get(i).bPRNum == physicalRegisterIndex){
			
			reservationStations.get(i).bReady = true;
			reservationStations.get(i).bData = physicalRegisters[physicalRegisterIndex].data;
			//System.out.println("updateRS b num/val: " + reservationStations.get(i).bPRNum + " " + reservationStations.get(i).bData );
			//physicalRegisters[physicalRegisterIndex].valid = true;
		} else {//do nothing
		}
		}
		
	}
	//issue instructions
	public static boolean issueInstruction(int opcode, long jumpLoc, int rs, int rt, int imm, long fct){
		//Init variables
		boolean issue = true;
		String instruction = "invalid inst";
		
		reservationStation RS = new reservationStation();
		RS.available = true;
		RS.memIndex = cycle;
		
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
				programCounter = (programCounter & 0xf0000000) | jumpLoc;
				//programCounter = jumpLoc;
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
				if (rs == rt) {
					RS.aReused = false;
					//RS.aReady = true;
				}
				RS.instruction = instruction;
				reservationStations.add(RS);
				
				//debugging
			//	System.out.println("Addi RE: aPRNum/Val: " + RS.aPRNum + " " + Long.toHexString(RS.aData) + " phREg num/val: " + AVR[rs] + " " + Long.toHexString(physicalRegisters[RS.aPRNum].data) );
			//	System.out.println("Addi RE: bPRNum/Val: " + RS.bPRNum + " " + Long.toHexString(RS.bData) + " phReg num/val: " + AVR[rt] + " " + Long.toHexString(physicalRegisters[RS.bPRNum].data) + "\n" );
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
					//RS.aData = physicalRegisters[AVR[rs]].data;		//if valid use immediately
					//RS.aReady = true;
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
			out.print("instruction\t\topcode\tfct\taPRNum\taData\taReady\taRequired\taReused\tbPRNum\tbData\t" + 
					"bReady\tbRequired\tbReused\timmRequired\timm\n");

			for (reservationStation RE : reservationStations){
				if(RE.available == (false || true)){
		          out.print(RE.memIndex + "\t" +
		        		  RE.instruction + "\t" +
		        		  RE.opcode + "\t" +
		        		  RE.fct + "\t" +
		        		  RE.aPRNum + "\t" +
		        		  RE.aData + "\t" +
		        		  RE.aReady + "\t" +
		        		  RE.aRequired + "\t\t" +
		        		  RE.aReused + "\t" +
		        		  RE.bPRNum + "\t" +
		        		  RE.bData + "\t" +
		        		  RE.bReady + "\t" +
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
		if (retval == -1){
			programCont = false;
		} else {
			physicalRegisters[retval].available = false;
		}
		return retval;
	}	
	/* not needed - switched to list from array
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
	public static void printToFile(String filename, String input) throws UnsupportedEncodingException, FileNotFoundException, IOException{
		
		try (FileWriter fw = new FileWriter(filename, true);
				BufferedWriter bw = new BufferedWriter(fw);
				PrintWriter out = new PrintWriter(bw)){
			out.print(input + "\n");

		    }
	}
	
}
