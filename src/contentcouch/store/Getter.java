package contentcouch.store;

public interface Getter {
	public static class GetFailure extends RuntimeException {
		public String identifier;
		public GetFailure(String message, String identifier) {
			super(message);
			this.identifier = identifier;
		}
		public GetFailure(String message, String identifier, Throwable t) {
			super(message, t);
			this.identifier = identifier;
		}
		public String getMessage() {
			return super.getMessage() + ": " + identifier;
		}
	}
	
	public Object get( String identifier );
}
