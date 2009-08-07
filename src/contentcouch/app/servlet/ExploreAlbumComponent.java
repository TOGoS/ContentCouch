package contentcouch.app.servlet;

public class ExploreAlbumComponent extends ProcessorComponent {
	public ExploreAlbumComponent(String externalPath) {
		super(externalPath);
	}
	protected String getProcessorActiveFunctionName() {
		return "contentcouch.photoalbum.make-album-page";
	}
	protected String getVerb() {
		return "Viewing";
	}
}
