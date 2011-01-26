package access_list;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import common_utils.gen_utils;
import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew West - batch_tester.java - A script for running a large number of
 * URLs through the Safe-Browsing interface.
 */
public class batch_tester{

	// **************************** PUBLIC METHODS ***************************	
	
	/**
	 * Given a list of URLs, output a list of those that are active on at
	 * least one of the Google Safe Browsing lists.
	 * @param args One argument is required: (1) The path to a file
	 * containing, one per line, those URLs that should be examined
	 */
	public static void main(String[] args) throws Exception{

		sb_interface face_phishing = new sb_interface(LIST_TYPE.phishing);
		sb_interface face_malware = new sb_interface(LIST_TYPE.malware);
		
		List<String> url_list = list_from_file(args[0]);
		for(int i=0; i <  url_list.size(); i++){
			if(face_phishing.url_is_active(url_list.get(i)))
				System.out.println(url_list.get(i) + ",phishing");
			if(face_malware.url_is_active(url_list.get(i)))
				System.out.println(url_list.get(i) + ",malware");	
		} // Just iterate over all URLs in file
		
		face_phishing.shutdown();
		face_malware.shutdown();
	}

	
	// *************************** PRIVATE METHODS ***************************
	
	/**
	 * Given a text-file, read that into a local-array, line by line.
	 * @param filepath Location of file to be read in.
	 * @return The content of the file at 'filepath', converted to a String,
	 * line-delimited, and returned in the format of a Java list.
	 */
	private static List<String> list_from_file(String filepath) 
			throws Exception{
		BufferedReader in = gen_utils.create_reader(filepath);
		List<String> local = new ArrayList<String>();
		String cur_line = in.readLine();
		while(cur_line != null){
			local.add(cur_line);
			cur_line = in.readLine();
		} // Just iterate over file, adding lines to list
		in.close();
		return(local);
	}
	
}
