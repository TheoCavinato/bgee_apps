package org.bgee.model.dao.api.ontologycommon;

import static org.junit.Assert.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.TestAncestor;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationStatus;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationType;
import org.junit.Test;

/**
 * Unit tests for inner classes of {@link RelationDAO}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class RelationDAOTest extends TestAncestor {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(RelationDAOTest.class.getName());
    
    public RelationDAOTest() {
        super();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Test {@link RelationTO.RelationType#convertToRelationType(String)}.
     */
    @Test
    public void shouldConvertToRelationType() {
        assertEquals("Incorrect RelationType returned", RelationType.ISA_PARTOF, 
                RelationType.convertToRelationType(
                        RelationType.ISA_PARTOF.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationType.ISA_PARTOF, 
                RelationType.convertToRelationType(RelationType.ISA_PARTOF.name()));

        assertEquals("Incorrect RelationType returned", RelationType.DEVELOPSFROM, 
                RelationType.convertToRelationType(
                        RelationType.DEVELOPSFROM.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationType.DEVELOPSFROM, 
                RelationType.convertToRelationType(RelationType.DEVELOPSFROM.name()));

        assertEquals("Incorrect RelationType returned", RelationType.TRANSFORMATIONOF, 
                RelationType.convertToRelationType(
                        RelationType.TRANSFORMATIONOF.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationType.TRANSFORMATIONOF, 
                RelationType.convertToRelationType(RelationType.TRANSFORMATIONOF.name()));
        
        //should throw an IllegalArgumentException when not matching any RelationType
        try {
            RelationType.convertToRelationType("whatever");
            //test failed
            throw new AssertionError("convertToRelationType did not throw " +
                    "an IllegalArgumentException as expected");
        } catch (IllegalArgumentException e) {
            //test passed, do nothing
            log.catching(e);
        }
    }
    
    /**
     * Test {@link RelationTO.RelationStatus#convertToRelationStatus(String)}.
     */
    @Test
    public void shouldConvertToRelationStatus() {
        assertEquals("Incorrect RelationStatus returned", RelationStatus.DIRECT, 
                RelationStatus.convertToRelationStatus(
                        RelationStatus.DIRECT.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationStatus.DIRECT, 
                RelationStatus.convertToRelationStatus(RelationStatus.DIRECT.name()));

        assertEquals("Incorrect RelationStatus returned", RelationStatus.INDIRECT, 
                RelationStatus.convertToRelationStatus(
                        RelationStatus.INDIRECT.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationStatus.INDIRECT, 
                RelationStatus.convertToRelationStatus(RelationStatus.INDIRECT.name()));

        assertEquals("Incorrect RelationStatus returned", RelationStatus.REFLEXIVE, 
                RelationStatus.convertToRelationStatus(
                        RelationStatus.REFLEXIVE.getStringRepresentation()));
        assertEquals("Incorrect RelationType returned", RelationStatus.REFLEXIVE, 
                RelationStatus.convertToRelationStatus(RelationStatus.REFLEXIVE.name()));
        
        //should throw an IllegalArgumentException when not matching any RelationStatus
        try {
            RelationStatus.convertToRelationStatus("whatever");
            //test failed
            throw new AssertionError("convertToRelationStatus did not throw " +
                    "an IllegalArgumentException as expected");
        } catch (IllegalArgumentException e) {
            //test passed, do nothing
            log.catching(e);
        }
    }
}
