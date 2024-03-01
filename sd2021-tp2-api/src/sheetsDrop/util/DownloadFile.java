package sheetsDrop.util;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.pac4j.scribe.builder.api.DropboxApi20;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import tp1.api.Spreadsheet;

public class DownloadFile {

	private static final String apiKey = "jnyxc48mt6ct2ad";
	private static final String apiSecret = "18x8wnpizgkmxja";
	private static final String accessTokenStr = "c98FvIb-zAwAAAAAAAAAAU2z8JIpJ2RiYf4iK_Mh7yS3t-81jPFjQ7wXvEWmFGtm";
	protected static final String CONTENT_TYPE = "application/octet-stream";

	private static final String DOWNLOAD_FILE_V2_URL = "https://content.dropboxapi.com/2/files/download";

	private OAuth20Service service;
	private OAuth2AccessToken accessToken;

	private Gson json;

	public DownloadFile() {
		service = new ServiceBuilder(apiKey).apiSecret(apiSecret).build(DropboxApi20.INSTANCE);
		accessToken = new OAuth2AccessToken(accessTokenStr);

		json = new Gson();
	}

	public Spreadsheet execute(String id, String destination) {
		OAuthRequest DownloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_FILE_V2_URL);
		DownloadFile.addHeader("Dropbox-API-Arg", json.toJson(new DownloadFileArgs("/" + destination + "/" + id)));
		
		DownloadFile.addHeader("Content-Type", CONTENT_TYPE);

		service.signRequest(accessToken, DownloadFile);

		Response r = null;

		try {
			r = service.execute(DownloadFile);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		if (r.getCode() == 200) {
			try {
				return json.fromJson(new String(r.getBody()), Spreadsheet.class);
			} catch (JsonSyntaxException | IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return null;
		}
		return null;

	}
	
	public List<String> executeU(String id, String destination) {
		OAuthRequest DownloadFile = new OAuthRequest(Verb.POST, DOWNLOAD_FILE_V2_URL);
		DownloadFile.addHeader("Dropbox-API-Arg", json.toJson(new DownloadFileArgs("/" + destination + "/" + id)));
		
		DownloadFile.addHeader("Content-Type", CONTENT_TYPE);

		service.signRequest(accessToken, DownloadFile);

		Response r = null;

		try {
			r = service.execute(DownloadFile);
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		if (r.getCode() == 200) {
			try {
				return json.fromJson(new String(r.getBody()), LinkedList.class);
			} catch (JsonSyntaxException | IOException e) {
				e.printStackTrace();
			}
		} else {
			System.err.println("HTTP Error Code: " + r.getCode() + ": " + r.getMessage());
			try {
				System.err.println(r.getBody());
			} catch (IOException e) {
				System.err.println("No body in the response");
			}
			return null;
		}
		return null;

	}

}
