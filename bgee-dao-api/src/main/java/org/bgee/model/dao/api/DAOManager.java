package org.bgee.model.dao.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract factory of DAOs, following the abstract factory pattern. 
 * This abstract class list all the methods that must be implemented by concrete managers 
 * extending this class, to obtain and manage DAOs. It also provides 
 * the <code>getDAOManager</code> methods, which allow to obtain a concrete manager 
 * extending this class. And finally, it provides methods to close the DAOs.
 * The point is that the client will not be aware of which concrete 
 * implementation it obtained, so that the client code is not dependent of 
 * any concrete implementation. 
 * <p>
 * When calling the <code>getDAOManager</code> methods, the <code>DAOManager</code> returned 
 * is a "per-thread singleton": a <code>DAOManager</code> is instantiated 
 * the first time a <code>getDAOManager</code> method is called inside a given thread, 
 * and the same instance is then always returned when calling 
 * a <code>getDAOManager</code> method inside the same thread. 
 * An exception is if you call this method after having called {@link #close()}.
 * In that case, a call to a <code>getDAOManager</code> method from this thread 
 * would return a new <code>DAOManager</code> instance. 
 * <p>
 * {@link #close()} should always be called at the end of the applicative code, 
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
 * <p>
 * Important note about <code>ServiceLoader</code> and shared <code>ClassLoader</code> 
 * (like in tomcat): http://stackoverflow.com/a/7220918/1768736
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public abstract class DAOManager implements AutoCloseable
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
     * for a given <code>Thread</code> (unless {@link #close()} is called). 
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
     * when the method {@link #close()} is called from this thread, 
     * or when {@link #kill(long)} is called using the ID assigned to this thread, 
     * or when the method  {@link #closeAll()} is called. 
     * All <code>DAOManager</code>s are removed when {@link #closeAll()} is called.
     */
	private static final ConcurrentMap<Long, DAOManager> managers = 
			new ConcurrentHashMap<Long, DAOManager>(); 
	
	/**
	 * A unmodifiable <code>List</code> containing all available providers of 
	 * the <code>DAOManager</code> service, in the order they were obtained 
	 * from the <code>ServiceLoader</code>. This is needed because we want 
	 * to load services from the <code>ServiceLoader</code> only once 
	 * by <code>ClassLoader</code>. So we could have used as attribute  
	 * a <code>static final ServiceLoader</code>, but <code>ServiceLoader</code> 
	 * lazyly instantiate service providers and is not thread-safe, so we would 
	 * have troubles in a multi-threading context. So we load all providers 
	 * at once, we don't except this pre-loading to require too much memory 
	 * (very few service providers available, used in very few libraries). 
	 * <p>
	 * As this <code>List</code> is unmodifiable and declared <code>final</code>, 
	 * and the stored <code>DAOManager</code>s will always be copied before use, 
	 * this attribute can safely be accessed in a multi-threading context. 
	 */
	private static final List<DAOManager> serviceProviders = 
			DAOManager.getServiceProviders();
	/**
	 * Get all available providers of the <code>DAOManager</code> service 
	 * from the <code>ServiceLoader</code>, as an unmodifiable <code>List</code>. 
	 * Empty <code>List</code> if no service providers could be found. 
	 * 
	 * @return 	An unmodifiable <code>List</code> of <code>DAOManager</code>s, 
	 * 			in the same order they were obtained from the <code>ServiceLoader</code>. 
	 * 			Empty <code>List</code> if no service providers could be found.
	 */
	private final static List<DAOManager> getServiceProviders() {
		log.entry();
		log.info("Loading DAOManager service providers");
		ServiceLoader<DAOManager> loader = 
				ServiceLoader.load(DAOManager.class);
		List<DAOManager> providers = new ArrayList<DAOManager>();
		for (DAOManager provider: loader) {
			providers.add(provider);
		}
		log.info("Providers found: {}", providers);
		return log.exit(Collections.unmodifiableList(providers));
	}
	
	/**
     * A volatile <code>boolean</code> to define if <code>DAOManager</code>s 
     * can still be acquired (using the <code>getDAOManager</code> methods), 
     * or if it is not possible anymore (meaning that the method {@link #closeAll()} 
     * has been called).
     */
    private static volatile boolean allClosed = false;
	
	/**
	 * Return a <code>DAOManager</code> instance with its parameters set using 
	 * the provided <code>Map</code>. If it is the first call to one 
	 * of the <code>getDAOManager</code> methods within a given thread, 
	 * the <code>getDAOManager</code> methods return a newly instantiated 
	 * <code>DAOManager</code>. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the <code>close</code> 
	 * or <code>kill</code> method was called. 
	 * <p>
	 * If no <code>DAOManager</code> is available for this thread yet, this method 
	 * will try to obtain from a <code>Service Provider</code> a concrete implementation 
	 * that accepts its <code>parameters</code> to be set by calling 
	 * {@link #setParameters(Map)} on it, or will return <code>null</code> 
	 * if none could be found, or if no service providers were available at all. 
	 * If <code>parameters</code> is <code>null</code>, 
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
	 * different parameters, then <code>close</code> or <code>kill</code> 
	 * should first be called. 
	 * <p>
	 * This method will throw an <code>IllegalStateException</code> if {@link #closeAll()} 
	 * was called prior to calling this method, and a <code>ServiceConfigurationError</code> 
	 * if an error occurred while trying to find a service provider from the 
	 * <code>ServiceLoader</code>. 
	 * 
	 * @param parameters 	A <code>Map</code> with keys that are <code>String</code>s 
	 * 						representing parameter names, and values that 
	 * 						are <code>String</code>s representing parameters values, 
	 * 						to be passed to the <code>DAOManager</code> instance. 
	 * @return 				A <code>DAOManager</code> accepting <code>parameters</code>. 
	 * 						<code>null</code> if none could be found, or 
	 * 						if no service providers were available at all.
	 * @throws IllegalArgumentException	if a  <code>DAOManager</code> is already 
	 * 									available for this thread, but not accepting 
	 * 									<code>parameters</code>. 
	 * @throws IllegalStateException 	if <code>closeAll</code> was already called, 
	 * 									so that no <code>DAOManager</code>s can be 
	 * 									acquired anymore. 
	 * @throws ServiceConfigurationError	If an error occurred while trying to find 
	 * 										a <code>DAOManager</code> service provider 
	 * 										from the <code>ServiceLoader</code>. 
	 */
	public final static DAOManager getDAOManager(Map<String, String> parameters) 
	    throws IllegalArgumentException, IllegalStateException
	{
		log.entry(parameters);

        if (DAOManager.allClosed) {
            throw log.throwing(
            		new IllegalStateException("closeAll() has been already called, " +
                    "it is not possible to acquire a DAOManager anymore"));
        }

		//this thread-local ID should be release if a DAOManager is not acquired
        long idAssigned = uniqueNumber.get();
        log.debug("Trying to obtain a DAOManager with ID {}", idAssigned);

        DAOManager manager = managers.get(idAssigned);
        Throwable toThrow = null;
        if (manager == null) {
            //obtain a DAOManager from a Service Provider accepting the parameters
        	log.debug("No DAOManager available for this thread, trying to obtain a DAOManager from a Service provider");
        	
        	Iterator<DAOManager> managerIterator = DAOManager.serviceProviders.iterator();
        	providers: while (managerIterator.hasNext()) {
        		try {
        			//need to get a new instance, because as we store 
        			//in a static attribute the providers, it always returns 
        			//a same instance, while we want one instance per thread. 
        			DAOManager testManager = 
        					managerIterator.next().getClass().newInstance();

        			log.trace("Testing: {}", testManager);
        			if (parameters != null) {
        				testManager.setParameters(parameters);
        			}
        			//parameters accepted, we will use this manager
        			manager = testManager;
        			log.debug("Valid DAOManager: {}", manager);
        			break providers;

        		} catch (IllegalArgumentException e) {
        			//do nothing, this exception is thrown when calling 
        			//setParameters to try to find the appropriate service provider. 
        		} catch (Exception e) {
        			//this catch block is needed only because of the line 
        			//managerIterator.next().getClass().newInstance();
        			//These exceptions should never happen, as service providers 
        			//must implement a default public constructor with no arguments. 
        			//If such an exception occurred, it could be seen as 
        			//a ServiceConfigurationError
        			toThrow = new ServiceConfigurationError(
        					"DAOManager service provider instantiation error: " +
        					"service provider did not provide a valid constructor", e);
        			break providers;
        		}
        	}	
        	if (manager == null) {
        		log.debug("No DAOManager could be found");
        	} else {
        		//we don't use putifAbsent, as idAssigned make sure 
        		//there won't be any multi-threading key collision
        		managers.put(idAssigned, manager);
        	}
        } else {
            log.debug("Get an already existing DAOManager instance");
            if (parameters != null) {
            	try {
                    manager.setParameters(parameters);
            	} catch (IllegalArgumentException e) {
            		toThrow = e;
            	}
            }
        }
        if (manager != null && toThrow == null) {
        	//check that the manager was not closed by another thread while we were 
        	//acquiring it
        	synchronized (manager.closed) {
        		if (manager.isClosed()) {
        			//if the manager was closed following a call to closeAll
        			if (DAOManager.allClosed) {
        				toThrow = new IllegalStateException(
        						"closeAll() has been already called, " +
        						"it is not possible to acquire a DAOManager anymore");
        			}
        			//otherwise, it means it was killed following a call to kill(long)
        			//we just return the closed DAOManager, this will throw 
        			//an IllegalStateException when trying to acquire a DAO from it
        		}
        		if (toThrow == null) {
        			return log.exit(manager);
        		}
        	}
        }
        
		//remove thread-local ID from uniqueNumber
		DAOManager.removePromPool();
		
		if (toThrow != null) {
			if (toThrow instanceof IllegalStateException) {
				throw log.throwing((IllegalStateException) toThrow);
			} else if (toThrow instanceof ServiceConfigurationError) {
				throw log.throwing((ServiceConfigurationError) toThrow);
			} else if (toThrow instanceof IllegalArgumentException) {
				throw log.throwing((IllegalArgumentException) toThrow);
			} else {
				throw log.throwing(
						new ServiceConfigurationError("Unexpected error", toThrow));
			}
		}
		return log.exit(null);
	}
	
	/**
	 * Return a <code>DAOManager</code> instance. If it is the first call to one 
	 * of the <code>getDAOManager</code> methods within a given thread, 
	 * the <code>getDAOManager</code> methods return a newly instantiated 
	 * <code>DAOManager</code>. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the <code>close</code> 
	 * or <code>kill</code> method was called. 
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
	 * This method will throw a <code>IllegalStateException</code> if {@link #closeAll()} 
	 * was called prior to calling this method, and a <code>ServiceConfigurationError</code> 
	 * if an error occurred while trying to find a service provider from the 
	 * <code>ServiceLoader</code>. 
	 * 
	 * @return 				The first <code>DAOManager</code> available,  
	 * 						<code>null</code> if no service providers were available at all.
	 * @throws IllegalStateException 	if <code>closeAll</code> was already called, 
	 * 									so that no <code>DAOManager</code>s can be 
	 * 									acquired anymore. 
	 * @throws ServiceConfigurationError	If an error occurred while trying to find 
	 * 										a <code>DAOManager</code> service provider 
	 * 										from the <code>ServiceLoader</code>. 
	 */
	public final static DAOManager getDAOManager() throws IllegalStateException {
		log.entry();
		return log.exit(DAOManager.getDAOManager(null));
	}
	
	/**
     * Call {@link #close()} on all <code>DAOManager</code> instances currently registered,
     * and prevent any new <code>DAOManager</code> instance to be obtained again 
     * (calling a <code>getDAOManager</code> method from any thread 
     * after having called this method will throw an <code>IllegalStateException</code>). 
     * <p>
     * This method returns the number of <code>DAOManager</code>s that were closed. 
     * <p>
     * This method is called for instance when a <code>ShutdownListener</code> 
     * want to release all resources using a data source.
     * 
     * @return 	An <code>int</code> that is the number of <code>DAOManager</code> instances  
     * 			that were closed.
     */
    public final static int closeAll()
    {
        log.entry();

        //this volatile boolean will act more or less like a lock 
        //(no new DAOManager can be obtained after this boolean is set to true).
        //It's not totally true, but we don't except any major error 
        //if it doesn't act like a lock.
        DAOManager.allClosed = true;

        int managerCount = 0;
        for (DAOManager manager: managers.values()) {
        	managerCount++;
        	manager.close();
        }

        return log.exit(managerCount);
    }
    
    /**
     * Call {@link #kill()} on the <code>DAOManager</code> currently registered 
     * with an ID (returned by {@link #getId()}) equals to <code>managerId</code>.
     * 
     * @param managerId 	A <code>long</code> corresponding to the ID of 
     * 						the <code>DAOManager</code> to kill. 
     * @see #kill()
     */
    public final static void kill(long managerId) {
    	log.entry(managerId);
    	DAOManager manager = managers.get(managerId);
        if (manager != null) {
        	manager.kill();
        }
        log.exit();
    }
    
    //*****************************************
    //  INSTANCE ATTRIBUTES AND METHODS
    //*****************************************	
    /**
     * A <code>volatile</code> <code>Boolean</code> to indicate whether 
     * this <code>DAOManager</code> was closed (following a call to {@link #close()}, 
     * {@link #closeAll()}, {@link #kill()}, or {@link #kill(long)}).
     * <p>
     * This attribute is used as a lock to perform some atomic operations. It is also 
     * <code>volatile</code> as it can be read by method not acquiring a lock on it.
     * 
     */
    private volatile Boolean closed;
    /**
     * A <code>volatile</code> <code>boolean</code> to indicate whether 
     * this <code>DAOManager</code> was requested to be killed ({@link #kill()} 
     * or {@link #kill(long)}). This does not necessarily mean that a query 
     * was interrupted, only that the <code>DAOManager</code> received 
     * a <code>kill</code> command (if no query was running when receiving the command, 
     * none were killed).
     * <p>
     * This attribute is <code>volatile</code> as it can be read and written 
     * from different threads.
     * 
     */
    private volatile boolean killed;
    /**
     * Every concrete implementation must provide a default constructor 
     * with no parameters. 
     */
	public DAOManager() {
		log.entry();
		this.setClosed(false);
		this.setKilled(false);
		log.exit();
	}
	
	/**
	 * Return the thread-local ID associated to this <code>DAOManager</code>. 
	 * This ID can be used to call {@link #kill(long)}.
	 * <p>
	 * If this <code>DAOManager</code> is already closed, the returned value is 
	 * <code>-1</code>. This avoids to have to call <code>isClosed</code> before 
	 * this method, in an non-atomic way (as closed <code>DAOManager</code> 
	 * are not associated to a thread-local ID). 
	 * 
	 * @return 	A <code>long</code> that is the ID of this <code>DAOManager</code>.
	 * 			-1 if this <code>DAOManager</code> is closed, and is not associated 
	 * 			to an ID anymore. 
	 */
	public final long getId() {
		log.entry();
		//to ensure we don't create a new thread-local ID if it doesn't have one
		synchronized(this.closed) {
			if (this.isClosed()) {
				return log.exit(-1L);
			}
			return log.exit(uniqueNumber.get());
		}
	}
	
	/**
     * Close all resources managed by this <code>DAOManager</code> instance, 
     * and release it (a call to a <code>getDAOManager</code> method from the thread 
     * that was holding it will return a new <code>DAOManager</code> instance).
     * <p>
     * Following a call to this method, it is not possible to acquire DAOs 
     * from this <code>DAOManager</code> instance anymore.
     * <p>
     * Specified by {@link java.lang.AutoCloseable#close()}.
     * 
     * @see #closeAll()
     * @see #kill()
     * @see #kill(long)
     */
	@Override
    public final void close()
    {
        log.entry();
        if (this.atomicCloseAndRemoveFromPool(false)) {
        	//implementation-specific code here
        	this.closeDAOManager();
        }
        
        log.exit();
    }
	/**
     * Determine whether this <code>DAOManager</code> was closed 
     * (following a call to {@link #close()}, {@link #closeAll()}, 
     * {@link #kill()}, or {@link #kill(long)}).
     * 
     * @return	<code>true</code> if this <code>DAOManager</code> was closed, 
     * 			<code>false</code> otherwise.
     */
    public final boolean isClosed()
    {
        log.entry();
        return log.exit(closed);
    }
    /**
     * Set {@link #closed}. The only method that should call this one besides constructors 
     * is {@link #atomicCloseAndRemoveFromPool(boolean)}. 
     * 
     * @param closed 	a <code>boolean</code> to set {@link #closed}
     */
    private final void setClosed(boolean closed) {
    	this.closed = closed;
    }
    
    /**
     * Try to kill immediately all ongoing processes performed by DAOs of this 
     * <code>DAOManager</code>, and immediately call {@link #close()} on it. 
     * If a query was interrupted following a call to this method, the service provider 
     * should throw a <code>QueryInterruptedException</code> from the thread 
     * that was interrupted. 
     * 
     * @see #kill(long)
     */
    public final void kill() {
    	log.entry();
    	if (this.atomicCloseAndRemoveFromPool(true)) {
    		//implementation-specific code here
    		this.killDAOManager();
    		this.closeDAOManager();
    	}
    	
    	log.exit();
    }
    
    /**
     * Determine whether this <code>DAOManager</code> was killed 
     * (following a call to {@link #kill()} or {@link #kill(long)}).
     * This does not necessarily mean that a query was interrupted, only that 
     * the <code>DAOManager</code> received a <code>kill</code> command 
     * (if no query was running when receiving the command, none were killed).
     * <p>
     * If a query was actually interrupted, then the service provider 
     * should have thrown a <code>QueryInterruptedException</code> from the thread 
     * that was interrupted. 
     * 
     * @return	<code>true</code> if this <code>DAOManager</code> was killed, 
     * 			<code>false</code> otherwise.
     * @see #kill()
     * @see #kill(long)
     */
    public final boolean isKilled() {
    	log.entry();
    	return log.exit(this.killed);
    }
    /**
     * Set {@link #killed}. The only method that should call this one besides constructors 
     * is {@link #atomicCloseAndRemoveFromPool(boolean)}. 
     * 
     * @param killed 	a <code>boolean</code> to set {@link #killed}
     */
    private final void setKilled(boolean killed) {
    	this.killed = killed;
    }
    
    /**
     * Atomic operation to set {@link #closed} to <code>true</code>, 
     * {@link #killed} to <code>true</code> if the parameter is <code>true</code>,
     * and to call {@link #removePromPool()}. 
     * This method returns <code>true</code> if the operations were actually performed, 
     * and <code>false</code> if this <code>DAOManager</code> was actually 
     * already closed. 
     * 
     * @param killed 	To indicate whether this <code>DAOManager</code> 
     * 					is being closed following a {@link #kill()} command.
     * @return 			A <code>boolean</code> <code>true</code> if the operations 
     * 					were actually performed, <code>false</code> if this 
     * 					<code>DAOManager</code> was already closed. 
     */
    private final boolean atomicCloseAndRemoveFromPool(boolean killed) {
    	log.entry(killed);
    	synchronized(this.closed) {
    		if (!this.isClosed()) {
    			this.setClosed(true);
    			if (killed) {
    				this.setKilled(true);
    			}
    			DAOManager.removePromPool();
    			return log.exit(true);
    		}
    		return log.exit(false);
    	}
    }
    
    /**
     * Performs the operations to remove the <code>DAOManager</code> associated
     * to this thread from {@link #managers} (optional operation) and to remove 
     * the thread-local ID from {@link #uniqueNumbers}.
     */
    private final static void removePromPool() {
    	log.entry();
    	managers.remove(uniqueNumber.get());
        uniqueNumber.remove();
        log.exit();
    }
    
    
    //*****************************************
    //  ABSTRACT METHODS TO IMPLEMENT
    //*****************************************	
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to release all resources managed by this <code>DAOManager</code> instance.
     * For instance, if a service provider uses the JDBC API to use a SQL database, 
     * the manager should close all <code>Connection</code>s to the database 
     * hold by its DAOs. Calling this method should not affect other 
     * <code>DAOManager</code> instances (so in our previous example, 
     * <code>Connection</code>s hold by other managers should not be closed).
     * <p>
     * This method is called by {@link #close()} after having remove this 
     * <code>DAOManager</code> from the pool. 
     */
    protected abstract void closeDAOManager();
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to immediately kill all ongoing processes handled by this <code>DAOManager</code>. 
     * It should not affect other <code>DAOManager</code>s.
     * <p>
     * If a query was interrupted following a call to this method, the service provider 
     * should make sure that a <code>QueryInterruptedException</code> will be thrown 
     * from the thread that was interrupted. To help determining 
     * if the method <code>kill</code> was called, service provider can use 
     * {@link #isKilled()}. If no query was running, then nothing should happen.
     * <p>
     * For instance, if a service provider uses the JDBC API to use a SQL database,
     * the <code>DAOManager</code> should keep track of which <code>Statement</code> 
     * is currently running, in order to be able to call <code>cancel</code> on it.
     * The thread using the <code>Statement</code> should have checked <code>isKilled</code> 
     * before calling <code>execute</cod>, and after returning from <code>execute</code>, 
     * should immediately check {@link #isKilled()} to determine if it was interrupted, 
     * and throw a <code>QueryInterruptedException</code> if it was the case. 
     * <p>
     * This method is called by {@link #kill()} after having remove this 
     * <code>DAOManager</code> from the pool. {@link #close()} will be called 
     * immediately after this method. 
     * <p>
     * Note that {@link #closeDAOManager()} will be immediately called after 
     * this method, by the method {@link #kill()}.
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
     * the <code>DAOManager</code> does not accept these parameters. 
     * This is the method used to find an appropriate Service provider 
     * when calling {@link #getDAOManager(Map)}.
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
