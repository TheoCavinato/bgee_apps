package org.bgee.model.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.BgeeProperties;

/**
 * Abstraction layer to obtain {@link BgeeConnection}s. 
 * <p>
 * This class first tries to load a <code>DataSource</code> 
 * from an <code>InitialContext</code> (most likely provided by Tomcat 
 * when this application is used in a webapp context), to obtain <code>Connection</code>s from. 
 * If the <code>DataSource</code> cannot be obtained, it means that the application 
 * is most likely used in a standalone context, and a classic <code>DriverManager</code> 
 * approach is used to obtain <code>Connection</code>s. 
 * In the standalone context, the parameters to establish a connection are retrieved from 
 * the {@link org.bgee.model.BgeeProperties BgeeProperties}.
 * <p>
 * Any call, <b>inside a thread</b>, to the method {@link #getBgeeDataSource()}, 
 * will always return the same <code>BgeeDataSource</code> instance. 
 * An exception is if you call this method 
 * after having called {@link #close()} or {@link #closeAll()}.
 * In that case, a call <code>getBgeeDataSource()</code> from this thread 
 * would return a new <code>BgeeDataSource</code> instance. 
 * <p>
 * Following the first call <b>on a given <code>BgeeDataSource</code> instance</b> 
 * to a method to obtain a connection, 
 * with a given <code>username</code> and a given <code>password</code>,
 * (meaning, using {@link #getConnection()} or {@link #getConnection(String, String)}), 
 * this class obtains a <code>Connection</code>, either from a <code>DataSource</code> 
 * or the <code>DriverManager</code>, depending on the context, and return it. 
 * This <code>BgeeDataSource</code> object stores these <code>BgeeConnection</code>s, 
 * so that any consecutive call to obtain a <code>Connection</code> 
 * with the same <code>username</code> and <code>password</code>, 
 * will return the same <code>BgeeConnection</code> object, 
 * without trying to obtain a new <code>Connection</code> from a <code>DataSource</code> 
 * or a <code>DriverManager</code>. 
 * <p>
 * An exception is that, when a <code>BgeeConnection</code> is closed 
 * (by calling {@link BgeeConnection#close()}), this <code>BgeeDataSource</code> release it, 
 * so that a consecutive call to obtain a <code>Connection</code> 
 * with the same <code>username</code> and <code>password</code>, 
 * will obtain a new <code>Connection</code> from a <code>DataSource</code> 
 * or a <code>DriverManager</code> again.
 * <p>
 * You should always call <code>BgeeDataSource.getBgeeDataSource().close()</code> 
 * at the end of the execution of a thread, 
 * and {@link #closeAll()} in multi-threads context (for instance, in a webapp context 
 * when the webapp is shutdown). 
 *  
 * @author Frederic Bastian
 * @author Mathieu Seppey
 * @version Bgee 13, May 2013
 * @since Bgee 13
 */
public class BgeeDataSource implements AutoCloseable
{
    //****************************
    // CLASS ATTRIBUTES
    //****************************
    /**
     * A <code>ConcurrentMap</code> used to store all <code>BgeeDataSource</code>s, 
     * associated with the <code>Thread</code> object that requested it. 
     * <p>
     * This <code>Map</code> is used to provide a unique and independent 
     * <code>BgeeDataSource</code> instance to each thread: 
     * a <code>BgeeDataSource</code> is added to this <code>Map</code> 
     * when {@link #getBgeeDataSource()} is called, if the thread ID is not already 
     * present in the <code>keySet</code> of the <code>Map</code>. 
     * Otherwise, the already stored <code>BgeeDataSource</code> is returned. 
     * <p>
     * If a <code>ThreadLocal</code> was not used, it is because 
     * this <code>Map</code> is used by other treads, 
     * for instance when a <code>ShutdownListener</code> 
     * want to properly close all <code>BgeeDataSource</code>s; 
     * or when a thread performing monitoring of another thread want to interrupt it.
     * <p>
     * A <code>BgeeDataSource</code> is removed from this <code>Map</code> for a thread
     * when the method {@link #close()} is called from this thread, 
     * or when the method  {@link #closeAll()} is called. 
     * All <code>BgeeDataSource</code> are removed when {@link #releaseAll()} is called.
     */
    private static final ConcurrentMap<Thread, BgeeDataSource> bgeeDataSources = 
            new ConcurrentHashMap<Thread, BgeeDataSource>();
    /**
     * An <code>AtomicBoolean</code> to define if <code>BgeeDataSource</code>s 
     * can still be acquired (using {@link #getBgeeDataSource()}), 
     * or if it is not possible anymore (meaning that the method {@link #closeAll()} 
     * has been called)
     */
    private static final AtomicBoolean dataSourcesClosed = new AtomicBoolean();
    /**
     * The real <code>DataSource</code> that this class wraps. 
     * <code>null</code> if no <code>DataSource</code> could be obtained 
     * from <code>InitialContext</code> (see static initializer).
     */
    private static final DataSource realDataSource;
    /**
     * <code>Logger</code> of the class. 
     */
    private final static Logger log = LogManager.getLogger(BgeeDataSource.class.getName());

