package contentcouch.app;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Log {
	interface Logger {
		public void log(String eventName, String[] arguments);
	}

	// Errors (not necessarily fatal, but process should not be considered
	// successful if any occured):
	public static final String EVENT_ERROR           = "error"; // error message X, backtrace Y
	public static final String EVENT_NOT_FOUND       = "not-found"; // X was not found

	// Warnings:
	public static final String EVENT_WARNING         = "warning"; // message X
	public static final String EVENT_PERFORMANCE_WARNING = "performance-warning"; // message X
	
	// Informational:
	public static final String EVENT_DOWNLOAD_STARTED = "download-started"; // uri X, length Y
	public static final String EVENT_DOWNLOAD_FINISHED = "download-finished"; // uri X
	public static final String EVENT_REQUEST_SUBMITTED = "request-submitted"; // method X, uri Y
	public static final String EVENT_REQUEST_HANDLED = "request-handled"; // method X, uri Y, handled by Z
	public static final String EVENT_PUT             = "put";  // X put as Y
	public static final String EVENT_REPLACED        = "replaced"; // X overwrite Y
	public static final String EVENT_KEPT            = "kept"; // X was kept
	public static final String EVENT_SKIPPED_CACHED  = "skipped-cached"; // X was skipped due to already being cached
	public static final String EVENT_DELETED         = "deleted"; // X was kept
	public static final String EVENT_STORED          = "stored"; // X was stored as Y
	
	public static Map loggers = new HashMap();
	
	public static final void addLogger( String eventName, Logger logger ) {
		List l = (List)loggers.get(eventName);
		if( l == null ) loggers.put(eventName, l = new ArrayList());
		l.add( logger );
	}
	
	public static int errorCount = 0;
	protected static Logger errorLogger = new Logger() {
		public void log(String eventName, String[] arguments) {
			++errorCount;
		}
	};
	
	public static final Logger createStreamLogger( final PrintStream stream, final String separator ) {
		return new Logger() {
			public void log(String eventName, String[] arguments) {
				stream.print(eventName);
				for( int i=0; i<arguments.length; ++i ) {
					stream.print(separator);
					stream.print(arguments[i]);
				}
				stream.println();
			}
		};
	}
	
	public static Logger stderrLogger = null;
	public static final Logger getStderrLogger() {
		if( stderrLogger == null ) stderrLogger = createStreamLogger( System.err, " " );
		return stderrLogger;
	}
		
	public static final int LEVEL_SILENT    = 00;
	public static final int LEVEL_QUIET     = 10; // Errors only
	public static final int LEVEL_NORMAL    = 20; // Errors, warnings
	public static final int LEVEL_DOWNLOADS = 30; // Errors, warnings, downloads, fs changes
	public static final int LEVEL_VERBOSE   = 40; // Errors, warnings, downloads, fs changes, fs non-changes
	public static final int LEVEL_DEBUG     = 50; // Errors, warnings, downloads, fs changes, fs non-changes, requests
	
	public static final void initErrorLogger() {
		addLogger(EVENT_ERROR, errorLogger);
		addLogger(EVENT_NOT_FOUND, errorLogger);
	}
	
	public static final void setStandardLogLevel( int level ) {
		final Logger stderrLogger = getStderrLogger();
		
		if( level < LEVEL_QUIET     ) return;
		addLogger( EVENT_ERROR, stderrLogger );
		addLogger( EVENT_NOT_FOUND, stderrLogger );

		if( level < LEVEL_NORMAL    ) return;
		addLogger( EVENT_WARNING, stderrLogger );
		addLogger( EVENT_DELETED, stderrLogger );

		if( level < LEVEL_DOWNLOADS ) return;
		addLogger( EVENT_DOWNLOAD_STARTED, new Logger() {
			public void log(String eventName, String[] arguments) {
				String uri = arguments[0];
				String length = arguments[1];
				System.err.println("Downloading " + uri + " (" + length + " bytes)");
			}
		});
		addLogger( EVENT_PUT, stderrLogger );
		addLogger( EVENT_STORED, stderrLogger );
		addLogger( EVENT_REPLACED, new Logger() {
			public void log(String eventName, String[] arguments) {
				String replacedUri = arguments[1];
				String replacedWithUri = arguments[0];
				System.err.println("Replaced " + replacedUri + " with " + replacedWithUri);
			}
		});

		if( level < LEVEL_VERBOSE   ) return;
		addLogger( EVENT_KEPT, stderrLogger );
		addLogger( EVENT_PERFORMANCE_WARNING, stderrLogger );
		addLogger( EVENT_SKIPPED_CACHED, stderrLogger );

		if( level < LEVEL_DEBUG     ) return;
		addLogger( EVENT_REQUEST_SUBMITTED, stderrLogger );
	}
	
	public static final void log( String eventName, String[] arguments ) {
		List l = (List)loggers.get(eventName);
		if( l == null ) return;
		for( Iterator i=l.iterator(); i.hasNext(); ) {
			((Logger)i.next()).log( eventName, arguments );
		}
	}
	
	public static final void log( String eventName, String arg1 ) {
		log( eventName, new String[]{arg1} );
	}

	public static final void log( String eventName, String arg1, String arg2 ) {
		log( eventName, new String[]{arg1,arg2} );
	}

	public static final void log( String eventName, String arg1, String arg2, String arg3 ) {
		log( eventName, new String[]{arg1,arg2,arg3} );
	}
}
