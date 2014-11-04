package org.bgee.model.dao.mysql.connector;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.TransferObject.EnumDAOField;
import org.bgee.model.dao.api.exception.QueryInterruptedException;

/**
 * Abstraction layer to use a {@code java.sql.PreparedStatement}. 
 * <p> 
 * It provides only the functionalities of the {@code java.sql.PreparedStatement} interface 
 * that are used in the Bgee project. So it does not implement the actual interface.
 * <p>
 * It implements the {@code AutoCloseable} interface so that it can be used in a 
 * {@code try-with-resources} statement.
 * <p>
 * Note that the {@code executeQuery} method should not be used directly by {@code DAO}s, 
 * but only by {@link MySQLDAOResultSet}s, this is why it is not public. See documentation 
 * of {@link MySQLDAOResultSet} for more details. {@code DAO}s should only create 
 * the SQL statement, and set the parameters.
 * <p>
 * Note that you should not use a {@code try-with-resource} statement with this class, 
 * for methods returning a pointer to the results, and not the actual results. Indeed, 
 * {@code SELECT} methods return a pointer to the results (a {@code DAOResultSet}), 
 * not the results themselves, to not overload the memory. If we were using 
 * the {@code AutoCloseable} feature in such methods, the {@code BgeePreparedStatement} 
 * would be closed, as well as its underlying {@code ResultSet}, before retrieving 
 * the results. 
 * <p>
 * You can of course use {@code AutoCloseable} feature in methods that do not return 
 * a pointer to the results (for instance, an {@code INSERT} method). 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @see MySQLDAOResultSet
 * @since Bgee 13
 */
public class BgeePreparedStatement implements AutoCloseable {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(BgeePreparedStatement.class.getName());

    /**
     * Returns a {@code String} to be used in a parameterized query with the number of parameters 
     * equals to the given {@code size}. For instance, if {@code size} is equal to {@code 3}, 
     * the returned {@code String} is: {@code ?, ?, ?}.
     * 
     * @param size  An {@code int} that is the number of parameters in the returned {@code String}.
     * @return      A {@code String} to be used in a parameterized query with the number of 
     *              parameters equals to the given {@code size}. 
     */
    public static String generateParameterizedQueryString(int size) {
        log.entry(size);
        
        StringBuilder sb = new StringBuilder((size * 2) - 1);
        for (int i = 0; i < size; i++) {
            if (i > 0) { 
                sb.append(", ");
            }
            sb.append("?");
         }

        return log.exit(sb.toString());
    }
    
    /**
     * The {@code BgeeConnection} that was used 
     * to obtain this {@code BgeePreparedStatement}.
     * Used for notification purpose. 
     */
    private final BgeeConnection bgeeConnection;
    /**
     * The real {@code java.sql.PreparedStatement} that this class wraps.
     */
    private final PreparedStatement realPreparedStatement;
    /**
     * An {@code boolean} set to {@code true} if the method {@code cancel} 
     * was called. A {@code BgeePreparedStatement} should then launch a 
     * {@code QueryInterruptedException} when it realizes that it was canceled. 
     * The exception should not be thrown by the thread asking the cancellation, 
     * but by the thread that was running the query killed. It is a {@code volatile}, 
     * as its only purpose is to be accessed by different threads.
     */
    private volatile boolean canceled;
    /**
     * A {@code boolean} defining if a call to an {@code execute} method was performed 
     * on this {@code BgeePreparedStatement}.
     */
    private boolean executed;
    /**
     * Default constructor private, should not be used. 
     */
    @SuppressWarnings("unused")
    private BgeePreparedStatement() {
        this(null, null);
    }
    /**
     * Constructor used to provide the real {@code java.sql.PreparedStatement} 
     * that this class wraps, and the {@code BgeeConnection} used to obtain 
     * this {@code BgeePreparedStatement}, for notification purpose.
     * <p>
     * Constructor package-private, so that only a {@link BgeeConnection} can provide 
     * a {@code BgeePreparedStatement}.
     * 
     * @param connection				The {@code BgeeConnection} that was used 
     * 									to obtain this {@code BgeePreparedStatement}.
     * @param realPreparedStatement     The {@code java.sql.PreparedStatement} 
     *                                  that this class wraps
     *                                                         
     */
    BgeePreparedStatement(BgeeConnection connection, 
            PreparedStatement realPreparedStatement) {
        this.bgeeConnection = connection;
        this.realPreparedStatement = realPreparedStatement;
        this.setCanceled(false);
    }  
    