    /**
     * An <code>AtomicInteger</code> that keep count of all pooled 
     * <code>BgeePreparedStatement</code> by every <code>BgeeConnection</code>
     *  of every instantiated <code>BgeeDatasource</code>
     */
    private final static AtomicInteger totalPrepStatPooled = new AtomicInteger();
    /**
     * The <code>BgeeDataSource</code>
     * with the most pooled <code>BgeePreparedStatement</code> in it
     */
    private static BgeeDataSource dataSourceWithMaxPrepStatPooled;


    //****************************
    // INSTANCE ATTRIBUTES
    //****************************
    /**
     * A <code>Map</code> to store the opened <code>BgeeConnection</code>s 
     * provided by this <code>BgeeDataSource</code>, associated to their <code>ID</code> 
     * (see {@link BgeeConnection#getId()}). 
     */
    private Map<String, BgeeConnection> openConnections;

    /**
     * An <code>AtomicInteger</code> that keep count of all pooled 
     * <code>BgeePreparedStatement</code> by every <code>BgeeConnection</code>
     *  of the current <code>BgeeDatasource</code>
     */
    private AtomicInteger totalPrepStatPooledInDataSource;    

    /**
     * The <code>BgeeConnection</code> with the most pooled
     * <code>BgeePreparedStatement</code> in it
     */
    private BgeeConnection connWithMaxPrepStatPooled ;


    //****************************
    // CLASS METHODS
    //****************************
    /**
     * Static initializer, initialize {@link realDataSource}, 
     * or try to register a <code>Driver</code>.
     */
    static {
        log.entry();
        log.info("Initializing BgeeDataSource...");

        dataSourcesClosed.set(false);

        // Inits the variables that keep count of the global prepared statement pooling
        BgeeDataSource.dataSourceWithMaxPrepStatPooled = null;

        DataSource dataSourceTemp = null;
        BgeeProperties props = BgeeProperties.getBgeeProperties();
        try {
            //try to get a DataSource using JNDI
            Context ctx = new InitialContext();
            dataSourceTemp = (DataSource) ctx.lookup(props.getDataSourceResourceName());
            log.info("DataSource obtained from InitialContext {} using JNDI", 
                    props.getDataSourceResourceName());
        } catch (NamingException e) {

            log.info("No DataSource obtained from InitialContext using JNDI, will rely on the DriverManager using URL {}", 
                    props.getJdbcUrl());
            //if the name of a JDBC driver was provided, try to load it.
            //it should not be needed, but some buggy JDBC Drivers need to be 
            //"manually" loaded.
            if (props.getJdbcDriver() != null) {
                try {
                    //also, calling newInstance() should not be needed, 
                    //but some buggy JDBC Drivers do not properly initialized 
                    //themselves in the static initializer.
                    Class.forName(props.getJdbcDriver()).newInstance();
                } catch (InstantiationException | IllegalAccessException
                        | ClassNotFoundException e1) {
                    //here, we do nothing: the JDBC Driver could not be loaded, 
                    //SQLExceptions will be thrown when getConnection will be called anyway.
                    log.error("Could not load the JDBC Driver " + 
                            props.getJdbcDriver(), e1);
                }
            }
        }
        realDataSource = dataSourceTemp;

        log.info("BgeeDataSource initialization done.");
        log.exit();
    }

