package common_utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Andrew G. West - gen_utils.java - This is a class to provide the variable
 * constants and utility methods used throughout the project
 */
public class gen_utils{
	
	// **************************** PUBLIC FIELDS ****************************	
	
	/**
	 * Google Safe Browsing API key. FWIW, this key was generated from my
	 * [west.andrew.g@gmail.com] account.
	 */
	public static final String API_KEY = 
		"ABQIAAAAS_TrK22uuKxz0lOBfaJTdBTDG-WfxRnvltIq43SdMklW-NdqqQ";
	
	/**
	 * Major API version currently being used for Safe Browsing API calls. 
	 */
	public static final int MAJ_API_VERSION = 1;
	
	/**
	 * Enumeration listing the different blacklists maintained by Google.
	 */
	public enum LIST_TYPE{phishing, malware};
	
	
	// ******* TABLE NAMES ********	
	
	/**
	 * Table where [pagecount] (hourly) processing is logged.
	 */
	public static final String tbl_pagecount_log = "pagecount_log";

	
	// **************************** PUBLIC METHODS ***************************
	// ****** CASUAL JAVA STUFF *******
	
	/**
	 * Open an input reader over a file whose location/name is provided.
	 * @param filename File which is to read in
	 * @return A reader capable of reading the file line-by-line, or NULL if 
	 * an error occured during the creation of said reader
	 */
	public static BufferedReader create_reader(String filename){
		BufferedReader in = null;
		try{ // If an exception is raised, return NULL
			File inFile = new File(filename);
			FileInputStream fis = new FileInputStream(inFile);
			DataInputStream dis = new DataInputStream(fis);
			InputStreamReader isr = new InputStreamReader(dis);
			in = new BufferedReader(isr);
		} catch (Exception e){
			System.err.println("Error opening reader over file: " + filename);
		} // And output a message concerning nature of error
		return in;
	}
	
	/**
	 * Open an output writer over a file whose location/name is provided.
	 * @param filename File which is to be written
	 * @param append TRUE if data should be appending to file; FALSE, otherwise
	 * @return A string writer suitable for file authoring, or NULL if an
	 * error occurred during the creation of said writer. 
	 */
	public static BufferedWriter create_writer(String filename, boolean append){
		BufferedWriter out = null;
		try{ // If an exception is raised, return NULL
			File outFile = new File(filename);
			out = new BufferedWriter(new FileWriter(outFile, append));
		} catch(Exception e){
			System.err.println("Error opening writer over file: " + filename);
		} // And output a message concerning nature of error 
		return out;
	}
	
	/**
	 * Read the contents of an InputStream into a String.
	 * @param in InputStream whose content is being captured
	 * @return String object containing all data in 'in'
	 */
	public static String capture_stream(InputStream in) throws Exception{
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line = null;
		String response = "";
		while((line = br.readLine()) != null)
			response += line;
		return(response);
	}
	
	/**
	 * Given a string of an IP address in conventional ???.???.???.????
	 * decimal octet format, compute the 32-bit decimal (sans octet) equivalent. 
	 * Note: Return type is a LONG because Java integers are signed. 
	 * @param ip String representation of an IP address ("???.???.???.???")
	 * @return Long containing decimal representation of the IP
	 */
	public static long ip_to_long(String ip){
		String[] ip_octets = ip.split("\\.");
		
			// Integers used because 'byte' type is signed
		int octet1 = Integer.parseInt(ip_octets[0]);
		int octet2 = Integer.parseInt(ip_octets[1]);
		int octet3 = Integer.parseInt(ip_octets[2]);
		int octet4 = Integer.parseInt(ip_octets[3]);
		
			// Long used because 'int' types are signed
		long dec_rep = octet1;
		dec_rep <<= 8;
		dec_rep |= octet2;
		dec_rep <<=8;
		dec_rep |= octet3;
		dec_rep <<=8;
		dec_rep |= octet4;
		return dec_rep;
	}
	
