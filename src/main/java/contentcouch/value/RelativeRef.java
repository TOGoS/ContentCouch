package contentcouch.value;

public interface RelativeRef extends Ref {
	public boolean isRelative();
	public String getTargetBaseUri();
	public String getTargetRelativeUri();
}
