package contentcouch.app.servlet;

public class ExploreComponent extends ProcessorComponent {
	public ExploreComponent(String externalPath) {
		super(externalPath);
	}
	protected String getProcessorActiveFunctionName() {
		return "contentcouch.explorify";
	}
	protected String getVerb() {
		return "Exploring";
	}
}
