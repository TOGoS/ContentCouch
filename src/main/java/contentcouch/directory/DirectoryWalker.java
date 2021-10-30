package contentcouch.directory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import contentcouch.framework.TheGetter;
import contentcouch.rdf.CCouchNamespace;
import contentcouch.value.Directory;
import contentcouch.value.Ref;

public class DirectoryWalker implements Iterator {
	protected Stack entryIteratorStack = new Stack();
	protected Stack directoryStack = new Stack();
	protected boolean followRefs;
	protected Comparator entryComparator = EntryComparators.TYPE_THEN_NAME_COMPARATOR;
	
	protected void push( Directory d ) {
		ArrayList l = new ArrayList(d.getDirectoryEntrySet());
		Collections.sort(l, entryComparator);
		entryIteratorStack.push( l.iterator() );
		directoryStack.push(d);
	}
	
	protected void pop() {
		entryIteratorStack.pop();
		directoryStack.pop();
	}
	
	public DirectoryWalker(Directory d, boolean followRefs) {
		push(d);
		this.followRefs = followRefs;
	}
	
	public boolean hasNext() {
		while( entryIteratorStack.size() > 0 ) {
			if( ((Iterator)entryIteratorStack.peek()).hasNext() ) return true;
			pop();
		}
		return false;
	}
	
	/** Returns a copy of the current list of active directories,
	 * starting with the outermost and going inwards */
	public List getCurrentDirectories() {
		return new ArrayList(directoryStack);
	}
	/** Returns a copy of the current list of active directories,
	 * starting with the innermost and going outwards */
	public List getCurrentDirectoriesDownwards() {
		LinkedList goingDown = new LinkedList();
		for( Iterator i=directoryStack.iterator(); i.hasNext(); ) {
			goingDown.add(0, i.next());
		}
		return goingDown;
	}
	
	public Object next() {
		while( entryIteratorStack.size() > 0 ) {
			if( ((Iterator)entryIteratorStack.peek()).hasNext() ) {
				Directory.Entry e = (Directory.Entry)((Iterator)entryIteratorStack.peek()).next();
				Object target = e.getTarget();
				if( CCouchNamespace.DIRECTORY.equals(e.getTargetType()) && followRefs && target instanceof Ref ) {
					target = TheGetter.get( ((Ref)target).getTargetUri() );
				}
				if( e.getTarget() instanceof Directory ) {
					push( (Directory)e.getTarget() );
				}
				return e;
			} else {
				pop();
			}
		}
		return null;
	}
	
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
