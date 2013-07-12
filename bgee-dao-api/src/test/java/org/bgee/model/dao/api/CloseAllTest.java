package org.bgee.model.dao.api;

import static org.junit.Assert.*;

import java.util.concurrent.Exchanger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 * A class to test the functionality of {@link DAOManager#closeAll()}.
 * Following a call to this method, it is not possible to acquire 
 * new <code>DAOManager</code>s from the same <code>ClassLoader</code>. 
 * So we configured the project to use a different <code>ClassLoader</code> 
 * for each test class, and we put the test of <code>closeAll</code> 
 * in a separate class.
 *  
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class CloseAllTest extends TestAncestor {
	/**
     * <code>Logger</code> of the class. 
     */
    private final static Logger log = 
    		LogManager.getLogger(CloseAllTest.class.getName());

	@Override
	protected Logger getLogger() {
		return log;
	}
	
	/**
	 * Test {@link DAOManager#closeAll()}.
	 */
	@Test
	public void shouldCloseAll() throws Exception {
		/**
		 * An anonymous class to acquire <code>DAOManager</code>s 
		 * from a different thread than this one, 
		 * and to be run alternatively to the main thread.
		 */
		class ThreadTest extends Thread {
			public volatile DAOManager manager;
			public volatile Throwable exceptionThrown;
			/**
			 * An <code>Exchanger</code> that will be used to run threads alternatively. 
			 */
			public final Exchanger<Integer> exchanger = new Exchanger<Integer>();
			
			@Override
			public void run() {
				try {
					//acquire a DAOManager
			        manager = DAOManager.getDAOManager();
			        
			        //main thread's turn
			        this.exchanger.exchange(null);
			        //wait for this thread's turn
			        this.exchanger.exchange(null);
			        
			        //the main thread has called closeAll, trying to acquire 
			        //a DAOManager should throw an IllegalStateException, 
			        //that will be catch and stored
			        try {
			            DAOManager.getDAOManager();
			        } catch (IllegalStateException e) {
			        	exceptionThrown = e;
			        }
			        
			        //main thread's turn
			        this.exchanger.exchange(null);
			        
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					exceptionThrown = e;
				} 
			}
		}
		
		try {
			//get a DAOManager in the main thread
			DAOManager manager = DAOManager.getDAOManager();
			//launch a second thread also acquiring DAOManager
	        ThreadTest test = new ThreadTest();
	        test.start();
	        //wait for this thread's turn
	        test.exchanger.exchange(null);
	        //check that no exception was thrown in the second thread 
	        if (test.exceptionThrown != null) {
	        	throw new Exception("An Exception occurred in the second thread.", 
	        			test.exceptionThrown);
	        }

			//test closeAll
	        assertEquals("closeAll did not return the expected number of managers closed", 
	        		2, DAOManager.closeAll());
	        //The managers in both threads should have been closed
	        assertTrue("DAOManager in main thread was not closed", manager.isClosed());
	        assertTrue("DAOManager in second thread was not closed", 
	        		test.manager.isClosed());
	        
	        //check that an IllegalStateException is thrown if we try 
	        //to acquire a new DAOManager.
	        try {
	        	DAOManager.getDAOManager();
	        	//if we reach this point, test failed
	        	throw new AssertionError("IllegalStateException not thrown in main thread");
	        } catch (IllegalStateException e) {
	        	log.catching(Level.DEBUG, e);
	        }
	        
	        //relaunch the other thread so that it can try a DAOManager again 
			//(it should throw an IllegalStateException)
	        test.exchanger.exchange(null);
	        //wait for this thread's turn
	        test.exchanger.exchange(null);
	        //check that an IllegalStateException was thrown 
	        if (test.exceptionThrown == null || 
	        	!(test.exceptionThrown instanceof IllegalStateException)) {
	        	throw new Exception("IllegalStateException not thrown in second thread.");
	        }
			
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} 
	}
}
