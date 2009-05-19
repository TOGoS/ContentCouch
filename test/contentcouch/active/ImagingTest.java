package contentcouch.active;

import java.awt.image.BufferedImage;

import contentcouch.repository.ContentCouchRepository;
import contentcouch.store.Getter;
import contentcouch.value.Blob;
import junit.framework.TestCase;

public class ImagingTest extends TestCase {
	ContentCouchRepository repo; 
	protected ContentCouchRepository getRepo() {
		if( repo == null ) repo = new ContentCouchRepository("junk-repo");
		return repo;
	}
	
	protected Getter getGenericGetter() {
		return getRepo().getGenericGetter();
	}
	
	protected void assertInstanceOf( Class c, Object obj ) {
		if( obj == null ) assertTrue("Object is null", false);
		if( c.isAssignableFrom(obj.getClass()) ) return;
		assertTrue("A " + obj.getClass() + " is not an instance of " + c.getName(), false);
	}
	
	////

	public void testFetchImage() {
		String uri = "http://www.nuke24.net/images/bunny.jpg";
		Object result = getGenericGetter().get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testScaleImage() {
		String uri = "(contentcouch.graphics.scale-image http://www.nuke24.net/images/bunny.jpg scale=\"0.5\")";
		Object result = getGenericGetter().get(uri);
		assertInstanceOf( BufferedImage.class, result );
	}

	public void testSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image http://www.nuke24.net/images/bunny.jpg format=\"image/jpeg\")";
		Object result = getGenericGetter().get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testScaleAndSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image (contentcouch.graphics.scale-image http://www.nuke24.net/images/bunny.jpg) format=\"image/jpeg\")";
		Object result = getGenericGetter().get(uri);
		assertInstanceOf( Blob.class, result );
	}
}
