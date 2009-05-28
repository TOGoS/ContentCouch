// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

import contentcouch.blob.BlobUtil;
import contentcouch.date.DateUtil;
import contentcouch.file.FileBlob;
import contentcouch.file.FileDirectory;
import contentcouch.file.FileUtil;
import contentcouch.misc.Function1;
import contentcouch.path.PathUtil;
import contentcouch.rdf.RdfDirectory;
import contentcouch.rdf.RdfIO;
import contentcouch.rdf.RdfNamespace;
import contentcouch.rdf.RdfNode;
import contentcouch.repository.ContentCouchRepository;
import contentcouch.store.FileBlobMap;
import contentcouch.store.Identifier;
import contentcouch.store.Pusher;
import contentcouch.store.StoreFileGetter;
import contentcouch.value.Blob;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class Importer implements Pusher, StoreFileGetter {
	Pusher blobSink;
	FileBlobMap namedStore;
	public boolean shouldLinkStored;
	public boolean shouldRelinkImported;
	public ImportListener importListener;
	public boolean shouldStoreDirs;
	public boolean shouldStoreFiles;
	public boolean shouldStoreHeads;
	public boolean shouldNestSubdirs;
	
	public Function1 entityTargetRdfifier = new Function1() {
		public Object apply( Object input ) {
			if( input instanceof Directory && shouldNestSubdirs ) {
				return new RdfDirectory( (Directory)input, this );
			} else if( input instanceof Directory || input instanceof Blob || input instanceof File ) {
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
	
	public Importer( ContentCouchRepository repo, String storeSector ) {
		this( repo.getBlobStore(storeSector), (FileBlobMap)repo.headPutter );
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
			return BlobUtil.getBlob(obj);
		}
	}
	
	protected String getFileUri(File file ) {
		try {
			String path = file.getCanonicalPath();
			path = path.replace('\\', '/');
			if( path.charAt(1) == ':' ) {
				// Windows path!
				return "file:///" + PathUtil.uriEscapePath(path);
			} else if( path.charAt(0) == '/' ) { 
				// Unix path, leading slash already included!
				return "file://" + PathUtil.uriEscapePath(path);
			} else {
				return "file:" + PathUtil.uriEscapePath(path);
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
	
	//// StoreFileGetter ////
	
	public File getStoreFile( String uri ) {
		if( blobSink instanceof StoreFileGetter ) {
			return ((StoreFileGetter)blobSink).getStoreFile(uri);
		} else {
			return null;
		}
	}
	
	public File getStoreFile( Blob b ) {
		if( blobSink instanceof StoreFileGetter ) {
			return ((StoreFileGetter)blobSink).getStoreFile(b);
		} else {
			return null;
		}
	}

	public File getStoreFile( String uri, Blob b ) {
		if( blobSink instanceof StoreFileGetter ) {
			File f = ((StoreFileGetter)blobSink).getStoreFile(uri);
			if( f != null ) return f;
			return ((StoreFileGetter)blobSink).getStoreFile(b);
		} else {
			return null;
		}
	}
	
	public RdfNode getCommitRdfNode( String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode n = new RdfNode(RdfNamespace.CCOUCH_COMMIT);
		n.add(RdfNamespace.DC_CREATED, DateUtil.formatDate(date));
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
	
	protected void linkOrWriteBlob( Blob b, File dest ) {
		File importFile = (File)b;
		if( !dest.exists() ) {
			FileUtil.mkParentDirs(dest);
			Linker.getInstance().link( importFile, dest );
			dest.setReadOnly();
		}
	}
	
	/** Imports a blob and returns a Ref to its contents.
	 * 
	 * @param b the blob to be store.
	 * @param store whether or not to actually store it.  If false, will still return what it would have been stored as.
	 * @return Ref with a URN identifying the blob.  
	 */
	public Ref importBlob( Blob b, boolean store ) {
		String contentUri;
		File dest;
		if( store && shouldLinkStored && b instanceof File && blobSink instanceof StoreFileGetter && blobSink instanceof Identifier && (dest = ((StoreFileGetter)blobSink).getStoreFile(b)) != null ) {
			// It may be that this should be part of the blob sink, 
			// so that Sha1BlobStore can check hashes while importing
			linkOrWriteBlob( b, dest );
			contentUri = ((Identifier)blobSink).identify(b);
		} else if( store ) {
			contentUri = blobSink.push(b);
		} else {
			contentUri = ((Identifier)blobSink).identify(b);
		}

		if( shouldRelinkImported && b instanceof File && ((File)b).isFile() && blobSink instanceof StoreFileGetter ) {
			File relinkTo = ((StoreFileGetter)blobSink).getStoreFile(contentUri);
			if( relinkTo != null ) {
				Linker.getInstance().relink( relinkTo, (File)b );
				relinkTo.setReadOnly();
			}
		}
		if( importListener != null ) importListener.objectImported( b, contentUri );
		return new Ref(contentUri);
	}
	
	public RdfNode rdfifyDirectory( Directory dir ) {
		return new RdfDirectory(dir, entityTargetRdfifier);
	}
	
	public Ref importDirectory( Directory dir ) {
		String uri = RdfNamespace.URI_PARSE_PREFIX + importBlob(createMetadataBlob(rdfifyDirectory(dir), RdfNamespace.CCOUCH_NS), shouldStoreDirs).targetUri;
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
	public void saveNamedHead( String name, String targetUri, Blob blob ) {
		String filename = getNextFilenameForName(name);
		File targetFile;
		if( shouldLinkStored && (targetFile = getStoreFile( targetUri, blob )) != null ) {
			Linker.getInstance().link( targetFile, namedStore.getStoreFile(filename) );
		} else if( blob == null ) {
			throw new RuntimeException("Could not create hard link to " + targetUri + ", and no blob given to copy");
		} else {
			namedStore.put(filename, blob);
		}
	}
	
	public String saveHead( RdfNode commit, String name ) {
		Blob b = createMetadataBlob(commit, RdfNamespace.CCOUCH_NS);
		String commitUri = importBlob( b, shouldStoreHeads ).targetUri;
		if( shouldStoreHeads && name != null ) saveNamedHead( name, commitUri, b );
		return RdfNamespace.URI_PARSE_PREFIX + commitUri;
	}
	
	public String saveHead( String name, String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode commit = getCommitRdfNode(targetType, targetUri, date, creator, description, parentUris );
		return saveHead( commit, name );
	}
}
