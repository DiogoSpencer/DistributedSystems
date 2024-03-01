package extras;

public class Cache {
	
	private long time;
	private String[][] values;
	
	public Cache(long l, String[][] values) {
		
		this.time = l;
		this.values = values;
		
	}
	
	public long getTime() {
		return time;
	}
	
	public String[][] getValues(){
		return values;
	}
	
	public void setTime(long time) {
		this.time = time;
	}
	
	public void setValues(String[][] values){
		this.values = values;
	}

}
