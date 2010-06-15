package contentcouch.merge;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import contentcouch.misc.ValueUtil;

public class Changeset {
	Map fileChanges = new HashMap();
	
	public void addChange( FileChange fa ) {
		fileChanges.put(fa.getPath(), fa);
	}
	
	protected void dump( Writer w, FileChange c ) throws IOException {
		if( c.getPrev() != null ) dump(w,c.getPrev());
		
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
		ArrayList changes = new ArrayList(fileChanges.values());
		Collections.sort(changes);
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
