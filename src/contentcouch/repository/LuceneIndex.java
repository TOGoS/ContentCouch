package contentcouch.repository;

import java.io.IOException;
import java.util.Iterator;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.StaleReaderException;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

public class LuceneIndex {
	Directory dir;
//	IndexReader indexReader;
//	IndexWriter indexWriter;
	Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
	String defaultSearchField = "tags";
	
	public LuceneIndex( Directory dir ) {
		this.dir = dir;
	}
	
	//// Low-level functions
	
	public void addDocument( IndexWriter indexWriter, Document doc ) {
		try {
			indexWriter.addDocument(doc);
		} catch( CorruptIndexException e ) {
			throw new RuntimeException(e);
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
		
	public TopDocs search( IndexReader indexReader, Query q, int nDocs ) {
		try {
			return new IndexSearcher( indexReader ).search( q, nDocs );
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public Query parseQuery( String qs ) {
		try {
			return new QueryParser(defaultSearchField, analyzer).parse(qs);
		} catch( ParseException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public TopDocs search( IndexReader indexReader, String qs, int nDocs ) {
		return search( indexReader, parseQuery(qs), nDocs );
	}
	
	//// For treating search results as iterators
	
	static class ArrayIterator implements Iterator {
		Object[] items;
		int position;
		int end;
		
		public ArrayIterator( Object[] items, int start, int count ) {
			this.position = start;
			this.end = start+count;
			if( this.end > items.length ) this.end = items.length;
		}
		
		public Object next() {
			return this.items[position++];
		}
		
		public boolean hasNext() {
			return this.position < this.end;
		}
		
		public void remove() {
			throw new UnsupportedOperationException("Can't remove from array iterator");
		}
	}

	abstract class TransformingIterator implements Iterator {
		Iterator backingIterator;
		public TransformingIterator(Iterator backingIterator) {
			this.backingIterator = backingIterator;
		}
		
		protected Object nextRawValue() {
			return backingIterator.next();
		}
		
		abstract Object transform(Object rawValue);
		
		public Object next() {
			return transform(nextRawValue());
		}
		
		public boolean hasNext() {
			return backingIterator.hasNext();
		}
		
		public void remove() {
			backingIterator.remove();
		}
	}
	
	static class TopDocsDocumentIterator extends ArrayIterator {
		int currentDocId = -1;
		
		IndexReader indexReader;
		public TopDocsDocumentIterator( IndexReader ir, TopDocs docs, int begin, int count ) {
			super( docs.scoreDocs, begin, count );
			this.indexReader = ir;
		}
		
		public void remove() {
			try {
				indexReader.deleteDocument(currentDocId);
			} catch( StaleReaderException e ) {
				throw new RuntimeException(e);
			} catch( CorruptIndexException e) {
				throw new RuntimeException(e);
			} catch( LockObtainFailedException e) {
				throw new RuntimeException(e);
			} catch( IOException e ) {
				throw new RuntimeException(e);
			}
		}
		
		Object transform(Object item) {
			ScoreDoc sd = (ScoreDoc)item;
			try {
				return indexReader.document(currentDocId = sd.doc);
			} catch(CorruptIndexException e) {
				throw new RuntimeException(e);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		public Object next() {
			return transform(super.next());
		}
	}
	
	public Iterator searchAsIterator( IndexReader indexReader, Query q, int nDocs ) {
		return new TopDocsDocumentIterator( indexReader, search(indexReader,q,nDocs), 0, nDocs );
	}
	
	//// Use index as k->v store
	
	public void storePair( IndexWriter indexWriter, String keyFieldName, String key, String valueFieldName, String value ) {
		Document newDoc = new Document();
		newDoc.add(new Field( keyFieldName, key, Store.YES, Index.NOT_ANALYZED ));
		newDoc.add(new Field( valueFieldName, value, Store.YES, Index.NOT_ANALYZED ));
		try {
			indexWriter.deleteDocuments(new Term(key, value));
			indexWriter.addDocument(newDoc);
		} catch (CorruptIndexException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	Object writeLock = new Object();
	
	protected IndexWriter openIndexWriter() {
		try {
			return new IndexWriter( dir, analyzer, MaxFieldLength.LIMITED );
		} catch( CorruptIndexException e ) {
			throw new RuntimeException(e);
		} catch (LockObtainFailedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void closeIndexWriter( IndexWriter indexWriter ) {
		try {
			indexWriter.close();
		} catch( CorruptIndexException e ) {
			return;
		} catch( IOException e ) {
			return;
		}
	}
	
	protected IndexReader openIndexReader( boolean writable ) {
		try {
			return IndexReader.open( dir, writable );
		} catch( CorruptIndexException e ) {
			throw new RuntimeException(e);
		} catch (LockObtainFailedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	protected void closeIndexReader( IndexReader indexReader ) {
		try {
			indexReader.close();
		} catch( CorruptIndexException e ) {
			return;
		} catch( IOException e ) {
			return;
		}
	}

	public void storePair( String keyFieldName, String key, String valueFieldName, String value ) {
		synchronized( writeLock ) {
			IndexWriter indexWriter = openIndexWriter();
			try {
				storePair( indexWriter, keyFieldName, key, valueFieldName, value );
			} finally {
				closeIndexWriter(indexWriter);
			}
		}
	}
	
	public Document getPairDocument( String keyFieldName, String key ) {
		IndexReader indexReader = openIndexReader(false);
		try {
			TopDocs r = search(indexReader, new TermQuery(new Term(keyFieldName, key)), 1);
			ScoreDoc[] s = r.scoreDocs;
			if( s.length > 0 ) try {
				return indexReader.document(s[0].doc);
			} catch (CorruptIndexException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return null;
		} finally {
			closeIndexReader(indexReader);
		}
	}
	
	
	public String getPairValue( String keyFieldName, String key, String valueFieldName ) {
		Document doc = getPairDocument(keyFieldName, key);
		return doc.get(valueFieldName);
	}
}
