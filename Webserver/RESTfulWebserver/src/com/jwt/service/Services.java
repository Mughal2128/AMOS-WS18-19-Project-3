package com.jwt.service;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Array;
import java.sql.Connection;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.format.DateTimeFormatter;
import com.jwt.service.mail.Mailer;
import com.sun.org.apache.bcel.internal.generic.Select;

import java.util.*;
import java.text.*;

/**
 * This class provides all web services.
 */
/**
 * @author User
 *
 */
@Path("/services")
public class Services {

	@Context
	private ServletContext context;

	/**
	 * Gives greetings to an Request. Testable with
	 * http://localhost:8080/RESTfulWebserver/services/Tester
	 * 
	 * @param name path ending of tester
	 * @return Response with status 200 and a JSONObject with greetings.
	 * @throws JSONException
	 */
	@GET
	@Path("/{name}")
	public Response getMsg(@PathParam("name") String name) throws JSONException {
		JSONObject output = new JSONObject("{Welcome : " + name + "}");
		System.out.println("...TestRequest from " + name);
		return Response.status(200).entity(output.toString()).build();
	}

	/**
	 * TODO Checks users authentication.
	 * 
	 * @param urlReq with user name and password
	 * @return Response with status 200 and massage for valid user or status 400
	 *         with InvalidRequestBody-message.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/checkUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response checkUser(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("email") && JSONreq.has("password")) {
			String email = JSONreq.getString("email"); 
			String password = JSONreq.getString("password");

			System.out.println("...userLoginRequest from " + email);

			// check users info in DB
			DatabaseProvider db = new DatabaseProvider(context);
			ResultSet rs = db.querySelectDB("SELECT * FROM user_reg WHERE email = ? AND password = ?", email, password);

			JSONObject response = new JSONObject();
			if(!rs.next()) {
				response.put("login", "wrongCredentials");
				return Response.status(200).entity(response.toString()).build();
			}
			
			response.put("login", "successfulLogin");
			return Response.status(200).entity(response.toString()).build();
		} else {
			return Response.status(400).entity("InvalidRequestBody").build();
		}
	}

	/**
	 * TODO Adds new user to database.
	 * 
	 * @param urlReq with user name, password, email, ...
	 * @return Response with status 200 and a message or status 400 with
	 *         InvalidRequestBody-message.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/userRegistration")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response userRegistration(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("name") && JSONreq.has("password") && JSONreq.has("email")) {
			try {
				JSONObject response = new JSONObject();

				// get data
				String username = JSONreq.getString("name");
				String password = JSONreq.getString("password");
				String email = JSONreq.getString("email");
				// TODO: more data... city, birth?

				System.out.println("...userRegistrationRequest from " + username);

				// check if user already exists
				DatabaseProvider db = new DatabaseProvider(context);

				ResultSet result = db.querySelectDB("SELECT * FROM user_reg WHERE email = '" + email + "'");
				while (result.next()) {
					response.put("userRegistration", "emailExistsAlready");
					return Response.status(200).entity(response.toString()).build();
				}

				// insert into DB
				db.queryInsertDB("INSERT INTO user_reg (password,email) VALUES (?,?)", password,
						email);


				// send registration mail
				Mailer mailer = new Mailer(context);
				boolean messageSent = mailer.sendRegistrationMail(email, username);

				if (!messageSent) {
					response.put("userRegistraion", "invalidMail");
					return Response.status(400).entity(response.toString()).build();
				}

				response.put("userRegistration", "successfullRegistration");
				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	// Create New Event Type

	@POST
	@Path("/createEventType")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEventType(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println(".. Create New Event Type");

		if (JSONreq.has("name")) {
			try {

				String eventname = JSONreq.getString("name");
				String eventdescription = JSONreq.getString("description");

				System.out.println("...New Event Type Created:" + eventname);

				try {
					DatabaseProvider postgreSQLExample = new DatabaseProvider(context);
					Connection conn = postgreSQLExample.getPostgreSQLConnection();

					PreparedStatement st = conn
							.prepareStatement("INSERT INTO EVENT_TYPE (name,description) VALUES (?,?)");
					st.setString(1, eventname);
					st.setString(2, eventdescription);
					st.executeUpdate();
					st.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("eventTypeCreation", "successfullCreation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * TODO
	 * 
	 * @param urlReq
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@POST
	@Path("/createEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...createEventRequest");

		if (JSONreq.has("name") && JSONreq.has("description") && JSONreq.has("date") && JSONreq.has("time")) {
			try {

				String eventname = JSONreq.getString("name");
				String eventdescription = JSONreq.getString("description");
				String eventdate = JSONreq.getString("date");
				String eventtime = JSONreq.getString("time");

				System.out.println("...newEventCreated:" + eventname);

				try {
					DatabaseProvider postgreSQLExample = new DatabaseProvider(context);
					Connection conn = postgreSQLExample.getPostgreSQLConnection();

					PreparedStatement st = conn.prepareStatement(
							"INSERT INTO EVENT (name,description,date,time) VALUES (?,?,?::date,?::time)");
					st.setString(1, eventname);
					st.setString(2, eventdescription);
					st.setString(3, eventdate);
					st.setString(4, eventtime);
					st.executeUpdate();
					st.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("eventCreation", "successfullCreation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	/**
	 * Sets options for headers.
	 */
	@OPTIONS
	public Response optionsOptions() {
		return Response.ok().header("Allow-Control-Allow-Methods", "POST,GET,OPTIONS")
				.header("Access-Control-Allow-Origin", "*").build();
	}