    /**
     * Return a <code>BgeeDataSource</code> object. At the first call of this method 
     * inside a given thread, a new <code>BgeeDataSource</code> will be instantiated 
     * and returned. Then all subsequent calls to this method inside the same thread 
     * will return the same <code>BgeeDataSource</code> object. 
     * <p>
     * This is to ensure that each thread uses one and only one 
     * <code>BgeeDataSource</code> instance, 
     * independent from other threads ("per-thread singleton").
     * <p>
     * An exception is if you call this method from a thread
     * after having called {@link #release()} from this thread, 
     * or {@link #release(long)} by providing the ID of the thread as a parameter.
     * In that case, this method would return a new <code>BgeeDataSource</code> instance 
     * when called from this thread. 
     * <p>
     * Note that after having called {@link #releaseAll()}, no <code>BgeeDataSource</code> 
     * can be obtained anymore. This method will throw a <code>SQLException</code> 
     * if <code>releaseAll()</code> has been previously called.
     *  
     * @return	A <code>BgeeDataSource</code> object, instantiated at the first call 
     * 			of this method. Subsequent calls will return the same object. 
     * @throws SQLException 	If no <code>BgeeDataSource</code> can be obtained anymore. 
     */
    public static BgeeDataSource getBgeeDataSource() throws SQLException
    {
        log.entry();

        if (dataSourcesClosed.get()) {
            throw new SQLException("releaseAll() has been already called, " +
                    "it is not possible to acquire a BgeeDataSource anymore");
        }

        Thread currentThread = Thread.currentThread();
        log.debug("Trying to obtain a BgeeDataSource instance from Thread {}", 
                currentThread.getId());

        BgeeDataSource source = bgeeDataSources.get(currentThread);
        if (source == null) {
            //instantiate the BgeeDataSource only if needed
            source = new BgeeDataSource();

            // Inits the variables that keep count 
            // of the prepared statement pooling for this BgeeDataSource
            source.connWithMaxPrepStatPooled = null;
            source.totalPrepStatPooledInDataSource = new AtomicInteger(0);
            //we don't use putifAbsent, as the thread object make sure 
            //there won't be any multi-threading key collision
            bgeeDataSources.put(currentThread, source);
            log.debug("Return a new BgeeDataSource instance");
        } else {
            log.debug("Return an already existing BgeeDataSource instance");
        }
        return log.exit(source);
    }

    /**
     * Call {@link #close()} on all <code>BgeeDataSource</code>s currently registered 
     * (so it also closes all opened connections that they hold),
     * and prevent any new <code>BgeeDataSource</code> to be obtained again 
     * (calling {@link #getBgeeDataSource()} from any thread 
     * after having called this method will throw a <code>SQLException</code>). 
     * <p>
     * This method returns the number of <code>BgeeDataSource</code>s that were released. 
     * <p>
     * This method is called for instance when a <code>ShutdownListener</code> 
     * want to close all <code>Connection</code>s and release all 
     * <code>BgeeDataSource</code>s.
     * 
     * @return 	An <code>int</code> that is the number of <code>BgeeDataSource</code>s 
     * 			that were closed
     */
    public static int closeAll()
    {
        log.entry();

        //this AtomicBoolean will act more or less like a lock 
        //(no new BgeeDataSource can be obtained after this AtomicBoolean is set to true).
        //It's not totally true, but we don't except any major error if it doesn't act like a lock.
        dataSourcesClosed.set(true);

        int sourcesCount = 0;
        for (BgeeDataSource source: bgeeDataSources.values()) {
            sourcesCount++;
            source.close();
        }

        return log.exit(sourcesCount);
    }

    /**
     * @return An <code>AtomicInteger</code> that keep count of all pooled 
     * <code>BgeePreparedStatement</code> by every <code>BgeeConnection</code>
     * of every instantiated <code>BgeeDatasource</code>
     */
    private static AtomicInteger getPrepStatPoolsTotalSize(){
        return BgeeDataSource.totalPrepStatPooled;
    }

    /**
     * @return The <code>BgeeDataSource</code>
     * with the most pooled <code>BgeePreparedStatement</code> in it
     */
    private static BgeeDataSource getDataSourceWithMaxPrepStatPooled(){
        return BgeeDataSource.dataSourceWithMaxPrepStatPooled;
    }

    /**
     * Sets the <code>BgeeDataSource</code>
     * with the most pooled <code>BgeePreparedStatement</code> in it
     * 
     * @param ds    The <code>BgeeDataSource</code> 
     *              with the most pooled <code>BgeePreparedStatement</code> in it
     * 
     */
    private static void setDataSourceWithMaxPrepStatPooled(BgeeDataSource ds){
        BgeeDataSource.dataSourceWithMaxPrepStatPooled = ds;
    }    

