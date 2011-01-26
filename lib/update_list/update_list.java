package update_list;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;

import common_utils.db_con;
import common_utils.gen_utils;
import common_utils.db_con.DBS;
import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew G. West - update_list.java - Driver class. Update all lists
 * provided as part of the Google Safe Browsing project. Esentially this
 * is diff-processing, inserting new entries, and migrating expired ones
 * to archival tables.
 */
public class update_list{
		
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Update local copies of Safe Browsing blacklists. Google suggest
	 * this be done once every 30 minutes, presumable via a 'cron' task. 
	 * @param args No arguments are required by this method.
	 */
	public static void main(String[] args) throws Exception{
		process_list(LIST_TYPE.phishing);
		process_list(LIST_TYPE.malware);
	}

	
	// *************************** PRIVATE METHODS ***************************

	/**
	 * Extremely high-level processing call. 
	 * @param list_type Provide the Google list to be processed/updated.
	 */
	private static void process_list(LIST_TYPE list_type) throws Exception{
		
		long timestamp = gen_utils.cur_unix_time();
		Connection con = db_con.get_con(DBS.safe_browsing);
		db_sb_update db_handler = new db_sb_update(con, list_type);
		
		int local_version = db_handler.get_local_version_id();
		String content = fetch_url(api_url(list_type, local_version));
		if(content.equals("")){
			db_handler.shutdown();
			con.close();
			System.exit(0);
		} // If at most recent version, diff will be completely empty
		
		String hash; int new_ver_id = -1; int db_adds = 0;
		String[] content_lines = content.split("\n"); // Split on lines
		for(int i=0; i < content_lines.length; i++){
		
			content_lines[i] = content_lines[i].trim();
			if(content_lines[i].equals(""))
				continue; // The whitespace in this doc is quite odd
			
			if(i == 0){ // Example first line: "[goog-black-hash 1.372 update]"
				
				if(!content_lines[i].contains("update")){
					throw new Exception("PANIC: Google requesting list to " +
							"be flushed rather than updated. If not first " +
							"run, this behavior is not supported");
				} // Nature of update being pulled should be predictable
				
				new_ver_id = Integer.parseInt(
						content_lines[i].split(" ")[1].split("\\.|\\]")[1]);
				
			} else{ // Example normal line: "+00386acdf6010e6472b3e34b2f8f0872"

				hash = content_lines[i].substring(1);				
				if(content_lines[i].charAt(0) == '+'){
					db_adds++;
					db_handler.new_active_entry(hash, timestamp);
				} else if(content_lines[i].charAt(0) == '-')
					db_handler.close_active(hash, timestamp);

			} // First line of file handled differently than all others
		} // Iterate over all entries in the diff file
		
			// MIGRATE ALL ROWS THAT WERE CLOSED
		int mig_size = db_handler.archive_migrate();
		
			// LEAVE LOG ENTRY
		db_handler.insert_log_entry(new_ver_id, timestamp, db_adds, mig_size);
		
			// CLOSE UP AND SHUTDOWN
		db_handler.shutdown();
		con.close();
	}
	
	/**
	 * Create an API URL to retrieve blacklist copies or changes. 
	 * @param list_type Blacklist type to process ("malware" or "phishing")
	 * @param last_version Last version of 'list_type' for which a local 
	 * copy is stored, this is the basis on which diff's are computed
	 * @return URL to fetch, containing desired blacklist copy or diff
	 */
	private static String api_url(LIST_TYPE list_type, int last_version){
		String url = " http://sb.google.com/safebrowsing/update?client=api";
		url += "&apikey=" + gen_utils.API_KEY;
		if(list_type.equals(LIST_TYPE.phishing))
			url += "&version=goog-black-hash";
		else if(list_type.equals(LIST_TYPE.malware))
			url += "&version=goog-malware-hash";
		url += ":" + gen_utils.MAJ_API_VERSION;
		url += ":" + last_version;
		return(url);
	}

	/**
	 * Read the contents of some URL into a string.
	 * @param str_url String version of the URL to be fetched
	 * @return String created over InputStream over 'str_url'
	 */
	private static String fetch_url(String str_url) throws Exception{
		URL url = new URL(str_url);
		URLConnection conn = url.openConnection();
		InputStream is = conn.getInputStream();
		
		int read; 
		final char[] buffer = new char[0x10000]; // 64k buffer
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(is, "UTF-8");
		do{ // Read InputStream into a String for ease-of-ise
		  read = in.read(buffer, 0, buffer.length);
		  if(read > 0)
			  out.append(buffer, 0, read);
		} while (read>=0);
		return(out.toString());
	}
	
}
