package org.bgee.model.dao.mysql;

import static org.junit.Assert.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.concurrent.Exchanger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.mysql.BgeeConnection;
import org.bgee.model.dao.mysql.MySQLDAOManager;
import org.bgee.model.dao.mysql.mock.MockDataSource;
import org.bgee.model.dao.mysql.mock.MockDriver;
import org.bgee.model.dao.mysql.mock.MockInitialContextFactory;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.mockito.Mockito.*;

/**
 * Test the behavior of {@link MySQLDAOManager} when acquiring {@code BgeeConnection}s, 
 * either from a JDBC {@code Driver}, or a {@code DataSource}.
 * The getters and setters are also tested in this class, as the {@code Driver} or 
 * the {@code DataSource} must be available when setting parameters.
 * 
 * @author Frederic Bastian
 * @author Mathieu Seppey
 * @version Bgee 13
 * @since Bgee 13
 */
public class MySQLDAOManagerTest extends TestAncestor
{
    private final static Logger log = 
            LogManager.getLogger(MySQLDAOManagerTest.class.getName());
    
	
	/**
	 * Default Constructor. 
	 */
	public MySQLDAOManagerTest()
	{
		super();
	}
	@Override
	protected Logger getLogger() {
		return log;
	}
	
	/**
     * Create a naming service initial context in order to use a JNDI DataSource
     * 
     * @throws NamingException 
     * @throws IllegalStateException 
     */
	@BeforeClass
    public static void initInitialContext() throws IllegalStateException, NamingException{

        // Set our mock factory as initial context factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                MockInitialContextFactory.class.getName());     

        // Create a reference to a datasource object...
        Reference ref = new Reference(MockDataSource.class.getName());

        // And bind it to the initial context 
        InitialContext ic = new InitialContext();
        ic.rebind(MockDataSource.DATASOURCENAME, ref);   
    } 
	
	/**
	 * Test the acquisition of a {@code BgeeConnection} when using a JDBC {@code Driver}, 
	 * and only a connection URL (meaning username and password should be provided in the URL).
	 * 
	 * @throws SQLException
	 */
	@Test
	public void getDriverBgeeConnectionUrlOnly() throws SQLException {
        MockDriver.initialize();
        //set the properties to use it
	    Properties props = new Properties();
	    props.setProperty(MySQLDAOManager.JDBCURLKEY, MockDriver.MOCKURL);
	    //test to provide several JDBC driver names
	    props.setProperty(MySQLDAOManager.JDBCDRIVERNAMESKEY, MockDriver.class.getName());
	    //we "manually" obtain a MySQLDAOManager and set parameters, rather than 
	    //going through the DAOManager#getDAOManager() method
		MySQLDAOManager manager = new MySQLDAOManager();
		manager.setParameters(props);
		
		BgeeConnection conn = manager.getConnection();
		//verify that there is an actual underlying real connection
		assertNotNull("Real underlying connection missing.", conn.getRealConnection());
		//verify that the correct Driver was used
		verify(MockDriver.getMockDriver()).connect(eq(MockDriver.MOCKURL), (Properties) anyObject());
		
		manager.shutdown();
        MockDriver.initialize();
	}
    
    /**
     * Test the acquisition of a {@code BgeeConnection} when using a {@code DataSource}, 
     * with default parameters (no username/password provided to the {@code getConnection} 
     * method).
     * 
     * @throws SQLException
     */
    @Test
    public void getDataSourceBgeeConnection() throws SQLException {
        MockDataSource.initialize();
        //set the properties to use it
        Properties props = new Properties();
        props.setProperty(MySQLDAOManager.RESOURCENAMEKEY, MockDataSource.DATASOURCENAME);
        //we "manually" obtain a MySQLDAOManager and set parameters, rather than 
        //going through the DAOManager#getDAOManager() method
        MySQLDAOManager manager = new MySQLDAOManager();
        manager.setParameters(props);
        
        BgeeConnection conn = manager.getConnection();
        //verify that there is an actual underlying real connection
        assertNotNull("Real underlying connection missing.", conn.getRealConnection());
        //verify that the DataSource was correctly used
        verify(MockDataSource.getMockDataSource()).getConnection();
        
        manager.shutdown();
        MockDataSource.initialize();
    }
    