    /**
     * Checks if the maximum of <code>BgeePreparedStatement</code> allowed is reached, and
     * if it is the case, retrieves the <code>BgeeDatasource</code> 
     * which has the most pooled
     * <code>BgeePreparedStatement</code> to call its own pool cleaning method
     */
    private synchronized static void checkAndCleanPrepStatPools(){

        log.entry();

        if(BgeeDataSource.getPrepStatPoolsTotalSize().intValue() >=  
                BgeeProperties.getBgeeProperties().getPrepStatPoolsMaxTotalSize()){
            log.info("Too many prepared statement pooled among all datasources");
            BgeeDataSource.dataSourceWithMaxPrepStatPooled.cleanPrepStatPools();
        }

        log.exit();

    }
    /**
     * Update the total number of pooled <code>BgeePreparedStatement</code>
     * It also registers the given <code>BgeeDataSource</code> as the one with
     * the most pooled <code>BgeePreparedStatement</code> if this is the case.
     * 
     *  @param deltaPrepStatNumber      An <code>int</code> which represents the change
     *                                  in the <code>BgeePreparedStatement</code> number
     *            
     *                                                        
     *  @param dataSource               The <code>BgeeDataSource</code> from which the
     *                                  report came
     *                                                                       
     */    
    private static void reportPoolState(int deltaPrepStatNumber,
            BgeeDataSource dataSource){

        log.entry(deltaPrepStatNumber,dataSource);

        // Update the number of PreparedStatment pooled, 
        // at the global level
        // No need to synchronize as it is an atomic integer
        BgeeDataSource.getPrepStatPoolsTotalSize().addAndGet(deltaPrepStatNumber);

        // Update the datasource which has the most prep stat pooled, if needed
        // Has to be in a synchronized block 
        BgeeDataSource.registerDataSourceWithMaxPrepStatPooled(dataSource);

        log.exit();

    }
    /**
     * This method contains the <code>synchronized</code> part of the report of the 
     * prepared statement pool state.
     * It registers the given <code>BgeeDataSource</code> as the one with
     * the most pooled <code>BgeePreparedStatement</code> if this is the case.
     *          
     *                                                        
     *  @param dataSource               The <code>BgeeDataSource</code> from which the
     *                                  report came
     *                                  
     *  @see #reportPoolState       
     *                                                                       
     */    
    private static synchronized void registerDataSourceWithMaxPrepStatPooled(
            BgeeDataSource dataSource){
        // Update the datasource which has the most prep stat pooled, if needed
        if(BgeeDataSource.getDataSourceWithMaxPrepStatPooled() == null || 
                BgeeDataSource.getDataSourceWithMaxPrepStatPooled()
                .getTotalPrepStatPooledInDataSource().intValue() <
                dataSource.getTotalPrepStatPooledInDataSource().intValue()){

            BgeeDataSource.setDataSourceWithMaxPrepStatPooled(dataSource);

            log.debug("Register the following datasource as the one with the most prepared statements pooled : {} with {} items",
                    dataSource,dataSource.getTotalPrepStatPooledInDataSource().intValue()); 
        }
    }

    //****************************
    // INSTANCE METHODS
    //****************************
    /**
     * Private constructor. Instances of this class can only be obtained 
     * through the <code>static</code> method {@link getBgeeDataSource()}. 
     * This is to ensure that a thread will use its own and only one instance of this class.
     */
    private BgeeDataSource()
    {
        log.entry();
        this.setOpenConnections(new HashMap<String, BgeeConnection>());
        log.exit();
    }

