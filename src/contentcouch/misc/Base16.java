package contentcouch.misc;

public class Base16
{
	public static char[] LOWER = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	public static char[] UPPER = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static byte decode( char hex ) {
		switch( hex ) {
		case('0'): return 0;
		case('1'): return 1;
		case('2'): return 2;
		case('3'): return 3;
		case('4'): return 4;
		case('5'): return 5;
		case('6'): return 6;
		case('7'): return 7;
		case('8'): return 8;
		case('9'): return 9;
		case('a'): case('A'): return 0xA;
		case('b'): case('B'): return 0xB;
		case('c'): case('C'): return 0xC;
		case('d'): case('D'): return 0xD;
		case('e'): case('E'): return 0xE;
		case('f'): case('F'): return 0xF;
		default: return -1;
		}
	}
	
	public static byte[] decode( String hex ) {
		if( hex.length() % 2 == 1 ) hex += '0';
		int bc = hex.length()/2;
		byte[] dat = new byte[bc];
		for( int i=0; i<bc; ++i ) {
			dat[i] = (byte)((decode( hex.charAt(i*2+0) ) << 4) + decode( hex.charAt(i*2+1) ));
		}
		return dat;
	}
	
	public static String encode( byte[] dat, char[] table ) {
		char[] res = new char[dat.length*2];
		for( int i=0; i<dat.length; ++i ) {
			res[i*2+0] = table[(dat[i]>>4)&0x0F];
			res[i*2+1] = table[(dat[i]   )&0x0F];
		}
		return new String(res);
	}
}
