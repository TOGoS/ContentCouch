// -*- tab-width:4 -*-
package contentcouch.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import contentcouch.data.Blob;
import contentcouch.data.BlobUtil;
import contentcouch.data.FileBlob;
import contentcouch.file.FileUtil;
import contentcouch.store.Pusher;
import contentcouch.store.FileBlobMap;
import contentcouch.store.FileForBlobGetter;
import contentcouch.store.FileGetter;
import contentcouch.store.Sha1BlobStore;
import contentcouch.store.UrnForBlobGetter;
import contentcouch.xml.RDF;
import contentcouch.xml.RDF.RdfNode;
import contentcouch.xml.RDF.Ref;

public class Importer {
	Pusher blobSink;
	FileBlobMap namedStore;
	public boolean shouldLinkStored;
	public boolean shouldRelinkImported;
	
	public Importer( Pusher blobSink, FileBlobMap namedStore ) {
		this.blobSink = blobSink;
		this.namedStore = namedStore;
	}
	
	protected String uriEscapePath( String path ) {
		// TODO
		return path;
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
	
	protected Blob createMetadataBlob( RDF.RdfNode desc, String defaultNamespace ) {
		return BlobUtil.getBlob(RDF.xmlEncodeRdf(desc, defaultNamespace));
	}
	
	protected Blob createMetadataBlob( String aboutUri, Map properties ) {
		RDF.Description desc = new RDF.Description();
		desc.importValues(properties);
		desc.about = new RDF.Ref(aboutUri);
		return createMetadataBlob(desc, null);
	}
	
	public String importContent( Blob b ) {
		String contentUri;
		if( shouldLinkStored && b instanceof FileBlob && blobSink instanceof FileForBlobGetter && blobSink instanceof UrnForBlobGetter ) {
			// It may be that this should be part of the blob sink, 
			// so that Sha1BlobStore can check hashes while importing
			File importFile = ((FileBlob)b).getFile();
			File storeFile = ((FileForBlobGetter)blobSink).getFileForBlob(b);
			if( !storeFile.exists() ) {
				FileUtil.mkParentDirs(storeFile);
				Linker.getInstance().link( importFile, storeFile );
				storeFile.setReadOnly();
			}
			contentUri = ((UrnForBlobGetter)blobSink).getUrnForBlob(b);
		} else {
			contentUri = blobSink.push( b );
		}

		if( shouldRelinkImported && b instanceof FileBlob && ((FileBlob)b).getFile().isFile() && blobSink instanceof FileGetter ) {
			File relinkTo = ((FileGetter)blobSink).getFile(contentUri);
			if( relinkTo != null ) {
				//System.err.println( "Relinking " + file + " to " + relinkTo );
				Linker.getInstance().relink( relinkTo, ((FileBlob)b).getFile() );
			}
			relinkTo.setReadOnly();
		}

		return contentUri;
	}
	
	public String importFileContent( File file, FileImportListener fileImportListener ) {
		String contentUri = importContent( new FileBlob(file) );
		if( fileImportListener != null ) {
			fileImportListener.fileImported( file, contentUri );
		}
		return contentUri;
	}
	
	public File getFile( String uri ) {
		if( blobSink instanceof FileGetter ) {
			return ((FileGetter)blobSink).getFile(uri);
		} else {
			return null;
		}
	}
	
	protected void addTarget( RdfNode node, String targetType, String targetUri ) {
		node.add(RDF.CCOUCH_TARGETTYPE, targetType);
		node.add(RDF.CCOUCH_TARGET, new Ref(targetUri));
	}
	
	public RdfNode getCommitRdfNode( String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_COMMIT;
		n.add(RDF.DC_CREATED, RDF.CCOUCH_DATEFORMAT.format(date));
		n.add(RDF.DC_CREATOR, creator);
		n.add(RDF.DC_DESCRIPTION, description);
		addTarget( n, targetType, targetUri );
		if( parentUris != null ) for( int i=0; i<parentUris.length; ++i ) {
			n.add(RDF.CCOUCH_PARENT, new Ref(parentUris[i]));
		}
		return n;
	}
	
	public RdfNode getRedirectRdfNode( String targetType, String targetUri ) {
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_REDIRECT;
		addTarget( n, targetType, targetUri );
		return n;
	}
	
	public RdfNode getDirectoryEntryRdfNode( File file, FileImportListener fileImportListener ) {
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_DIRECTORYENTRY;
		n.add(RDF.CCOUCH_NAME, file.getName());
		if( file.isDirectory() ) {
			addTarget(n, RDF.OBJECT_TYPE_DIRECTORY, importDirectory(file, fileImportListener));
		} else {
			n.add(RDF.DC_MODIFIED, RDF.CCOUCH_DATEFORMAT.format(new Date(file.lastModified())));
			n.add(RDF.CCOUCH_SIZE, String.valueOf(file.length()) );
			addTarget(n, RDF.OBJECT_TYPE_BLOB, importFileContent(file, fileImportListener));
		}
		return n;
	}

	public String importDirectory( File dir, FileImportListener fileImportListener ) {
		if( !dir.isDirectory() ) {
			throw new RuntimeException("Cannot import a plain file with importDir!");
		}
		// TODO: Use RDF.rdfifyDirectory
		RdfNode n = new RdfNode();
		n.typeName = RDF.CCOUCH_DIRECTORY;
		List entries = new ArrayList();
		File[] subFiles = dir.listFiles();
		for( int i=0; i<subFiles.length; ++i ) {
			File subFile = subFiles[i];
			if( subFile.getName().startsWith(".") ) continue;
			entries.add( getDirectoryEntryRdfNode(subFile, fileImportListener) );
		}
		n.add(RDF.CCOUCH_ENTRIES, entries);
		String objectUri = RDF.URI_PARSE_PREFIX + importContent(createMetadataBlob(n, RDF.CCOUCH_NS));
		if( fileImportListener != null ) {
			fileImportListener.fileImported( dir, objectUri );
		}
		return objectUri;
	}
	
	public String importFileOrDirectory( File file, FileImportListener fileImportListener ) {
		if( file.isDirectory() ) {
			return importDirectory( file, fileImportListener );
		} else {
			return importFileContent( file, fileImportListener );
		}
	}
	
	public void recursivelyImportFiles(File dir, FileImportListener fileImportListener) {
		if( dir.isFile() ) {
			importFileContent(dir, fileImportListener);
		} else {
			String[] subNames = dir.list();
			for( int i=0; i<subNames.length; ++i ) {
				String subName = subNames[i];
				if( subName.startsWith(".") ) continue;
				recursivelyImportFiles(new File(dir + "/" + subName), fileImportListener);
			}
		}
	}
	
	//// Name stuff ////

	public long getHighestNameVersion( String name ) {
		File nameDir = namedStore.getFile(name);
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
		Blob b = createMetadataBlob(redirect, RDF.CCOUCH_NS);
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
		
		if( blobSink instanceof FileGetter ) {
			File targetFile = ((FileGetter)blobSink).getFile(targetUri);
			if( targetFile != null ) {
				Linker.getInstance().link( targetFile, namedStore.getFile(filename) );
				return;
			}
		}
		// Otherwise it didn't work - copy instead.
		if( blob != null ) {
			namedStore.put(filename, blob);
		}
		// And if we can't do that, blow up
		throw new RuntimeException("Could not create hard link to " + targetUri + ", and no blob given to copy");
	}
	
	public String saveHead( String name, String targetType, String targetUri, Date date, String creator, String description, String[] parentUris ) {
		RdfNode commit = getCommitRdfNode(targetType, targetUri, date, creator, description, parentUris );
		Blob b = createMetadataBlob(commit, RDF.CCOUCH_NS);
		String commitUri = blobSink.push(b);
		if( name != null ) {
			saveLink( name, commitUri, b );
		}
		return commitUri;
		//saveRedirect( name, RDF.OBJECT_TYPE_RDF, commitUri );
	}
	
	public static void main(String[] argc) {
		String filename = "junk/import";
		
		File file = new File(filename);
		Importer importer = new Importer(new Sha1BlobStore("junk-datastore"), null);
		//importer.recursivelyImportFiles(file);
		
		String ref = importer.importDirectory(file, null);
		System.out.println(ref);
	}
}
