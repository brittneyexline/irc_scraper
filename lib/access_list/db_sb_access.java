package access_list;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew G. West - db_sb_access.java - Class handling all DB-access
 * as it pertains to local retrieval of Google Safe Browsing data. Note
 * that general storage/insertion/update uses a different handler.
 */
public class db_sb_access{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL corresponding to a simple active lookup for a hash.
	 */
	private PreparedStatement pstmt_is_active;
	
	/**
	 * "Base table" over which this instance should operate. Since the format
	 * of different lists is basically identical, this variable allows this
	 * class to be generalized -- just pointing to different DB tables.
	 */
	private final String BASE_TBL;
	
	/**
	 * Connection to the local [safe_browsing] database.
	 */
	private Connection con;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct a [db_sb_access] object.
	 * @param con Connection to the [safe_browsing] database
	 * @param list_type Blacklist being handled by this instance -- such
	 * that all SQL points to the proper set of tables
	 */
	public db_sb_access(Connection con, LIST_TYPE list_type) 
			throws Exception{
		this.con = con;
		if(list_type.equals(LIST_TYPE.phishing))
			BASE_TBL = "phishing";
		else BASE_TBL = "malware";
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
		
	/**
	 * Determine if some hash is actively listed on a blacklist.
	 * @param hash Hash-code corresponding to potential blacklist item
	 * @return TRUE if 'hash' is active on the Google list type provided
	 * at construction. FALSE, otherwise.
	 */
	public boolean hash_is_active(String hash) throws Exception{
		
		boolean is_active = false;
		pstmt_is_active.setString(1, hash);
		ResultSet rs = pstmt_is_active.executeQuery();
		if(rs.next() && rs.getInt(1) > 0)
			is_active = true;
		rs.close();
		return(is_active);
	}
	
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_is_active.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		String is_active = "SELECT COUNT(*) FROM " + BASE_TBL + " WHERE HASH=?";
		pstmt_is_active = con.prepareStatement(is_active);
	}

}
