package contentcouch.junk;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

public class LuceneTest extends TestCase {
	Directory dir;
	
	public void setUp() {
		try {
			dir = FSDirectory.open(new File("junk.lucdir"));
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void tearDown() {
		
	}
	
	protected IndexWriter createIndexWriter() {
		try {
			return new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), MaxFieldLength.LIMITED);
		} catch( CorruptIndexException e) {
			throw new RuntimeException(e);
		} catch (LockObtainFailedException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void testInsert() {
		Document d = new Document();
		d.add(new Field("tags", "testy festy", Field.Store.YES, Field.Index.ANALYZED));
		IndexWriter w = createIndexWriter();
		try {
			w.addDocument(d);
			w.commit();
			w.close();
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}
	
	public void testSearch() {
		try {
			Searcher s = new IndexSearcher(dir, true);
			TopDocs topDocs = s.search(new TermQuery(new Term("tags","testy")), 40);
			ScoreDoc[] docs = topDocs.scoreDocs;
			for( int i=0; i<docs.length; ++i ) {
				Document doc = s.doc(docs[0].doc);
				System.err.println(doc.get("tags"));
			}
		} catch( Exception e ) {
			throw new RuntimeException(e);
		}
	}
}
