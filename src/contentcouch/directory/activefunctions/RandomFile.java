package contentcouch.directory.activefunctions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import togos.mf.api.Request;
import togos.mf.api.Response;
import togos.mf.api.ResponseCodes;
import togos.mf.base.BaseResponse;
import contentcouch.active.BaseActiveFunction;
import contentcouch.directory.DirectoryUtil;
import contentcouch.directory.EntryFilters;
import contentcouch.directory.LongPathEntryWrapper;
import contentcouch.misc.Function1;
import contentcouch.misc.SimpleDirectory;
import contentcouch.misc.ValueUtil;
import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Directory;

public class RandomFile extends BaseActiveFunction {
	protected static List homogenizeEntries( Collection entries ) {
		List files = new ArrayList();
		List dirs = new ArrayList();
		
		for( Iterator i=entries.iterator(); i.hasNext(); ) {
			Directory.Entry de = (Directory.Entry)i.next();
			String ttype = de.getTargetType();
			if( CcouchNamespace.TT_SHORTHAND_DIRECTORY.equals(ttype) ) {
				dirs.add(de);
			} else if( CcouchNamespace.TT_SHORTHAND_BLOB.equals(ttype) ) {
				files.add(de);
			}
		}
		
		if( dirs.size() == 0 && files.size() == 0 ) {
			return Collections.EMPTY_LIST;
		} else if( dirs.size() == 0 ) {
			return files;
		} else if( files.size() == 0 ) {
			return dirs;			
		} else {
			SimpleDirectory sd = new SimpleDirectory();
			for( Iterator i=files.iterator(); i.hasNext(); ) {
				sd.addDirectoryEntry((Directory.Entry)i.next());
			}
			dirs.add(new SimpleDirectory.Entry("__files", sd, CcouchNamespace.TT_SHORTHAND_DIRECTORY));
			return dirs;
		}
	}
	
	protected static String subPath( String outer, String inner ) {
		return outer == null ? inner : outer + "/" + inner;
	}
	
	protected static Directory.Entry getRandomEntry( Directory d, Random r, Function1 returnFilter, Function1 recurseFilter, String path ) {
		List homo = homogenizeEntries(d.getDirectoryEntrySet());
		if( homo.size() == 0 ) return null;
		
		int begin = r.nextInt(homo.size());
		for( int i=0; i<homo.size(); ++i ) {
			Directory.Entry e = (Directory.Entry)homo.get((begin+i)%homo.size());
			
			Directory.Entry ret = (Directory.Entry)returnFilter.apply(e);
			if( ret != null ) {
				return new LongPathEntryWrapper( ret, subPath(path,ret.getName()) );
			}
			
			Directory.Entry rec = (Directory.Entry)recurseFilter.apply(e);
			if( rec != null ) {
				Directory subDir = (Directory)DirectoryUtil.resolveTarget(e);
				ret = getRandomEntry( subDir, r, returnFilter, recurseFilter, subPath(path,rec.getName()) );
				if( ret != null ) return ret;
			}
		}
		return null;
	}
	
	public Response call(Request req, Map argumentExpressions) {
		Directory d = (Directory)getArgumentValue(req, argumentExpressions, "directory", null);
		if( d == null ) {
			return new BaseResponse( ResponseCodes.RESPONSE_CALLER_ERROR, "directory not specified" );
		}
		boolean returnEntries = ValueUtil.getBoolean( getArgumentValue(req, argumentExpressions, "return-entries", null), false);
		boolean returnRefs = ValueUtil.getBoolean( getArgumentValue(req, argumentExpressions, "return-refs", null), false);
		
		// TODO: allow "*.jpg"-style patterns to be passed in to be used as the filter

		Object seedo = getArgumentValue(req,argumentExpressions,"seed",null);
		Random r;
		if( seedo != null ) {
			r = new Random(ValueUtil.getNumber( seedo ).longValue());
		} else {
			r = new Random();
			// Skip the first few, since they're not usually all that random...
			r.nextLong();
			r.nextLong();
			r.nextLong();
		}
		
		Directory.Entry e = getRandomEntry( d, r, EntryFilters.BLOBFILTER, EntryFilters.DIRECTORYFILTER, null );
		
		return new BaseResponse( ResponseCodes.RESPONSE_NORMAL,
			returnEntries ? e : returnRefs ? e.getTarget() : DirectoryUtil.resolveTarget(e) );
	}
}
