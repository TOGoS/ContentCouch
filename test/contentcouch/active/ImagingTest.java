package contentcouch.active;

import java.awt.image.BufferedImage;

import junit.framework.TestCase;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;
import contentcouch.value.Blob;

public class ImagingTest extends TestCase {
	MetaRepoConfig repo; 

	public void setUp() {
		repo = new MetaRepoConfig();
		repo.handleArguments(new String[]{"-repo","junk-repo/"}, 0, "./");
		TheGetter.globalInstance = repo.getRequestKernel();
	}
	
	protected void assertInstanceOf( Class c, Object obj ) {
		if( obj == null ) assertTrue("Object is null", false);
		if( c.isAssignableFrom(obj.getClass()) ) return;
		assertTrue("A " + obj.getClass() + " is not an instance of " + c.getName(), false);
	}
	
	////

	public void testFetchImage() {
		String uri = "http://www.nuke24.net/images/bunny.jpg";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testScaleImage() {
		String uri = "(contentcouch.graphics.scale-image http://www.nuke24.net/images/bunny.jpg scale=\"0.5\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( BufferedImage.class, result );
	}

	public void testSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image http://www.nuke24.net/images/bunny.jpg format=\"image/jpeg\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testSerializeImageWithQuality() {
		String uri = "(contentcouch.graphics.serialize-image http://www.nuke24.net/images/bunny.jpg format=\"image/jpeg\" quality=\"50\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}

	public void testScaleAndSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image (contentcouch.graphics.scale-image http://www.nuke24.net/images/bunny.jpg) format=\"image/jpeg\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
}
