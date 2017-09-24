import java.util.*;

class MIPSmemory {
	//use linkedhashmap instead of hashtable to preserve order
	private LinkedHashMap<Long,Long> memory =new LinkedHashMap<Long,Long>();  
	private int memSize = 0;

	public MIPSmemory(int type, int size) {
		if (type == 1){
			//initial data address = "0x10010000"
			
			//long initialAddress = Long.decode("0x10010000")-4;
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
		//
		return memory.get(id);
	}
	public void print(){
		//System.out.println(memory);
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
