package usersSoap;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


import extras.Connection;
import extras.Discovery;
import jakarta.jws.WebService;
import tp1.api.User;
import tp1.api.service.soap.SoapUsers;
import tp1.api.service.soap.UsersException;

@WebService(serviceName = SoapUsers.NAME, targetNamespace = SoapUsers.NAMESPACE, endpointInterface = SoapUsers.INTERFACE)
public class UsersWS implements SoapUsers {

	private final Map<String, User> users;
	private String domain;
	private Discovery d;
	private Connection c;
	

	private static Logger Log = Logger.getLogger(UsersWS.class.getName());

	public UsersWS(String domain, Discovery d, String secret) {
		this.users = new HashMap<String, User>();
		this.domain = domain;
		this.d = d;
		c = new Connection(secret);
	}

	@Override
	public String createUser(User user) throws UsersException {
		Log.info("createUser : " + user);

		// Check if user is valid, if not throw exception
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new UsersException("Invalid user instance.");
		}

		synchronized (this) {
			// Check if userId does not exist exists, if not throw exception
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new UsersException("User already exists.");
			}

			// Add the user to the map of users
			users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) throws UsersException {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid, if not throw exception
		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or password are null.");
		}

		User user = null;

		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists, if yes throw exception
		if (user == null) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist.");
		}

		// Check if the password is correct, if not throw exception
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect.");
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) throws UsersException {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user + "\n");

		// Check if user is valid, if not return HTTP Bad Request (400)
		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or passwrod null.");
		}

		User u;
		synchronized (this) {
			u = users.get(userId);

			// Check if user exists
			if (u == null) {
				Log.info("User does not exist.");
				throw new UsersException("User does not exist.");
			}

			// Check if the password is correct
			if (!u.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new UsersException("Password is incorrect.");
			}

			if (user.getPassword() != null) {
				u.setPassword(user.getPassword());
			}
			if (user.getEmail() != null) {
				u.setEmail(user.getEmail());
			}
			if (user.getFullName() != null) {
				u.setFullName(user.getFullName());
			}

			users.replace(u.getUserId(), u);

		}

		return u;
	}

	@Override
	public User deleteUser(String userId, String password) throws UsersException {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password + "\n");

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or passwrod null.");
		}

		User user;
		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new UsersException("User does not exist.");
		}

		if (password == null) {
			Log.info("UserId or passwrod null.");
			throw new UsersException("UserId or passwrod null.");
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new UsersException("Password is incorrect.");
		}

		removeSpreadSheet(userId);

		synchronized (this) {
			users.remove(user.getUserId(), user);
		}

		return user;
	}

	private void removeSpreadSheet(String userId) {
		Log.info("deleting users SpreadSheets " + userId + ".\n");
		URI serviceUri = d.knownUrisOf(domain + ":sheets");
		c.deleteSpread(serviceUri, userId);
	}

	@Override
	public List<User> searchUsers(String pattern) throws UsersException {
		Log.info("searchUsers : pattern = " + pattern + "\n");
		if (pattern == null) {
			Log.info("pattern null.\n");
			throw new UsersException("pattern null.\n");
		}
		List<User> l = new LinkedList<User>();
		Iterator<User> i;
		synchronized (this) {
			i = users.values().iterator();
			User user;
			if (pattern.equals("")) {
				while (i.hasNext()) {
					user = i.next();
					User u = new User(user.getUserId(), user.getFullName(), user.getEmail(), "");
					l.add(u);
				}
			} else {
				while (i.hasNext()) {
					user = i.next();
					if (user.getFullName().toLowerCase().contains(pattern.toLowerCase())) {
						User u = new User(user.getUserId(), user.getFullName(), user.getEmail(), "");
						l.add(u);
					}
				}
			}
		}
		return l;
	}

}
