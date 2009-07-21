package contentcouch.directory;

import java.util.Iterator;

import contentcouch.misc.Function1;

public class FilterIterator implements Iterator {
	Iterator source;
	Function1 filter;
	Object next = null;
	
	public FilterIterator( Iterator source, Function1 filter ) {
		this.source = source;
		this.filter = filter;
	}
	
	public boolean hasNext() {
		while( next == null && source.hasNext() ) {
			next = filter.apply(source.next());
		}
		return next != null;
	}

	public Object next() {
		hasNext();
		Object n = next;
		next = null;
		return n;
	}

	public void remove() {
		source.remove();
	}
}
