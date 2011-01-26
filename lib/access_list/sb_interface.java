package access_list;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import common_utils.db_con;
import common_utils.db_con.DBS;
import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew G. West - sb_interface,java - Interface into the Safe-Browsing
 * project. Determine both active and inactive listing status of the
 * MD5 hash of some URL (and its many reduced forms).
 * 
 * Note that this class is non-static so that consistent database connections
 * can be maintained, making batch processing far-far quicker.
 */
public class sb_interface{
	
	// **************************** PRIVATE FIELDS ***************************

	/**
	 * DB-handler providing access to Google Safe-Browsing data.
	 */
	private db_sb_access db_handler; 
	
	/**
	 * Connection to the local [safe_browsing ] database.
	 */
	private Connection con;
	
	
	// ***************************** CONSTRUCTORS ****************************
	
	/**
	 * Construct an [sb_interface], intializing database structs.
	 * @param list_type Blacklist which an interface is being provided to
	 */
	public sb_interface(LIST_TYPE list_type) throws Exception{
		this.con = db_con.get_con(DBS.safe_browsing);
		this.db_handler = new db_sb_access(con, list_type); 
	}
	
	
	// ***************************** TEST HARNESS ****************************
	
	/**
	 * Driver method. A test-harness for the methods of this class.
	 * @param args No arguments are taken by this method
	 */
	public static void main(String[] args) throws Exception{
		
		sb_interface face_phishing = new sb_interface(LIST_TYPE.phishing);
		sb_interface face_malware = new sb_interface(LIST_TYPE.malware);
		
		String url = "http://malware.testing.google.test/testing/malware/";
		System.out.println("URL: " + url);
		System.out.println("Phishing: " + face_phishing.url_is_active(url));
		System.out.println("Malware: " + face_malware.url_is_active(url));
		
		face_phishing.shutdown();
		face_malware.shutdown();
	}
	
	
	// **************************** PUBLIC METHODS ***************************
	
	/**
	 * Determine if some URL is actively listed on a Google blacklist.
	 * @param url URL to check. It should be passed in the most complete 
	 * format possible. This method will perform all canonicalization and
	 * perform all attempts for any reduced forms.
	 * @return TRUE if 'url' or one of its acceptable sub-forms is actively
	 * listed. Otherwise, FALSE will be returned.
	 */
	public boolean url_is_active(String url) throws Exception{
		
		List<String> url_forms = get_url_forms(canonical_url(url));
		for(int i=0; i < url_forms.size(); i++){
			if(this.db_handler.hash_is_active(hash_from_url(url_forms.get(i))))
				return(true);
		} // Iterate over legal sub-formats of original url
		return(false);
	}
	
	/**
	 * Shutdown all database objects created by this handler.
	 */
	public void shutdown() throws Exception{
		this.db_handler.shutdown();
		this.con.close();
	}
	
	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Hash a provided String (URL) using the MD5 algorithm.
	 * @param url URL to be hashed, per Google specification
	 * @return MD5 hash (a string of hex) of 'url'
	 */
	private static String hash_from_url(String url) throws Exception{
		MessageDigest hasher = MessageDigest.getInstance("MD5");
		byte[] raw_hash = hasher.digest(url.getBytes());
		String str_hash = byte_array_to_hex_str(raw_hash);
		return(str_hash);
	}
	
	/**
	 * Convert a byte array to a hexadecimal String.
	 * @param b Array of bytes to be converted to hex.
	 * @return String in lower-case hexadecimal; the conversion of 'b'
	 */
	private static String byte_array_to_hex_str(byte[] b){
	    StringBuffer sb = new StringBuffer(b.length * 2);
	    for(int i = 0; i < b.length; i++){
	      int v = b[i] & 0xff;
	      if(v < 16)
	    	  sb.append('0');
	      sb.append(Integer.toHexString(v));
	    } // Iterate over byte aray
	    return sb.toString().toLowerCase(); // How Google represents
	}
	
	
	// *** PER J-GOOGLE-SAFEBROWSER ***
	
