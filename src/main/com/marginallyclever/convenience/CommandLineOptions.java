package com.marginallyclever.convenience;

import com.marginallyclever.convenience.log.Log;

/**
 * store command line options for use in the app
 * @author Admin
 *
 */
public class CommandLineOptions {
	protected static String [] argv;
	
	static void setFromMain(String [] argv) {
		CommandLineOptions.argv = argv;

		for(int i=0;i<argv.length;++i) {
			String msg = "START OPTION "+argv[i];
			Log.message(msg);
		}
	}
	
	static public boolean hasOption(String option) {
		for(int i=0;i<argv.length;++i) {
			if(argv[i].equals(option)) {
				return true;
			}
		}
		return false;
	}
}
