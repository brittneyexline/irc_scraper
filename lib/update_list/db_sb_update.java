package update_list;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew G. West - db_sb_update - Class handling all DB-access
 * as it pertains to local storage of Google Safe Browsing data. Note
 * that general access uses a different handler.
 */
public class db_sb_update{
	
	// **************************** PRIVATE FIELDS ***************************
	
	/**
	 * SQL corresponding to the addition of a new entry to an active list.
	 */
	private PreparedStatement pstmt_new_entry;
	
	/**
	 * SQL corresponding to the removal of an entry from an active list.
	 */
	private PreparedStatement pstmt_close_entry;
	
	/**
	 * SQL adding an entry to the archival table.
	 */
	private PreparedStatement pstmt_archive_entry;
	
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
	 * Construct a [db_sb_update] object.
	 * @param con Connection to the [safe_browsing] database
	 * @param list_type Blacklist being handled by this instance -- such
	 * that all SQL points to the proper set of tables
	 */
	public db_sb_update(Connection con, LIST_TYPE list_type) 
			throws Exception{
		this.con = con;
		if(list_type.equals(LIST_TYPE.phishing))
			BASE_TBL = "phishing";
		else BASE_TBL = "malware";
		prep_statements();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Record the entry of a new "item" onto the active blacklist. 
	 * @param hash Hash-code which is the new blacklist item
	 * @param ts_in Time at which 'hash' was first observed as active
	 */
	public void new_active_entry(String hash, long ts_in) throws Exception{
		pstmt_new_entry.setString(1, hash);
		pstmt_new_entry.setLong(2, ts_in);
		pstmt_new_entry.executeUpdate();
	}

	/**
	 * Record the removal of an "item" from the active blacklist
	 * @param hash Hash-code which uniquely identifies BL items
	 * @param ts_out Time at which the removal was first observed
	 */
	public void close_active(String hash, long ts_out) throws Exception{
		pstmt_close_entry.setLong(1, ts_out);
		pstmt_close_entry.setString(2, hash);
		pstmt_close_entry.executeUpdate();
	}
	
	/**
	 * Complete the DB-migration from "active" to "archival" tables.
	 * @return Quantity of rows migrated
	 */
	public int archive_migrate() throws Exception{

			// First move all the rows
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT HASH,TS_IN,TS_OUT FROM " + 
				BASE_TBL + " WHERE TS_OUT!=-1");
		int num_rows_moved = 0;
		while(rs.next()){
			num_rows_moved++;
			pstmt_archive_entry.setString(1, rs.getString(1));
			pstmt_archive_entry.setLong(2, rs.getLong(2));
			pstmt_archive_entry.setLong(3, rs.getLong(3));
			pstmt_archive_entry.executeUpdate();
		} // Migrate all "expired" rows
		rs.close();
		stmt.close();
		
			// Then delete them from the source table
		Statement stmt2 = con.createStatement();
		stmt2.executeUpdate("DELETE FROM " + BASE_TBL + " WHERE TS_OUT!=-1");
		stmt2.close();
		return(num_rows_moved);
	}
	
	/**
	 * Insert a log entry summarizing a diff-processing run.
	 * @param version_id Version of a blacklist processed
	 * @param ts_proc Timestamp at which processing completed
	 * @param db_adds Number of entry additions made in run
	 * @param migration_size Number of archivals made in run
	 */
	public void insert_log_entry(int version_id, long ts_proc, int db_adds, 
			int migration_size) throws Exception{
		Statement stmt = con.createStatement();
		stmt.executeUpdate("INSERT INTO " + BASE_TBL + "_log " + "VALUES" +
				"(" + version_id + "," + ts_proc + "," + 
				db_adds + "," + migration_size + ")");
		stmt.close();
	}
	
	/**
	 * Return the blacklist version our local DB contains.
	 * @return the blacklist version our local DB contains. Negative one
	 * (-1) is returned if no log history exists.
	 */
	public int get_local_version_id() throws Exception{
		int local_vid = -1;
		Statement stmt = con.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT MAX(VERSION_ID) FROM " + 
				BASE_TBL + "_log");
		
		if(rs.next()){
			local_vid = rs.getInt(1);
			if(rs.wasNull())
				local_vid = -1;
		} // Check ResultSet. Note that NULL will parse to int as "0"

		rs.close();
		stmt.close();
		return(local_vid);
	}
		
	/**
	 * Shutdown and close all DB objects created by this instance.
	 */
	public void shutdown() throws Exception{
		pstmt_new_entry.close();
		pstmt_close_entry.close();
		pstmt_archive_entry.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Prepare all SQL statements required by this class instance. 
	 */
	private void prep_statements() throws Exception{	
		
		String new_entry = "INSERT INTO " + BASE_TBL + " VALUES (?,?,-1)";
		pstmt_new_entry = con.prepareStatement(new_entry);
		
		String close_entry = "UPDATE " + BASE_TBL + " ";
		close_entry += "SET TS_OUT=? WHERE HASH=?";
		pstmt_close_entry = con.prepareStatement(close_entry);
		
		String archive_entry = "INSERT INTO " + BASE_TBL + "_old ";
		archive_entry += "VALUES (?,?,?)";
		pstmt_archive_entry = con.prepareStatement(archive_entry);
	}

}
