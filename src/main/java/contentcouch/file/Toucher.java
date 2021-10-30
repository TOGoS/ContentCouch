package contentcouch.file;

import java.io.File;

import contentcouch.app.Log;

public class Toucher
{
	/** 
	 * Touch a single directory
	 */
	public static void touchSingle( File f, long ts ) {
		if( f.isDirectory() ) {
			File uriFile = new File(f + "/.ccouch-uri");
			if( uriFile.exists() ) {
				Log.log(Log.EVENT_DELETED, uriFile.getPath());
				uriFile.delete();				
			}
			f.setLastModified(ts);
		}
	}
	
	protected static void touchParents( File f, long ts ) {
		f = f.getParentFile();
		while( f != null ) {
			touchSingle(f, ts);
			f = f.getParentFile();
		}
	}
	
	protected static void touchChildren( File f, long ts ) {
		if( f.isDirectory() ) {
			File[] subs = f.listFiles();
			for( int i=0; i<subs.length; ++i ) {
				File sub = subs[i];
				if( sub.isDirectory() ) {
					touchSingle(sub, ts);
					touchChildren(sub, ts);
				}
			}
		}
	}
	
	public static void touch( File f, long ts, boolean parents, boolean children ) {
		// System.err.println("Touching "+f+(parents?" and parents":"")+(children?" and children":""));
		
		touchSingle(f, ts);
		if( parents ) touchParents(f, ts);
		if( children ) touchChildren(f, ts);
	}
}
