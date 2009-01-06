package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.store.BlobStore;
import contentcouch.store.Sha1BlobStore;

public class ContentCouchCommand {
	
	public String USAGE =
		"Usage: ccouch [general options] <sub-command> [command-args]\n" +
		"Run ccouch <subcommand> -? for further help\n" +
		"General options:\n" +
		"  -repo <path>   ; specify a local repository to use\n" +
		"Sub-commands:\n" +
		"  store <file>\n" +
		"  checkout <uri> <dest>";
	
	public String STORE_USAGE =
		"Usage: ccouch [general options] store [store options] <file1> <file2> ...\n" +
		"Store options:\n" +
		"  -relink            ; relink files to their canonical name\n" +
		"  -show-sub-mappings ; report every path -> urn mapping";

	protected String repoPath = ".";
	protected ContentCouchRepository repositoryCache = null;
	
	public ContentCouchRepository getRepository() {
		if( repositoryCache == null ) {
			repositoryCache = ContentCouchRepository.getIfExists(repoPath);
		}
		return repositoryCache;
	}
	
	public BlobStore getDatastore( Map options ) {
		String repoPath = (String)options.get("repo-path");
		if( repoPath == null ) {
			return null;
		} else {
			return new Sha1BlobStore(repoPath + "/data/");
		}
	}
	
	public Importer getImporter( Map options ) {
		BlobStore ds = getDatastore(options);
		if( ds == null ) throw new RuntimeException("Repository unspecified");
		return new Importer(ds);
	}
	
	public void relink( File target, File link ) {
		if( link.exists() ) {
			try {
				String linkParentPath = link.getParent();
				if( linkParentPath == null || linkParentPath.length() == 0 ) {
					linkParentPath = ".";
				} else {
					linkParentPath += "/.";
				}
				File lnTemp = new File(linkParentPath + link.getName() + ".cc-ln-temp");
				if( lnTemp.exists() ) {
					lnTemp.delete();
				}
				
				Process lnProc;
				// Unix:
				//lnProc = Runtime.getRuntime().exec(new String[] {"ln", target.getCanonicalPath(), lnTemp.getPath()});
				// Windows:
				lnProc = Runtime.getRuntime().exec(new String[] {"fsutil", "hardlink", "create", lnTemp.getPath(), target.getCanonicalPath()});
				int lnProcReturn = lnProc.waitFor();
				if( !lnTemp.exists() ) {
					System.err.println("Failed to create hard link from " + link + " to " + target + " (link does not exist after running 'ln', which returned " + lnProcReturn + ")");
					return;
				}
				if( link.exists() && !link.delete() ) {
					lnTemp.delete();
					System.err.println("Failed to create hard link from " + link + " to " + target + " (could not delete old file to replace with link)");
					return;
				}
				if( !lnTemp.renameTo(link) ) {
					System.err.println("Failed to create hard link from " + link + " to " + target + " (could not rename temporary link to final location)");
					return;				
				}
			} catch (InterruptedException e) {
			} catch (IOException e) {
				System.err.println("Failed to create hard link from " + link + " to " + target + " (exception)");
				e.printStackTrace();
			}
		}
	}
	
	public void runStoreCmd( String[] args, Map options ) {
		List files = new ArrayList();
		final Importer importer = getImporter(options);
		boolean pShowSubMappings = false;
		boolean pRelink = false;
		for( int i=0; i < args.length; ++i ) {
			String arg = args[i];
			if( arg.length() == 0 ) {
				System.err.println(STORE_USAGE);
				System.exit(1);
			} else if( "-show-sub-mappings".equals(arg) ) {
				pShowSubMappings = true;
			} else if( "-relink".equals(arg) ) {
				pRelink = true;
			} else if( "-h".equals(arg) || "-?".equals(arg) ) {
				System.out.println(STORE_USAGE);
				System.exit(0);
			} else if( arg.charAt(0) != '-' ) {
				files.add(arg);
			} else {
				System.err.println("ccouch store: Unrecognised argument: " + arg);
				System.err.println(STORE_USAGE);
				System.exit(1);
			}
		}
		if( files.size() == 0 ) {
			System.err.println("ccouch store: No files given");
			System.err.println(STORE_USAGE);
			System.exit(0);
		}
		Boolean oRelink = (Boolean)options.get("relink");
		final boolean relink = pRelink || ((oRelink != null) && oRelink.booleanValue());
		final boolean showSubMappings = pShowSubMappings;
		
		FileImportListener fileImportListener = new FileImportListener() {
			public void fileImported(File file, String urn) {
				if( showSubMappings ) {
					System.out.println(importer.getFileUri(file) + "\t" + urn);
				}
				if( file.isFile() && relink ) {
					File relinkTo = importer.getFile(urn);
					if( relinkTo != null ) {
						//System.err.println( "Relinking " + file + " to " + relinkTo );
						relink( relinkTo, file );
					}
				}
			}
		};
		
		for( Iterator i=files.iterator(); i.hasNext(); ) {
			File file = new File((String)i.next());
			String urn = importer.importFileOrDirectory( file, fileImportListener );
			if( !showSubMappings ) {
				// otherwise, this will already have been printed
				System.out.println(importer.getFileUri(file) + "\t" + urn);
			}
		}
	}
	
	public void run( String[] args ) {
		if( args.length == 0 ) {
			System.err.println(USAGE);
			System.exit(1);
		}
		String cmd = null;
		Map options = new HashMap();
		int i;
		for( i=0; i<args.length; ++i ) {
			if( "-h".equals(args[i]) || "-?".equals(args[i]) ) {
				System.out.println(USAGE);
				System.exit(0);
			} else if( "-repo".equals(args[i]) ) {
				options.put("repo-path", args[++i]);
			} else if( args[i].length() > 0 && args[i].charAt(0) != '-' ) {
				cmd = args[i++];
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
		if( "store".equals(cmd) ) {
			runStoreCmd( cmdArgs, options );
		} else {
			System.err.println("ccouch: Unrecognised sub-command: " + cmd);
			System.err.println(USAGE);
			System.exit(1);
		}
	}
	
	public static void main( String[] args ) {
		new ContentCouchCommand().run( args );
	}
}

// Used: 42294853632
// Copied yayshots
// Used: 42331856896

// Stored yayshots-relink-test without relinking
// Used: 42338385920
// Used: 42341187584

// After apparently successful relink of files in yayshots-relink-test
// Used: 52309570560