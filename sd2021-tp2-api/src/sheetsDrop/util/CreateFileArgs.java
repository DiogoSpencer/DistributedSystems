package sheetsDrop.util;

public class CreateFileArgs {
	final String path;
	final String mode = "overwrite";
	final boolean autorename = false;
	final boolean mute = false;
	final boolean strict_conflict = false;

	public CreateFileArgs(String id, String destination) {
		this.path = "/" + destination +"/"+ id;
	}
	
}