package com.jwt.DataBaseConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Date;

import javax.servlet.ServletContext;

public class DatabaseProvider {
	private static DatabaseProvider instance;
	private Connection connection;
	Config config = Config.getInstance();
	private String postgresURL = "jdbc:postgresql://" + config.host + ":" + config.port + "/" + config.database;

	private DatabaseProvider() throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
			this.connection = DriverManager.getConnection(postgresURL, config.username, config.password);
		} catch (SQLException ex) {
			System.out.println("Database Connection Creation Failed : " + ex.getMessage());
		} catch (ClassNotFoundException ex) {
			System.out.println("Database Connection Creation Failed : " + ex.getMessage());
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public static synchronized DatabaseProvider getInstance(ServletContext context) throws SQLException {
		if (instance == null) {
			Config conf = Config.getInstance();
			conf.setContext(context);
			instance = new DatabaseProvider();
		} else if (instance.getConnection().isClosed()) {
			Config conf = Config.getInstance();
			conf.setContext(context);
			instance = new DatabaseProvider();
		}
		return instance;
	}

	public static synchronized DatabaseProvider getInstance() {
		if (instance == null) {
			System.out.println(
					"Could not find the context for the DatabaseProvider instance, make sure you called DatabaseProvider.getInstance(context) in the upstream code");
		}
		return instance;
	}

	public PreparedStatement queryInsertDB(String query, Object... arguments) {
		System.out.println("...... Execute Insert Query: " + query);
		try {
			if (this.connection.isClosed()) {
				this.connection = DriverManager.getConnection(postgresURL, config.username, config.password);
			}

			PreparedStatement preparedStatement = this.connection.prepareStatement(query,
					Statement.RETURN_GENERATED_KEYS);

			preparedStatement = fillPreparedStatement(preparedStatement, arguments);

			if (preparedStatement.executeUpdate() < 0) {
				throw new SQLException("Insert into DB failed, no rows affected.");
			}
			preparedStatement.closeOnCompletion();
			return preparedStatement;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("...... Insert into database done");
		return null;
	}

	public ResultSet querySelectDB(String query, Object... arguments) {
		ResultSet rs = null;
		System.out.println("...... Execute Select Query: " + query);

		try {
			if (this.connection.isClosed()) {
				this.connection = DriverManager.getConnection(postgresURL, config.username, config.password);
			}
			PreparedStatement ur = this.connection.prepareStatement(query);

			ur = fillPreparedStatement(ur, arguments);
			rs = ur.executeQuery();
			ur.closeOnCompletion();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (this.connection != null) {
				try {
					this.connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		System.out.println("...... Select Statement Successful");
		return rs;
	}
	
	public ResultSet querySelectDB(String query){
		return querySelectDB(query, true);
	}
	
	public ResultSet querySelectDB(String query, boolean closeConnection) {
		ResultSet rs = null;
		System.out.println("...... Execute Select Query: " + query);
		try {
			if (this.connection.isClosed()) {
				this.connection = DriverManager.getConnection(postgresURL, config.username, config.password);
			}
			Statement ss = this.connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			rs = ss.executeQuery(query);
			ss.closeOnCompletion();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (this.connection != null && closeConnection) {
				try {
					this.connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("...... Select Statement Successful");

		return rs;
	}

	private PreparedStatement fillPreparedStatement(PreparedStatement ps, Object... arguments) {
		try {
			for (int i = 0; i < arguments.length; i++) {
				if (arguments[i] instanceof String) {
					String string = (String) arguments[i];
					ps.setString(i + 1, string);
				} else if (arguments[i] instanceof Integer) {
					int integer = (Integer) arguments[i];
					ps.setInt(i + 1, integer);
				} else if (arguments[i] instanceof Double) {
					Double doubleValue = (Double) arguments[i];
					ps.setDouble(i + 1, doubleValue);
				} else if (arguments[i] instanceof Date) {
					Date date = (Date) arguments[i];
					ps.setDate(i + 1, date);
				} else if (arguments[i] instanceof Time) {
					Time time = (Time) arguments[i];
					ps.setTime(i + 1, time);
				}else if(arguments[i] instanceof Timestamp) { 
					Timestamp timestamp = (Timestamp) arguments[i];
					ps.setTimestamp(i+1, timestamp);
				}else {
					// TODO: improve this with additional instances of
					ps.setString(i + 1, (String) arguments[i]);
				}
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		
		return ps;
	}
}