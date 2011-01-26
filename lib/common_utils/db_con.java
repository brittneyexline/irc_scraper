package common_utils;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Andrew G. West - db_con.java - Streamlining DB-connectivity for the project
 */
public class db_con{

	// **************************** PUBLIC FIELDS ****************************
	
	/**
	 * Enumeration listing the DBs which can be connected to.
	 */
	public enum DBS{presta_stiki, presta_spam, wiki_link_spam, dmoz, 
		safe_browsing};
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Retrieve a connection to some database.
	 * @param DBS db Specific database to which connected should be made
	 * @return Connection to DB, or NULL if there was a connection error
	 */
	public static Connection get_con(DBS db){
			
		String url, user, pass;
		url = "jdbc:mysql://hincapie.cis.upenn.edu:3306/safe_browsing";
		user = "seniordesign";
		pass = "qtm2009";
		
		Connection con = null; // Then proceed to connect
		try{ // In the case of an error, just return NULL
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection(url, user, pass);
		} catch(Exception e){
			System.err.println("Error opening DB connection");
			e.printStackTrace();
		} // Also output a message to system.err
		return con;	
	}
	
}