    /**
     * Delegated to {@link java.sql.PreparedStatement#setString(int, String)}.
     * 
     * @param parameterIndex    {@code int} that is the index of the parameter to set.
     *                          the first parameter is 1, the second is 2, ...
     * @param x                 {@code String} that is the value of the parameter 
     *                          to set.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setString(int parameterIndex, String x) throws SQLException {
        this.getRealPreparedStatement().setString(parameterIndex, x);
    }
    /**
     * Sets the designated parameters of this {@code BgeePreparedStatement} to the values 
     * given in {@code values}. 
     * 
     * @param startIndex        An {@code int} that is the first index of the parameter to set.
     *                          If these are the first parameters set for this 
     *                          {@code BgeePreparedStatement}, the first parameter is 1.
     * @param values            A {@code List} of {@code String}s that are values to be used 
     *                          to set the parameters.
     * @throws SQLException     If parameterIndex does not correspond to a parameter marker in the 
     *                          SQL statement; if a database access error occurs or this method is 
     *                          called on a closed {@code PreparedStatement}.
     */
    public void setStrings(int startIndex, List<String> values) throws SQLException {
        log.entry(startIndex, values);
        for (String value: values) {
            this.setString(startIndex, value);
            startIndex++;
        }
    }
    /**
     * Delegated to {@link java.sql.PreparedStatement#setString(int, String)}.
     * 
     * @param parameterIndex    {@code int} that is the index of the parameter to set.
     * @param x                 {@code int} that is the value of the parameter 
     *                          to set.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setInt(int parameterIndex, int x) throws SQLException {
        this.getRealPreparedStatement().setInt(parameterIndex, x);
    }
    /**
     * Sets the designated parameters of this {@code BgeePreparedStatement} to the values 
     * given in {@code values}. 
     * 
     * @param startIndex        An {@code int} that is the first index of the parameter to set.
     *                          If these are the first parameters set for this 
     *                          {@code BgeePreparedStatement}, the first parameter is 1.
     * @param values            A {@code List} of {@code Integer}s that are values to be used 
     *                          to set the parameters.
     * @throws SQLException     If parameterIndex does not correspond to a parameter marker in the 
     *                          SQL statement; if a database access error occurs or this method is 
     *                          called on a closed {@code PreparedStatement}.
     */
    public void setIntegers(int startIndex, List<Integer> values) throws SQLException {
        log.entry(startIndex, values);
        for (int value: values) {
            this.setInt(startIndex, value);
            startIndex++;
        }
    }
    /**
     * Delegated to {@link java.sql.PreparedStatement#setBoolean(int, boolean)}.
     * 
     * @param parameterIndex    {@code int} that is the index of the parameter to set.
     * @param x                 {@code boolean} that is the value of the parameter 
     *                          to set.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setBoolean(int parameterIndex, boolean x) throws SQLException {
        this.getRealPreparedStatement().setBoolean(parameterIndex, x);
    }
    /**
     * Sets the designated parameters of this {@code BgeePreparedStatement} to the values 
     * given in {@code values}. 
     * 
     * @param startIndex        An {@code int} that is the first index of the parameter to set.
     *                          If these are the first parameters set for this 
     *                          {@code BgeePreparedStatement}, the first parameter is 1.
     * @param values            A {@code List} of {@code Boolean}s that are values to be used 
     *                          to set the parameters.
     * @throws SQLException     If parameterIndex does not correspond to a parameter marker in the 
     *                          SQL statement; if a database access error occurs or this method is 
     *                          called on a closed {@code PreparedStatement}.
     */
    public void setBooleans(int startIndex, List<Boolean> values) throws SQLException {
        log.entry(startIndex, values);
        for (boolean value: values) {
            this.setBoolean(startIndex, value);
            startIndex++;
        }
    }
    /**
     * Converts {@code x} into a {@code String} using the method {@code getStringRepresentation} 
     * and delegates to {@link #setString(int, String)}.
     * 
     * @param parameterIndex    An {@code int} that is the index of the parameter to set.
     * @param x                 An {@code EnumDAOField} that is the value of the parameter 
     *                          to set.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setEnumDAOField(int parameterIndex, EnumDAOField x) throws SQLException {
        this.setString(parameterIndex, x.getStringRepresentation());
    }
    /**
     * Sets the designated parameters of this {@code BgeePreparedStatement} to the values 
     * given in {@code values}. 
     * Generic method to avoid cast compilation errors (for instance, {@code cannot 
     * convert from List<RelationType> to List<EnumDAOField>}).
     * 
     * @param startIndex        An {@code int} that is the first index of the parameter to set.
     *                          If these are the first parameters set for this 
     *                          {@code BgeePreparedStatement}, the first parameter is 1.
     * @param values            A {@code List} of {@code Boolean}s that are values to be used 
     *                          to set the parameters.
     * @throws SQLException     If parameterIndex does not correspond to a parameter marker in the 
     *                          SQL statement; if a database access error occurs or this method is 
     *                          called on a closed {@code PreparedStatement}.
     * @param T                 The type of {@code EnumDAOField}
     */
    public <T extends Enum<T> & EnumDAOField> void setEnumDAOFields(int startIndex, 
            List<T> values) 
            throws SQLException {
        log.entry(startIndex, values);
        for (EnumDAOField value: values) {
            this.setEnumDAOField(startIndex, value);
            startIndex++;
        }
    }
    
