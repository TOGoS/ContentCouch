package contentcouch.rdf;

import contentcouch.value.Commit;
import contentcouch.value.Directory;
import contentcouch.misc.Function1;

public class RdfUtil {
	public static RdfNode toRdfNode( Object o, Function1 targetRdfifier ) {
		if( o == null ) return null;
		if( o instanceof RdfNode ) return (RdfNode)o;
		if( o instanceof Directory ) return new RdfDirectory((Directory)o, targetRdfifier );
		if( o instanceof Commit ) return new RdfCommit((Commit)o, targetRdfifier );
		throw new RuntimeException("Don't know how to turn " + o.getClass().getName() + " into RDF node");
	}
}