	/**
	 * Given a URL, un-escape all hex characters found therein.
	 * @param url URL to be processed
	 * @return Version of URL with all hex characters unescaped (into UTF-8).
	 * If any part of the process fails, NULL will be returned
	 */
	private String unescape(String url) throws Exception{
		if(url == null)
			return(null);
		
		StringBuffer text1 = new StringBuffer(url);
		int p = 0;
		while ((p = text1.indexOf("%", p)) != -1){
			char c1 = ' ';
			char c2 = ' ';
			if (++p <= text1.length() - 2){
				c1 = text1.charAt(p);
				c2 = text1.charAt(p + 1);
			} // Count the num. of chars. after percentage, then check validity
			if(!(((c1 >= '0' && c1 <= '9') || (c1 >= 'a' && c1 <= 'f') || 
					(c1 >= 'A' && c1 <= 'F')) && ((c2 >= '0' && c2 <= '9') ||
					(c2 >= 'a' && c2 <= 'f') || (c2 >= 'A' && c2 <= 'F'))))
				return(null); // Percent must be followed by two-digit hex
		} // Ensure this is a URL that URLDecoder can handle

		String unescape_url = url; // Repeatedly un-escape
		try{
			while (unescape_url.indexOf("%") != -1)
				unescape_url = URLDecoder.decode(unescape_url, "UTF-8");
		} catch(Exception e){return null;} // In case decode fails
		return(unescape_url);
	}
	