    /**
     * Test the acquisition of a {@code BgeeConnection} when using a JDBC {@code Driver}, 
     * a connection URL, a username and a passord.
     * 
     * @throws SQLException
     */
    @Test
    public void getDriverBgeeConnectionUsernamePassword() throws SQLException {
        MockDriver.initialize();
        //set the properties to use it
        Properties props = new Properties();
        props.setProperty(MySQLDAOManager.JDBCURLKEY, MockDriver.MOCKURL);
        props.setProperty(MySQLDAOManager.JDBCDRIVERNAMESKEY, MockDriver.class.getName());
        props.setProperty(MySQLDAOManager.USERKEY, "bgee.jdbc.username.test");
        props.setProperty(MySQLDAOManager.PASSWORDKEY, "bgee.jdbc.password.test");
        //we "manually" obtain a MySQLDAOManager and set parameters, rather than 
        //going through the DAOManager#getDAOManager() method
        MySQLDAOManager manager = new MySQLDAOManager();
        manager.setParameters(props);
        
        BgeeConnection conn = manager.getConnection();
        //verify that there is an actual underlying real connection
        assertNotNull("Real underlying connection missing.", conn.getRealConnection());
        //verify that the correct Driver properly used
        Properties driverProps = new Properties();
        driverProps.put("user", "bgee.jdbc.username.test");
        driverProps.put("password", "bgee.jdbc.password.test");
        //this test is based on the fact that the JDBC DriverManager add two properties 
        //when a user and a password are provided. If the DriverManager implementation 
        //changed, this test would fail.
        verify(MockDriver.getMockDriver()).connect(eq(MockDriver.MOCKURL), eq(driverProps));
        
        manager.shutdown();
        MockDriver.initialize();
    }
    
    /**
     * Test the acquisition of a {@code BgeeConnection} when using a {@code DataSource}, 
     * a username and a passord.
     * 
     * @throws SQLException
     */
    @Test
    public void getDataSourceBgeeConnectionUsernamePassword() throws SQLException {
        MockDataSource.initialize();
        //set the properties to use it
        Properties props = new Properties();
        props.setProperty(MySQLDAOManager.RESOURCENAMEKEY, MockDataSource.DATASOURCENAME);
        props.setProperty(MySQLDAOManager.USERKEY, "bgee.jdbc.username.test");
        props.setProperty(MySQLDAOManager.PASSWORDKEY, "bgee.jdbc.password.test");
        //we "manually" obtain a MySQLDAOManager and set parameters, rather than 
        //going through the DAOManager#getDAOManager() method
        MySQLDAOManager manager = new MySQLDAOManager();
        manager.setParameters(props);
        
        BgeeConnection conn = manager.getConnection();
        //verify that there is an actual underlying real connection
        assertNotNull("Real underlying connection missing.", conn.getRealConnection());
        //verify that the DataSource was correctly used
        verify(MockDataSource.getMockDataSource()).getConnection(
                "bgee.jdbc.username.test", "bgee.jdbc.password.test");
        
        manager.shutdown();
        MockDataSource.initialize();
    }
    