    /**
     * Attempt to establish a connection, either from a <code>DataSource</code> 
     * if one was provided using JNDI, or from a JDBC <code>Driver</code> 
     * obtained thanks to the <code>BgeeProperties</code>. 
     * Default username and password are used. 
     * <p>
     * If a <code>BgeeConnection</code> with the same parameters is already hold 
     * by this <code>BgeeDataSource</code>, then return it without creating a new one. 
     * This <code>BgeeDataSource</code> will hold a <code>BgeeConnection</code> 
     * as long as it is not closed (by a call to {@link BgeeConnection#close()}). 
     * <p>
     * If this <code>BgeeDataSource</code> was closed ({@link #isClosed()} 
     * returns <code>true</code>), following a call to {@link #close()} or 
     * {@link #closeAll()}, this method will throw a <code>SQLException</code>.
     * 
     * @return	A <code>BgeeConnection</code> opened to the data source. 
     * @throws SQLException 	If an error occurred while trying to obtain the connection, 
     * 							of if this <code>BgeeDataSource</code> was closed.
     * @see #getConnection(String, String)
     */
    public BgeeConnection getConnection() throws SQLException
    {
        log.entry();

        String username = null;
        String password = null;
        //if we rely on a DriverManager and not on a DataSource, 
        //we get the username and password from the Bgee properties.
        //if we rely on a DataSource, we do not obtain the default username and password 
        //from the InitialContext.

        //TODO: try to obtain the default username and password from the InitialContext.
        //Because, when using a DataSource, if we call getConnection(), 
        //then getConnection(String, String) with the default values, 
        //it would be seen as two different connections, while it should be the same.
        if (realDataSource == null) {
            BgeeProperties props = BgeeProperties.getBgeeProperties();
            username = props.getJdbcUsername();
            password = props.getJdbcPassword();
        }

        return log.exit(this.getConnection(username, password));
    }
    /**
     * Attempt to establish a connection, either from a <code>DataSource</code> 
     * if one was provided using JNDI, or from a JDBC <code>Driver</code> 
     * obtained thanks to the <code>BgeeProperties</code>, using the provided 
     * <code>username</code> and <code>password</code>. 
     * If a <code>DataSource</code> is provided, 
     * and if <code>username</code> and <code>password</code> are <code>null</code>, 
     * then return the value returned by <code>DataSource.getConnection()</code>. 
     * <p>
     * If a <code>BgeeConnection</code> with the same parameters is already hold 
     * by this <code>BgeeDataSource</code>, then return it without creating a new one. 
     * This <code>BgeeDataSource</code> will hold a <code>BgeeConnection</code> 
     * as long as it is not closed (by a call to {@link BgeeConnection#close()}). 
     * <p>
     * If this <code>BgeeDataSource</code> was closed ({@link #isClosed()} 
     * returns <code>true</code>), following a call to {@link #close()} or 
     * {@link #closeAll()}, this method will throw a <code>SQLException</code>.
     * 
     * @param username 	A <code>String</code> defining the username to use 
     * 					to open the connection.
     * @param password 	A <code>String</code> defining the password to use 
     * 					to open the connection.
     * @return			A <code>BgeeConnection</code> opened to the data source. 
     * @throws SQLException 	If an error occurred while trying to obtain the connection, 
     * 							of if this <code>BgeeDataSource</code> was closed.
     * @see #getConnection()
     */
    public BgeeConnection getConnection(String username, String password) throws SQLException
    {
        log.entry(username, password);
        log.debug("Trying to obtain a BgeeConnection using username {}...", 
                username);

        if (this.isClosed()) {
            throw new SQLException("This BgeeDataSource is already closed.");
        }

        BgeeProperties props = BgeeProperties.getBgeeProperties();
        String connectionId = this.generateConnectionId(props.getJdbcUrl(), 
                username, password);
        BgeeConnection connection = this.getOpenConnections().get(connectionId);
        //if the connection already exists, return it
        if (connection != null) {
            log.debug("Return an already opened Connection with ID {}", connection.getId());
            return log.exit(connection);
        }
        //otherwise, create a new connection
        Connection realConnection = null;
        if (realDataSource != null) {
            if (username == null && password == null) {
                log.debug("Trying to obtain a new Connection from the DataSource using default username");
                realConnection = realDataSource.getConnection();
            } else {
                log.debug("Trying to obtain a new Connection from the DataSource using username {}", 
                        username);
                realConnection = realDataSource.getConnection(username, password);
            }
        } else {
            log.debug("Trying to obtain a new Connection from the DriverManager, using URL {} and username {}", 
                    props.getJdbcUrl(), username);
            realConnection = DriverManager.getConnection(props.getJdbcUrl(), 
                    username, password);
        }
        //just in case we couldn't obtain the connection, without exception
        if (realConnection == null) {
            throw new SQLException("Could not obtain a Connection");
        }
        //now create the new BgeeConnection
        connection = new BgeeConnection(this, realConnection, connectionId);
        //store and return it
        this.storeOpenConnection(connection);

        log.debug("Return a newly opened Connection with ID {}", connection.getId());
        return log.exit(connection);
    }

