package org.bgee.model.expressiondata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.TestAncestor;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.species.Species;
import org.junit.Test;

/**
 * Unit tests for {@link Condition}. 
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, July 2016
 * @since   Bgee 13, June 2016
 */
public class ConditionTest extends TestAncestor {
    private final static Logger log = LogManager.getLogger(ConditionTest.class.getName());
    
    @Override
    protected Logger getLogger() {
        return log;
    } 
    
    /**
     * Test that equals/hashCode/compareTo are consistent for {@code Condition} class.
     */
    @Test
    //TODO: to remove to rely on the Entity hashCode/equals method
    public void testEqualsHashCodeCompareTo() {
        // Assert with same conditions
        Condition c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        Condition c2 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        assertTrue("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect hashCode", c1.hashCode(), c2.hashCode());
        assertEquals("Incorrect compareTo", 0, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 0, c2.compareTo(c1));
        
        c1 = new Condition(new AnatEntity("Anat1"), null, new Species(1));
        c2 = new Condition(new AnatEntity("Anat1"), null, new Species(1));
        assertTrue("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect hashCode", c1.hashCode(), c2.hashCode());
        assertEquals("Incorrect compareTo", 0, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 0, c2.compareTo(c1));
        
        c1 = new Condition(null, new DevStage("stage1"), new Species(1));
        c2 = new Condition(null, new DevStage("stage1"), new Species(1));
        assertTrue("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect hashCode", c1.hashCode(), c2.hashCode());
        assertEquals("Incorrect compareTo", 0, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 0, c2.compareTo(c1));
        
        // Assert with different conditions
        // different anat entity IDs
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat2"), new DevStage("stage1"), new Species(1));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
        
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage2"), new Species(1));
        c2 = new Condition(null, new DevStage("stage2"), new Species(1));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));

        // different stage IDs
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat1"), new DevStage("stage2"), new Species(1));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
        
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat1"), null, new Species(1));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
        
        // different species IDs
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage2"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat1"), new DevStage("stage2"), new Species(2));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
        
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(2));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
        
        // different IDs
        c1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage2"), new Species(1));
        c2 = new Condition(new AnatEntity("Anat2"), new DevStage("stage1"), new Species(2));
        assertFalse("Incorrect equals", c1.equals(c2));
        assertEquals("Incorrect compareTo", -1, c1.compareTo(c2));
        assertEquals("Incorrect compareTo", 1, c2.compareTo(c1));
    }
}