	@GET
	@Path("/testMail")
	public Response sendTestMail() throws JSONException {
		JSONObject output = new JSONObject("{Test: send Mail}");
		System.out.println("...sendTestMail Request ");

		Mailer mailer = new Mailer(context);
		// send test registration mail

		// mailer.sendRegistrationMail("anika.apel@gmx.de", "test");

		return Response.status(200).entity(output.toString()).build();
	}

	@GET
	@Path("/getEvents")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAllEvents() throws JSONException {
		JSONObject jobj1 = new JSONObject();

		System.out.println("...getAllEvetns");
		try {
			DatabaseProvider postgreSQLExample = new DatabaseProvider(context);
			Connection conn = postgreSQLExample.getPostgreSQLConnection();

			// PreparedStatement st = conn.prepareStatement("SELECT
			// name,description,date,time FROM EVENT");

			Statement ss = conn.createStatement();
			ResultSet result = ss.executeQuery("SELECT id_event,name,description,date,time FROM EVENT");
			System.out.println("thiss" + result.getClass().getName());

			JSONArray jArray = new JSONArray();
			while (result.next()) {
				String id_json = result.getString("id_event");
				String name_json = result.getString("name");
				String desc_json = result.getString("description");
				String date_json = result.getString("date");
				String time_json = result.getString("time");
				System.out.println(result);

				JSONObject jobj = new JSONObject();
				jobj.put("id_event", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("date", date_json);
				jobj.put("time", time_json);
				jArray.put(jobj);

			}

			jobj1.put("event", jArray);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("eventCreation", jobj1);

		return Response.status(200).entity(response.toString()).build();

	}

	@GET
	@Path("/getEventById/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getEventById(@PathParam("id") int id) throws JSONException {
		JSONObject jobj = new JSONObject();

		System.out.println("...Get Event By ID ");
		try {
			DatabaseProvider postgreSQLExample = new DatabaseProvider(context);
			Connection conn = postgreSQLExample.getPostgreSQLConnection();

			Statement statement = conn.createStatement();
			ResultSet result = statement.executeQuery("SELECT * FROM EVENT WHERE id_event=" + id);
			System.out.println("...Get Event By ID ssssss");

			System.out.println("thiss" + result.getClass().getName());

			if (result.next()) {
				String id_json, name_json, desc_json, date_json, time_json;
				id_json = result.getString("id_event");
				name_json = result.getString("name");
				desc_json = result.getString("description");
				date_json = result.getString("date");
				time_json = result.getString("time");
				System.out.println(result);

				jobj.put("id_event", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("date", date_json);
				jobj.put("time", time_json);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("eventCreation", jobj);

		return Response.status(200).entity(response.toString()).build();

	}

	@POST
	@Path("/updateEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...updateEventRequest");

		if (JSONreq.has("id_event") && JSONreq.has("name") && JSONreq.has("description") && JSONreq.has("date")
				&& JSONreq.has("time")) {
			try {

				int eventid = (int) JSONreq.get("id_event");
				String eventname = JSONreq.getString("name");
				String eventdescription = JSONreq.getString("description");
				String eventdate = JSONreq.getString("date");
				String eventtime = JSONreq.getString("time");

				System.out.println("...updateEvent:" + eventname + "...updateEvent Id:" + eventid
						+ "...updateEvent Date:" + eventdate + "...updateEvent tinme:" + eventtime);

				try {

					DatabaseProvider postgreSQLExample = new DatabaseProvider(context);
					Connection conn = postgreSQLExample.getPostgreSQLConnection();

					PreparedStatement statement = conn.prepareStatement(
							"UPDATE EVENT SET name =?, description =?, date=?::date, time=?::time WHERE id_event="
									+ eventid);

					statement.setString(1, eventname);
					statement.setString(2, eventdescription);
					statement.setString(3, eventdate);
					statement.setString(4, eventtime);
					statement.executeUpdate();
					statement.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("eventUpdate", "successfullUpdation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

}
