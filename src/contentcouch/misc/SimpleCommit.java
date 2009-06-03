package contentcouch.misc;

import java.util.Date;

import contentcouch.value.Commit;

public class SimpleCommit implements Commit {
	public Object[] parents;
	public String author;
	public String message;
	public Date date;
	public Object target;
	
	public Object[] getParents() { return parents; }
	public String getAuthor() { return author; }
	public String getMessage() { return message; }
	public Date getDate() { return date; }
	public Object getTarget() { return target; }
}
