package contentcouch.rdf;

import contentcouch.value.Commit;
import contentcouch.value.Directory;

public class CCouchRdfInterpreter {
	public static CCouchRdfInterpreter instance = new CCouchRdfInterpreter();

	public static CCouchRdfInterpreter getInstance() {
		return instance;
	}
	
	public Object interpretSubject( RdfNode node ) {
		// Cheat for now, as long as node *is* the subject
		if( node instanceof Directory ) {
			return node;
		} else if( node instanceof Commit ) {
			return node;
		} else if( node instanceof Directory.Entry ) {
			return node;
		}
		
		return null;
	}
}
