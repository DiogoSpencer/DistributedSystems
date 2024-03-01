package sheetsDrop.util;

import java.io.IOException;
import java.util.List;

import com.google.api.services.sheets.v4.Sheets;

import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;

import tp1.api.Spreadsheet;


public class CreateFile {

	private static final String apiKey = "jnyxc48mt6ct2ad";
	private static final String apiSecret = "18x8wnpizgkmxja";
	private static final String accessTokenStr = "c98FvIb-zAwAAAAAAAAAAU2z8JIpJ2RiYf4iK_Mh7yS3t-81jPFjQ7wXvEWmFGtm";
	protected static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
	protected static final String CONTENT_TYPE = "application/octet-stream";
	
	private static final String CREATE_FOLDER_V2_URL = "https://content.dropboxapi.com/2/files/upload";
	
	private OAuth20Service service;
	private OAuth2AccessToken accessToken;
	
	private Sheets services;
	
	private Gson json;
	
	public CreateFile() {
		service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
		accessToken = new OAuth2AccessToken(accessTokenStr);
		
		
		json = new Gson();
	}
	
	public boolean execute( String id, String destination, Spreadsheet sheet) {
		OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
		
		createFolder.addHeader("Dropbox-API-Arg", json.toJson(new CreateFileArgs(id, destination)));
		
		createFolder.addHeader("Content-Type", CONTENT_TYPE);
		createFolder.setPayload(json.toJson(sheet));

		

		service.signRequest(accessToken, createFolder);
		
		Response r = null;
		
		try {
			r = service.execute(createFolder);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(r.getCode() == 200) {
			return true;
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return false;
		}
		
	}
	
	public boolean execute( String id, String destination, List<String> sheet) {
		OAuthRequest createFolder = new OAuthRequest(Verb.POST, CREATE_FOLDER_V2_URL);
		
		createFolder.addHeader("Dropbox-API-Arg", json.toJson(new CreateFileArgs(id, destination)));
		
		createFolder.addHeader("Content-Type", CONTENT_TYPE);
		createFolder.setPayload(json.toJson(sheet));

		

		service.signRequest(accessToken, createFolder);
		
		Response r = null;
		
		try {
			r = service.execute(createFolder);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(r.getCode() == 200) {
			return true;
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return false;
		}
		
		
		
	}
	
}
