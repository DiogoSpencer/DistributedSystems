package usersRest;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;


import extras.Connection;
import extras.Discovery;
import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String, User> users = new HashMap<String, User>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	private String domain;
	private Discovery d;
	private Connection c;

	public UsersResource(String domain, Discovery d, String secret) {
		this.domain = domain;
		this.d = d;
		c = new Connection(secret);
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user + "\n");

		// Check if user is valid, if not return HTTP Bad Request (400)
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (this) {
			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}

			// Add the user to the map of users
			users.put(user.getUserId(), user);
		}

		return user.getUserId();
	}

	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password + "\n");

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		User user;
		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user + "\n");

		// Check if user is valid, if not return HTTP Bad Request (400)
		if (userId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User u;
		synchronized (this) {
			u = users.get(userId);

			// Check if user exists
			if (u == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// Check if the password is correct
			if (!u.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
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
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password + "\n");

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		User user;
		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		if (password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		removeSpreadSheet(userId);

		synchronized (this) {
			users.remove(user.getUserId(), user);
		}

		return user;

	}

	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern + "\n");
		if (pattern == null) {
			Log.info("pattern null.\n");
			throw new WebApplicationException(Status.BAD_REQUEST);
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

	private void removeSpreadSheet(String userId) {
		URI serviceUri = d.knownUrisOf(domain + ":sheets");
		c.deleteSpread(serviceUri, userId);

	}
}
