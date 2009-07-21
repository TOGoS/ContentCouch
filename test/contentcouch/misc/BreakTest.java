package contentcouch.misc;

import junit.framework.TestCase;

public class BreakTest extends TestCase {
	public void testBreakOutOfIf() {
		boolean result = false;
		
		state: if( true ) {
			if( true ) break state; // Haha, awesome.  Java doesn't suck as much as I thought.
			result = true;
		}
		
		assertEquals( false, result );
	}
}
