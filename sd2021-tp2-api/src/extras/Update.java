package extras;

import java.util.Map;

import tp1.api.Spreadsheet;

public class Update {
	
	private Map<String, Spreadsheet> sheets = null;
	
	private Long version;
	
	public Update() {
	}
	
	public Update(Map<String, Spreadsheet> sheets, Long version) {
		this.sheets = sheets;
		this.version = version;
	}
	
	public Map<String, Spreadsheet> getSheets(){
		return sheets;
	}
	
	public Long getVersion() {
		return version;
	}

}
