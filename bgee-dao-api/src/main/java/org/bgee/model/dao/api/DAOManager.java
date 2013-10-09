package org.bgee.model.dao.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.source.SourceDAO;

/**
 * Manager of DAOs, following the abstract factory pattern, to obtain and manage DAOs. 
 * This abstract class is then implemented by {@code Service Provider}s, 
 * that are automatically discovered and loaded. 
 * <p>
 * To obtain a DAOManager, clients should call one of the {@code getDAOManager} methods.
 * When calling the {@code getDAOManager} methods, the {@code DAOManager} returned 
 * is a "per-thread singleton": a {@code DAOManager} is instantiated 
 * the first time a {@code getDAOManager} method is called inside a given thread, 
 * and the same instance is then always returned when calling 
 * a {@code getDAOManager} method inside the same thread. 
 * An exception is if you call this method after having closed the {@code DAOManager}.
 * In that case, a call to a {@code getDAOManager} method from this thread 
 * would return a new {@code DAOManager} instance. 
 * <p>
 * Parameters can be provided to the DAOManager either via System properties, 
 * or via configuration file, or by using the method {@link #getDAOManager(Properties)}. 
 * Parameters to be provided are specific to the Service Provider used. 
 * For instance, if this {@code DAOManager} was obtained from a Service provider 
 * using the JDBC API to use a SQL database, then the parameters might contain 
 * the URL to connect to the database. It is up to each Service provider to specify 
 * what are the parameters needed. 
 * <p>
 * If a configuration file is used, it must be placed in the classpath. Its default 
 * name is {@link #DEFAULTCONFIGFILE}. This can be changed via System properties, 
 * using the key {@link #CONFIGFILEKEY}.
 * <p>
 * See {@link #getDAOManager(Properties)} and {@link #getDAOManager()} for more details 
 * about the instantiation process of a {@code DAOManager}.
 * <p>
 * Please note that it is extremely important to close {@code DAOManager}s 
 * when done using them, otherwise a {@code DAOManager} could be improperly 
 * reused if this API is used in an application using thread pooling, or if a thread 
 * re-uses a previously-used ID (which is standard behavior of the JDK). Methods 
 * available to close {@code DAOManager}s are {@link #close()}, {@link #kill()}, 
 * {@link #closeAll()}, {@link #kill(long)}. This class implements the 
 * {@code AutoCloseable} interface, so that it can be used in a 
 * {@code try-with-resources} statement.
 * <p>
 * This class supports the standard <a href=
 * 'http://docs.oracle.com/javase/6/docs/technotes/guides/jar/jar.html#Service%20Provider'> 
 * Service Provider</a> mechanism. Concrete implementations must include the file 
 * {@code META-INF/services/org.bgee.model.dao.api.DAOManager}. The file must contain 
 * the name of the implementation of {@code DAOManager}. For example, 
 * to load the {@code my.sql.Manager} class, 
 * the {@code META-INF/services/org.bgee.model.dao.api.DAOManager} file 
 * would contain the entry:
 * <pre>my.sql.Manager</pre>
 * To conform to the {@code Service Provider} requirements, the class implementing 
 * {@code DAOManager} must provide a default constructor with no arguments. 
 * <p>
 * Important note about {@code ServiceLoader} and shared {@code ClassLoader} 
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
     * {@code Logger} of the class. 
     */
    private final static Logger log = LogManager.getLogger(DAOManager.class.getName());
    
    /**
     * A {@code String} representing the default name of the configuration file 
     * to retrieve parameters from. The name of the file to use can be changed 
     * via system properties, using the key {@link #CONFIGFILEKEY};
     */
    public static final String DEFAULTCONFIGFILE = "bgee.dao.properties";
    /**
     * A {@code String} representing the key to use to retrieve from system properties 
     * an alternative name for the configuration file.
     */
    public static final String CONFIGFILEKEY = "bgee.dao.properties.file";
    
    /**
     * The {@code Properties} obtained at class loading either from system properties, 
     * or from a configuration file (see {@link #DEFAULTCONFIGFILE}). They will be used 
     * when {@link #getDAOManager()} is called, as default properties.
     */
    private static final Properties properties = DAOManager.getProperties();
    
    /**
     * Get the default <code>java.util.Properties</code> either from the System properties, 
     * or from a configuration file. The name of the configuration file can be changed 
     * via System properties (see {@link #CONFIGFILEKEY}). This default properties 
     * will be used when {@link #getDAOManager()} is called. 
     * @return      The <code>java.util.Properties</code> to get properties from.
     */
    private final static Properties getProperties() {
        log.entry();
        
        Properties props = new Properties();
        //try to get the properties file.
        //default name is bgee.dao.properties
        //check first if an alternative name has been provided in the System properties
        String propertyFile = System.getProperty(CONFIGFILEKEY, DEFAULTCONFIGFILE);
        log.debug("Trying to use properties file " + propertyFile);
        InputStream propStream = DAOManager.class.getResourceAsStream(propertyFile);
        if (propStream != null) {
            try {
                props.load(propStream);
            } catch (IOException e) {
                //if properties are not correctly set, we let the getDAOManager method 
                //throw an Exception if no DAOManager accepting the parameters is found.
                log.catching(e);
                return log.exit(null);
            } finally {
                try {
                    propStream.close();
                } catch (IOException e) {
                    log.catching(e);
                    return log.exit(null);
                }
            }
            log.debug("{} loaded from classpath", propertyFile);
        } else {
            props.putAll(System.getProperties());
            log.debug("{} not found in classpath. Using System properties.", propertyFile);
        }
        
        return log.exit(props);
    }
    
    /**
     * A {@code ConcurrentMap} used to store {@code DAOManager}s, 
     * associated to their ID as key (corresponding to the ID of the thread 
     * who requested the {@code DAOManager}). 
     * <p>
     * This {@code Map} is used to provide a unique and independent 
     * {@code DAOManager} instance to each thread: a {@code DAOManager} is added 
     * to this {@code Map} when a {@code getDAOManager} method is called, 
     * if the thread ID is not already present in the {@code keySet} 
     * of the {@code Map}. Otherwise, the already stored {@code DAOManager} 
     * is returned. 
     * <p>
     * If a {@code ThreadLocal} was not used, it is because 
     * this {@code Map} is used by other treads, 
     * for instance when a {@code ShutdownListener} 
     * want to properly release all {@code DAOManager}s; 
     * or when a thread performing monitoring of another thread want to kill it.
     * <p>
     * A {@code DAOManager} is removed from this {@code Map} when 
     * {@link #close()} is called on it, 
     * or when {@link #kill(long)} is called using the ID assigned to this thread, 
     * or when the method {@link #closeAll()} is called. 
     * All {@code DAOManager}s are removed when {@link #closeAll()} is called.
     */
	private static final ConcurrentMap<Long, DAOManager> managers = 
			new ConcurrentHashMap<Long, DAOManager>(); 
	
	/**
	 * A unmodifiable {@code List} containing all available providers of 
	 * the {@code DAOManager} service, in the order they were obtained 
	 * from the {@code ServiceLoader}. This is needed because we want 
	 * to load services from the {@code ServiceLoader} only once 
	 * by {@code ClassLoader}. So we could have used as attribute  
	 * a {@code static final ServiceLoader}, but {@code ServiceLoader} 
	 * lazyly instantiate service providers and is not thread-safe, so we would 
	 * have troubles in a multi-threading context. So we load all providers 
	 * at once, we don't except this pre-loading to require too much memory 
	 * (very few service providers available, used in very few libraries). 
	 * <p>
	 * As this {@code List} is unmodifiable and declared {@code final}, 
	 * and the stored {@code DAOManager}s will always be copied before use, 
	 * this attribute can safely be accessed in a multi-threading context. 
	 */
	private static final List<DAOManager> serviceProviders = 
			DAOManager.getServiceProviders();
	/**
	 * Get all available providers of the {@code DAOManager} service 
	 * from the {@code ServiceLoader}, as an unmodifiable {@code List}. 
	 * Empty {@code List} if no service providers could be found. 
	 * 
	 * @return 	An unmodifiable {@code List} of {@code DAOManager}s, 
	 * 			in the same order they were obtained from the {@code ServiceLoader}. 
	 * 			Empty {@code List} if no service providers could be found.
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
     * A volatile {@code boolean} to define if {@code DAOManager}s 
     * can still be acquired (using the {@code getDAOManager} methods), 
     * or if it is not possible anymore (meaning that the method {@link #closeAll()} 
     * has been called).
     */
    private static volatile boolean allClosed = false;
	
	/**
	 * Return a {@code DAOManager} instance with its parameters set using 
	 * the provided {@code Properties}. If it is the first call to one 
	 * of the {@code getDAOManager} methods within a given thread, 
	 * the {@code getDAOManager} methods return a newly instantiated 
	 * {@code DAOManager}. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the {@code close} 
	 * or {@code kill} method was called. Please note that it is extremely 
	 * important to close or kill {@code DAOManager} when done using it. 
	 * <p>
	 * If no {@code DAOManager} is available for this thread yet, this method 
	 * will return the first available {@code Service Provider} that accepts 
	 * {@code props}, meaning that it finds all its required parameters in it. 
	 * This method will return {@code null} if none could be found, or if no service 
	 * providers were available at all. {@code props} can be {@code null} or empty, 
	 * but that would mean that the {@code Service Provider} obtained had  
	 * absolutely no mandatory parameters.
	 * <p>
	 * If a {@code DAOManager} is already available for this thread, 
	 * then this method will simply return it, after having provided {@code props} 
	 * to it, so that its parameters can be changed after obtaining it. 
	 * If {@code props} is {@code null} or empty, then the previously provided 
	 * parameters will still be used. If {@code props} is not null nor empty, 
	 * but the already instantiated {@code DAOManager} does not find its required 
	 * parameters in it, an {@code IllegalStateException} will be thrown.
	 * <p>
	 * If caller wants to use a different {@code Service Provider}, accepting 
	 * different parameters, then {@code close} or {@code kill} 
	 * should first be called. 
	 * <p>
	 * This method will throw an {@code IllegalStateException} if {@link #closeAll()} 
	 * was called prior to calling this method, and a {@code ServiceConfigurationError} 
	 * if an error occurred while trying to find a service provider from the 
	 * {@code ServiceLoader}. 
	 * 
	 * @param props    A {@code java.util.Properties} object, 
	 *                 to be passed to the {@code DAOManager} instance. 
	 * @return 	       A {@code DAOManager} accepting the parameters provided 
	 *                 in {@code props}. {@code null} if none could be found 
	 *                 accepting the parameters, or if no service providers 
	 *                 were available at all.
	 * @throws IllegalStateException   if {@code closeAll} was already called, 
	 *                                 so that no {@code DAOManager}s can be 
	 *                                 acquired anymore. Or if the already instantiated 
	 *                                 {@code DAOManager} does not accept the provided 
	 *                                 parameters.
	 * @throws ServiceConfigurationError   If an error occurred while trying to find 
	 * 									   a {@code DAOManager} service provider 
	 * 									   from the {@code ServiceLoader}. 
	 * @see #getDAOMAnager()
	 */
	public final static DAOManager getDAOManager(Properties props) 
	    throws IllegalStateException, ServiceConfigurationError
	{
		log.entry(props);

        if (DAOManager.allClosed) {
            throw log.throwing(
            		new IllegalStateException("closeAll() has been already called, " +
                    "it is not possible to acquire a DAOManager anymore"));
        }

		//get Thread ID as key. As Thread IDs can be reused, it is extremely important 
        //to call close() on a DAOManager after use. We use Thread ID because 
        //ThreadLocal are source of troubles. 
        long threadId = Thread.currentThread().getId();
        log.debug("Trying to obtain a DAOManager with ID {}", threadId);

        DAOManager manager = managers.get(threadId);
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
        			if (props != null && !props.isEmpty()) {
        				testManager.setParameters(props);
        			}
        			//parameters accepted, we will use this manager
        			manager = testManager;
        			manager.setId(threadId);
        			log.debug("Valid DAOManager: {}", manager);
        			break providers;

        		} catch (IllegalArgumentException e) {
        			//do nothing, this exception is thrown when calling 
        			//setParameters to try to find the appropriate service provider. 
        			log.catching(Level.TRACE, e);
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
        		managers.put(threadId, manager);
        	}
        } else {
            log.debug("Get an already existing DAOManager instance");
            if (props != null && !props.isEmpty()) {
            	try {
                    manager.setParameters(props);
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
        			//otherwise, it means it was killed following a call to kill(long).
        			//we just return the closed DAOManager, this will throw 
        			//an IllegalStateException when trying to acquire a DAO from it
        		}
        		if (toThrow == null) {
        			return log.exit(manager);
        		}
        	}
        }
		
		if (toThrow != null) {
			if (toThrow instanceof IllegalStateException) {
				throw log.throwing((IllegalStateException) toThrow);
			} else if (toThrow instanceof ServiceConfigurationError) {
				throw log.throwing((ServiceConfigurationError) toThrow);
			} else if (toThrow instanceof IllegalArgumentException) {
			    //this exception means that an already instantiated DAOManager
			    //refused the parameters. This is then an illegal state.
				throw log.throwing(new IllegalStateException(toThrow));
			} else {
				throw log.throwing(
						new ServiceConfigurationError("Unexpected error", toThrow));
			}
		}
		
		return log.exit(null);
	}
	
	/**
	 * Return a {@code DAOManager} instance. If it is the first call to one 
	 * of the {@code getDAOManager} methods within a given thread, 
	 * the {@code getDAOManager} methods return a newly instantiated 
	 * {@code DAOManager}. Following calls from the same thread will always 
	 * return the same instance ("per-thread singleton"), unless the {@code close} 
	 * or {@code kill} method was called. 
	 * <p>
	 * If no {@code DAOManager} is available for this thread yet, this method 
	 * will try to obtain a {@code DAOManager} from the first Service provider 
	 * available, or will return {@code null} if no appropriate Service provider 
	 * could be found. When using this method, the parameters are retrieved either 
	 * from system properties, or from a configuration file. This method then calls 
	 * {@link #getDAOManager(Properties)} and provide to it these 
	 * properties. If these properties were not {@code null} nor empty, the 
	 * {@code DAOManager} obtained has to accept them, meaning that it found 
	 * all its mandatory parameters in it. Note that the properties obtained from 
	 * system properties or configuration file are obtained only once at class loading.
     * <p>
     * If a {@code DAOManager} is already available for this thread, 
     * then this method will simply return it, with the previously provided properties 
     * still in use. If you want to change these properties, you need to directly 
     * call {@link #getDAOManager(Properties)}.
	 * <p>
	 * This method will throw an {@code IllegalStateException} if {@link #closeAll()} 
	 * was called prior to calling this method, and a {@code ServiceConfigurationError} 
	 * if an error occurred while trying to find a service provider from the 
	 * {@code ServiceLoader}. 
	 * 
	 * @return 				The first {@code DAOManager} available,  
	 * 						{@code null} if no service providers were available at all.
	 * @throws IllegalStateException   if {@code closeAll} was already called, 
	 * 								   so that no {@code DAOManager}s can be 
	 * 								   acquired anymore. 
	 * @throws ServiceConfigurationError	If an error occurred while trying to find 
	 * 										a {@code DAOManager} service provider 
	 * 										from the {@code ServiceLoader}. 
	 * @see #getDAOManager(Properties)
	 */
	public final static DAOManager getDAOManager() throws IllegalStateException, ServiceConfigurationError {
		log.entry();
		
		if (hasDAOManager()) {
		    //this will avoid useless parsing of the properties.
		    return log.exit(getDAOManager(null));
		}
		
		//otherwise, we use the properties obtained at class loading.
		return log.exit(DAOManager.getDAOManager(properties));
	}
	
	/**
	 * Determine whether the {@code Thread} calling this method already 
	 * holds a {@code DAOManager}. It is useful for instance when willing 
	 * to close all resources at the end of an applicative code. For instance, 
	 * if the thread was not holding a {@code DAOManager}, calling 
	 * {@code DAOManager.getDAOManager().close()} would instantiate 
	 * a {@code DAOManager} just for closing it... Applicative code should rather do: 
	 * <pre>if (DAOManager.hasDAOManager()) {
	 *     DAOManager.getDAOManager().close();
	 * }</pre>
	 * There is a risk that another thread could interleave to close 
	 * the {@code DAOManager} between the test and the call to {@code close}, 
	 * but it would not have any harmful effect. 
	 * 
	 * @return	A {@code boolean} {@code true} if the {@code Thread} 
	 * 			calling this method currently holds a {@code DAOManager}, 
	 * 			{@code false} otherwise. 
	 */
	public final static boolean hasDAOManager() {
		log.entry();
		return log.exit(managers.containsKey(Thread.currentThread().getId()));
	}
	
	/**
     * Call {@link #close()} on all {@code DAOManager} instances currently registered,
     * and prevent any new {@code DAOManager} instance to be obtained again 
     * (calling a {@code getDAOManager} method from any thread 
     * after having called this method will throw an {@code IllegalStateException}). 
     * <p>
     * This method returns the number of {@code DAOManager}s that were closed. 
     * <p>
     * This method is called for instance when a {@code ShutdownListener} 
     * want to release all resources using a data source.
     * 
     * @return 	An {@code int} that is the number of {@code DAOManager} instances  
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
     * Call {@link #kill()} on the {@code DAOManager} currently registered 
     * with an ID (returned by {@link #getId()}) equals to {@code managerId}.
     * 
     * @param managerId 	A {@code long} corresponding to the ID of 
     * 						the {@code DAOManager} to kill. 
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
    /**
     * Call {@link #kill()} on the {@code DAOManager} currently associated 
     * with {@code thread}.
     * 
     * @param thread 	A {@code Thread} associated with a {@code DAOManager}. 
     * @see #kill()
     */
    /*
     * This method is used to hide the implementation detail that the ID associated 
     * to a DAOManager is its holder Thread ID.
     */
    public final static void kill(Thread thread) {
    	log.entry(thread);
    	DAOManager.kill(thread.getId());
        log.exit();
    }
    
    //*****************************************
    //  INSTANCE ATTRIBUTES AND METHODS
    //*****************************************	
    /**
     * An {@code AtomicBoolean} to indicate whether 
     * this {@code DAOManager} was closed (following a call to {@link #close()}, 
     * {@link #closeAll()}, {@link #kill()}, or {@link #kill(long)}).
     * <p>
     * This attribute is {@code final} because it is used as a lock to perform 
     * some atomic operations. It is an {@code AtomicBoolean} as it can be read 
     * by method not acquiring a lock on it.
     */
    private final AtomicBoolean closed;
    /**
     * A {@code volatile} {@code boolean} to indicate whether 
     * this {@code DAOManager} was requested to be killed ({@link #kill()} 
     * or {@link #kill(long)}). This does not necessarily mean that a query 
     * was interrupted, only that the {@code DAOManager} received 
     * a {@code kill} command (if no query was running when receiving the command, 
     * none were killed).
     * <p>
     * This attribute is {@code volatile} as it can be read and written 
     * from different threads.
     * 
     */
    private volatile boolean killed;
    /**
     * The ID of this {@code DAOManager}, corresponding to the Thread ID 
     * who requested it. This is for the sake of avoiding using a {@code ThreadLocal} 
     * to associate a {@code DAOManager} to a thread (generates issues 
     * in a thread pooling context). As Thread IDs can be reused, it is extremely important 
     * to call close() on a DAOManager after use.
     */
    private volatile long id;
    /**
     * Every concrete implementation must provide a default constructor 
     * with no parameters. 
     */
	public DAOManager() {
		log.entry();
		this.closed = new AtomicBoolean(false);
		this.setKilled(false);
		log.exit();
	}
	
	/**
	 * Return the ID associated to this {@code DAOManager}. 
	 * This ID can be used to call {@link #kill(long)}.
	 * 
	 * @return 	A {@code long} that is the ID of this {@code DAOManager}.
	 */
	public final long getId() {
		log.entry();
		return log.exit(this.id);
	}
	/**
	 * Set the ID of this {@code DAOManager}. 
	 * 
	 * @param id 	the ID of this {@code DAOManager}
	 */
	private final void setId(long id) {
		this.id = id;
	}
	
	/**
     * Close all resources managed by this {@code DAOManager} instance, 
     * and release it (a call to a {@code getDAOManager} method from the thread 
     * that was holding it will return a new {@code DAOManager} instance).
     * <p>
     * Following a call to this method, it is not possible to acquire DAOs 
     * from this {@code DAOManager} instance anymore.
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
     * Determine whether this {@code DAOManager} was closed 
     * (following a call to {@link #close()}, {@link #closeAll()}, 
     * {@link #kill()}, or {@link #kill(long)}).
     * 
     * @return	{@code true} if this {@code DAOManager} was closed, 
     * 			{@code false} otherwise.
     */
    public final boolean isClosed()
    {
        log.entry();
        return log.exit(closed.get());
    }
    /**
     * Set {@link #closed}. The only method that should call this one besides constructors 
     * is {@link #atomicCloseAndRemoveFromPool(boolean)}. 
     * 
     * @param closed 	a {@code boolean} to set {@link #closed}
     */
    private final void setClosed(boolean closed) {
    	this.closed.set(closed);
    }
    
    /**
     * Try to kill immediately all ongoing processes performed by DAOs of this 
     * {@code DAOManager}, and close and release all its resources. 
     * Closing the resources will have the same effects then calling {@link #close()}.
     * If a query was interrupted following a call to this method, the service provider 
     * should throw a {@code QueryInterruptedException} from the thread 
     * that was interrupted. 
     * 
     * @see #kill(long)
     */
    public final void kill() {
    	log.entry();
    	if (this.atomicCloseAndRemoveFromPool(true)) {
    		//implementation-specific code here
    		this.killDAOManager();
    	}
    	
    	log.exit();
    }
    
    /**
     * Determine whether this {@code DAOManager} was killed 
     * (following a call to {@link #kill()} or {@link #kill(long)}).
     * This does not necessarily mean that a query was interrupted, only that 
     * the {@code DAOManager} received a {@code kill} command 
     * (if no query was running when receiving the command, none were killed).
     * <p>
     * If a query was actually interrupted, then the service provider 
     * should have thrown a {@code QueryInterruptedException} from the thread 
     * that was interrupted. 
     * <p>
     * If this method returns {@code true}, then a call to {@link #isClosed()} 
     * will also return {@code true}.
     * 
     * @return	{@code true} if this {@code DAOManager} was killed, 
     * 			{@code false} otherwise.
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
     * @param killed 	a {@code boolean} to set {@link #killed}
     */
    private final void setKilled(boolean killed) {
    	this.killed = killed;
    }
    
    /**
     * Atomic operation to set {@link #closed} to {@code true}, 
     * {@link #killed} to {@code true} if the parameter is {@code true},
     * and to call {@link #removePromPool()}. 
     * This method returns {@code true} if the operations were actually performed, 
     * and {@code false} if this {@code DAOManager} was actually 
     * already closed. 
     * 
     * @param killed 	To indicate whether this {@code DAOManager} 
     * 					is being closed following a {@link #kill()} command.
     * @return 			A {@code boolean} {@code true} if the operations 
     * 					were actually performed, {@code false} if this 
     * 					{@code DAOManager} was already closed. 
     */
    private final boolean atomicCloseAndRemoveFromPool(boolean killed) {
    	log.entry(killed);
    	synchronized(this.closed) {
    		if (!this.isClosed()) {
    			this.setClosed(true);
    			if (killed) {
    				this.setKilled(true);
    			}
    			managers.remove(this.getId());
    			return log.exit(true);
    		}
    		return log.exit(false);
    	}
    }
    
    //*****************************************
    //  PUBLIC GET DAO METHODS
    //*****************************************
    /**
     * Method used before acquiring a DAO to check if this {@code DAOManager} 
     * is closed. It throws an {@code IllegalStateException} with proper message 
     * if it is closed. 
     * 
     * @throws IllegalStateException 	If this {@code DAOManager} is closed. 
     */
    private void checkClosed() {
    	if (this.isClosed()) {
    		throw log.throwing(new IllegalStateException(
    			"It is not possible to acquire a DAO after the DAOManager has been closed."));
    	}
    }
    /**
     * Get a new {@link org.bgee.model.dao.api.source.SourceDAO SourceDAO}, 
     * unless this {@code DAOManager} is already closed. 
     * 
     * @return 	a new {@code SourceDAO}.
     * @throws IllegalStateException 	If this {@code DAOManager} is already closed.
     * @see org.bgee.model.dao.api.source.SourceDAO SourceDAO
     */
    public SourceDAO getSourceDAO() {
    	log.entry();
    	this.checkClosed();
    	return log.exit(this.getNewSourceDAO());
    }
    
    
    //*****************************************
    //  ABSTRACT METHODS TO IMPLEMENT
    //*****************************************	
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to release all resources managed by this {@code DAOManager} instance.
     * For instance, if a service provider uses the JDBC API to use a SQL database, 
     * the manager should close all {@code Connection}s to the database 
     * hold by its DAOs. Calling this method should not affect other 
     * {@code DAOManager} instances (so in our previous example, 
     * {@code Connection}s hold by other managers should not be closed).
     * <p>
     * This method is called by {@link #close()} after having remove this 
     * {@code DAOManager} from the pool. 
     */
    protected abstract void closeDAOManager();
    
    /**
     * Service providers must implement in this method the operations necessary 
     * to immediately kill all ongoing processes handled by this {@code DAOManager}, 
     * and release and close all resources it hold. 
     * It should not affect other {@code DAOManager}s.
     * <p>
     * If a query was interrupted following a call to this method, the service provider 
     * should make sure that a {@code QueryInterruptedException} will be thrown 
     * from the thread that was interrupted. To help determining 
     * if the method {@code kill} was called, service provider can use 
     * {@link #isKilled()}. If no query was running, then nothing should happen.
     * <p>
     * For instance, if a service provider uses the JDBC API to use a SQL database,
     * the {@code DAOManager} should keep track of which {@code Statement} 
     * is currently running, in order to be able to call {@code cancel} on it.
     * The thread using the {@code Statement} should have checked {@code isKilled} 
     * before calling {@code execute</cod>, and after returning from <code>execute}, 
     * should immediately check {@link #isKilled()} to determine if it was interrupted, 
     * and throw a {@code QueryInterruptedException} if it was the case. 
     * <p>
     * This method is called by {@link #kill()} after having remove this 
     * {@code DAOManager} from the pool. Note that {@link #closeDAOManager()} 
     * is not called, it is up to the implementation to do it when needed, 
     * because it might require some atomic operations, but this {@code DAOManager} 
     * <strong>must</strong> be closed following a call to this method, 
     * as if {@link #closeDAOManager()} had been called. 
     */
    protected abstract void killDAOManager();
    
    /**
     * Set the parameters of this {@code DAOManager}. For instance, 
     * if this {@code DAOManager} was obtained from a Service provider using 
     * the JDBC API to use a SQL database, then the provided properties might 
     * contain a key/value pair defining the {@code URI} to connect to the database. 
     * It is up to each Service provider to specify what are the parameters needed. 
     * <p>
     * This method throws an {@code IllegalArgumentException} if 
     * the {@code DAOManager} does not accept these parameters, meaning that it could 
     * not find its mandatory parameters in it. This is the method used to find 
     * an appropriate Service provider when calling {@link #getDAOManager(Properties)}.
     * 
     * @param props     A {@code Properties} object containing parameter names 
     *                  and values to be used by this {@code DAOManager}. 
     * @throws IllegalArgumentException     If this {@code DAOManager} does not accept 
     * 										{@code props}. 
     */
    public abstract void setParameters(Properties props) 
    		throws IllegalArgumentException;
    
    /**
     * Service provider must return a new 
     * {@link org.bgee.model.dao.api.source.SourceDAO SourceDAO} instance 
     * when this method is called. 
     * 
     * @return 	A new {@code SourceDAO}
     */
    protected abstract SourceDAO getNewSourceDAO();
}
