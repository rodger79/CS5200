import java.io.*;
import java.util.*;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class MIPSdecode {
	
	//full list of instructions in string format
	private static String[] instructions = new String[13];
	//address only in string format
	private static String[] address = new String[13];
	//MIPS instruction in HEX saved as char arrays
	private static char[][] MIPSInstruction = new char[13][8];
	//binary string representation of hex
	private static String[] binaryInst = new String[13];
	//output string
	private static String[] output = new String[13];
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException  {

		//read Instructions file
		readInstructions("MachineInstructions.txt");
		
		//pull out addresses
		for (int i = 0; i < address.length; i++){
 		   address[i] = instructions[i].substring(0,10);	   
		}
		//convert MIPS instruction to Int array
		for (int i = 0; i < instructions.length; i++){
			String temp = instructions[i].substring(13,21);
			for (int j = 0; j < 8 ; j++) {
				MIPSInstruction[i][j] = new Character(temp.charAt(j));
				//System.out.println(MIPSInstruction[i][j]);
			}
		}
		
		//build binary string
		for (int i = 0; i < instructions.length; i++){
			for (int j = 0; j < 8 ; j++) {
				if (j == 0)
					binaryInst[i] = hextoString(MIPSInstruction[i][j]);
				else
					binaryInst[i] += hextoString(MIPSInstruction[i][j]);
			}
		}
 	   
		//decode instructions
		for (int i = 0; i < instructions.length; i++){
			System.out.println(MIPSInstruction[i]);
			System.out.println(binaryInst[i]);
			System.out.println(decode(binaryInst[i]));
			writeResults(decode(binaryInst[i]));
		
		}
		
		//call to write to output
			writeResults("");
		
		//Debug
		/*
		for (int i = 0; i < instructions.length; i++){
		   System.out.println(instructions[i]);
		   System.out.println(address[i]);
		   System.out.println(binaryInst[i]);
		}
	
		for (int i = 0; i < instructions.length; i++){
				for (int j = 0; j < 8 ; j++) {
					System.out.println(MIPSInstruction[i][j]);
				}
		}*/


	}
	
	public static String decode(String instruction){
		String result = "";
		String t = instruction.substring(11,16);
		String s = instruction.substring(6,11);
		String opcode = instruction.substring(0,6);
		String funct = instruction.substring(26,32);
		//System.out.println( opcode + " " + funct);
		switch (opcode) {
			case "001000": //addi
				result = "addi" + "\t"; 
				result += registerLookup(t) + ", ";
				result += registerLookup(s) + ", ";
				//result += registerLookup(instruction.substring(27,32));
				result += Integer.parseInt(instruction.substring(16,32), 2);
				break;	
			case "001111": //lui
				result = "lui " + "\t" + registerLookup(t) + ", ";
				result += Integer.parseInt(instruction.substring(16,32), 2);
				break;	
			case "001101": //ori
				result = "ori " + "\t"; 
				result += registerLookup(t) + ", ";
				result += registerLookup(s) + ", ";
				result += Integer.parseInt(instruction.substring(27,32), 2);
				break;
			case "100011": //lw
				result = "lw" + "\t\t"; 
				result += registerLookup(t) + ", ";
				result += Integer.parseInt(instruction.substring(16,32), 2);
				result += "(" +registerLookup(s) + ")";
				break;	
			case "101011": //sw
				result = "sw" + "\t\t"; 
				result += registerLookup(t) + ", ";
				result += Integer.parseInt(instruction.substring(16,32), 2);
				result += "(" +registerLookup(s) + ")";
				break;	
			case "000100": //beq
				result = "beq" + "\t\t"; 
				result += registerLookup(s) + ", ";
				result += registerLookup(t) + ", ";
				result += Integer.parseInt(instruction.substring(16,32), 2);
				break;	
			case "000010": //jump
				result = "j" + "\t\t"; 
				int temp = Integer.parseInt(instruction.substring(7,32), 2);
				result += String.format("0x%08X",temp);
				break;	
			case "000000": 
				result = "syscall";
				/*
				System.out.println("funct = " + funct +" for syscall");
				if (funct == "001100"){
					
					result = "syscall";
				} else{
					result = "error";
				}
					*/
				
				break;	
			default:
				result = "error"; 
				break;
		}
		return result;
	}
	
	public static void readInstructions(String filename) throws IOException{

		File file = new File("MachineInstructions.txt");
		/*
        if(file.exists()){
            System.out.println("file exists");
        }
        else{
            System.out.println("file doesn't exist");
        }
        //System.out.println("[" + file.getAbsolutePath() + "]");

        if(file.canRead()){
            System.out.println("can read");
        }
        if(file.canWrite()){
            System.out.println("can write");
        }*/
        
       try {
    	   InputStream inputStream = new FileInputStream(file);
    	   Reader      inputStreamReader = new InputStreamReader(inputStream);
    	   BufferedReader br = new BufferedReader(inputStreamReader);
    	   
    	   int i = 0;
    	   String strLine;
    	   while ((strLine = br.readLine()) != null) {
    		   instructions[i] = strLine;
    		   i++;

    	    }
    	    inputStream.close();
    	   
	   } catch (FileNotFoundException e1) {
		   e1.printStackTrace();
	   }

	}
	public static void writeResults(String input){

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("Program1.txt"), "utf-8"))) {
			for (int i = 0; i < instructions.length; i++){

				writer.write(address[i] + "\t" + decode(binaryInst[i])+"\n");
			}
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	public static String registerLookup (String input){
		String result = "";
		switch (input) {
		case "00000":
			result = "$0";
			break;
		case "00001":
			result = "$at";
			break;
		case "00010":
			result = "$v0";
			break;
		case "00011":
			result = "$v1";
			break;
		case "00100":
			result = "$a0";
			break;
		case "00101":
			result = "$a1";
			break;
		case "00110":
			result = "$a2";
			break;
		case "00111":
			result = "$a3";
			break;
		case "01000":
			result = "$t0";
			break;
		case "01001":
			result = "$t1";
			break;
		case "01010":
			result = "$t2";
			break;
		case "01011":
			result = "$t3";
			break;
		case "01100":
			result = "$t4";
			break;
		case "01101":
			result = "$t5";
			break;
		case "01110":
			result = "$t6";
			break;
		case "01111":
			result = "$t7";
			break;
		case "10000":
			result = "$s0";
			break;
		case "10001":
			result = "$s1";
			break;
		case "10010":
			result = "$s2";
			break;
		case "10011":
			result = "$s3";
			break;
		case "10100":
			result = "$s4";
			break;
		case "10101":
			result = "$s5";
			break;
		case "10110":
			result = "$s6";
			break;
		case "10111":
			result = "$s7";
			break;
		case "11000":
			result = "$t8";
			break;
		case "11001":
			result = "$t9";
			break;
		case "11010":
			result = "$k0";
			break;
		case "11011":
			result = "$k1";
			break;
		case "11100":
			result = "$gp";
			break;
		case "11101":
			result = "$sp";
			break;
		case "11110":
			result = "$fp";
			break;
		case "11111":
			result = "$ra";
			break;
		default:
			result = "error";
			break;
		}
		return result;
	}
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
