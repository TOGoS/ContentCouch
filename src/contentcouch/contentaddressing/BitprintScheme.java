package contentcouch.contentaddressing;

import org.bitpedia.util.Base32;

import contentcouch.rdf.CcouchNamespace;
import contentcouch.value.Blob;

public class BitprintScheme implements ContentAddressingScheme {
	public static class Bitprint {
		protected byte[] sha1Hash;
		protected byte[] tigerTreeHash;
		
		public byte[] getSha1Hash() { return sha1Hash; }
		public byte[] getTigerTreeHash() { return tigerTreeHash; }
		
		public String getSha1Base32() { return sha1Hash == null ? null : Base32.encode(sha1Hash); }
		public String getTigerTreeBase32() { return sha1Hash == null ? null : Base32.encode(tigerTreeHash); }
		
		public String toString() {
			if( sha1Hash != null && tigerTreeHash != null ) {
				return BitprintScheme.BITPRINTURNPREFIX + Base32.encode(sha1Hash) + "." + Base32.encode(tigerTreeHash);
			} else if( sha1Hash != null ) {
				return Sha1Scheme.SHA1URNPREFIX + Base32.encode(sha1Hash);
			} else if( tigerTreeHash != null ) {
				return TigerTreeScheme.TIGERTREEURNPREFIX + Base32.encode(tigerTreeHash);
			} else {
				throw new RuntimeException("Cannot create URI from bitprint with no SHA-1 part and no TigerTree part");
			}
		}
		
		public void setSha1( byte[] hash ) {
			if( hash.length != Sha1Scheme.SHA1HASHLENGTH ) {
				throw new SomethingIsBadlyFormedException( "<bytes>", "Length of SHA-1 hash should be " + Sha1Scheme.SHA1HASHLENGTH + " but was " + hash.length );
			} else {
				sha1Hash = hash;
			}
		}
		
		public void setTigerTree( byte[] hash ) {
			if( hash.length != TigerTreeScheme.TIGERTREEHASHLENGTH ) {
				throw new SomethingIsBadlyFormedException( "<bytes>", "Length of SHA-1 hash should be " + TigerTreeScheme.TIGERTREEHASHLENGTH + " but was " + hash.length );
			} else {
				tigerTreeHash = hash;
			}
		}

		public void setSha1( String base32 ) {
			if( base32.length() != Sha1Scheme.SHA1BASE32LENGTH ) {
				throw new SomethingIsBadlyFormedException( base32, "Length of base32-encoded SHA-1 part should be " + Sha1Scheme.SHA1BASE32LENGTH + " but was " + base32.length() );
			} else {
				sha1Hash = Base32.decode(base32);
			}
		}
		
		public void setTigerTree( String base32 ) {
			if( base32.length() != TigerTreeScheme.TIGERTREEBASE32LENGTH ) {
				throw new SomethingIsBadlyFormedException( base32, "Length of base32-encoded TigerTree part should be " + TigerTreeScheme.TIGERTREEBASE32LENGTH + " but was " + base32.length() );
			} else {
				tigerTreeHash = Base32.decode(base32);
			}
		}
		
		public static Bitprint parse( String uri ) {
			if( uri.startsWith(BitprintScheme.BITPRINTURNPREFIX) ) {
				String[] parts = uri.substring(BitprintScheme.BITPRINTURNPREFIX.length()).split("\\.");
				if( parts.length < 2 ) {
					throw new SomethingIsBadlyFormedException(uri, "Bitprint URN should contain 2 parts separated by a period, but only " + parts.length + " parts were found");
				}
				Bitprint bp = new Bitprint();
				bp.setSha1(parts[0]);
				bp.setTigerTree(parts[1]);
				return bp;
			} else if( uri.startsWith(Sha1Scheme.SHA1URNPREFIX) ) {
				Bitprint bp = new Bitprint();
				bp.setSha1(uri.substring(Sha1Scheme.SHA1URNPREFIX.length()));
				return bp;
			} else if( uri.startsWith(TigerTreeScheme.TIGERTREEURNPREFIX) ) {
				Bitprint bp = new Bitprint();
				bp.setTigerTree(uri.substring(TigerTreeScheme.TIGERTREEURNPREFIX.length()));
				return bp;
			} else {
				return null;
			}
		}
		
		public static final boolean bytesEqual( byte[] b1, byte[] b2 ) {
			if( b1.length != b2.length ) return false;
			for( int i=b1.length-1; i>=0; --i ) {
				if( b1[i] != b2[i] ) return false;
			}
			return true;
		}
		
