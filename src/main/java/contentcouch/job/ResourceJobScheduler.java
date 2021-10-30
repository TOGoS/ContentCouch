package contentcouch.job;

import java.lang.ref.WeakReference;
import java.util.Queue;
import java.util.WeakHashMap;

public class ResourceJobScheduler
{
	class Handle implements Runnable {
		String resourceId;
		ResourceJob resourceJob;
		Object result;
		Object error;
		boolean completed = false;
		
		public Handle( ResourceJob next ) {
			this.resourceId = next.getResourceId();
			this.resourceJob = next;
		}
		
		public void run() {
			Object r = null;
			try {
				r = resourceJob.getResult();
			} catch( Exception e ) {
				e.printStackTrace();
				setResult(null,e);
			}
			setResult(r,null);
		}
		
		public synchronized void setResult( Object res, Object error ) {
			this.error = error;
			this.result = res;
			this.completed = true;
			this.notifyAll();
		}
		
		public synchronized Object getResult() throws InterruptedException {
			while( !completed ) {
				wait();
			}
			return this.result;
		}
		
		public boolean equals( Object oth ) {
			if( !(oth instanceof Handle) ) return false;
			return resourceId.equals(((Handle)oth).resourceId);
		}
		
		public int hashCode() {
			return resourceId.hashCode()+1;
		}
	}
	
	protected static ResourceJobScheduler instance; 
	public synchronized static ResourceJobScheduler getInstance() {
		if( instance == null ) {
			instance = new ResourceJobScheduler( JobService.getInstance().getJobQueue() );
		}
		return instance;
	}
	
	final Queue jobQueue;
	final WeakHashMap handles = new WeakHashMap();
	
	public ResourceJobScheduler( Queue jobQueue ) {
		this.jobQueue = jobQueue;
	}
	
	protected synchronized Handle getHandle( ResourceJob job ) {
		Handle h0 = new Handle(job);
		WeakReference hr = (WeakReference)handles.get(h0);
		Handle h1 = hr == null ? null : (Handle)hr.get();
		if( h1 == null ) {
			jobQueue.add( h0 );
			handles.put(h0, new WeakReference(h0));
			return h0;
		} else {
			return h1;
		}
	}
	
	public Object getJobResult( ResourceJob j ) throws InterruptedException {
		return getHandle(j).getResult();
	}
}