    /**
     * Close all <code>BgeeConnection</code>s that this <code>BgeeDataSource</code> holds, 
     * and release this <code>BgeeDataSource</code> 
     * (a call to {@link #getBgeeDataSource()} from the thread that was holding it 
     * will return a new <code>BgeeDataSource</code> instance).
     * <p>
     * Following a call to this method, it is not possible to acquire new 
     * <code>BgeeConnection</code>s by a call to {@link #getConnection()} or 
     * {@link #getConnection(String, String)} on this object 
     * (it would throw a <code>SQLException</code>). A new instance should be acquired 
     * by a call to {@link #getBgeeDataSource()}.
     */
    public void close()
    {
        log.entry();
        //we remove this BgeeDataSource from the Map before closing the connections, 
        //as the absence from the map will prevent getConnection methods to be 
        //successfully called.
        bgeeDataSources.values().remove(this);
        this.closeConnections();

        log.exit();
    }

    /**
     * Close all <code>BgeeConnection</code>s that this <code>BgeeDataSource</code> holds.
     * Return the number of <code>BgeeConnection</code>s 
     * that were closed following a call to this method.
     * 
     * @return 	An <code>int</code> that is the number of <code>BgeeConnection</code>s 
     * 			that were closed following a call to this method.
     */
    private int closeConnections()
    {
        log.entry();
        int connectionCount = this.getOpenConnections().size();
        //get a shallow copy of the collection, so that the removal of a connection 
        //will not interfere with the iteration
        Collection<BgeeConnection> shallowCopy = 
                new ArrayList<BgeeConnection>(this.getOpenConnections().values());
        for (BgeeConnection connToClose: shallowCopy) {
            try {
                //calling this method will automatically remove the connection 
                //from openConnections
                connToClose.close();
            } catch (SQLException e) {
                //do nothing, because the connection will be removed from openConnections
                //even if an Exception occurs
            }
        }
        return log.exit(connectionCount);
    }

    /**
     * Determine whether this <code>BgeeDataSource</code> was closed 
     * (following a call to {@link #close()} or {@link #closeAll()}).
     * 
     * @return	<code>true</code> if this <code>BgeeDataSource</code> was closed, 
     * 			<code>false</code> otherwise.
     */
    public boolean isClosed()
    {
        log.entry();
        return log.exit(
                !bgeeDataSources.containsValue(this));
    }

    /**
     * Notification that a <code>BgeeConnection</code>, with an <code>ID</code> 
     * equals to <code>connectionId</code>, holds by this <code>BgeeDataSource</code>, 
     * has been closed. 
     * <p>
     * This method will thus removed from {@link #openConnections} 
     * the <code>BgeeConnection</code> with the corresponding key. 
     * 
     * @param connectionId 	A <code>String</code> representing the <code>ID</code> 
     * 						of the <code>BgeeConnection</code> that was closed. 
     */
    protected void connectionClosed(String connectionId)
    {
        log.entry(connectionId);
        log.debug("Releasing BgeeConnection with ID {}", connectionId);
        this.removeFromOpenConnection(connectionId);
        log.exit();
    }

    /**
     * Retrieves the <code>BgeeConnection</code> which has the most pooled
     * <code>BgeePreparedStatement</code> to call its own pool cleaning method
     */
    private void cleanPrepStatPools(){

        log.entry();

        this.getConnWithMaxPrepStatPooled().cleanPrepStatPools();

        log.exit();

    }    

    /**
     * This method simply passes the call to the corresponding method at the static level
     * which will check if the global maximum of <code>BgeePreparedStatement</code>
     * allowed is reached and starts the cleaning process.
     * @see #checkAndCleanPrepStatPools
     */
    protected void checkPrepStatPools(){
        log.entry();
        BgeeDataSource.checkAndCleanPrepStatPools();
        log.exit();
    }