    /**
     * Delegated to {@link java.sql.PreparedStatement#setFloat(int, float)}.
     * 
     * @param parameterIndex    {@code int} that is the index of the parameter to set.
     *                          the first parameter is 1, the second is 2, ...
     * @param x                 {@code float} that is the value of the parameter 
     *                          to set.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setFloat(int parameterIndex, float x) throws SQLException {
        this.getRealPreparedStatement().setFloat(parameterIndex, x);
    }
    /**
     * Sets the designated parameters of this {@code BgeePreparedStatement} to the values 
     * given in {@code values}. 
     * 
     * @param startIndex        An {@code int} that is the first index of the parameter to set.
     *                          If these are the first parameters set for this 
     *                          {@code BgeePreparedStatement}, the first parameter is 1.
     * @param values            A {@code List} of {@code float}s that are values to be used 
     *                          to set the parameters.
     * @throws SQLException     If parameterIndex does not correspond to a parameter marker in the 
     *                          SQL statement; if a database access error occurs or this method is 
     *                          called on a closed {@code PreparedStatement}.
     */
    public void setFloats(int startIndex, List<Float> values) throws SQLException {
        log.entry(startIndex, values);
        for (float value: values) {
            this.setFloat(startIndex, value);
            startIndex++;
        }
    }