	/**
	 * Given an integer format IP address, convert to a string representation.
	 * Against, a long is used because Java integers are always signed.
	 * @param int_ip 32-bit IP address, in long format
	 * @return Parameter 'int_ip' in conventional ???.???.???.??? format
	 */
	public static String ip_to_string(long int_ip){
		
		int mask= 255; // (2^8 - 1)
		long octet4, octet3, octet2, octet1;
		
		octet4 = int_ip & mask;
		mask <<= 8;
		octet3 = (int_ip & mask) >> 8;
		mask <<= 8;
		octet2 = (int_ip & mask) >> 16;
		mask <<= 8;
		octet1 = (int_ip & mask) >> 24;
		
		return(octet1 + "." + octet2 + "." + octet3 + "." + octet4); 
	}
	
	/**
	 * Find the substrings that match some pattern, in a larger string.
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return All substrings of 'corpus' matching 'regex'
	 */
	public static List<String> all_pattern_matches_within(String regex, 
			String corpus){
		List<String> matches = new ArrayList<String>();
		Matcher match = Pattern.compile(regex).matcher(corpus);
		while(match.find())
			matches.add(match.group());
		return(matches);
	}
	
	/**
	 * Return the first substring that matches some pattern, in a larger string.
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return First substring of 'corpus' matching 'regex'. Return NULL if
	 * no matches are present.
	 */
	public static String first_match_within(String regex, String corpus){
		Matcher match = Pattern.compile(regex).matcher(corpus);
		while(match.find())
			return(match.group());
		return(null);
	}
	
	/**
	 * Number of substrings that match some pattern, in a larger string.
	 * @param regex Pattern by which matches should be determined
	 * @param corpus Larger string in which to search for matches
	 * @return Number of substrings of 'corpus' matching 'regex'
	 */
	public static int num_matches_within(String regex, String corpus){
		int matches = 0;
		Matcher match = Pattern.compile(regex).matcher(corpus);
		while(match.find())
			matches++;
		return(matches);
	}	
	
	/**
	 * If a string is over some length, reduce its size to that length.
	 * @param input Input string under examination
	 * @param cap Number of characters permitted in output string
	 * @return If (input.length <= cap), return 'cap', else return the first
	 * 'cap' characters of 'input'.
	 */
	public static String cap_string(String input, int cap){
		if(input.length() >= cap)
			return(input.substring(0, cap));
		else return(input);
	}
	
	// ***** TIME-CENTRIC METHODS *****

	/**
	 * Time decay an event, from ''calc_time', using the half-life param.
	 * @param calc_ts UNIX timestamp when calculation is being made
	 * @param event_ts UNIX timestamp at which event in question occured
	 * @param hl Half-life of expotential decay, in seconds
	 * @return Using the half-life, decay the duration from 
	 * (this.calc_time-event_time), using the base-quantity of 1.
	 */
	public static double decay_event(long calc_ts, long event_ts, long hl){
		double decay = Math.pow(0.5,((calc_ts-event_ts)/(hl * 1.0)));
		if(decay > 1.0)
			return (1.0);
		return(decay); 
	}
	
	/**
	 * Provide the number of elapsed seconds from UNIX epoch until the 
	 * time specified by the arguments (in GMT/UTC time zone). 
	 * @param y Year (in UTC locale) from which to calculate time
	 * @param mon Month (in UTC locale) from which to calculate time
	 * @param d Day (in UTC locale) from which to calculate time
	 * @param h Hour (in UTC locale) from which to calculate time
	 * @param min Min (in UTC locale) from which to calculate time
	 * @return Number of seconds between UNIX epoch at UTC date provided
	 */
	public static long arg_unix_time(int y, int mon, int d, int h, 
			int min, int sec){
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.setTimeZone(TimeZone.getTimeZone("GMT+0000"));
		cal.set(y, (mon-1), d, h, min, sec); // ZERO-INDEXED months?! WTF?!?!
		return (cal.getTimeInMillis() / 1000);
	}
	
	/**
	 * Convert a Wikipedia timestamp (2001-01-21T02:12:21Z) into a UNIX one
	 * @param wiki_ts Wikipedia timestamp, in string format
	 * @return The same time as 'wiki_ts' expressed in UNIX format
	 */
	public static long wiki_ts_to_unix(String wiki_ts){
		
		int year=0, month=0, day=0, hour=0, min=0, sec=0;
		
			// Being a fixed format, we can parse the parts out easily
		year = Integer.parseInt(wiki_ts.substring(0, 4));
		month = Integer.parseInt(wiki_ts.substring(5,7));
		day = Integer.parseInt(wiki_ts.substring(8, 10));
		hour = Integer.parseInt(wiki_ts.substring(11, 13));
		min = Integer.parseInt(wiki_ts.substring(14, 16));
		sec = Integer.parseInt(wiki_ts.substring(17, 19));
		return (gen_utils.arg_unix_time(year, month, day, hour, min, sec));
	}
	
