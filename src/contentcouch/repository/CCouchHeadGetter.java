package contentcouch.repository;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.eekboom.utils.Strings;

import contentcouch.app.Log;
import contentcouch.store.Getter;

public class CCouchHeadGetter implements Getter {
	public ContentCouchRepository mainRepo;
	public boolean checkRemotes = false;
	
	public CCouchHeadGetter( ContentCouchRepository mainRepo ) {
		this.mainRepo = mainRepo;
	}
	
	protected Pattern PATTERN = Pattern.compile("^x-ccouch-head:(?://([^/]*)/|/)?(.*)$");

	public String findHead( String headName ) {
		String latest = mainRepo.findHead(headName);
		String maybeLatest = null;
		for( Iterator i=mainRepo.localRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository r = (ContentCouchRepository)i.next();
			maybeLatest = r.findHead(headName);
			if( latest == null || (maybeLatest != null && Strings.compareNatural(maybeLatest, latest) > 0) ) latest = maybeLatest;
		}
		if( checkRemotes ) for( Iterator i=mainRepo.remoteRepositories.iterator(); i.hasNext(); ) {
			ContentCouchRepository r = (ContentCouchRepository)i.next();
			maybeLatest = r.findHead(headName);
			if( latest == null || (maybeLatest != null && Strings.compareNatural(maybeLatest, latest) > 0) ) latest = maybeLatest;
		}
		ContentCouchRepository r = mainRepo.cacheRepository;
		if( r != null ) maybeLatest = r.findHead(headName);
		if( latest == null || (maybeLatest != null && Strings.compareNatural(maybeLatest, latest) > 0) ) latest = maybeLatest;
		if( latest == null ) return null; // nobody has it
		return latest;
	}
	
	public Object get( String identifier ) {
		Matcher m = PATTERN.matcher(identifier);
		if( !m.matches() ) return null;

		String repoName = m.group(1);
		String headName = m.group(2);
		
		ContentCouchRepository repo;
		if( repoName == null ) {
			if( headName.endsWith("/latest") ) headName = findHead(headName);
			if( headName == null ) return null;

			Object o;
			if( (o = mainRepo.getHead(headName)) != null ) return o;
			for( Iterator i=mainRepo.localRepositories.iterator(); i.hasNext(); ) {
				ContentCouchRepository r = (ContentCouchRepository)i.next();
				if( (o = r.getHead(headName)) != null ) return o;
			}
			ContentCouchRepository r = mainRepo.cacheRepository;
			if( r != null && (o = r.getHead(headName)) != null ) return o;
			if( checkRemotes ) for( Iterator i=mainRepo.remoteRepositories.iterator(); i.hasNext(); ) {
				ContentCouchRepository rr = (ContentCouchRepository)i.next();
				if( (o = rr.getHead(headName)) != null ) return o;
			}
			return null;
		} else {
			repo = (ContentCouchRepository)mainRepo.namedRepositories.get(repoName);
			if( repo == null ) {
				Log.log(Log.LEVEL_ERRORS, "Could not locate repo: '" + repoName + "'");
				return null;
			}
			return repo.getHead(headName);
		}
	}
}