    /**
     * Test the behavior of {@link MySQLDAOManager#getConnection()}.
     */
    @Test
    public void shouldGetAndReleaseConnections() throws SQLException {

        MockDriver.initialize();

        //set the properties to use it
        final Properties props = new Properties();
        props.setProperty(MySQLDAOManager.JDBCURLKEY, MockDriver.MOCKURL);
        //test to provide several JDBC driver names
        props.setProperty(MySQLDAOManager.JDBCDRIVERNAMESKEY, MockDriver.class.getName());
        
        /**
         * An anonymous class to acquire {@code MySQLDAOManager}s 
         * from a different thread than this one, 
         * and to be run alternatively to the main thread.
         */
        class ThreadTest extends Thread {
            public volatile MySQLDAOManager manager1;
            public volatile BgeeConnection conn1;
            public volatile BgeeConnection conn2;
            public volatile boolean exceptionThrown = false;
            /**
             * An {@code Exchanger} that will be used to run threads alternatively. 
             */
            public final Exchanger<Integer> exchanger = new Exchanger<Integer>();
            
            @Override
            public void run() {
                try {
                    //acquire a BgeeDataSource
                    manager1 = new MySQLDAOManager();
                    manager1.setParameters(props);
                    //acquire 2 different connection
                    conn1 = manager1.getConnection();
                    props.setProperty(MySQLDAOManager.USERKEY, "test");
                    manager1.setParameters(props);
                    conn2 = manager1.getConnection();
                    props.remove(MySQLDAOManager.USERKEY);
                    manager1.setParameters(props);
                    
                    //main thread's turn
                    this.exchanger.exchange(null);
                    //wait for this thred's turn
                    this.exchanger.exchange(null);
                    
                    //try to get a connection again. It should not fail, 
                    //as the BgeeDataSource of this thread was not closed
                    manager1.getConnection();
                    
                    //main thread's turn
                    this.exchanger.exchange(null);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (SQLException e) {
                    exceptionThrown = true;
                }
            }
        }
        
        try {
            //get a BgeeDataSource in the main thread
            MySQLDAOManager manager1 = new MySQLDAOManager();

            //acquire 2 different connections
            manager1.setParameters(props);
            BgeeConnection conn1 = manager1.getConnection();
            
            props.setProperty(MySQLDAOManager.USERKEY, "test");
            manager1.setParameters(props);
            BgeeConnection conn2 = manager1.getConnection();
            props.remove(MySQLDAOManager.USERKEY);
            manager1.setParameters(props);
            
            //launch a second thread also acquiring BgeeConnections
            ThreadTest test = new ThreadTest();
            test.start();
            //wait for this thread's turn
            test.exchanger.exchange(null);
            //check that no exception was thrown in the second thread 
            if (test.exceptionThrown) {
                throw new SQLException("A SQLException occurred in the second thread.");
            }
            
            //the two connection in the main thread should be different
            assertNotNull("Failed to acquire a BgeeConnection", conn1);
            assertNotNull("Failed to acquire a BgeeConnection", conn2);
            assertNotEquals("A same thread acquire a same BgeeConnection instance " + 
                    "for different parameters", conn1, conn2);
            
            //as well as in the second thread
            assertNotNull("Failed to acquire a BgeeConnection", test.conn1);
            assertNotNull("Failed to acquire a BgeeConnection", test.conn2);
            assertNotEquals("A same thread acquire a same BgeeConnection instance " + 
                    "for different parameters", test.conn1, test.conn2);
            
            //the connections with the same parameters should be different in different threads
            assertNotEquals("Two threads acquire a same BgeeConnection instance ", 
                    conn1, test.conn1);
            assertNotEquals("Two threads acquire a same BgeeConnection instance ", 
                    conn2, test.conn2);
            
            //trying to acquire connections with the same parameters should return the same connection
            assertEquals("Get two BgeeConnection instances for the smae parameters", 
                    conn1, manager1.getConnection());
            props.setProperty(MySQLDAOManager.USERKEY, "test");
            manager1.setParameters(props);
            assertEquals("Get two BgeeConnection instances for the smae parameters", 
                    conn2, manager1.getConnection());
            props.remove(MySQLDAOManager.USERKEY);
            manager1.setParameters(props);
            
            //close the source in the main thread
            manager1.closeDAOManager();

            //close() should have been called on the mocked connection exactly two times 
            //(because we opened two connections in this thread)
            verify(MockDriver.getMockConnection(), times(2)).close();
            
            //relaunch the other thread to check if it can still acquire connections, 
            //as its BgeeDataSource was not closed
            test.exchanger.exchange(null);
            //wait for this thread's turn
            test.exchanger.exchange(null);
            //check that no exception was thrown in the second thread 
            if (test.exceptionThrown) {
                throw new SQLException("A SQLException occurred in the second thread.");
            }
            
            
            //close the BgeeDataSource one by one without calling closeAll(), 
            //that would make other test to fail
            manager1.closeDAOManager();
            test.manager1.closeDAOManager();
            
            manager1.shutdown();
            MockDriver.initialize();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } 

    }
    
    /**
     * Try to set parameters and to read them.
     * @throws SQLException 
     */
    @Test
    public void shouldGetSet() throws SQLException {
        //we need to initialize the mock DataSource, because some attributes can only 
        //be set if they are valid, corresponding to real java.sql or javax.sql objects.
        MockDataSource.initialize();
        //we will try to load the mock Driver and the MySQL Driver
        Properties props = new Properties();
        props.setProperty(MySQLDAOManager.JDBCURLKEY, MockDriver.MOCKURL);
        props.setProperty(MySQLDAOManager.RESOURCENAMEKEY, MockDataSource.DATASOURCENAME);
        props.setProperty(MySQLDAOManager.JDBCDRIVERNAMESKEY, 
                MockDriver.class.getName() + "," + com.mysql.jdbc.Driver.class.getName());
        props.setProperty(MySQLDAOManager.USERKEY, "bgee.jdbc.username.test");
        props.setProperty(MySQLDAOManager.PASSWORDKEY, "bgee.jdbc.password.test");
        
        MySQLDAOManager manager = new MySQLDAOManager();
        manager.setParameters(props);
        
        assertEquals("Incorrect JDBC URL read", MockDriver.MOCKURL, manager.getJdbcUrl());
        assertEquals("Incorrect DataSource name read", MockDataSource.DATASOURCENAME, 
                manager.getDataSourceResourceName());
        assertEquals("Incorrect JDBC Driver names read", new HashSet<String>(
                Arrays.asList(MockDriver.class.getName(), com.mysql.jdbc.Driver.class.getName())), 
                manager.getJdbcDriverNames());
        assertEquals("Incorrect username name read", "bgee.jdbc.username.test", 
                manager.getUser());
        assertEquals("Incorrect password name read", "bgee.jdbc.password.test", 
                manager.getPassword());

        manager.shutdown();
        MockDataSource.initialize();
        
    }
}
