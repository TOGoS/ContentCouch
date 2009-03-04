package contentcouch.repository;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import contentcouch.store.Getter;

public class CCouchHeadGetter implements Getter {
	public ContentCouchRepository mainRepo;
	
	public CCouchHeadGetter( ContentCouchRepository mainRepo ) {
		this.mainRepo = mainRepo;
	}
	
	protected Pattern PATTERN = Pattern.compile("^x-ccouch-head:(?://([^/]*)/|/)?(.*)$");
	
	public Object get( String identifier ) {
		Matcher m = PATTERN.matcher(identifier);
		if( !m.matches() ) return null;

		String repoName = m.group(1);
		String headName = m.group(2);
		
		ContentCouchRepository repo;
		if( repoName == null ) {
			repo = mainRepo;
			Object o = mainRepo.getHead(headName);
			if( o != null ) return o;
			for( Iterator i=repo.localRepositories.iterator(); i.hasNext(); ) {
				ContentCouchRepository r = (ContentCouchRepository)i.next();
				o = r.getHead(headName);
				if( o != null ) return o;
			}
			return null;
		} else {
			repo = (ContentCouchRepository)mainRepo.namedRepositories.get(repoName);
			if( repo == null ) {
				System.err.println("Could not locate repo: '" + repoName + "'");
				return null;
			}
			return repo.getHead(headName);
		}
	}
}