	/**
	 * Escape a string, replacing special chars with UTF-8 escape codes
	 * @param url URL to be processed
	 * @return Copy of URL, with all characters having ASCII <=32, >=127, or % 
	 * replaced with their UTF-8-escaped codes 
	 */
	private String escape(String url) throws Exception{
		if(url == null)
			return(null);
		
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < url.length(); i++){
			char c = url.charAt(i);
			if(c == ' ')
				sb.append("%20");
			else if(c <= 32 || c >= 127 || c == '%')
				sb.append(URLEncoder.encode("" + c, "UTF-8"));
			else sb.append(c);
		} // Just iterate over length, checking ASCII values
		return sb.toString();
	}
	
	/**
	 * Given a URL, make that URL canonical per Google specification.
	 * @param url URL to be processed
	 * @return Canonical version of the URL passed in, per the spec. at
	 * http://code.google.com/apis/safebrowsing/developers_guide.html
	 */
	public String canonical_url(String str_url) throws Exception{
		
		if(str_url == null)
			return(null);

			// Create a URL object and extract the fields we need
		URL theURL; // Catch an MalformedURL exceptions
		try{theURL = new URL(str_url);
		} catch(Exception e){return null;}
		String host = theURL.getHost();
		String path = theURL.getPath();
		String query = theURL.getQuery();
		String protocol = theURL.getProtocol();
		int port = theURL.getPort();
		String user = theURL.getUserInfo();

		// ***** HOST PROCESSING
		
		try{	// Unescape any hex-encodings, make lower-case.
			host = unescape(host).toLowerCase();
	
				// Escape non-standard characters (escape once).
			StringBuffer sb = new StringBuffer();
			for(int i = 0; i < host.length(); i++){
				char c = host.charAt(i);
				if((c>='0' && c<='9') || (c>='a' && c<='z') || c=='.' || c=='-')
					sb.append(c);
				else sb.append(URLEncoder.encode(c + "", "UTF-8")); // Escape
			} // AW: How does this escape differ from the escape() method?
			host = sb.toString();
	
				// Remove leading and trailing dots 
			while(host.startsWith("."))
				host = host.substring(1);
			while(host.endsWith("."))
				host = host.substring(0, host.length() - 1);
	
			int p = 0; // Replace consecutive dots with a single dot
			while((p = host.indexOf("..")) != -1)
				host = host.substring(0, p + 1) + host.substring(p + 2);
	
				// Add trailing slash if path is empty
			if(path.equals(""))
				host = host + "/";		
			
			// ***** PATH PROCESSING
			
			path = unescape(path); // Unescape until no more hex-encodings
			while ((p = path.indexOf("//")) != -1) // Remove consecutive slashes
				path = path.substring(0, p + 1) + path.substring(p + 2);		
			while ((p = path.indexOf("/./")) != -1) // // Remove /./ occurences
				path = path.substring(0, p + 1) + path.substring(p + 3);
	
			while ((p = path.indexOf("/../")) != -1){
				int previousSlash = path.lastIndexOf("/", p-1);
				path = path.substring(0, previousSlash) + path.substring(p + 3);
				p = previousSlash;
			} // Resolve /../ occurences in path
			path = escape(path); // Escape once
	
			// ***** QUERY PROCESSING
			
			query = unescape(query); 	// Unescape all
			query = escape(query);		// Escape just once
			
			// ***** PUTTING IT ALL BACK TOGETHER
			
			sb.setLength(0);
			sb.append(protocol + ":");
			if(port != -1)
				sb.append(port);
			sb.append("//");
			if(user != null)
				sb.append(user + "@");
			sb.append(host);
			sb.append(path);
			if(query != null)
				sb.append("?" + query);
			return(sb.toString());
		} catch(Exception e){return null;}
	}
	
	/**
	 * Given a "raw url", compute all forms (reductions) of that URL which
	 * should be checked per Google Safe Browsing specifications at
	 * http://code.google.com/apis/safebrowsing/developers_guide.html
	 * @param raw_url URL to be processed (in most verbose form). HOWEVER,
	 * this URL should already be in a canonical format.
	 * @return List of all "sub-forms" of the URL which also be
	 * queryed as part of a Safe Browsing lookup.
	 */
	public ArrayList<String> get_url_forms(String raw_url) throws Exception{
		
		ArrayList<String> urls = new ArrayList<String>();
		if(raw_url != null){
			URL url = new URL(raw_url);
			String host = url.getHost();
			String path = url.getPath();
			String query = url.getQuery();
			if (query != null)
				query = "?" + query;
	
				// Generate a list of the HOSTS to test 
				// (exact hostname plus up to four truncated hostnames) 
			ArrayList<String> hosts = new ArrayList<String>();
			hosts.add(host); // Should always test the exact hostname
			String[] host_array = host.split("\\.");
			StringBuffer sb = new StringBuffer();
			int start = (host_array.length < 6 ? 1 : host_array.length - 5);
			int stop = host_array.length;
			for(int i = start; i < stop - 1; i++){
				sb.setLength(0);
				for (int j = i; j < stop; j++)
					sb.append(host_array[j] + ".");
				sb.setLength(sb.length() - 1); // Trim trailing dot
				hosts.add(sb.toString());
			} // Make host-names of variable length, splitting on "."
	
				// Generate a list of PATHS to test
			ArrayList<String> paths = new ArrayList<String>();
			if (query != null)
				paths.add(path + query); // exact path including query
			paths.add(path); // exact path excluding query
			if (!paths.contains("/"))
				paths.add("/");
			int max_count = (query == null ? 5 : 6);
			String path_element = "/";
			StringTokenizer st = new StringTokenizer(path, "/");
			while (st.hasMoreTokens() && paths.size() < max_count){
				String thisToken = st.nextToken();
				path_element = path_element + thisToken + 
					(thisToken.indexOf(".") == -1 ? "/" : "");
				if (!paths.contains(path_element))
					paths.add(path_element);
			} // Make varibale length paths, splitting on "/"
	
			for(int i = 0; i < hosts.size(); i++){
				for(int j = 0; j < paths.size(); j++)
					urls.add(hosts.get(i).toString() + paths.get(j).toString());
			} // Build list from cross-product of HOSTS and PATHS
		} // Make sure we do not have a null URL
		return(urls);
	}

}