    /**
     * Delegated to {@link java.sql.PreparedStatement#setNull(int, int)}.
     * 
     * @param parameterIndex    {@code int} that is the index of the parameter to set.
     * @param sqlType           {@code int} that is the SQL type code defined in 
     *                          {@code java.sql.Types}.
     * @throws SQLException     if parameterIndex does not correspond to a parameter 
     *                          marker in the SQL statement; if a database access error 
     *                          occurs or this method is called on a closed PreparedStatement.
     */
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        this.getRealPreparedStatement().setNull(parameterIndex, sqlType);
    }
    
    
    /**
     * Executes the SQL query in this {@code BgeePreparedStatement} object, and 
     * returns the {@code ResultSet} object generated by the query.
     * <p>
     * This method is package-private, because only a {@link MySQLDAOResultSet} 
     * is supposed to perform this call.
     * 
     * @return  a {@code ResultSet} object that contains the data produced by the query; 
     *          never {@code null}
     * @throws SQLException If a database access error occurs; this method is called 
     *                      on a closed |@code BgeePreparedStatement}, or the SQL 
     *                      statement does not return a {@code ResultSet} object.
     */
    ResultSet executeQuery() throws SQLException {
        log.entry();
        
        //before launching the query, we check the interruption flag, 
        //maybe another thread used the method cancel and does not want 
        //the query to be executed
        if (this.isCanceled()) {
            //we throw an exception even if the query was not "interrupted" as such, 
            //to let know our thread that it should not pursue its execution.
            throw log.throwing(new QueryInterruptedException());
        }
        
        //now we launch the query. Maybe another thread is requesting cancellation 
        //at this point, just before the query is launched, so that it will not 
        //be actually cancelled. But we cannot do anything to ensure atomicity, 
        //except having this thread to put a lock while launching the query in 
        //another thread, to release the lock while the query is running. Without 
        //such a mechanism, the cancel method would not be able to acquire the lock 
        //before the end of the query...
        try {
            return log.exit(this.getRealPreparedStatement().executeQuery());
        } finally {
            this.setExecuted(true);
            //check that we did not return from the executeQuery method because of 
            //a cancellation
            if (this.isCanceled()) {
                throw log.throwing(new QueryInterruptedException());
            }
        }
    }
    
    public int executeUpdate() throws SQLException {
        log.entry();
        //TODO: DRY. We just copied pasted here, because this method is used 
        //for insertion methods only, that are only used by the pipeline 
        //(it's not a good reason, I know...)
        
        //before launching the query, we check the interruption flag, 
        //maybe another thread used the method cancel and does not want 
        //the query to be executed
        if (this.isCanceled()) {
            //we throw an exception even if the query was not "interrupted" as such, 
            //to let know our thread that it should not pursue its execution.
            throw log.throwing(new QueryInterruptedException());
        }
        
        //now we launch the query. Maybe another thread is requesting cancellation 
        //at this point, just before the query is launched, so that it will not 
        //be actually cancelled. But we cannot do anything to ensure atomicity, 
        //except having this thread to put a lock while launching the query in 
        //another thread, to release the lock while the query is running. Without 
        //such a mechanism, the cancel method would not be able to acquire the lock 
        //before the end of the query...
        try {
            return log.exit(this.getRealPreparedStatement().executeUpdate());
        } finally {
            this.setExecuted(true);
            //check that we did not return from the executeQuery method because of 
            //a cancellation
            if (this.isCanceled()) {
                throw log.throwing(new QueryInterruptedException());
            }
        }
    }

    /**
     * Call {@code clearParameters} method on the real {@code PreparedStatement} 
     * that this class wraps.
     * 
     * @throws SQLException     If the real {@code PreparedStatement} that this class 
     *                          wraps throws a {@code SQLException}.  
     */
    public void clearParameters() throws SQLException {
        log.entry();
        try {
            this.getRealPreparedStatement().clearParameters();
        } catch (SQLException e) {
            throw log.throwing(e);
        } 
        log.exit();
    }

    /**
     * Close the real {@code PreparedStatement} that this class wraps, 
     * and notify of the closing the {@code BgeeConnection} used to obtain 
     * this {@code BgeePreparedStatement}.
     * 
     * @throws SQLException     If the real {@code PreparedStatement} that this class 
     *                          wraps throws a {@code SQLException} when closing.  
     */
    @Override
    public void close() throws SQLException {
        log.entry();
        try {
            this.getRealPreparedStatement().close();
        } catch (SQLException e) {
            throw log.throwing(e);
        } finally {
            this.bgeeConnection.statementClosed(this);
        }
        log.exit();
    }
    
    /**
     * Cancels this Statement object. This method can be used by one thread 
     * to cancel a statement that is being executed by another thread. 
     * A {@code BgeePreparedStatement} should then launch a {@code QueryInterruptedException} 
     * when it realizes that it was canceled. The exception should not be thrown 
     * by the thread requesting the cancellation, but by the thread that was running 
     * the query killed.
     * 
     * @throws SQLException if a database access error occurs or this method 
     *                      is called on a closed statement.
     */
    void cancel() throws SQLException {
        //set the interrupted flag before canceling, so that the thread running 
        //the query can see the flag when returning from the execution of the query.
        this.setCanceled(true);
        //here we cannot ensure any atomicity, maybe the other thread will launch 
        //its query just after the cancel method is called, but before it is completed, 
        //so that the query will not be prevented to be run. But there is nothing 
        //we can do, we can not simply use a lock in the other thread, otherwise 
        //we would need to wait for the query to end before entering this block...
        this.getRealPreparedStatement().cancel();
        this.close();
    }
    
    //***********************************
    // GETTERS/SETTERS
    //***********************************
    /**
     * @return The real {@code java.sql.PreparedStatement} that this class wraps.
     */
    public PreparedStatement getRealPreparedStatement() {
        return realPreparedStatement;
    }
    /**
     * @return {@code BgeeConnection} that was used  to obtain this 
     * {@code BgeePreparedStatement}.    
     */
    protected BgeeConnection getBgeeConnection() {
        return bgeeConnection;
    }   
    /**
     * Returns the {@code boolean} determining if the method {@code cancel} was called. 
     * A {@code BgeePreparedStatement} should then launch a {@code QueryInterruptedException} 
     * when it realizes that it was canceled. The exception should not be thrown 
     * by the thread asking the cancellation, but by the thread that was running 
     * the query killed. It is a {@code volatile}, as its only purpose is to be 
     * accessed by different threads.
     * 
     * @return  A {@code boolean} determining if the method {@code cancel} was called.
     */
    public boolean isCanceled() {
        return this.canceled;
    }
    /**
     * @param canceled the {@link #canceled} to set.
     */
    private void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
    /**
     * @return  A {@code boolean} defining if a call to an {@code execute} method 
     *          was performed on this {@code BgeePreparedStatement}.
     */
    boolean isExecuted() {
        return executed;
    }
    /**
     * @param executed  set {@link #executed}.
     */
    protected void setExecuted(boolean executed) {
        this.executed = executed;
    }   

    
}
