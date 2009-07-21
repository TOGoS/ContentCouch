package contentcouch.directory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Stack;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.store.TheGetter;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class DirectoryWalker implements Iterator {
	protected Stack entryIteratorStack = new Stack();
	protected boolean followRefs;
	protected Comparator entryComparator = EntryComparators.TYPE_THEN_NAME_COMPARATOR;
	
	protected void push( Directory d ) {
		ArrayList l = new ArrayList(d.getDirectoryEntrySet());
		Collections.sort(l, entryComparator);
		entryIteratorStack.push( l.iterator() );
	}
	
	public DirectoryWalker(Directory d, boolean followRefs) {
		push(d);
		this.followRefs = followRefs;
	}
	
	public boolean hasNext() {
		while( entryIteratorStack.size() > 0 ) {
			if( ((Iterator)entryIteratorStack.peek()).hasNext() ) return true;
			entryIteratorStack.pop();
		}
		return false;
	}
	
	public Object next() {
		while( entryIteratorStack.size() > 0 ) {
			if( ((Iterator)entryIteratorStack.peek()).hasNext() ) {
				Directory.Entry e = (Directory.Entry)((Iterator)entryIteratorStack.peek()).next();
				Object target = e.getTarget();
				if( CcouchNamespace.DIRECTORY.equals(e.getTargetType()) && followRefs && target instanceof Ref ) {
					target = TheGetter.get( ((Ref)target).getTargetUri() );
				}
				if( e.getTarget() instanceof Directory ) {
					push( (Directory)e.getTarget() );
				}
				return e;
			} else {
				entryIteratorStack.pop();
			}
		}
		return null;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
