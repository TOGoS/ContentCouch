package contentcouch.app;

import java.util.ArrayList;
import java.util.Iterator;

public class MultiArgumentHandler implements ArgumentHandler {
	protected ArrayList handlers = new ArrayList();
	
	public void addArgumentHandler( ArgumentHandler h ) {
		handlers.add( h );
	}
	
	public boolean handleArguments( String current, Iterator rest ) {
		for( int i=handlers.size()-1; i>=0; --i ) {
			if( ((ArgumentHandler)handlers.get(i)).handleArguments(current, rest) ) return true;
		}
		return false;
	}

}
