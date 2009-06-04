package contentcouch.misc;

import java.util.Iterator;

public class FlatteningIterator implements Iterator {
	Iterator currentIterator = null;
	Iterator iteratorIterator;
	
	public FlatteningIterator( Iterator i ) {
		iteratorIterator = i;
	}
	
	public boolean hasNext() {
		if( currentIterator != null && currentIterator.hasNext() ) return true;
		while( iteratorIterator.hasNext() ) {
			currentIterator = (Iterator)iteratorIterator.next();
			if( currentIterator.hasNext() ) return true;
		}
		return false;
	}

	public Object next() {
		if( currentIterator != null && currentIterator.hasNext() ) return currentIterator.next();
		while( iteratorIterator.hasNext() ) {
			currentIterator = (Iterator)iteratorIterator.next();
			if( currentIterator.hasNext() ) return currentIterator.next();
		}
		return null;
	}

	public void remove() {
		currentIterator.remove();
	}
}
