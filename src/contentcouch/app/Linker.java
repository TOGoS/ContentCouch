package contentcouch.app;

import java.io.File;
import java.io.IOException;

public abstract class Linker {
	public static class LinkException extends RuntimeException {
		public LinkException( String from, String to, String problem ) {
			super( "Failed to create link from " + from + " to " + to + ": " + problem );
		}
		public LinkException( String from, String to, Exception e ) {
			super( "Failed to create link from " + from + " to " + to , e );
		}
		public LinkException( File from, File to, String problem ) {
			this( from.getPath(), to.getPath(), problem );
		}
		public LinkException( File from, File to, Exception e ) {
			this( from.getPath(), to.getPath(), e );
		}
	}
	
	public static class WinLinker extends Linker {
		public void link( File target, File link ) {
			try {
				Process lnProc = Runtime.getRuntime().exec(new String[] {"fsutil", "hardlink", "create", link.getPath(), target.getCanonicalPath()});
				int lnProcReturn = lnProc.waitFor();
				if( !link.exists() ) {
					throw new LinkException( link, target, "link does not exist after running 'fsutil hardlink create ...', which returned " + lnProcReturn);
				}
			} catch( InterruptedException e ) {
				throw new LinkException( link, target, e );
			} catch( IOException e ) {
				throw new LinkException( link, target, e );
			}
		}
	}
	
	public static class UnixLinker extends Linker {
		public void link( File target, File link ) {
			try {
				Process lnProc = Runtime.getRuntime().exec(new String[] {"ln", target.getCanonicalPath(), link.getPath()});
				int lnProcReturn = lnProc.waitFor();
				if( !link.exists() ) {
					throw new LinkException( link, target, "link does not exist after running 'fsutil hardlink create ...', which returned " + lnProcReturn);
				}
			} catch( InterruptedException e ) {
				throw new LinkException( link, target, e );
			} catch( IOException e ) {
				throw new LinkException( link, target, e );
			}
		}
	}
	
	protected static Linker instance;
	public static Linker getInstance() {
		if( instance == null ) {
			String whichOS = System.getProperty("os.name");
			if( whichOS.contains("Windows") ) {
				return new WinLinker();
			} else {
				return new UnixLinker();
			}
		}
		return instance;
	}
	
	public abstract void link( File target, File link );
	
	public void relink( File target, File link ) {
		if( link.exists() ) {
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
			
			link( target, lnTemp );
			if( link.exists() && !link.delete() ) {
				lnTemp.delete();
				System.err.println("Failed to create hard link from " + link + " to " + target + " (could not delete old file to replace with link)");
				return;
			}
			if( !lnTemp.renameTo(link) ) {
				System.err.println("Failed to create hard link from " + link + " to " + target + " (could not rename temporary link to final location)");
				return;				
			}
		} else {
			link( target, link );
		}
	}

	
	public static void main( String[] args ) {
		String whichOS = System.getProperty("os.name");
		System.out.println("You're running " + whichOS);
	}
}