		public static Boolean getEquivalence( byte[] b1, byte[] b2 ) {
			if( b1 == null || b2 == null ) return null;
			return Boolean.valueOf(bytesEqual(b1, b2));
		}
		
		public static Boolean getEquivalence( Bitprint bp1, Bitprint bp2 ) {
			Boolean sha1eq = getEquivalence(bp1.getSha1Hash(), bp2.getSha1Hash() );
			if( sha1eq != null && !sha1eq.booleanValue() ) return sha1eq; 
			Boolean tt1eq = getEquivalence(bp1.getTigerTreeHash(), bp2.getTigerTreeHash() );
			if( tt1eq != null && !tt1eq.booleanValue() ) return tt1eq;
			if( sha1eq != null || tt1eq != null ) return Boolean.TRUE;
			return null;
		}
		
		public static Boolean getUriEquivalence( String uri1, String uri2 ) {
			Bitprint bp1 = parse(uri1);
			if( bp1 == null ) return null;
			Bitprint bp2 = parse(uri2);
			if( bp2 == null ) return null;
			return getEquivalence(bp1, bp2);
		}
		
		/** Returns true if the given uri is any of the bitprint-compatible schemes (bitprint,sha1,tigertree) */
		public static boolean isBitprintCompatibleUri( String uri ) {
			return
				uri.startsWith(BitprintScheme.BITPRINTURNPREFIX) ||
				uri.startsWith(Sha1Scheme.SHA1URNPREFIX) ||
				uri.startsWith(TigerTreeScheme.TIGERTREEURNPREFIX);
		}
	}
	
	public static final BitprintScheme instance = new BitprintScheme();
	public static final BitprintScheme getInstance() { return instance; }
	
	public String getSchemeDisplayName() {
		return "Bitprint";
	}
	public String getRdfKey() {
		return CcouchNamespace.BITPRINT;
	}
	
	public static String BITPRINTURNPREFIX = "urn:bitprint:";
	
	/** Return true if the given URN is in the domain of this addressing scheme */ 
	public boolean wouldHandleUrn( String urn ) {
		return urn.startsWith(BITPRINTURNPREFIX);
	}
	
	protected byte[] joinHashes( byte[] sha1Hash, byte[] tigerTreeHash ) {
		byte[] hash = new byte[44];
		System.arraycopy( sha1Hash, 0, hash, 0, 20);
		System.arraycopy( tigerTreeHash, 0, hash, 20, 24);
		return hash;
	}

	/** Return the canonical identifier of the given blob */
	public byte[] getHash( Blob blob ) {
		return joinHashes( Sha1Scheme.getInstance().getHash(blob), TigerTreeScheme.getInstance().getHash(blob) );
	}
	
	// Convert to/from RDF value
	public String hashToRdfValue( byte[] hash ) {
		return hashToFilename(hash);
	}
	public byte[] rdfValueToHash( String value ) {
		return filenameToHash(value);
	}

	// Convert to/from URN
	public String hashToUrn( byte[] hash ) {
		return BITPRINTURNPREFIX + hashToFilename(hash);
	}
	
	public byte[] urnToHash( String urn ) {
		if( urn.startsWith(BITPRINTURNPREFIX) ) {
			return filenameToHash(urn.substring(BITPRINTURNPREFIX.length()));
		} else {
			throw new BadlyFormedUrnException(urn, "Doesn't start with '" + BITPRINTURNPREFIX + "'");
		}
	}
	
	// Convert to/from filename
	public String hashToFilename( byte[] hash ) {
		byte[] sha1Hash = new byte[20];
		System.arraycopy( hash, 0, sha1Hash, 0, 20);
		byte[] tigerTreeHash = new byte[24];
		System.arraycopy( hash, 20, tigerTreeHash, 0, 24);
		return Base32.encode(sha1Hash) + "." + Base32.encode(tigerTreeHash);
	}
	public byte[] filenameToHash( String filename ) {
		String sha1Base32 = filename.substring(0, Sha1Scheme.SHA1BASE32LENGTH);
		String tigerTreeBase32 = filename.substring(Sha1Scheme.SHA1BASE32LENGTH + 1, Sha1Scheme.SHA1BASE32LENGTH + 1 + TigerTreeScheme.TIGERTREEBASE32LENGTH);
		return joinHashes( Base32.decode(sha1Base32), Base32.decode(tigerTreeBase32) );
	}
}
