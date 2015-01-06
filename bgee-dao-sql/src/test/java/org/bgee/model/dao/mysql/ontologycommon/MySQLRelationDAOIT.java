package org.bgee.model.dao.mysql.ontologycommon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.TOComparator;
import org.bgee.model.dao.api.ontologycommon.RelationDAO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationStatus;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationType;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTOResultSet;
import org.bgee.model.dao.mysql.MySQLITAncestor;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


/**
 * Integration tests for {@link MySQLRelationDAO}, performed on a real MySQL database. 
 * See the documentation of {@link org.bgee.model.dao.mysql.MySQLITAncestor} for 
 * important information.
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @see org.bgee.model.dao.api.ontologycommon.RelationDAO
 * @since Bgee 13
 */
public class MySQLRelationDAOIT extends MySQLITAncestor {
    
    private final static Logger log = LogManager.getLogger(MySQLRelationDAOIT.class.getName());

    public MySQLRelationDAOIT() {
        super();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Test the select method {@link MySQLRelationDAO#getAnatEntityRelations()}.
     */
    @Test
    public void shouldGetAnatEntityRelations() throws SQLException {

        this.useSelectDB();

        MySQLRelationDAO dao = new MySQLRelationDAO(this.getMySQLDAOManager());
        List<RelationTO> allRelTOs = Arrays.asList(
        new RelationTO("1", "Anat_id1", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("10", "Anat_id10", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("22", "Anat_id10", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("23", "Anat_id11", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("11", "Anat_id11", "Anat_id11", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("12", "Anat_id2", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("2", "Anat_id2", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("13", "Anat_id3", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("3", "Anat_id3", "Anat_id3", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("14", "Anat_id4", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("4", "Anat_id4", "Anat_id4", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("15", "Anat_id5", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("5", "Anat_id5", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("16", "Anat_id5", "Anat_id7", RelationType.DEVELOPSFROM, RelationStatus.INDIRECT),
        new RelationTO("17", "Anat_id5", "Anat_id8", RelationType.DEVELOPSFROM, RelationStatus.DIRECT),
        new RelationTO("18", "Anat_id6", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("6", "Anat_id6", "Anat_id6", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("19", "Anat_id7", "Anat_id6", RelationType.DEVELOPSFROM, RelationStatus.DIRECT),
        new RelationTO("7", "Anat_id7", "Anat_id7", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("20", "Anat_id8", "Anat_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("8", "Anat_id8", "Anat_id8", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("21", "Anat_id9", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        new RelationTO("9", "Anat_id9", "Anat_id9", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE));
                
        // Test recovery of all attributes without filters.
        // Warning, List is ordered by sourceId, targetId as Strings
        List<RelationTO> expectedRelations = allRelTOs;
        RelationTOResultSet resultSet = dao.getAnatEntityRelations(null, null, null);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on species IDs ONLY
        dao.setAttributes(Arrays.asList(RelationDAO.Attribute.RELATION_ID));
        Set<String> speciesIds = new HashSet<String>(Arrays.asList("11","44"));
        expectedRelations = Arrays.asList(
                new RelationTO("1", null, null, null, null),
                new RelationTO("10", null, null, null, null),
                new RelationTO("23", null, null, null, null),
                new RelationTO("12", null, null, null, null),
                new RelationTO("2", null, null, null, null),
                new RelationTO("14", null, null, null, null),
                new RelationTO("4", null, null, null, null),
                new RelationTO("15", null, null, null, null),
                new RelationTO("5", null, null, null, null),
                new RelationTO("18", null, null, null, null),
                new RelationTO("19", null, null, null, null),
                new RelationTO("8", null, null, null, null));
        resultSet = dao.getAnatEntityRelations(speciesIds, null, null);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on species IDs AND relation types
        dao.clearAttributes();
        EnumSet<RelationType> relationTypes = EnumSet.of(RelationType.ISA_PARTOF, RelationType.TRANSFORMATIONOF);
        expectedRelations = Arrays.asList(
                new RelationTO("1", "Anat_id1", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
                new RelationTO("10", "Anat_id10", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
                new RelationTO("23", "Anat_id11", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("12", "Anat_id2", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("2", "Anat_id2", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
                new RelationTO("14", "Anat_id4", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("4", "Anat_id4", "Anat_id4", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
                new RelationTO("15", "Anat_id5", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("5", "Anat_id5", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
                new RelationTO("18", "Anat_id6", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("8", "Anat_id8", "Anat_id8", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE));
        resultSet = dao.getAnatEntityRelations(speciesIds, relationTypes, null);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on species IDs AND relation status
        EnumSet<RelationStatus> relationStatus = EnumSet.of(RelationStatus.DIRECT, RelationStatus.INDIRECT);
        expectedRelations = Arrays.asList(
                new RelationTO("23", "Anat_id11", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("12", "Anat_id2", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("14", "Anat_id4", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("15", "Anat_id5", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("18", "Anat_id6", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("19", "Anat_id7", "Anat_id6", RelationType.DEVELOPSFROM, RelationStatus.DIRECT));
        resultSet = dao.getAnatEntityRelations(speciesIds, null, relationStatus);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on species IDs, relation types, 
        // and relations status,
        expectedRelations = Arrays.asList(
                new RelationTO("23", "Anat_id11", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("12", "Anat_id2", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("14", "Anat_id4", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("15", "Anat_id5", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("18", "Anat_id6", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        resultSet = dao.getAnatEntityRelations(speciesIds, relationTypes, relationStatus);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on species IDs AND relation status
        relationStatus = EnumSet.of(RelationStatus.DIRECT, RelationStatus.INDIRECT);
        expectedRelations = Arrays.asList(
                new RelationTO("22", "Anat_id10", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("23", "Anat_id11", "Anat_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("12", "Anat_id2", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("13", "Anat_id3", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("14", "Anat_id4", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("15", "Anat_id5", "Anat_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("18", "Anat_id6", "Anat_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("20", "Anat_id8", "Anat_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("21", "Anat_id9", "Anat_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        resultSet = dao.getAnatEntityRelations(null, relationTypes, relationStatus);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));

        // Test recovery of one attribute with filter on relation types ONLY
        relationTypes = EnumSet.of(RelationType.DEVELOPSFROM);
        expectedRelations = Arrays.asList(
                new RelationTO("16", "Anat_id5", "Anat_id7", RelationType.DEVELOPSFROM, 
                        RelationStatus.INDIRECT),
                new RelationTO("17", "Anat_id5", "Anat_id8", RelationType.DEVELOPSFROM, 
                        RelationStatus.DIRECT),
                new RelationTO("19", "Anat_id7", "Anat_id6", RelationType.DEVELOPSFROM, 
                        RelationStatus.DIRECT));
        resultSet = dao.getAnatEntityRelations(null, relationTypes, null);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));
        
        // Test recovery of one attribute with filter on relation status ONLY
        relationStatus = EnumSet.of(RelationStatus.INDIRECT);
        expectedRelations = Arrays.asList(
                new RelationTO("16", "Anat_id5", "Anat_id7", RelationType.DEVELOPSFROM, 
                        RelationStatus.INDIRECT));
        resultSet = dao.getAnatEntityRelations(null, null, relationStatus);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));
        
        speciesIds = new HashSet<String>(Arrays.asList("11","21", "31", "44"));
        dao.clearAttributes();
        expectedRelations = allRelTOs;
        resultSet = dao.getAnatEntityRelations(speciesIds, null, null);
        assertTrue("RelationTOs incorrectly retrieved",
                TOComparator.areTOCollectionsEqual(expectedRelations, resultSet.getAllTOs()));
    }

    /**
     * Test the select method {@link MySQLRelationDAO#getStageRelations()}.
     */
    @Test
    public void shouldGetStageRelations() throws SQLException {

        this.useSelectDB();

        MySQLRelationDAO dao = new MySQLRelationDAO(this.getMySQLDAOManager());
        List<RelationTO> reflexiveRelTOs = Arrays.asList(
        new RelationTO("0", "Stage_id1", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id2", "Stage_id2", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id3", "Stage_id3", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id4", "Stage_id4", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id5", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id6", "Stage_id6", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id7", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id8", "Stage_id8", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id9", "Stage_id9", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id10", "Stage_id10", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id11", "Stage_id11", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id12", "Stage_id12", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id13", "Stage_id13", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id14", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id15", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id16", "Stage_id16", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id17", "Stage_id17", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id18", "Stage_id18", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE));
        List<RelationTO> directRelTOs = Arrays.asList(
        new RelationTO("0", "Stage_id2", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id5", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id10", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id14", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id3", "Stage_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id4", "Stage_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id6", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id11", "Stage_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id12", "Stage_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id13", "Stage_id10", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id9", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        List<RelationTO> indirectRelTOs = Arrays.asList(
        new RelationTO("0", "Stage_id3", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id4", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id6", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id9", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id11", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id12", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id13", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id9", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
                
        // Test recovery of all attributes without filters.
        List<RelationTO> expectedRels = new ArrayList<RelationTO>();
        expectedRels.addAll(reflexiveRelTOs);
        expectedRels.addAll(directRelTOs);
        expectedRels.addAll(indirectRelTOs);
        List<RelationTO> actualRels = dao.getStageRelations(null, null).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
        		"but was: " + actualRels, 
        		TOComparator.areTOCollectionsEqual(expectedRels, actualRels));
        
        //filter on RelationStatus
        expectedRels = new ArrayList<RelationTO>();
        expectedRels.addAll(directRelTOs);
        actualRels = dao.getStageRelations(null, 
                new HashSet<RelationStatus>(Arrays.asList(RelationStatus.DIRECT))).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
                "but was: " + actualRels, 
                TOComparator.areTOCollectionsEqual(expectedRels, actualRels));

        expectedRels = new ArrayList<RelationTO>();
        expectedRels.addAll(directRelTOs);
        expectedRels.addAll(indirectRelTOs);
        actualRels = dao.getStageRelations(null, new HashSet<RelationStatus>(
                Arrays.asList(RelationStatus.DIRECT, RelationStatus.INDIRECT))).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
                "but was: " + actualRels, 
                TOComparator.areTOCollectionsEqual(expectedRels, actualRels));
        
        //filter on speciesIds
        expectedRels = Arrays.asList(
        //reflexive relations
        new RelationTO("0", "Stage_id1", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id2", "Stage_id2", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id5", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id6", "Stage_id6", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id7", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id8", "Stage_id8", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id14", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id15", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id16", "Stage_id16", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE),
        new RelationTO("0", "Stage_id18", "Stage_id18", RelationType.ISA_PARTOF, RelationStatus.REFLEXIVE), 
        //direct relations
        new RelationTO("0", "Stage_id2", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id5", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id14", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id6", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        //indirect relations
        new RelationTO("0", "Stage_id6", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        actualRels = dao.getStageRelations(
                new HashSet<String>(Arrays.asList("11")), null).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
                "but was: " + actualRels, 
                TOComparator.areTOCollectionsEqual(expectedRels, actualRels));
        
        //filter on speciesIds and RelationStatus
        expectedRels = Arrays.asList(
        //direct relations
        new RelationTO("0", "Stage_id2", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id4", "Stage_id2", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id5", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id14", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id6", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id7", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id15", RelationType.ISA_PARTOF, RelationStatus.DIRECT), 
        //indirect relations
        new RelationTO("0", "Stage_id4", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id6", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id7", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id15", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id18", "Stage_id1", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id8", "Stage_id5", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id16", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.INDIRECT), 
        new RelationTO("0", "Stage_id17", "Stage_id14", RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        actualRels = dao.getStageRelations(
                new HashSet<String>(Arrays.asList("11", "31")), new HashSet<RelationStatus>(
                    Arrays.asList(RelationStatus.DIRECT, RelationStatus.INDIRECT))).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
                "but was: " + actualRels, 
                TOComparator.areTOCollectionsEqual(expectedRels, actualRels));
        
        //weird filter on species IDs
        expectedRels = directRelTOs;
        actualRels = dao.getStageRelations(
                new HashSet<String>(Arrays.asList("11", "21", "31", "44")), 
                new HashSet<RelationStatus>(Arrays.asList(RelationStatus.DIRECT))).getAllTOs();
        assertTrue("RelationTOs incorrectly retrieved, expected: " + expectedRels + " - " +
                "but was: " + actualRels, 
                TOComparator.areTOCollectionsEqual(expectedRels, actualRels));
    }

    /**
     * Test the insert method {@link MySQLRelationDAO#insertAnatEntityRelations()}.
     */
    @Test
    public void shouldInsertAnatEntityRelations() throws SQLException {
        
        this.useEmptyDB();

        //create a Collection of TaxonConstraintTO to be inserted
        Collection<RelationTO> relationTOs = Arrays.asList(
                new RelationTO("1", "sourceId1", "targetId1", RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("2", "sourceId2", "targetId2", RelationType.DEVELOPSFROM, RelationStatus.REFLEXIVE),
                new RelationTO("3", "sourceId3", "targetId3", RelationType.TRANSFORMATIONOF, RelationStatus.INDIRECT));

        try {
            MySQLRelationDAO dao = new MySQLRelationDAO(this.getMySQLDAOManager());
            assertEquals("Incorrect number of rows inserted", 3, 
                    dao.insertAnatEntityRelations(relationTOs));
            
            //we manually verify the insertion, as we do not want to rely on other methods 
            //that are tested elsewhere.
            try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
                    prepareStatement("select 1 from anatEntityRelation " +
                            "where anatEntityRelationId = ? AND anatEntitySourceId = ? " +
                            "AND anatEntityTargetId = ? AND relationType = ? AND relationStatus = ?")) {
                
                stmt.setInt(1, 1);
                stmt.setString(2, "sourceId1");
                stmt.setString(3, "targetId1");
                stmt.setString(4, RelationType.ISA_PARTOF.getStringRepresentation());
                stmt.setString(5, RelationStatus.DIRECT.getStringRepresentation());
                assertTrue("RelationTO (AnatEntityRelation) incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());
                
                stmt.setInt(1, 2);
                stmt.setString(2, "sourceId2");
                stmt.setString(3, "targetId2");
                stmt.setString(4, RelationType.DEVELOPSFROM.getStringRepresentation());
                stmt.setString(5, RelationStatus.REFLEXIVE.getStringRepresentation());
                assertTrue("RelationTO (AnatEntityRelation) incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());

                stmt.setInt(1, 3);
                stmt.setString(2, "sourceId3");
                stmt.setString(3, "targetId3");
                stmt.setString(4, RelationType.TRANSFORMATIONOF.getStringRepresentation());
                stmt.setString(5, RelationStatus.INDIRECT.getStringRepresentation());
                assertTrue("RelationTO (AnatEntityRelation) incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());
            }
            
            this.thrown.expect(IllegalArgumentException.class);
            dao.insertAnatEntityRelations(new HashSet<RelationTO>());
        } finally {
            this.emptyAndUseDefaultDB();
        }
    }
}
