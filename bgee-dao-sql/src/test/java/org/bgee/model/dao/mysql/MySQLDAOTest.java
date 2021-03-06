package org.bgee.model.dao.mysql;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.DAO;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for the class {@link MySQLDAO}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class MySQLDAOTest extends TestAncestor
{
    private final static Logger log = 
            LogManager.getLogger(MySQLDAOTest.class.getName());
    
    /**
     * Default constructor.
     */
    public MySQLDAOTest() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    //**************************************
    // INNER CLASSES TO CREATE A TEST DAO
    //**************************************
    enum Attribute implements DAO.Attribute {
        ATTR1, ATTR2, ATTR3;
    }
    class TestDAO extends MySQLDAO<Attribute> {
        public TestDAO(MySQLDAOManager manager) throws IllegalArgumentException {
            super(manager);
        }
    }
    
    /**
     * Test the method {@link MySQLDAO#generateSelectClause(String, Map)}.
     */
    @Test
    public void shouldGenerateSelectClause() {
        //use a test DAO
        TestDAO dao = new TestDAO(mock(MySQLDAOManager.class));
        String tableName = "mytable";
        //create the mapping Attribute -> select_expr
        Map<String, Attribute> map = new HashMap<String, Attribute>();
        map.put("my_attr1", Attribute.ATTR1);
        map.put("my_attr2", Attribute.ATTR2);
        map.put("my_attr3", Attribute.ATTR3);
        
        //generate select clause will all data requested
        String selectAll = "SELECT " + tableName + ".* ";
        dao.setAttributes((Collection<Attribute>) null);
        assertEquals("Incorrect select clause using null attributes", 
                selectAll, dao.generateSelectClause(tableName, map, false));
        dao.setAttributes(new HashSet<Attribute>());
        assertEquals("Incorrect select clause using empty attributes", 
                selectAll, dao.generateSelectClause(tableName, map, false));
        dao.setAttributes(Arrays.asList(Attribute.values()));
        assertEquals("Incorrect select clause using all attributes", 
                selectAll, dao.generateSelectClause(tableName, map, false));
        
        //same with distinct keyword
        selectAll = "SELECT DISTINCT " + tableName + ".* ";
        dao.setAttributes((Collection<Attribute>) null);
        assertEquals("Incorrect select clause using null attributes", 
                selectAll, dao.generateSelectClause(tableName, map, true));
        dao.setAttributes(new HashSet<Attribute>());
        assertEquals("Incorrect select clause using empty attributes", 
                selectAll, dao.generateSelectClause(tableName, map, true));
        dao.setAttributes(Arrays.asList(Attribute.values()));
        assertEquals("Incorrect select clause using all attributes", 
                selectAll, dao.generateSelectClause(tableName, map, true));
        
        //now, query with not all attributes requested
        dao.setAttributes(Attribute.ATTR2, Attribute.ATTR3);
        assertEquals("Incorrect select clause using selected attributes", 
                "SELECT " + tableName + ".my_attr2, " + tableName + ".my_attr3 ", 
                dao.generateSelectClause(tableName, map, false));
        assertEquals("Incorrect select clause using selected attributes", 
                "SELECT DISTINCT " + tableName + ".my_attr2, " + tableName + ".my_attr3 ", 
                dao.generateSelectClause(tableName, map, true));
        dao.setAttributes(Attribute.ATTR3);
        assertEquals("Incorrect select clause using one attribute", 
                "SELECT " + tableName + ".my_attr3 ", 
                dao.generateSelectClause(tableName, map, false));
        assertEquals("Incorrect select clause using one attribute", 
                "SELECT DISTINCT " + tableName + ".my_attr3 ", 
                dao.generateSelectClause(tableName, map, true));
        
        //now, test exception that should be thrown if a mapping is absent
        map.remove("my_attr3");
        try {
            dao.generateSelectClause(tableName, map, false);
            //test failed, an exception should have been thrown
            throw new AssertionError("An exception should be thrown when a mapping is missing");
        } catch (IllegalArgumentException e) {
            //test passed, nothing here. 
        }
        //also with distinct set to true
        try {
            dao.generateSelectClause(tableName, map, true);
            //test failed, an exception should have been thrown
            throw new AssertionError("An exception should be thrown when a mapping is missing");
        } catch (IllegalArgumentException e) {
            //test passed, nothing here. 
        }
        
        //and if an Attribute is mapped to two select_exprs
        map.put("my_attr3", Attribute.ATTR3);
        map.put("my_attr3_2", Attribute.ATTR3);
        try {
            dao.generateSelectClause(tableName, map, false);
            //test failed, an exception should have been thrown
            throw new AssertionError("An exception should be thrown when a mapping is missing");
        } catch (IllegalArgumentException e) {
            //test passed, nothing here. 
        }
    }
}