	/**
	 * Return the number of UTC seconds elapsed since UNIX epoch.
	 * @return number of seconds elapsed since UNIX epoch
	 */
	public static long cur_unix_time(){
		return (System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Return the current 'unix day'. The number of days elapsed since
	 * Jan. 1, 1970, which is considered day 0 (zero).
	 * @return Number of days elapsed since 1970/1/1.
	 */
	public static long cur_unix_day(){
		return(cur_unix_time()/(60*60*24));	
	}
	
	/**
	 * Return the 'UNIX day' at some UNIX second.
	 * @param unix_sec Unix timestamp (in the unit of seconds)
	 * @return Number of days between 1970/1/1 and 'unix_sec'
	 */
	public static long unix_day_at_unix_sec(long unix_sec){
		return(unix_sec/(60*60*24));	
	}
	
	/**
	 * Return the 'UNIX hour' at some UNIX second.
	 * @param unix_sec Unix timestamp (in the unit of seconds)
	 * @return Number of hours between 1970/1/1 and 'unix_sec'
	 */
	public static int unix_hour_at_unix_sec(long unix_sec){
		return((int) unix_sec/(60*60));	// will not overrun an integer
	}
	
	/**
	 * Return the 'UNIX day' at some UNIX hour.
	 * @param unix_hour UNIX hour
	 * @return Number of days between 1970/1/1 and 'unix_housr'
	 */
	public static int unix_day_at_unix_hour(int unix_hour){
		return((int) unix_hour/(24));
	}
	
	
	// **** BINARY-CENTRIC METHODS ****

	/**
	 * Convert a list of integers into an in-order byte-array 
	 * @param list List of integers to be backed in byte form
	 * @return A byte array of length [4 * list.size] representing
	 * all integers from 'list' in in-order left-to-right fashion.
	 */
	public static byte[] int_list_to_byte_array(List<Integer> list){
		
		byte[] bytes = new byte[4 * list.size()];
		int pointer_byte = 0;
		int current_int;
		
		for(int i=0; i < list.size(); i++){	
			current_int = list.get(i);
			bytes[pointer_byte+0] = (byte) (current_int >>> 24); 
			bytes[pointer_byte+1] = (byte) (current_int >>> 16);
			bytes[pointer_byte+2] = (byte) (current_int >>> 8);
			bytes[pointer_byte+3] = (byte) (current_int);
			pointer_byte += 4;
		} // Just bit-shift integer into bytes, and array properly
		return(bytes);
	}
	
	/**
	 * Pack two integers into a 8-byte (64-bit) array.
	 * @param val1 First integer (left-most) to be packed
	 * @param val2 Second integer (right-most) to be packed
	 * @return Byte-array of the form (val1,val2)
	 */
	public static final byte[] two_ints_to_byte_array(int val1, int val2){
        return new byte[]{ 
        	(byte)(val1 >>> 24), (byte)(val1 >>> 16), (byte)(val1 >>> 8),
            (byte)(val1), (byte)(val2 >>> 24), (byte)(val2 >>> 16),
            (byte)(val2 >>> 8),  (byte)(val2)
        }; // Simple bit shifting into a byte array
	}
	
	/**
	 * Given an byte-array, parse out an integer at some byte offset.
	 * @param array Byte-array from which integer should be parsed
	 * @param offset Left-most byte at which parsing should begin. Note that
	 * the array must contain at least 4 post-bytes, offset inclusive
	 * @return Integer intepretation of [byte, byte+3] in array
	 */
	public static int get_int_from_byte_array(byte[] array, int offset){
		return ((array[offset]   & 0xff) << 24) |
		       ((array[offset+1] & 0xff) << 16) |
		       ((array[offset+2] & 0xff) << 8)  |
		       ( array[offset+3] & 0xff);
	}
	
}
