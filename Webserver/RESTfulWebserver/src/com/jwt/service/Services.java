package com.jwt.service;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.sql.Connection;
import java.sql.Date;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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

import com.jwt.DataBaseConnection.DatabaseProvider;
import com.jwt.dao.ChatDao;
import com.jwt.dao.ChatDaoImplementation;
import com.jwt.dao.EventDao;
import com.jwt.dao.EventDaoImplementation;
import com.jwt.dao.EventTypeDao;
import com.jwt.dao.EventTypeDaoImplementation;
import com.jwt.dao.RouteDao;
import com.jwt.dao.RouteDaoImplementation;
import com.jwt.dao.UserDao;
import com.jwt.dao.UserDaoImplementation;
import com.jwt.model.Address;
import com.jwt.model.BasicUser;
import com.jwt.model.Event;
import com.jwt.model.EventType;
import com.jwt.model.Message;
import com.jwt.model.Route;
import com.jwt.model.User;
import com.jwt.service.mail.Mailer;

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
	 * Checks users authentication.
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
		DatabaseProvider.getInstance(context);
		JSONObject JSONreq = new JSONObject(urlReq);
		if (JSONreq.has("email") && JSONreq.has("password")) {

			String email = JSONreq.getString("email");
			String password = JSONreq.getString("password");

			System.out.println("...userLoginRequest from " + email);
			UserDao userDao = new UserDaoImplementation();
			User user = userDao.getUser(email, password);

			if (user == null) {
				return Response.status(401).entity("Unauthorized").build();
			}

			JSONObject response = new JSONObject();
			response.put("user_id", user.getUser_id());
			response.put("email", user.getEmail());

			response.put("success", true);
			
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
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("password") && JSONreq.has("email") && JSONreq.has("isBusinessUser"))  {
			try {
				JSONObject response = new JSONObject();

				// get data
				String password = JSONreq.getString("password");
				String email = JSONreq.getString("email"); 
				int idUserType = JSONreq.getBoolean("isBusinessUser") ? 2 : 1;
				// TODO: more data... city, birth?

				System.out.println("...userRegistrationRequest from " + email);

				// check if user already exists
				DatabaseProvider db = DatabaseProvider.getInstance(context);
				ResultSet result = db.querySelectDB("SELECT * FROM user_reg WHERE email = '" + email + "'");
				while (result.next()) {
					response.put("userRegistration", "emailExistsAlready");
					return Response.status(200).entity(response.toString()).build();
				}

				// insert into DB
				db.queryInsertDB("INSERT INTO user_reg (password,email, id_user_type) VALUES (?,?,?)", password, email, idUserType);
						
				// send registration mail
				Mailer mailer = new Mailer(context);
				boolean messageSent = mailer.sendRegistrationMail(email);

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
					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement st = provider.getConnection()
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
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		DatabaseProvider.getInstance(context);
		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...createEventRequest");

		if (JSONreq.has("event") && JSONreq.has("address") && JSONreq.has("user_id")) {
			try {
				int user_id = JSONreq.getInt("user_id");
				Event newEvent = new Event(JSONreq.getJSONObject("event"), user_id);
				Address address = new Address(JSONreq.getJSONObject("address"));

				EventDao eventDao = new EventDaoImplementation();
				eventDao.createEvent(newEvent, address);

				JSONObject response = new JSONObject();

				response.put("eventCreation", "successfullCreation");
				System.out.println("...newEventCreated:" + newEvent.getName());
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
	@POST
	@Path("/addUserBasic")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUserBasic(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		
		if (JSONreq.has("first_name") && JSONreq.has("last_name") && JSONreq.has("dob") && JSONreq.has("country")
				&& JSONreq.has("state") && JSONreq.has("city") && JSONreq.has("street") && JSONreq.has("postcode")
				&& JSONreq.has("housenumber") && JSONreq.has("gender") && JSONreq.has("user_id")) {
		
			try {
				
				int user_id = JSONreq.getInt("user_id");
				String fname = JSONreq.getString("first_name");
				String lname = JSONreq.getString("last_name");
				String dob = JSONreq.getString("dob");
				String country = JSONreq.getString("country");
				String state = JSONreq.getString("state");
				String city = JSONreq.getString("city");
				String street = JSONreq.getString("street");
				Integer postcode = JSONreq.getInt("postcode");
				String housenumber = JSONreq.getString("housenumber");
				String genderCol = JSONreq.getString("gender");
				try {
					
					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					Connection conn = provider.getConnection();

					PreparedStatement s1 = conn.prepareStatement(
							"INSERT INTO ADDRESS (country, state, city, street, postcode, housenumber) VALUES (?,?,?,?,?,?)");

					PreparedStatement s2 = conn.prepareStatement(
							"INSERT INTO BASIC_USER (id_user, first_name,last_name,dob, id_gender, id_address) VALUES (?,?,?,?::date,?,?)");

					s1.setString(1, country);
					s1.setString(2, state);
					s1.setString(3, city);
					s1.setString(4, street);
					s1.setInt(5, postcode);
					s1.setString(6, housenumber);
					s1.executeUpdate();
					s1.closeOnCompletion();

					s2.setInt(1, user_id);
					s2.setString(2, fname);
					s2.setString(3, lname);
					s2.setString(4, dob);
					
					String man = "M";
					String female = "F";
					
					if (genderCol.equals(man)) {
						s2.setInt(5, 1);
					}else if (genderCol.equals(female)) {
						s2.setInt(5, 2);
					} 

					String selectSQL = "SELECT ID_ADDRESS FROM ADDRESS ORDER BY ID_ADDRESS DESC LIMIT 1";
					PreparedStatement preparedStatement3 = conn.prepareStatement(selectSQL);
					ResultSet rs = preparedStatement3.executeQuery();
					int result;
					while (rs.next()) {
						String addressId = rs.getString("ID_ADDRESS");
						result = Integer.parseInt(addressId);
						s2.setInt(6, result);
					}

					s2.executeUpdate();
					s2.closeOnCompletion();
					
					System.out.println("UserInfoGotInserted");

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				JSONObject response = new JSONObject();
				response.put("addUserBasic", "successfullCreation");
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

	@POST
	@Path("/addUserRoute")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addUserRoute(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("route_id") && JSONreq.has("user_id")) {

			try {

				int route_id = JSONreq.getInt("route_id");
				int user_id = JSONreq.getInt("user_id");

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					Connection conn = provider.getConnection();

					PreparedStatement s2 = conn.prepareStatement(
							"INSERT INTO liked_route (id_user, id_route) VALUES (?,?)"
							);

					//PreparedStatement s2 = conn.prepareStatement(
						//	"INSERT INTO event_participation (id_event,id_user,name,description,date,time) VALUES (?,?,?,?::date,?)");
					s2.setInt(1, user_id);
					s2.setInt(2, route_id);

					s2.executeUpdate();
					s2.closeOnCompletion();

					System.out.println("MyRouteIsInserted");

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();
				response.put("success", "true");
				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}
	
	@POST
	@Path("/addmyeventlist")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addmyeventlist(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("event_id") && JSONreq.has("user_id")) {

			try {

				int event_id = JSONreq.getInt("event_id");
				int user_id = JSONreq.getInt("user_id");

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					Connection conn = provider.getConnection();

					PreparedStatement s2 = conn.prepareStatement(
							"INSERT INTO event_participation (id_event,id_user) VALUES (?,?)");

					//PreparedStatement s2 = conn.prepareStatement(
						//	"INSERT INTO event_participation (id_event,id_user,name,description,date,time) VALUES (?,?,?,?::date,?)");
					s2.setInt(1, event_id);
					s2.setInt(2, user_id);

					s2.executeUpdate();
					s2.closeOnCompletion();

					System.out.println("MyeventGotInserted");

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();
				response.put("addmyeventlist", "successfullAdded");
				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@GET
	@Path("/myEvents/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response myEvents(@PathParam("id") int id) throws JSONException {


		JSONObject j = new JSONObject();

		System.out.println("...myallEvents");

		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			ResultSet result = provider.querySelectDB("SELECT E.id_event,E.name,"
					+"E.description,E.date,E.time,"
					+"FROM event AS E INNER JOIN"
					+"event_participation AS P ON"
					+"E.id_event = P.id_event AND E.date >= now()"
					+"AND P.id_user="+id);
			System.out.println(result);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(j.toString()).build();

	}


	@GET
	@Path("/getEvents")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAllEvents() throws JSONException {

		JSONObject jobj1 = new JSONObject();
		JSONArray jArray = new JSONArray();
		JSONObject j = new JSONObject();
		
		System.out.println("...getAllEvetns");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			// PreparedStatement st = conn.prepareStatement("SELECT
			// name,description,date,time FROM EVENT");
			
			ResultSet result = provider.querySelectDB("SELECT DISTINCT ON (e.id_event) * FROM EVENT e"
					+ " LEFT JOIN ADDRESS a USING (id_address)"
					+ " WHERE e.date >= now() "
					+ " ORDER BY e.id_event, a.id_address" );
			System.out.println(result);
			
			while (result.next()) {	
	
				String id_json = result.getString("id_event");
				String id_address_json = result.getString("id_address");
				String name_json = result.getString("name");
				String desc_json = result.getString("description");
				String date_json = result.getString("date");
				String time_json = result.getString("time");
				String user_id_json = result.getString("id_user");
				
				String id_add_json = result.getString("id_address");
				String city_json = result.getString("city");
				String country_json = result.getString("country");
				System.out.println(result);

				JSONObject jobj = new JSONObject();
				jobj.put("id_event", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("date", date_json);
				jobj.put("time", time_json);
				jobj.put("id_user", user_id_json);
				
				JSONObject jobj2 = new JSONObject();
				jobj2.put("city", city_json);
				jobj2.put("country", country_json);
				jobj2.put("id_address", id_add_json);
				
				jobj.put("address", jobj2);
				j.append("event",jobj);

			}


		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(j.toString()).build();

	}
			
	
	@GET
	@Path("/getRoutes")
	@Consumes (MediaType.APPLICATION_JSON)
	public Response getAllRoutes() throws JSONException{

		JSONObject j = new JSONObject();
		
		System.out.println("...getAllRoutes");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);
			
			ResultSet result = provider.querySelectDB("Select * from ROUTE");
					//"SELECT a.*, r.* FROM ADDRESS AS a INNER JOIN ROUTE AS r ON "
					//+ "(a.id_address = r.startpoint OR a.id_address = r.endpoint)"
				//	+ ""
					//+ "SELECT DISTINCT ON (r.id_route) * FROM ROUTE r"
					//+ " JOIN ADDRESS a USING (id_address)"
					//+ " WHERE e.startpoint =a.id_address "
					//+ " ORDER BY r.id_route, a.id_address" 
					
			System.out.println(result);
			
			while (result.next()) {	
				String starting_id = result.getString("startpoint");
				String ending_id = result.getString("endpoint");
				
				ResultSet start = provider.querySelectDB("Select * from ADDRESS AS a Where a.id_address =" + starting_id);
				ResultSet end = provider.querySelectDB("Select * from ADDRESS AS a Where a.id_address =" + ending_id);
								
				String id_json = result.getString("id_route");
				String name_json = result.getString("name");
				String desc_json = result.getString("description");
				String user_id_json = result.getString("id_user");
				

				JSONObject jobj = new JSONObject();
				jobj.put("id_route", id_json);
				jobj.put("name", name_json);
				jobj.put("description", desc_json);
				jobj.put("start_Id", starting_id);
				jobj.put("end_Id", ending_id);
				jobj.put("id_user", user_id_json);
				
				if(start.next()) { 
					JSONObject jobj4 = new JSONObject();
					JSONObject jobj3 = new JSONObject();
					jobj4.put("country", start.getString("country"));
					jobj4.put("city", start.getString("city"));
					jobj4.put("street", start.getString("street"));
					jobj4.put("postcode", start.getString("postcode"));
					jobj4.put("house_number", start.getString("housenumber"));
					jobj4.put("id_address", start.getString("id_address"));
					jobj3.put("address",jobj4);
					jobj.put("start", jobj3);
					System.out.println(jobj4);

				}
				if(end.next()) { 
					JSONObject jobj5 = new JSONObject();
					JSONObject jobj3 = new JSONObject();
					jobj5.put("country", end.getString("country"));
					jobj5.put("city", end.getString("city"));
					jobj5.put("street", end.getString("street"));
					jobj5.put("postcode", end.getString("postcode"));
					jobj5.put("id_address", end.getString("id_address"));
					jobj5.put("house_number", end.getString("housenumber"));
					jobj3.put("address",jobj5);
					jobj.put("end", jobj3);
				}
				
				j.append("route",jobj);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(j.toString()).build();
		
	}
	
	@GET
	@Path("/getUserById/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserById(@PathParam("id") int id) throws JSONException, SQLException {

		JSONObject j = new JSONObject();
		
		System.out.println("...Get User By ID ");
		
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			ResultSet result = provider.querySelectDB("SELECT DISTINCT ON (u.id_user) * FROM BASIC_USER u"
					+ " LEFT JOIN ADDRESS a USING (id_address) "
					+ "WHERE u.id_user =" + id					
					+ " ORDER BY u.id_user, a.id_address" );
			System.out.println(result);
		
			
			
			System.out.println("this" + result.getClass().getName());
			
			while (result.next()) {	
				
				JSONObject jobj = new JSONObject();
				jobj.put("id_user", result.getString("id_user"));
				jobj.put("first_name", result.getString("first_name"));
				jobj.put("last_name", result.getString("last_name"));
				jobj.put("dob", result.getString("dob"));
				
				JSONObject jobj2 = new JSONObject();
				jobj2.put("city", result.getString("city"));
				jobj2.put("country", result.getString("country"));
				jobj2.put("id_address", result.getString("id_address"));
				
				jobj.put("address", jobj2);
				j.append("user",jobj);

			}


		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(j.toString()).build();

	}
	
	
	@GET
	@Path("getChat/{chat_id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAllChat(@PathParam("chat_id") int id_chat) throws JSONException {

		JSONObject jobj1 = new JSONObject();
		JSONArray jArray = new JSONArray();

		System.out.println("...getAllChat");

		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			ResultSet result = provider
					.querySelectDB("SELECT * from message "
							+ "where id_chat="+id_chat);

			while(result.next()) {
				System.out.println("this friend chat" + result.getString("message"));

				JSONObject jobj = new JSONObject();
				jobj.put("id_chat", result.getString("id_chat"));
				jobj.put("id_user", result.getString("id_user"));
				jobj.put("id_message", result.getString("id_message"));
				jobj.put("time_created", result.getString("time_created"));
				jobj.put("message", result.getString("message"));

				jArray.put(jobj);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();
		response.put("Chat", jArray);
		return Response.status(200).entity(response.toString()).build();

	}

	@GET
	@Path("/getAddress")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getAddress() throws JSONException {
		JSONObject jobj1 = new JSONObject();

		System.out.println("...get All Addresses");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			ResultSet result = provider.querySelectDB("SELECT id_address, country, city FROM ADDRESS ");
			System.out.println("this" + result.getClass().getName());

			JSONArray jArray = new JSONArray();
			while (result.next()) {
				String id_json = result.getString("id_address");
				String country = result.getString("country");
				String city_json = result.getString("city");
				System.out.println(result);

				JSONObject jobj = new JSONObject();
				jobj.put("id_address", id_json);
				jobj.put("country", country);
				jobj.put("city", city_json);
				jArray.put(jobj);

			}

			jobj1.put("Address", jArray);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("Address Display", jobj1);

		return Response.status(200).entity(response.toString()).build();

	}

	@GET
	@Path("/getEventById/{id}")
	
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getEventById(@PathParam("id") int id) throws JSONException, SQLException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject jobj = new JSONObject();

		System.out.println("...Get Event By ID ");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			Statement statement = provider.getConnection().createStatement();
			ResultSet result = statement.executeQuery("SELECT * FROM EVENT WHERE id_event=" + id);
			System.out.println("...Get Event By ID ");

			System.out.println("this" + result.getClass().getName());

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
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

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

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement statement = provider.getConnection().prepareStatement(
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

	@POST
	@Path("/changePassword")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response changePassword(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("user_id") && JSONreq.has("oldPassword") && JSONreq.has("newPassword")
				&& JSONreq.has("repeatNewPassword") 
//          && (JSONreq.getString("newPassword").equals(JSONreq.getString("repeatNewPassword")))
		) {
			try {
				int user_id = (int) JSONreq.get("user_id");
				String oldPassword = JSONreq.getString("oldPassword");
				String newPassword = JSONreq.getString("newPassword");
				String repeatNewPassword = JSONreq.getString("repeatNewPassword");

				System.out.println("...changePassword:" + newPassword + "...checkUser Id:" + user_id);
				
				try {

//					Statement statement = conn.createStatement();
//					
//					ResultSet result = statement.executeQuery("UPDATE USER_REG SET password =? WHERE email='"+ email +"'" + "AND password='" + oldPassword + "'", newPassword);

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					
					PreparedStatement statement = provider.getConnection()
							.prepareStatement("UPDATE USER_REG SET password =? WHERE id_user='" + user_id + "'"
									+ "AND password='" + oldPassword + "'");

					statement.setString(1, newPassword);
					int count = statement.executeUpdate();
					statement.closeOnCompletion();
					JSONObject response = new JSONObject();
					if (count > 0) {

						response.put("passwordUpdated", "successfullUpdation");

						return Response.status(200).entity(response.toString()).build();

					} else {
						response.put("passwordUpdated", "notUpdated");

						return Response.status(200).entity(response.toString()).build();
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

//				JSONObject response = new JSONObject();
//				response.put("passwordUpdated", "successfullUpdation");
//
//				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@POST
	@Path("/deleteEvent")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteEvent(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...Delete Event Request");

		if (JSONreq.has("id_event")) {
			try {

				int eventid = (int) JSONreq.get("id_event");

				System.out.println("...Delete Event ID" + eventid);

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					
					//step by step deletion from multiple tables
					//with the step by step information on the console it's more understandable what exactly got deleted
					
					//selects the address of the event that needs to be deleted
					PreparedStatement statementAddressSelect = provider.getConnection()
							.prepareStatement("SELECT ID_ADDRESS FROM EVENT WHERE id_event="+ eventid);
					ResultSet rsAddress = statementAddressSelect.executeQuery();
					int addressId = 0;
					while (rsAddress.next()) {
						addressId = rsAddress.getInt("id_address");
					}
					
					//correct row in event table gets deleted
					PreparedStatement statement = provider.getConnection()
							.prepareStatement("DELETE FROM EVENT WHERE id_event=" + eventid);

					statement.executeUpdate();
					statement.closeOnCompletion();
					
					System.out.println("Event with the ID " + eventid + " got deleted");
					
					//deletes the correct row in address table
					PreparedStatement statementAddressDelete = provider.getConnection()
							.prepareStatement("DELETE FROM ADDRESS WHERE id_address=" + addressId);

					statementAddressDelete.executeUpdate();
					statementAddressDelete.closeOnCompletion();
					
					System.out.println("Address with the ID " + addressId + " got deleted");

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("Event Deletion", "successfullDeletoion");

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
	@Path("/createRoute")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response createRoute(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...createRoute");
		try {
			DatabaseProvider.getInstance(context);

			NewRouteRequest newRoute = new NewRouteRequest(JSONreq);
			RouteDao dao = new RouteDaoImplementation();
			Route route = dao.createRoute(newRoute.getStartAddress(), newRoute.getEndAddress(), newRoute.getRoute());

			JSONObject response = new JSONObject();
			
			response.put("success", true);
			response.put("route_id", route.getId());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			System.out.println("InvalidRequestbody");
			return Response.status(400).entity("InvalidRequestBody").build();
		}
	}

	@POST
	@Path("/searchUser")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response searchUserByMail(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		JSONObject allUser = new JSONObject();
		String input;

		System.out.println("...Get User By Mail or Name");
		try {

			UserDaoImplementation dao = new UserDaoImplementation();

			int userId;
			if (JSONreq.has("id") && JSONreq.has("input")) {
				userId = JSONreq.getInt("id");
				input = JSONreq.getString("input");
			} else {
				return Response.status(500).entity("InvalidRequestBody".toString()).build();
			}

			List<BasicUser> searchResults = dao.searchUser(input, userId);

			if (searchResults != null && searchResults.isEmpty()) {
				allUser.put("foundUser", "unsuccessful");
			} else {
				allUser.put("foundUser", "successful");
				allUser.put("user", BasicUser.serializeUserList(searchResults));
			}

			return Response.status(200).entity(allUser.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return Response.status(500).entity("InvalidRequestBody".toString()).build();
	}

	@POST
	@Path("/editUserInfo")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response editUserInfo(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);

		if (JSONreq.has("last_name") && JSONreq.has("country") && JSONreq.has("state") && JSONreq.has("city")
				&& JSONreq.has("street") && JSONreq.has("postcode") && JSONreq.has("housenumber")&& JSONreq.has("user_id")) {
			
			try {

				int user_id = JSONreq.getInt("user_id");			
				String lname = JSONreq.getString("last_name");
				String country = JSONreq.getString("country");
				String state = JSONreq.getString("state");
				String city = JSONreq.getString("city");
				String street = JSONreq.getString("street");
				Integer postcode = JSONreq.getInt("postcode");
				String housenumber = JSONreq.getString("housenumber");

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					
					String selectSQL3 = "SELECT id_address FROM BASIC_USER WHERE id_user =" + user_id;
					PreparedStatement preparedStatement3 = provider.getConnection().prepareStatement(selectSQL3);
					ResultSet rs3 = preparedStatement3.executeQuery();
					int addressId = 0;
					while (rs3.next()) {
						addressId = rs3.getInt("ID_ADDRESS");
					}

					PreparedStatement s1 = provider.getConnection().prepareStatement(
							"UPDATE ADDRESS SET country =?, state =?, city =?, street =?, postcode =?, housenumber =? WHERE id_address = " + addressId);

					PreparedStatement s2 = provider.getConnection().prepareStatement(
							"UPDATE BASIC_USER SET last_name =? WHERE id_user="+user_id);
					
					s1.setString(1, country);
					s1.setString(2, state);
					s1.setString(3, city);
					s1.setString(4, street);
					s1.setInt(5, postcode);
					s1.setString(6, housenumber);
					s1.executeUpdate();
					s1.closeOnCompletion();

					s2.setString(1, lname);
					s2.executeUpdate();
					s2.closeOnCompletion();
					
					System.out.println("UserInfoGotUpdated");
					
					JSONObject response = new JSONObject();
					response.put("userInfoUpdate", "successful");
					System.out.println("Response: " + response.toString());
					return Response.status(200).entity(response.toString()).build();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@POST
	@Path("/addFriend")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addFriend(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...addFriendRequest");

		// TODO not tested yet
		if (JSONreq.has("idUser") && JSONreq.has("idFollower")) {

			JSONObject response = new JSONObject();
			DatabaseProvider db = DatabaseProvider.getInstance(context);
			int idUser = JSONreq.getInt("idUser");
			int idFollower = JSONreq.getInt("idFollower");
			try {
				db.queryInsertDB("INSERT into FRIENDSHIP VALUES (?,?)", idUser, idFollower);
				response.put("friendship", "successful");
			} catch (Exception ex) {
				ex.printStackTrace();
				response.put("friendship", "internalProblems");
			}
			return Response.status(200).entity(response.toString()).build();
		}
		return Response.status(400).entity("InvalidRequestBody").build();
	}

	@GET
	@Path("/get_event_type")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getEventType() throws JSONException, SQLException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject response = new JSONObject();
		System.out.println("Get Event Type... ");
		try {
			// Setting the DB context in case its not set
			DatabaseProvider.getInstance(context);
			EventTypeDao eventType = new EventTypeDaoImplementation();
			List<EventType> eventTypeList = eventType.getEventTypes();
			response.put("result", EventType.serializeEventTypeList(eventTypeList));
			System.out.println("Response: " + response.toString());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();

			response.put("error_code", "failed_to_get_event_type");
			response.put("description", "Failed to get event types");
			System.out.println("Response: " + response.toString());
			return Response.status(500).entity(response.toString()).build();
		}
	}

	/**
	 * Get UserID for user with given mail
	 *
	 * @param mail
	 * @return userID
	 * @throws JSONException
	 * @throws SQLException 
	 */
	@GET
	@Path("/getUserID/{mail}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserID(@PathParam("mail") String mail) throws JSONException, SQLException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject jobj = new JSONObject();

		System.out.println("...Get UserID By Email ");
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);
			ResultSet result = provider.querySelectDB("SELECT id_user FROM user_reg WHERE email = ?", mail);

			if (result.next()) {
				jobj.put("user_id", result.getString("id_user"));
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		JSONObject response = new JSONObject();

		response.put("getUserID", jobj);

		return Response.status(200).entity(response.toString()).build();

	}

	/**
	 * Returns list with every friend
	 *
	 * @param userId
	 * @return
	 * @throws JSONException
	 * @throws SQLException 
	 */
	@GET
	@Path("/getFriends/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getFriends(@PathParam("id") int userId) throws JSONException, SQLException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject response = new JSONObject();
		System.out.println("Get Friends... ");
		try {
			// Setting the DB context in case its not set
			DatabaseProvider.getInstance(context);
			UserDao userD = new UserDaoImplementation();
			List<BasicUser> friends = userD.getFriends(userId);
			response.put("friends", BasicUser.serializeUserList(friends));
			System.out.println("Response: " + response.toString());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			response.put("error_code", "failed_to_get_friends");
			response.put("description", "Failed to get Friends");
			System.out.println("Response: " + response.toString());
			return Response.status(500).entity(response.toString()).build();
		}
	}
	
	
	@GET
	@Path("/getFriendsRoutes/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getFriendsRoutes(@PathParam("id") int id) throws JSONException, SQLException {

		JSONObject j = new JSONObject();

		System.out.println("...Get Firends By ID ");
		
		try {
			DatabaseProvider provider = DatabaseProvider.getInstance(context);

			ResultSet result = provider.querySelectDB("SELECT * From FRIENDSHIP WHERE id_user1 =" + id);
			System.out.println(result);
						
			while (result.next()) {	
				String id_user2 = result.getString("id_user2");				
				ResultSet basicUser = provider.querySelectDB("Select * from BASIC_USER AS b Where b.id_user =" + id_user2 );
				
				JSONObject jobj = new JSONObject();
				JSONObject jobj1 = new JSONObject();
				JSONObject jobj2 = new JSONObject();

				jobj.put("id_user1", result.getString("id_user2"));
				
				if(basicUser.next()) { 
					jobj1.put("id", basicUser.getString("id_user"));
					jobj1.put("first_name", basicUser.getString("first_name"));
					jobj1.put("last_name", basicUser.getString("last_name"));
					
					ResultSet routes = provider.querySelectDB("Select * from ROUTE AS r Where r.id_user =" + basicUser.getString("id_user") );
					while(routes.next()) {
						jobj2.put("id_route", routes.getString("id_route"));
						jobj2.put("name", routes.getString("name"));
						jobj2.put("description", routes.getString("description"));
						jobj2.put("id_user", routes.getString("id_user"));
						jobj1.put("route", jobj2);
						
					}
					jobj.put("route", jobj1);
				}
				
				j.append("friend",jobj1);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(j.toString()).build();

	}
	

	/**
	 * Returns user information.
	 *
	 * @param userId
	 * @return
	 * @throws JSONException
	 * @throws SQLException 
	 */
	@GET
	@Path("/getUserInfo/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getUserInfo(@PathParam("id") int userId) throws JSONException, SQLException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject response = new JSONObject();
		System.out.println("Get Additional User Info... ");
		try {
			// Setting the DB context in case its not set
			DatabaseProvider.getInstance(context);
			UserDao userD = new UserDaoImplementation();
			BasicUser userInfo = userD.getAdditionalInfo(userId);

			response.put("UserInfo", BasicUser.serializeUser(userInfo));
			System.out.println("Response: " + response.toString());
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
			response.put("error_code", "failed_to_get_user_info");
			response.put("description", "Failed to get userInfo");
			System.out.println("Response: " + response.toString());
			return Response.status(500).entity(response.toString()).build();
		}
	}
	
	@PUT
	@Path("/resetPassword")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response resetPassword(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);
		
		JSONObject response = new JSONObject();
		
		JSONObject JSONreq = new JSONObject(urlReq);
	
		try {
			String currentEmail = JSONreq.getString("reset_email");
			UserDao userDao = new UserDaoImplementation();
			System.out.println("Reset User Password");
			String newPassword = userDao.changeUserPassword(currentEmail);
			if (newPassword == null) {
				return Response.status(200).entity(response.toString()).build();
			}
			// send registration mail
			Mailer mailer = new Mailer(context);
			boolean messageSent = mailer.sendResetPasswordMail(currentEmail, newPassword);
			if (!messageSent) {
				System.out.println("Failed to send mail?!");
			}
			
			return Response.status(200).entity(response.toString()).build();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		response.put("success", true);
		return Response.status(200).entity(response.toString()).build();
	}

	/**
	 * Saves a chat message in the database
	 *
	 * @param urlReq with message, time, id_chat, id_user
	 * @return Response with status 200 and message successful or status 400
	 *         with InvalidRequestBody-message.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 * @throws JSONException
	 * @throws UnsupportedEncodingException
	 */
	@PUT
	@Path("/saveMessage")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response saveMessage(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		System.out.println("New Message...");

		JSONObject response = new JSONObject();

		JSONObject JSONreq = new JSONObject(urlReq);

		try {

			if(! (JSONreq.has("message") && JSONreq.has("time") && JSONreq.has("id_chat") && JSONreq.has("id_user"))) {
				return Response.status(400).entity("InvalidRequestBody").build();
			}

			ChatDao chatD = new ChatDaoImplementation();
			chatD.saveMessage(new Message(JSONreq.getInt("id_chat"), JSONreq.getInt("id_user"), Timestamp.valueOf(JSONreq.getString("time")), JSONreq.getString("message")));
			response.put("saveMessage", "successful");

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(response.toString()).build();
	}


	@PUT
	@Path("/loadChat")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getChatId(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		System.out.println("Load Chat...");

		JSONObject response = new JSONObject();

		JSONObject JSONreq = new JSONObject(urlReq);

		try {

			if(!JSONreq.has("id_users")) {
				return Response.status(400).entity("InvalidRequestBody").build();
			}

			ChatDao chatD = new ChatDaoImplementation();
			int chatId = chatD.loadChat(JSONreq.getJSONArray("id_users"));
			response.put("id_chat", chatId);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Response.status(200).entity(response.toString()).build();
	}
	
	@POST
	@Path("/deleteRoute")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response deleteRoute(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...Delete Route Request");

		if (JSONreq.has("id_route")) {
			try {

				int routeid = (int) JSONreq.get("id_route");
				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					
					//this is a step by step deletion with multiple statements, it's for checking reasons: rows of 4 different tables get deleted
					//with the step by step deletion and the information of the console it's more understandable what exactly got deleted
					
					//selects the startpoint of the route that needs to be deleted
					PreparedStatement statementStartpointSelect = provider.getConnection()
							.prepareStatement("SELECT STARTPOINT FROM ROUTE WHERE id_route="+ routeid);
					ResultSet rsStartpoint = statementStartpointSelect.executeQuery();
					int addressIdStartpoint = 0;
					while (rsStartpoint.next()) {
						addressIdStartpoint = rsStartpoint.getInt("startpoint");
					}
					
					
					//selects endpoint of route that needs to be deleted
					PreparedStatement statementEndpointSelect = provider.getConnection()
							.prepareStatement("SELECT ENDPOINT FROM ROUTE WHERE id_route="+ routeid);
					ResultSet rsEndpoint = statementEndpointSelect.executeQuery();
					int addressIdEndpoint = 0;
					while (rsEndpoint.next()) {
						addressIdEndpoint = rsEndpoint.getInt("endpoint");
					}
					
					
					//deletes the according stop that is in relationship to the route that needs to be deleted
					PreparedStatement statementStop = provider.getConnection()
							.prepareStatement("DELETE FROM STOP WHERE id_route=" + routeid);

					statementStop.executeUpdate();
					statementStop.closeOnCompletion();
					//no need for printing out information on the console, id_route in stop table is a primary key
					//therefore one automatically receives console information if the correct stop didn't get deleted
					
					//deletes the according liked_route 
					PreparedStatement statementLiked = provider.getConnection()
							.prepareStatement("DELETE FROM LIKED_ROUTE WHERE id_route=" + routeid);

					statementLiked.executeUpdate();
					statementLiked.closeOnCompletion();
					//no need for printing out information on the console, id_route in liked_route table is a primary key
					//therefore one automatically receives console information if the correct liked_route didn't get deleted
					
					//deletes the correct row of the route table					
					PreparedStatement statementRoute = provider.getConnection()
							.prepareStatement("DELETE FROM ROUTE WHERE id_route=" + routeid);

					statementRoute.executeUpdate();
					statementRoute.closeOnCompletion();
					
					System.out.println("Route with the ID " + routeid + " got deleted");
					
					//deletes startpoint in address table
					PreparedStatement statementStartpointDelete = provider.getConnection()
							.prepareStatement("DELETE FROM ADDRESS WHERE id_address=" + addressIdStartpoint);

					statementStartpointDelete.executeUpdate();
					statementStartpointDelete.closeOnCompletion();
					
					System.out.println("Startpoint with the ID " + addressIdStartpoint + " got deleted");
					
					//deletes endpoint in the address table
					PreparedStatement statementEndpointDelete = provider.getConnection()
							.prepareStatement("DELETE FROM ADDRESS WHERE id_address=" + addressIdEndpoint);

					statementEndpointDelete.executeUpdate();
					statementEndpointDelete.closeOnCompletion();
					
					System.out.println("Endpoint with the ID " + addressIdEndpoint + " got deleted");

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("Route Deletion", "successfullDeletion");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}
	
	@POST
	@Path("/updateRoute")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updateRoute(String urlReq)
			throws ClassNotFoundException, SQLException, JSONException, UnsupportedEncodingException {
		// Setting the DB context in case its not set
		DatabaseProvider.getInstance(context);

		JSONObject JSONreq = new JSONObject(urlReq);
		System.out.println("...updateRouteRequest");

		if (JSONreq.has("id_route") && JSONreq.has("name") && JSONreq.has("description")) {
			try {

				int routeid = (int) JSONreq.get("id_route");
				String routename = JSONreq.getString("name");
				String routedescription = JSONreq.getString("description");

				System.out.println("...updateRoute:" + routename + "...updateRoute Id:" + routeid);

				try {

					DatabaseProvider provider = DatabaseProvider.getInstance(context);
					PreparedStatement statement = provider.getConnection().prepareStatement(
							"UPDATE Route SET name =?, description =? WHERE id_route="
									+ routeid);

					statement.setString(1, routename);
					statement.setString(2, routedescription);
					statement.executeUpdate();
					statement.closeOnCompletion();

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				JSONObject response = new JSONObject();

				response.put("routeUpdate", "successfullUpdation");

				return Response.status(200).entity(response.toString()).build();

			} catch (Exception e) {
				System.out.println("Wrong JSONFormat:" + e.toString());
			}
		}
		System.out.println("InvalidRequestbody");
		return Response.status(400).entity("InvalidRequestBody").build();
	}

}