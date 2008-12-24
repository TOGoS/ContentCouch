package contentcouch.app;

import java.util.ArrayList;
import java.util.List;

public class ContentCouchCommand {
	
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -h for further help\n" +
		"General options:\n" +
		"  -repo <path>   ; specify a local repository to use\n" +
		"Sub-commands:\n" +
		"  insert <file>\n" +
		"  checkout <uri> <dest>";
	
	public String INSERT_USAGE =
		"Usage: ccouch [general options] insert [insert options] <file1> <file2> ...";
	
	protected String repoPath = ".";
	protected ContentCouchRepository repositoryCache = null;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = ContentCouchRepository.getIfExists(repoPath);
		}
		return repositoryCache;
	}
	
	public void runInsertCmd( String[] args ) {
		List files = new ArrayList();
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(INSERT_USAGE);
				System.exit(1);
			} else if( arg.charAt(0) != '-' ) {
				files.add(arg);
			} else if( "-h".equals(arg) ) {
				System.out.println(INSERT_USAGE);
				System.exit(0);
			} else {
				System.err.println(INSERT_USAGE);
				System.exit(1);
			}
		}
	}
	
	public void run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String cmd = null;
		int i;
		for( i=0; i<args.length; ++i ) {
			if( "-h".equals(args[i]) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i];
				break;
			}
		}
		if( cmd == null ) {
			System.err.println("No command given");
			System.err.println(USAGE);
			System.exit(1);
		}
		String[] cmdArgs = new String[args.length-i];
		for( int j=0; j<cmdArgs.length; ++i, ++j ) {
			cmdArgs[j] = args[i];
		}
		if( "insert".equals(cmd) ) {
			runInsertCmd( cmdArgs );
		} else {
			System.err.println("Unrecognised command: " + cmd);
			System.err.println(USAGE);
			System.exit(1);
		}
	}
	
	public static void main( String[] args ) {
		new ContentCouchCommand().run( args );
	}
}
