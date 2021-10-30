package contentcouch.merge;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import contentcouch.misc.ValueUtil;

public class Changeset {
	Map fileChanges = new HashMap();
	
	public void addChange( FileChange fa ) {
		fileChanges.put(fa.getPath(), fa);
	}
	
	//// Get list of changes ////
	
	protected void collectOrderedChanges( FileChange c, List l ) {
		if( c.prev != null ) collectOrderedChanges(c.prev, l);
		l.add(c);
	}
	
	public void collectOrderedChanges( List l ) {
		ArrayList changes = new ArrayList(fileChanges.values());
		Collections.sort(changes);
		for( Iterator i=changes.iterator(); i.hasNext(); ) {
			collectOrderedChanges( (FileChange)i.next(), l );
		}
	}
	
	public List getOrderedChanges() {
		ArrayList l = new ArrayList();
		collectOrderedChanges(l);
		return l;
	}
	
	//// Dump ////
	
	protected void dump( Writer w, FileChange c ) throws IOException {
		if( c instanceof DirDelete ) {
			w.write("DD "+c.getPath()+"\n");
		} else if( c instanceof FileDelete ) {
			w.write("D  "+c.getPath()+"\n");
		} else if( c instanceof FileAdd ) {
			w.write("A  "+c.getPath()+"\n");
		} else {
			throw new RuntimeException("Don't know how to dump a "+ValueUtil.describe(c));
		}
	}

	public void dump( Writer w ) throws IOException {
		List changes = getOrderedChanges();
		for( Iterator i = changes.iterator(); i.hasNext(); ) {
			FileChange fc = (FileChange)i.next();
			dump(w,fc);
		}
	}
	
	public String dump() {
		StringWriter sw = new StringWriter();
		try {
			dump( sw );
		} catch( IOException e ) {
			throw new RuntimeException( e );
		}
		return sw.toString();
	}
}
