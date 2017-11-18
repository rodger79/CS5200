/****************************************************************
 * Rodger Byrd
 * Program 4
 * 9/24/2017
 * MIPS memory implementation
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

	//initialize, should probably pass in initial data address as a parameter
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
	//store to memory
	public void store (long id, long value){
		if(memory.containsKey(id)){
			//id already exists don't increase size
			//overwriting new value
		}else{
			memSize++;
		}
		memory.put(id,value);
	}	
	//didn't change this for program 4, instead the main program handles the delay by copying 
	//  the returned value into a list with the corresponding execution cycle that it becomes
	//  available
	public long load (long id){
		if (memory.containsKey(id)) 
			return memory.get(id);
		else 
			System.out.println("memError: " + Long.toHexString(id) + " not in memory");
			return -1; //error
	}
	
	//print to file
	public void print(String filename){

		Set<Long> keys = memory.keySet();

		try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
			
		    for(long key: keys){
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
	//debugging purposes, print to console
	public void printConsole() {
		Set<Long> keys = memory.keySet();
	    for(long key: keys){
	         System.out.println("0x"+String.format("%08X", key) + "\t 0x" + String.format("%08X", memory.get(key)));
	    }
	}
	public int count(){
		return memory.size();
	}
	public int size(){
		return memSize;
	}
}
