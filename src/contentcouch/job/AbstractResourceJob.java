package contentcouch.job;

public abstract class AbstractResourceJob implements ResourceJob
{
	protected String resourceId;
	
	public AbstractResourceJob( String resourceId ) {
		this.resourceId = resourceId;
	}
	
	public String getResourceId() {
		return resourceId;
	}
}
