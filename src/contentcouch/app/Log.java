package contentcouch.app;

public class Log {
	public interface Logger {
		public void setLevel(int level);
		public int getLevel();
		public void log(int level, String msgType, String text, Exception e);
	}
	
	public static class StderrLogger implements Logger {
		protected int level = LEVEL_CHANGES;
		
		public void setLevel(int level) {
			this.level = level;
		}
		
		public int getLevel() {
			return this.level;
		}
		
		public void log(int level, String msgType, String text, Exception e) {
			if( this.level >= level ) System.err.print(msgType + text + (e == null ? "\n" : ": " ));
			if( this.level >= level && e != null ) e.printStackTrace(System.err);
		}
	}
	
	public static final int LEVEL_SILENT   =  0;
	public static final int LEVEL_ERRORS   = 10;
	public static final int LEVEL_WARNINGS = 20;
	public static final int LEVEL_CHANGES  = 30;
	public static final int LEVEL_CHATTY   = 40;
	public static final int LEVEL_CHATTIER = 50;
	
	public static final String TYPE_GENERIC     = "";
	public static final String TYPE_EXPORTING   = "Exporting: ";
	public static final String TYPE_SKIP        = "Skipping:  ";
	public static final String TYPE_DOWNLOADING = "Download:  ";
	public static final String TYPE_UNCHANGED   = "Unchanged: ";
	public static final String TYPE_NOTFOUND    = "Not Found: ";
	public static final String TYPE_WARNING     = "Warning: ";
	public static final String TYPE_NOTICE      = "Notice: ";
	public static final String TYPE_ERROR       = "Error: ";
	
	protected static Logger logger = new StderrLogger();
	public static void setLogger(Logger logger) {
		Log.logger = logger;
	}
	public static Logger getLogger() {
		return Log.logger;
	}
	public static void log(int level, String msgType, String text, Exception e) {
		getLogger().log(level, msgType, text, e);
	}
	public static void log(int level, String msgType, String text) {
		log( level, msgType, text, null );
	}
	public static void log(int level, String text) {
		log( level, TYPE_GENERIC, text );
	}
	public static void setLevel(int level) {
		getLogger().setLevel(level);
	}
}
