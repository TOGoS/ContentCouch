package contentcouch.active;

import java.awt.image.BufferedImage;

import togos.mf.value.Blob;

import junit.framework.TestCase;
import contentcouch.repository.MetaRepoConfig;
import contentcouch.store.TheGetter;

public class ImagingTest extends TestCase {
	MetaRepoConfig repo; 

	public void setUp() {
		repo = new MetaRepoConfig();
		TheGetter.globalInstance = repo.getRequestKernel();
		repo.handleArguments(new String[]{"-repo","junk-repo/"}, 0, "./");
	}
	
	protected void assertInstanceOf( Class c, Object obj ) {
		if( obj == null ) assertTrue("Object is null", false);
		if( c.isAssignableFrom(obj.getClass()) ) return;
		assertTrue("A " + obj.getClass() + " is not an instance of " + c.getName(), false);
	}
	
	////

	protected String BUNNY_URI = "(contentcouch.builtindata.get \"bunny.jpg\")"; 
	
	public void testFetchImage() {
		String uri = BUNNY_URI;
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testScaleImage() {
		String uri = "(contentcouch.graphics.scale-image "+BUNNY_URI+" scale=\"0.5\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( BufferedImage.class, result );
	}

	public void testSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image "+BUNNY_URI+" format=\"image/jpeg\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
	
	public void testSerializeImageWithQuality() {
		String uri = "(contentcouch.graphics.serialize-image "+BUNNY_URI+" format=\"image/jpeg\" quality=\"50\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}

	public void testScaleAndSerializeImage() {
		String uri = "(contentcouch.graphics.serialize-image (contentcouch.graphics.scale-image "+BUNNY_URI+") format=\"image/jpeg\")";
		Object result = TheGetter.get(uri);
		assertInstanceOf( Blob.class, result );
	}
}
