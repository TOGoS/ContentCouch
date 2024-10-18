package contentcouch.rdf;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import contentcouch.context.Config;
import contentcouch.date.DateUtil;
import contentcouch.misc.Function1;
import contentcouch.value.Commit;
import contentcouch.value.Ref;

public class RdfCommit extends RdfNode implements Commit
{
	private static final long serialVersionUID = 1L;
	
	public RdfCommit() {
		super(CCouchNamespace.COMMIT);
	}
	
	public RdfCommit( Commit c, Function1 targetRdfifier, Function1 parentRdfifier ) {
		this();
		if( targetRdfifier == null ) targetRdfifier = RdfDirectory.DEFAULT_TARGET_RDFIFIER;
		if( parentRdfifier == null ) parentRdfifier = RdfDirectory.DEFAULT_TARGET_RDFIFIER;
		if( c.getAuthor() != null ) this.add(DcNamespace.DC_CREATOR, c.getAuthor());
		if( c.getDate() != null ) this.add(DcNamespace.DC_CREATED, DateUtil.formatDate(c.getDate()));
		if( c.getMessage() != null ) this.add(DcNamespace.DC_DESCRIPTION, c.getMessage());
		Object[] parents = c.getParents();
		if( parents != null ) for( int i=0; i<parents.length; ++i ) {
			this.add(CCouchNamespace.PARENT, parentRdfifier.apply(parents[i]));
		}
		this.add(CCouchNamespace.TARGET, targetRdfifier.apply(c.getTarget()));
	}
	
	public RdfCommit( Commit c, Ref target ) {
		this();
		if( c.getAuthor() != null ) this.add(DcNamespace.DC_CREATOR, c.getAuthor());
		if( c.getDate() != null ) this.add(DcNamespace.DC_CREATED, DateUtil.formatDate(c.getDate()));
		if( c.getMessage() != null ) this.add(DcNamespace.DC_DESCRIPTION, c.getMessage());
		Object[] parents = c.getParents();
		if( parents != null ) for( int i=0; i<parents.length; ++i ) {
			this.add(CCouchNamespace.PARENT, parents[i]);
		}
		this.add(CCouchNamespace.TARGET, target);
	}
	
	public RdfCommit( Commit c, Function1 targetRdfifier ) {
		this( c, targetRdfifier, null );
	}
	
	public Object getTarget() {
		return this.getSingle(CCouchNamespace.TARGET);
	}
	
	public Date getDate() {
		String lm = (String)this.getSingle(DcNamespace.DC_CREATED);
		if( lm == null ) return null;
		try {
			return DateUtil.parseDate(lm);
		} catch (ParseException e) {
			System.err.println("Error parsing created date in " + this.sourceUri);
			return null;
		}
	}
	
	public String getAuthor() {
		return (String)this.getSingle(DcNamespace.DC_CREATOR);
	}

	public String getMessage() {
		return (String)this.getSingle(DcNamespace.DC_DESCRIPTION);
	}

	public Object[] getParents() {
		Set s = this.getSet(CCouchNamespace.PARENT);
		if( s == null ) return new Object[]{};
		return s.toArray();
	}
	
	public String getUri() {
		return Config.getRdfSubjectPrefix() + this.sourceUri;
	}
}