    /**
     * Update the number of pooled <code>BgeePreparedStatement</code> for the current
     * <code>BgeeDataSource</code>
     * It also registers the given <code>BgeeConnection</code> as the one with
     * the most pooled <code>BgeePreparedStatement</code> if this is the case.
     * Finally, it follows the request at the global level by calling the corresponding
     * method at the static level
     * 
     *  @param deltaPrepStatNumber      An <code>int</code> which represents the change
     *                                  in the <code>BgeePreparedStatement</code> number
     *  
     *  @param con                      The <code>BgeeConnection</code> from which the
     *                                  report initially originated            
     *                                                        
     *  @see #reportPoolState
     *                                                                       
     */    
    protected void reportPoolState(int deltaPrepStatNumber,BgeeConnection con){

        log.entry(deltaPrepStatNumber,con);

        // Update the number of PreparedStatment pooled, 
        // at the DataSource level
        this.getTotalPrepStatPooledInDataSource().addAndGet(deltaPrepStatNumber);

        // Update the connection which has the most prep st pooled, if needed
        if(this.getConnWithMaxPrepStatPooled() == null || 
                this.getConnWithMaxPrepStatPooled()
                .getPreparedStatementPool().size() < con.getPreparedStatementPool().size()){

            this.setConnWithMaxPrepStatPooled(con);

            log.debug("Register the following connection as the one with the most prepared statements pooled : {} with {} items, for datasource {}",
                    con,con.getPreparedStatementPool().size(),this); 
        }

        BgeeDataSource.reportPoolState(deltaPrepStatNumber,this);

        log.exit();

    }


    /**
     * @param openConnections A <code>Map<String,BgeeConnection></code> to set {@link #openConnections} 
     */
    private void setOpenConnections(Map<String, BgeeConnection> openConnections) {
        this.openConnections = openConnections;
    }
    /**
     * Store <code>connection</code> in the <code>Map</code> {@link #openConnections}, 
     * associated to the key provided by a call on <code>connection</code>  
     * to the method <code>getId()</code>.
     * 
     * @param connection 	A <code>BgeeConnection</code> that is opened, to be stored. 
     */
    private void storeOpenConnection(BgeeConnection connection)
    {
        this.openConnections.put(connection.getId(), connection);
    }
    /**
     * Remove from {@link #openConnections} the <code>BgeeConnection</code> 
     * mapped to the key <code>key</code>. 
     * 
     * @param key 	A <code>String</code> representing the key of the 
     * 				<code>BgeeConnection</code> to be removed from <code>openConnections</code>.
     */
    private void removeFromOpenConnection(String key)
    {
        this.openConnections.remove(key);
    }
    /**
     * @return the {@link #openConnections}
     */
    private Map<String, BgeeConnection> getOpenConnections() {
        return this.openConnections;
    }
    /**
     * Generate an ID to uniquely identify the <code>BgeeConnection</code>s 
     * holded by this <code>BgeeDataSource</code>. It is based on  
     * <code>jdbcUrl</code>, <code>username</code>, and <code>password</code>. 
     * 
     * @param jdbcUrl 	A <code>String</code> defining the JDBC URL used to open 
     * 					the connection. Will be used to generate the ID.
     * @param username 	A <code>String</code> defining the username used to open 
     * 					the connection. Will be used to generate the ID.
     * @param password	A <code>String</code> defining the password used to open 
     * 					the connection. Will be used to generate the ID.
     * @return 			A <code>String</code> representing an ID generated from 
     * 					the JDBC URL, <code>username</code>, and <code>password</code>.
     */
    private String generateConnectionId(String jdbcUrl, String username, String password)
    {
        //I don't like much storing a password in memory, let's hash it
        //put the JDBC URL in the hash in case it was changed after the instantiation 
        //of this BgeeDataSource
        return DigestUtils.sha1Hex(jdbcUrl + "[sep]" + username + "[sep]" + password);
    }

    /**
     * @return an <code>AtomicInteger</code> that keep count of all pooled 
     *          <code>BgeePreparedStatement</code> by every <code>BgeeConnection</code>
     *          of the current <code>BgeeDatasource</code>
     */
    private AtomicInteger getTotalPrepStatPooledInDataSource(){
        return this.totalPrepStatPooledInDataSource;
    }

    /**
     * @return  The <code>BgeeConnection</code> with the most pooled
     *          <code>BgeePreparedStatement</code> in it
     */
    private BgeeConnection getConnWithMaxPrepStatPooled(){
        return this.connWithMaxPrepStatPooled;
    }

    /**
     * Sets the <code>BgeeConnection</code> with the most pooled
     *          <code>BgeePreparedStatement</code> in it
     *
     * @param con    The <code>BgeeConnection</code> with the most pooled
     *          <code>BgeePreparedStatement</code> in it
     */
    private void setConnWithMaxPrepStatPooled(BgeeConnection con){
        this.connWithMaxPrepStatPooled = con;
    }

}
