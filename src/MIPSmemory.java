/****************************************************************
 * Rodger Byrd
 * Program 1
 * 9/24/2017
 * MIPS instruction decoding and implementation
 */

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.*;

class MIPSmemory {
	//use linkedhashmap instead of hashtable to preserve order
	private LinkedHashMap<Long,Long> memory =new LinkedHashMap<Long,Long>();  
	private int memSize = 0;


	public MIPSmemory(int type, int size) {
		if (type == 1){
			//initial data address = "0x10010000"
			long initialAddress = Long.decode("0x10010000");
			memSize = size;
			//data memory initialize
			for (int i = 0; i < size; i++){
				memory.put(initialAddress+4*i,(long) i);
			}
		} else {
			//instruction memory, don't initialize
		}
	}

	public void store (long id, long value){
		if(memory.containsKey(id)){
			//id already exists don't increase size
			//overwriting new value
		}else{
			memSize++;
		}
		memory.put(id,value);
	}	
	public long load (long id){
		if (memory.containsKey(id)) 
			return memory.get(id);
		else 
			return -1; //error
	}
	public void print(String filename){

		Set<Long> keys = memory.keySet();

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
			
		    for(long key: keys){
		         // System.out.println("0x"+String.format("%08X", key) + "\t 0x" + String.format("%08X", memory.get(key)));
		          writer.write("0x"+String.format("%08X", key) + "\t 0x" + String.format("%08X", memory.get(key))+"\n");
		    }
				
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	public int count(){
		return memory.size();
	}
	public int size(){
		return memSize;
	}
	//public void setPC(long value){
	//	programCounter = value;
	//}
	//public long instructionPC(){
	//	return programCounter;
	//}
	//public void incPC(){
	//	programCounter = programCounter + 4;
	//}
}
