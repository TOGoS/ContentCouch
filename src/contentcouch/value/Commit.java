package contentcouch.value;

import java.util.Date;

public interface Commit {
	public Object getTarget();
	public String getMessage();
	public String getAuthor();
	public Date getDate();
	public Object[] getParents();
}
