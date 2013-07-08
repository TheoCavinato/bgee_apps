package org.bgee.model.dao.api;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract factory of DAOs, following the abstract factory pattern. 
 * This abstract class list all the methods that must be implemented by concrete managers 
 * extending this class, to obtain and manage DAOs. It also provides 
 * the <code>getDAOManager</code> methods, which allow to obtain a concrete manager 
 * extending this class. And finally, it also provides methods to release the DAOs.
 * The point is that the client will not be aware of which concrete 
 * implementation it obtained, so that the client code is not dependent of 
 * any concrete implementation. 
 * <p>
 * When calling the <code>getDAOManager</code> methods, the <code>DAOManager</code> returned 
 * is a "per-thread singleton": a <code>DAOManager</code> is instantiated 
 * the first time a <code>getDAOManager</code> method is called inside a given thread, 
 * and the same instance is then always returned when calling 
 * a <code>getDAOManager</code> method inside the same thread. 
 * An exception is if you call this method after having called {@link #release()}.
 * In that case, a call to a <code>getDAOManager</code> method from this thread 
 * would return a new <code>DAOManager</code> instance. 
 * <p>
 * {@link #release()} should always be called at the end of the applicative code, 
 * otherwise a <code>DAOManager</code> could be improperly reused if this API 
 * is used in an application using thread pooling.  
 * <p>
 * This class supports the standard <a href=
 * 'http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider'> 
 * Service Provider</a> mechanism. Concrete implementations must include the file 
 * <code>META-INF/services/org.bgee.model.dao.api.DAOManager</code>. The file must contain 
 * the name of the implementation of <code>DAOManager</code>. For example, 
 * to load the <code>my.sql.Manager</code> class, 
 * the <code>META-INF/services/org.bgee.model.dao.api.DAOManager</code> file 
 * would contain the entry:
 * <pre>my.sql.Manager</pre>
 * To conform to the <code>Service Provider</code> requirements, the class implementing 
 * <code>DAOManager</code> must provide a default constructor with no arguments. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public abstract class DAOManager 
{
	//*****************************************
    //  CLASS ATTRIBUTES AND METHODS
    //*****************************************
	/**
     * <code>Logger</code> of the class. 
     */
    private final static Logger log = LogManager.getLogger(DAOManager.class.getName());
    
    /**
     * An <code>AtomicLong</code> to generate <code>DAOManager</code> IDs. 
     */
    private static final AtomicLong seqNumber = new AtomicLong(1);
    /**
     * A <code>ThreadLocal</code> to obtain one and only one <code>DAOManager</code> ID 
     * for a given <code>Thread</code>. 
     */
    private static final ThreadLocal <Long> uniqueNumber = 
        new ThreadLocal <Long> () {
            @Override protected Long initialValue() {
                return seqNumber.getAndIncrement();
        }
    };
    /**
     * A <code>ConcurrentMap</code> used to store <code>DAOManager</code>s, 
     * associated to their ID as key. 
     * <p>
     * This <code>Map</code> is used to provide a unique and independent 
     * <code>DAOManager</code> instance to each thread: a <code>DAOManager</code> is added 
     * to this <code>Map</code> when a <code>getDAOManager</code> method is called, 
     * if the ID generated for a thread is not already present in the <code>keySet</code> 
     * of the <code>Map</code> (see {@link #uniqueNumber} for ID generation). 
     * Otherwise, the already stored <code>DAOManager</code> is returned. 
     * <p>
     * If a <code>ThreadLocal</code> was not used, it is because 
     * this <code>Map</code> is used by other treads, 
     * for instance when a <code>ShutdownListener</code> 
     * want to properly release all <code>DAOManager</code>s; 
     * or when a thread performing monitoring of another thread want to kill it.
     * <p>
     * A <code>DAOManager</code> is removed from this <code>Map</code> for a thread
     * when the method {@link #release()} is called from this thread, 
     * or when {@link #kill(long)} is called using the ID assigned to this thread, 
     * or when the method  {@link #releaseAll()} is called. 
     * All <code>DAOManager</code>s are removed when {@link #releaseAll()} is called.
     */
	private static final ConcurrentMap<Long, DAOManager> managers = 
			new ConcurrentHashMap<Long, DAOManager>(); 
	
	/**
	 * A <code>ServiceLoader</code> to obtain <code>Service providers</code> 
	 * providing <code>DAOManager</code>s. This <code>ServiceLoader</code> 
	 * is loaded only once for a given class loader. 
	 */
	private static final ServiceLoader<DAOManager> loader = 
			ServiceLoader.load(DAOManager.class);
	
	/**
     * An <code>AtomicBoolean</code> to define if <code>DAOManager</code>s 
     * can still be acquired (using the <code>getDAOManager</code> methods), 
     * or if it is not possible anymore (meaning that the method {@link #releaseAll()} 
     * has been called)
     */
    private static final AtomicBoolean allReleased = new AtomicBoolean();
	
    /**
     * Every concrete implementation must provide a default constructor 
     * with no parameters. 
     */
	public DAOManager() {
		
	}
	
	/**
	 * Return a <code>DAOManager</code> instance with its parameters set using 
	 * the provided <code>Map</code>. If it is the first call to one 
	 * of the <code>getDAOManager</code> methods within a given thread, 
	 * the <code>getDAOManager</code> methods return a newly instantiated 
	 * <code>DAOManager</code>. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the <code>release</code> 
	 * method was called. 
	 * <p>
	 * If no <code>DAOManager</code> is available for this thread yet, this method 
	 * will try to obtain from a <code>Service Provider</code> a concrete implementation 
	 * that accepts its <code>parameters</code> to be set by calling 
	 * {@link #setParameters(Map)} on it, or will return <code>null</code> 
	 * if none could be found. If <code>parameters</code> is <code>null</code>, 
	 * then the first available <code>Service Provider</code> will be used, and 
	 * {@link #setParameters(Map)} will not be called. 
	 * <p>
	 * If a <code>DAOManager</code> is already available for this thread, 
	 * then this method will simply return it after having called {@link #setParameters(Map)} 
	 * on it (unless <code>parameters</code> is <code>null</code>). This operation 
	 * could throw an <code>IllegalArgumentException</code> if <code>parameters</code> 
	 * are not supported by the current <code>DAOManager</code>. 
	 * <p>
	 * If caller wants to use a different <code>Service Provider</code>, accepting 
	 * different parameters, then <code>release</code> should first be called. 
	 * <p>
	 * This method will throw an <code>IllegalStateException</code> if {@link #releaseAll()} 
	 * was called prior to calling this method. 
	 * 
	 * @param parameters 	A <code>Map</code> with keys that are <code>String</code>s 
	 * 						representing parameter names, and values that 
	 * 						are <code>String</code>s representing parameters values, 
	 * 						to be passed to the <code>DAOManager</code> instance. 
	 * @return 				A <code>DAOManager</code> accepting <code>parameters</code>. 
	 * @throws IllegalArgumentException	if a  <code>DAOManager</code> is already 
	 * 									available for this thread, but not accepting 
	 * 									<code>parameters</code>. 
	 * @throws IllegalStateException 	if <code>releaseAll</code> was already called, 
	 * 									so that no <code>DAOManager</code>s can be 
	 * 									acquired anymore. 
	 */
	public final static DAOManager getDAOManager(Map<String, String> parameters) 
	    throws IllegalArgumentException, IllegalStateException
	{
		log.entry(parameters);

        long idAssigned = uniqueNumber.get();
        log.debug("Trying to obtain a DAOManager with ID {}", 
        		idAssigned);

        if (allReleased.get()) {
            throw new IllegalStateException("releaseAll() has been already called, " +
                    "it is not possible to acquire a DAOManager anymore");
        }

        DAOManager manager = managers.get(idAssigned);
        if (manager == null) {
            //obtain a DAOManager from a Service Provider accepting the parameters
        	log.debug("Trying to acquire a DAOManager from a Service provider");
        	for (DAOManager testManager: loader) {
        		log.trace("Testing {}", testManager);
        		try {
        			if (parameters != null) {
        			    testManager.setParameters(parameters);
        			}
        			//parameters accepted, we will use this manager
        			manager = testManager;
        			log.debug("DAOManager {} acquired", manager);
        			break;
        		} catch (IllegalArgumentException e) {
        			//do nothing, it is only to find a service provider 
        			//accepting the parameters
        		}
        	}
        	if (manager == null) {
        		log.debug("No DAOManager could be acquired");
        		return null;
        	}
        	
            //we don't use putifAbsent, as idAssigned make sure 
            //there won't be any multi-threading key collision
            managers.put(idAssigned, manager);
            log.debug("Return a new DAOManager instance");
        } else {
            log.debug("Return an already existing DAOManager instance");
            if (parameters != null) {
                manager.setParameters(parameters);
            }
        }
        return log.exit(manager);
	}
	
	/**
	 * Return a <code>DAOManager</code> instance>. If it is the first call to one 
	 * of the <code>getDAOManager</code> methods within a given thread, 
	 * the <code>getDAOManager</code> methods return a newly instantiated 
	 * <code>DAOManager</code>. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the <code>release</code> 
	 * method was called. 
	 * <p>
	 * If no <code>DAOManager</code> is available for this thread yet, this method 
	 * will try to obtain a <code>DAOManager</code> from the first Service provider 
	 * available, or will return <code>null</code> if no appropriate Service provider 
	 * could be found. If the caller needs more control on the Service provider used, 
	 * {@link #getDAOManager(Map)} could be used. 
	 * <p>
	 * If a <code>DAOManager</code> is already available for this thread, 
	 * then this method will simply return it. 
	 * <p>
	 * This method will throw a <code>IllegalStateException</code> if {@link #releaseAll()} 
	 * was called prior to calling this method. 
	 * 
	 * @throws IllegalStateException 	if <code>releaseAll</code> was already called, 
	 * 									so that no <code>DAOManager</code>s can be 
	 * 									acquired anymore. 
	 */
	public final static DAOManager getDAOManager() throws IllegalStateException {
		return getDAOManager(null);
	}
	
	/**
     * Call {@link #release()} on all <code>DAOManager</code> instances currently registered,
     * and prevent any new <code>DAOManager</code> instance to be obtained again 
     * (calling a <code>getDAOManager</code> method from any thread 
     * after having called this method will throw a <code>IllegalStateException</code>). 
     * <p>
     * This method returns the number of <code>DAOManager</code>s that were released. 
     * <p>
     * This method is called for instance when a <code>ShutdownListener</code> 
     * want to release all resources using a data source.
     * 
     * @return 	An <code>int</code> that is the number of <code>DAOManager</code> instances  
     * 			that were released.
     */
    public final static int releaseAll()
    {
        log.entry();

        //this AtomicBoolean will act more or less like a lock 
        //(no new DAOManager can be obtained after this AtomicBoolean is set to true).
        //It's not totally true, but we don't except any major error 
        //if it doesn't act like a lock.
        allReleased.set(true);

        int managerCount = 0;
        for (DAOManager manager: managers.values()) {
        	managerCount++;
        	manager.release();
        }

        return log.exit(managerCount);
    }
    
    /**
     * Call {@link #kill()} on the <code>DAOManager</code> currently registered 
     * with an ID (returned by {@link #getId()}) equals to <code>managerId</code>.
     * 
     * @param managerId 	A <code>long</code> corresponding to the ID of 
     * 						the <code>DAOManager</code> to kill. 
     */
    public final static void kill(long managerId) {
    	DAOManager manager = managers.get(managerId);
        if (manager != null) {
        	manager.kill();
        }
    }
    
    //*****************************************
    //  INSTANCE ATTRIBUTES AND METHODS
    //*****************************************

	/**
     * Close all resources managed by this <code>DAOManager</code> instance, 
     * and release it (a call to a <code>getDAOManager</code> method from the thread 
     * that was holding it will return a new <code>DAOManager</code> instance).
     * <p>
     * Following a call to this method, it is not possible to acquire DAOs 
     * from this <code>DAOManager</code> instance anymore.
     */
    public final void release()
    {
        log.entry();
        this.removePromPool();
        //implementation-specific code here
        this.releaseDAOManager();
        
        log.exit();
    }
    
    /**
     * Try to kill immediately all ongoing processes performed by this 
     * <code>DAOManager</code>, release all resources it handles, 
     * and release it (a call to a <code>getDAOManager</code> method from the thread 
     * that was holding it will return a new <code>DAOManager</code> instance).
     * <p>
     * Following a call to this method, it is not possible to acquire DAOs 
     * from this <code>DAOManager</code> instance anymore.
     */
    public final void kill() {
    	log.entry();
    	this.removePromPool();
        //implementation-specific code here
        this.killDAOManager();
    	
    	log.exit();
    }
    
    /**
     * Performs the operations to remove a <code>DAOManager</code> 
     * from {@link #managers} and to remove the thread-local ID from 
     * {@link #uniqueNumbers}.
     */
    private final void removePromPool() {
    	managers.remove(uniqueNumber.get());
        uniqueNumber.remove();
    }
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to release all resources managed by this <code>DAOManager</code> instance.
     * For instance, if a service provider uses the JDBC API to use a SQL database, 
     * the manager should close all <code>Connection</code>s to the database 
     * hold by its DAOs. Calling this method should not affect other 
     * <code>DAOManager</code> instances (so in our previous example, 
     * <code>Connection</code>s hold by other managers should not be closed).
     * <p>
     * Implementations should make sure that no DAOs can be obtained from 
     * this <code>DAOManager</code> instance once this method has been called. 
     * <p>
     * This method is called by {@link #release()} after having remove this 
     * <code>DAOManager</code> from the pool. 
     */
    protected abstract void releaseDAOManager();
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to immediately kill all ongoing processes handled by this <code>DAOManager</code>, 
     * and release all resources. It should not affect other <code>DAOManager</code>s.
     * <p>
     * For instance, if a service provider uses the JDBC API to use a SQL database,
     * the <code>DAOManager</code> should keep track of which <code>Statement</code> 
     * is currently running, in order to be able to call <code>cancel</code> on it, 
     * then close all <code>Connection</code>s hold by its DAOs. 
     * <p>
     * Implementations should make sure that no DAOs can be obtained from 
     * this <code>DAOManager</code> instance once this method has been called. 
     * <p>
     * This method is called by {@link #kill()} after having remove this 
     * <code>DAOManager</code> from the pool. Note that {@link #releaseDAOManager()} 
     * is not called, it is up to the implementation to do it if needed. 
     */
    protected abstract void killDAOManager();
    
    /**
     * Set the parameters of this <code>DAOManager</code>. For instance, 
     * if this <code>DAOManager</code> was obtained from a Service provider using 
     * the JDBC API to use a SQL database, then the parameters might contain 
     * the <code>URL</code> to connect to the database. It is up to each 
     * Service provider to specify what are the parameters needed. 
     * <p>
     * This method throws an <code>IllegalArgumentException</code> if 
     * the <code>DAOManager</code> does not accept this parameters. 
     * This is the method used to find an appropriate Service provider 
     * when calling {@link #getDAOManager(Map)}.
     * <p>
     * If an <code>IllegalArgumentException</code> is thrown when using this method, 
     * maybe a new <code>DAOManager</code> could be obtained 
     * from another Service provider, by calling <code>release</code>, 
     * then <code>getDAOManager(Map)</code>.
     * 
     * @param parameters	A <code>Map</code> with keys that are <code>String</code>s 
	 * 						representing parameter names, and values that 
	 * 						are <code>String</code>s representing parameters values. 
     * @throws IllegalArgumentException 	If this <code>DAOManager</code> does not accept 
     * 										<code>parameters</code>. 
     */
    public abstract void setParameters(Map<String, String> parameters) 
    		throws IllegalArgumentException;
}
