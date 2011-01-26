import access_list.sb_interface;

import common_utils.gen_utils.LIST_TYPE;

/**
 * Andrew G. West - test.java - A simple test-harness for callign into
 * the Google Safe Browsing API data.
 */
public class test{

	/**
	 * Test-harness for calling into the Google Safe Browsing lists 
	 * @param args No arguments are required by this method
	 */
	public static void main(String[] args) throws Exception{
		
		sb_interface face_phishing = new sb_interface(LIST_TYPE.phishing);
		sb_interface face_malware = new sb_interface(LIST_TYPE.malware);
		
		String url = args[0];
		System.out.println(face_phishing.url_is_active(url) + "," + face_malware.url_is_active(url));
		
		face_phishing.shutdown();
		face_malware.shutdown();

	}

}
