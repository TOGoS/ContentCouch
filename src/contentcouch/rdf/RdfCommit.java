/**
 * 
 */
package contentcouch.rdf;

import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import contentcouch.date.DateUtil;
import contentcouch.value.Commit;

public class RdfCommit extends RdfNode implements Commit {	
	public RdfCommit() {
		super(RdfNamespace.CCOUCH_COMMIT);
	}
	
	public Object getTarget() {
		return this.getSingle(RdfNamespace.CCOUCH_TARGET);
	}
	
	public Date getDate() {
		String lm = (String)this.getSingle(RdfNamespace.DC_CREATED);
		if( lm == null ) return null;
		try {
			return DateUtil.parseDate(lm);
		} catch (ParseException e) {
			System.err.println("Error parsing created date in " + this.sourceUri);
			return null;
		}
	}
	
	public String getAuthor() {
		return (String)this.getSingle(RdfNamespace.DC_CREATOR);
	}

	public String getMessage() {
		return (String)this.getSingle(RdfNamespace.DC_DESCRIPTION);
	}

	public Object[] getParents() {
		Set s = this.getSet(RdfNamespace.CCOUCH_PARENT);
		return s.toArray();
	}
	
	public String getUri() {
		return RdfNamespace.URI_PARSE_PREFIX + this.sourceUri;
	}
}