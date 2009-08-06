package togos.mf;

import java.util.ArrayList;
import java.util.Iterator;

import togos.rra.Response;

public class MessageIterators {

	
	public static MessageIterator createSingleResultIterator(Response res) {
		ArrayList responses = new ArrayList();
		responses.add(res);
		return wrapIterator( responses.iterator() );
	}

	public static final MessageIterator NORESPONSE = new MessageIterator() {
		public boolean hasNext() {return false;}
		public Object next() {return null;}
		public void remove() {}
		public void close() {}
	};
	
	public static final MessageIterator wrapIterator(final Iterator i) {
		return new MessageIterator() {
			public void close() {}
			public boolean hasNext() { return i.hasNext(); }
			public Object next() { return i.next(); }
			public void remove() { i.remove(); }
		};
	}
}
