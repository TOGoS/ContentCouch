// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

import contentcouch.blob.BlobUtil;
import contentcouch.file.FileBlob;
import contentcouch.file.FileDirectory;
import contentcouch.file.FileUtil;
import contentcouch.misc.Function1;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.store.FileBlobMap;
import contentcouch.store.FileForBlobGetter;
import contentcouch.store.StoreFileGetter;
import contentcouch.store.Identifier;
import contentcouch.store.Pusher;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class Importer implements Pusher {
	Pusher blobSink;
	FileBlobMap namedStore;
	public boolean shouldLinkStored;
	public boolean shouldRelinkImported;
	public ImportListener importListener;
	public boolean shouldStoreDirs;
	public boolean shouldStoreFiles;
	public boolean shouldStoreHeads;
	
	public Function1 entityTargetRdfifier = new Function1() {
		public Object apply( Object input ) {
			if( input instanceof Directory || input instanceof Blob || input instanceof File ) {
				return importObject( input );
			} else if( input instanceof RdfNode || input instanceof Ref ) {
				return input;
			} else {
				throw new RuntimeException("Don't know how to import " + input.getClass().getName());
			}
		}
	};
	
	public Importer( Pusher blobSink, FileBlobMap namedStore ) {
		this.blobSink = blobSink;
		this.namedStore = namedStore;
	}
	
	public Importer( ContentCouchRepository repo ) {
		this( repo, (FileBlobMap)repo.headPutter );
		if( !repo.initialized ) throw new RuntimeException("Repo is not yet initialized");
	}
	
	//// Utility functions //// 

	public static Object getImportableObject( Object obj ) {
		if( obj instanceof Directory || obj instanceof Blob ) {
			return obj;
		} else if( obj instanceof File ) {
			if( ((File)obj).isDirectory() ) {
				return new FileDirectory((File)obj);
			} else {
				return new FileBlob((File)obj); 
			}
		} else {
			throw new RuntimeException( "Don't know how to import " + obj.getClass().getName() );
		}
	}
	
	protected static final char[] hexChars = new char[]{'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	protected static String uriEscapePath( String path ) {
		byte[] bites;
		try {
			bites = path.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		StringBuffer b = new StringBuffer();
		for( int i=0; i<bites.length; ++i ) {
			char c = (char)bites[i];
			if( (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ) {
				b.append(c);
			} else {
				switch(c) {
				case('/'): case('+'): case('-'): case('.'): case(':'): case('~'): case('^'): case('('): case(')'): case('\\'):
					b.append(c); break;
				default:
					b.append('%');
					b.append(hexChars[(c >> 4) & 0xF]);
					b.append(hexChars[(c >> 0) & 0xF]);
				}
			}
			    
		}
		return b.toString();
	}
	
	protected String getFileUri(File file ) {
		try {
			String path = file.getCanonicalPath();
			path = path.replace('\\', '/');
			if( path.charAt(1) == ':' ) {
				// Windows path!
				return "file:///" + uriEscapePath(path);
			} else if( path.charAt(0) == '/' ) { 
				// Unix path, leading slash already included!
				return "file://" + uriEscapePath(path);
			} else {
				return "file:" + uriEscapePath(path);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected Blob createMetadataBlob( RdfNode desc, String defaultNamespace ) {
		return BlobUtil.getBlob(RdfIO.xmlEncodeRdf(desc, defaultNamespace));
	}
	
	protected Blob createMetadataBlob( String aboutUri, Map properties ) {
		RdfNamespace.Description desc = new RdfNamespace.Description();
		desc.importValues(properties);
		desc.about = new Ref(aboutUri);
		return createMetadataBlob(desc, null);
	}
	
	public File getFile( String uri ) {
		if( blobSink instanceof StoreFileGetter ) {
			return ((StoreFileGetter)blobSink).getStoreFile(uri);
		} else {
			return null;
		}
	}
	
	public RdfNode getCommitRdfNode( String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode n = new RdfNode(RdfNamespace.CCOUCH_COMMIT);
		n.add(RdfNamespace.DC_CREATED, RdfNamespace.CCOUCH_DATEFORMAT.format(date));
		n.add(RdfNamespace.DC_CREATOR, creator);
		n.add(RdfNamespace.DC_DESCRIPTION, description);
		n.add(RdfNamespace.CCOUCH_TARGET, new Ref(targetUri) );
		n.add(RdfNamespace.CCOUCH_TARGETTYPE, targetType);
		if( parentUris != null ) for( int i=0; i<parentUris.length; ++i ) {
			n.add(RdfNamespace.CCOUCH_PARENT, new Ref(parentUris[i]));
		}
		return n;
	}
	
	public RdfNode getRedirectRdfNode( String targetType, String targetUri ) {
		RdfNode n = new RdfNode(RdfNamespace.CCOUCH_REDIRECT);
		n.add(RdfNamespace.CCOUCH_TARGET, new Ref(targetUri) );
		n.add(RdfNamespace.CCOUCH_TARGETTYPE, targetType);
		return n;
	}
	
	//// Import objects ////
	
	public Ref importBlob( Blob b, boolean store ) {
		String contentUri;
		if( store && shouldLinkStored && b instanceof File && blobSink instanceof FileForBlobGetter && blobSink instanceof Identifier ) {
			// It may be that this should be part of the blob sink, 
			// so that Sha1BlobStore can check hashes while importing
			File importFile = (File)b;
			File storeFile = ((FileForBlobGetter)blobSink).getFileForBlob(b);
			if( !storeFile.exists() ) {
				FileUtil.mkParentDirs(storeFile);
				Linker.getInstance().link( importFile, storeFile );
				storeFile.setReadOnly();
			}
			contentUri = ((Identifier)blobSink).identify(b);
		} else if( store ) {
			contentUri = blobSink.push(b);
		} else {
			contentUri = ((Identifier)blobSink).identify(b);
		}

		if( shouldRelinkImported && b instanceof File && ((File)b).isFile() && blobSink instanceof StoreFileGetter ) {
			File relinkTo = ((StoreFileGetter)blobSink).getStoreFile(contentUri);
			if( relinkTo != null ) {
				//System.err.println( "Relinking " + file + " to " + relinkTo );
				Linker.getInstance().relink( relinkTo, (File)b );
			}
			relinkTo.setReadOnly();
		}
		if( importListener != null ) importListener.objectImported( b, contentUri );
		return new Ref(contentUri);
	}
	
	public Ref importDirectory( Directory dir ) {
		String uri = RdfNamespace.URI_PARSE_PREFIX + importBlob(createMetadataBlob(new RdfDirectory(dir, entityTargetRdfifier), RdfNamespace.CCOUCH_NS), shouldStoreDirs).targetUri;
		if( importListener != null ) importListener.objectImported( dir, uri );
		return new Ref(uri);
	}
	
	public Ref importObject( Object obj ) {
		obj = getImportableObject(obj);
		if( obj instanceof Directory ) {
			return importDirectory( (Directory)obj );
		} else if( obj instanceof Blob ) {
			return importBlob( (Blob)obj, shouldStoreFiles );
		} else {
			throw new RuntimeException( "Don't know how to import " + obj.getClass().getName() );
		}
	}
	
	public String push( Object obj ) {
		return importObject( obj ).targetUri;
	}
		
	//// Name stuff ////

	public long getHighestNameVersion( String name ) {
		File nameDir = namedStore.getStoreFile(name);
		FileUtil.mkdirs(nameDir);
		String[] nums = nameDir.list();
		long highest = 0;
		for( int i=0; i<nums.length; ++i ) {
			if( nums[i].matches("^\\d+") ) {
				long k = Long.parseLong(nums[i]);
				if( k > highest ) highest = k;
			}
		}
		return highest;
	}
	
	public String getNextFilenameForName( String name ) {
		return name + "/" + (getHighestNameVersion(name)+1);
	}
	
	public void saveRedirect( String name, String targetType, String targetRef ) {
		RdfNode redirect = getRedirectRdfNode( targetType, targetRef );
		Blob b = createMetadataBlob(redirect, RdfNamespace.CCOUCH_NS);
		namedStore.put( getNextFilenameForName(name) + "-redirect", b );
	}
	
	/** Attempt to name a stored blob by linking to the store file.
	 * 
	 * If hardlinking is turned off or not supported, this should instead
	 * create a copy of the blob (if the blob is given)
	 * 
	 * @param name - under what name this link should be saved
	 * @param destUri - URI to hardlink to
	 * @param blob - blob to copy in case a link can't be made
	 */
	public void saveLink( String name, String targetUri, Blob blob ) {
		String filename = getNextFilenameForName(name);
		
		if( blobSink instanceof StoreFileGetter ) {
			File targetFile = ((StoreFileGetter)blobSink).getStoreFile(targetUri);
			if( targetFile != null ) {
				Linker.getInstance().link( targetFile, namedStore.getStoreFile(filename) );
				return;
			}
		}
		
		if( blob == null ) {
			throw new RuntimeException("Could not create hard link to " + targetUri + ", and no blob given to copy");
		}
		
		namedStore.put(filename, blob);
	}
	
	public String saveHead( RdfNode commit, String name ) {
		Blob b = createMetadataBlob(commit, RdfNamespace.CCOUCH_NS);
		String commitUri = blobSink.push(b);
		if( shouldStoreHeads && name != null ) saveLink( name, commitUri, b );
		return commitUri;
	}
	
	public String saveHead( String name, String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode commit = getCommitRdfNode(targetType, targetUri, date, creator, description, parentUris );
		return saveHead( commit, name );
	}
}
