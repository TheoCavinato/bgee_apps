package org.bgee.model.expressiondata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.ServiceFactory;
import org.bgee.model.TestAncestor;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.anatdev.DevStageService;
import org.bgee.model.ontology.OntologyService;
import org.bgee.model.ontology.RelationType;
import org.bgee.model.ontology.Ontology;
import org.junit.Test;

/**
 * Unit tests for {@link ConditionUtils}.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, July 2016
 * @since   Bgee 13, Dec. 2015
 */
public class ConditionUtilsTest extends TestAncestor {
    private final static Logger log = LogManager.getLogger(ConditionUtilsTest.class.getName());
    
    @Override
    protected Logger getLogger() {
        return log;
    } 
    
    private ConditionUtils conditionUtils;
    private List<Condition> conditions;
    
    /**
     * @param fromService   A {@code boolean} {@code true} if the {@code ConditionUtils} 
     *                      should be loaded from the {@code ServiceFactory}, 
     *                      {@code false} if it should be loaded directly from {@code Ontology}s.
     */
    private void loadConditionUtils(boolean fromService) {
        String anatEntityId1 = "anat1";
        AnatEntity anatEntity1 = new AnatEntity(anatEntityId1);
        String anatEntityId2 = "anat2";
        AnatEntity anatEntity2 = new AnatEntity(anatEntityId2);
        String anatEntityId3 = "anat3";
        AnatEntity anatEntity3 = new AnatEntity(anatEntityId3);
        String anatEntityId4 = "anat4";
        AnatEntity anatEntity4 = new AnatEntity(anatEntityId4);
        String devStageId1 = "stage1";
        DevStage devStage1 = new DevStage(devStageId1);
        String devStageId2 = "stage2";
        DevStage devStage2 = new DevStage(devStageId2);
        String devStageId3 = "stage3";
        DevStage devStage3 = new DevStage(devStageId3);
        String devStageId4 = "stage4";
        DevStage devStage4 = new DevStage(devStageId4);

        String speciesId = "9606";
        
        Condition cond1 = new Condition(anatEntityId1, devStageId1, speciesId);
        Condition cond2 = new Condition(anatEntityId2, devStageId2, speciesId);
        Condition cond3 = new Condition(anatEntityId3, devStageId3, speciesId);
        Condition cond4 = new Condition(anatEntityId2, devStageId1, speciesId);
        Condition cond5 = new Condition(anatEntityId1, devStageId3, speciesId);
        Condition cond6 = new Condition(anatEntityId2, devStageId3, speciesId);
        Condition cond7 = new Condition(anatEntityId3, devStageId2, speciesId);
        Condition cond8 = new Condition(anatEntityId4, devStageId4, speciesId);
        Condition cond1_anatOnly = new Condition(anatEntityId1, null, speciesId);
        Condition cond2_anatOnly = new Condition(anatEntityId2, null, speciesId);
        Condition cond1_stageOnly = new Condition(null, devStageId1, speciesId);
        Condition cond3_stageOnly = new Condition(null, devStageId3, speciesId);
        this.conditions = Arrays.asList(cond1, cond2, cond3, cond4, cond5, cond6, cond7, cond8, 
                cond1_anatOnly, cond2_anatOnly, cond1_stageOnly, cond3_stageOnly);
        
        ServiceFactory mockFact = mock(ServiceFactory.class);
        OntologyService ontService = mock(OntologyService.class);
        AnatEntityService anatEntityService = mock(AnatEntityService.class);
        DevStageService devStageService = mock(DevStageService.class);
        when(mockFact.getOntologyService()).thenReturn(ontService);
        when(mockFact.getAnatEntityService()).thenReturn(anatEntityService);
        when(mockFact.getDevStageService()).thenReturn(devStageService);
        
        //suppress warning as we cannot specify generic type for a mock
        @SuppressWarnings("unchecked")
        Ontology<AnatEntity> anatEntityOnt = mock(Ontology.class);
        @SuppressWarnings("unchecked")
        Ontology<DevStage> devStageOnt = mock(Ontology.class);
        
        when(ontService.getAnatEntityOntology("9606", new HashSet<>(Arrays.asList(
                anatEntityId1, anatEntityId2, anatEntityId3, anatEntityId4)), 
                EnumSet.of(RelationType.ISA_PARTOF), false, false, mockFact))
        .thenReturn(anatEntityOnt);
        when(ontService.getDevStageOntology("9606", new HashSet<>(Arrays.asList(
                devStageId1, devStageId2, devStageId3, devStageId4)), false, false, mockFact))
        .thenReturn(devStageOnt);
        
        when(anatEntityOnt.getElements()).thenReturn(
                new HashSet<>(Arrays.asList(anatEntity1, anatEntity2, anatEntity3, anatEntity4)));
        when(anatEntityOnt.getElement(anatEntityId1)).thenReturn(anatEntity1);
        when(anatEntityOnt.getElement(anatEntityId2)).thenReturn(anatEntity2);
        when(anatEntityOnt.getElement(anatEntityId3)).thenReturn(anatEntity3);
        when(anatEntityOnt.getElement(anatEntityId4)).thenReturn(anatEntity4);
        when(devStageOnt.getElements()).thenReturn(
                new HashSet<>(Arrays.asList(devStage1, devStage2, devStage3, devStage4)));
        when(devStageOnt.getElement(devStageId1)).thenReturn(devStage1);
        when(devStageOnt.getElement(devStageId2)).thenReturn(devStage2);
        when(devStageOnt.getElement(devStageId3)).thenReturn(devStage3);
        when(devStageOnt.getElement(devStageId4)).thenReturn(devStage4);
        
        when(anatEntityOnt.getAncestors(anatEntity1)).thenReturn(new HashSet<>());
        when(anatEntityOnt.getAncestors(anatEntity2)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1)));
        when(anatEntityOnt.getAncestors(anatEntity3)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1)));
        when(anatEntityOnt.getAncestors(anatEntity4)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1, anatEntity3)));
        when(devStageOnt.getAncestors(devStage1)).thenReturn(new HashSet<>());
        when(devStageOnt.getAncestors(devStage2)).thenReturn(new HashSet<>(Arrays.asList(devStage1)));
        when(devStageOnt.getAncestors(devStage3)).thenReturn(new HashSet<>(Arrays.asList(devStage1)));
        when(devStageOnt.getAncestors(devStage4)).thenReturn(new HashSet<>(Arrays.asList(devStage1, devStage3)));

        when(anatEntityOnt.getAncestors(anatEntity1, false)).thenReturn(new HashSet<>());
        when(anatEntityOnt.getAncestors(anatEntity2, false)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1)));
        when(anatEntityOnt.getAncestors(anatEntity3, false)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1)));
        when(anatEntityOnt.getAncestors(anatEntity4, false)).thenReturn(new HashSet<>(Arrays.asList(anatEntity1, anatEntity3)));
        when(devStageOnt.getAncestors(devStage1, false)).thenReturn(new HashSet<>());
        when(devStageOnt.getAncestors(devStage2, false)).thenReturn(new HashSet<>(Arrays.asList(devStage1)));
        when(devStageOnt.getAncestors(devStage3, false)).thenReturn(new HashSet<>(Arrays.asList(devStage1)));
        when(devStageOnt.getAncestors(devStage4, false)).thenReturn(new HashSet<>(Arrays.asList(devStage1, devStage3)));
        
        when(anatEntityOnt.getDescendants(anatEntity1, false)).thenReturn(
                new HashSet<>(Arrays.asList(anatEntity2, anatEntity3, anatEntity4)));
        when(devStageOnt.getDescendants(devStage1, false)).thenReturn(
                new HashSet<>(Arrays.asList(devStage2, devStage3, devStage4)));
        
        if (fromService) {
            this.conditionUtils = new ConditionUtils(this.conditions, mockFact);
        } else {
            this.conditionUtils = new ConditionUtils(this.conditions, anatEntityOnt, devStageOnt);
        }
    }

    /**
     * Test the method {@link ConditionUtils#isConditionMorePrecise(Condition, Condition)}.
     */
    @Test
    public void testIsConditionMorePreciseDifferentLoadings() {
        this.loadConditionUtils(true);
        this.testIsConditionMorePrecise();
        this.loadConditionUtils(false);
        this.testIsConditionMorePrecise();
    }
    /**
     * Test the method {@link ConditionUtils#isConditionMorePrecise(Condition, Condition)}.
     */
    private void testIsConditionMorePrecise() {
        assertTrue("Incorrect determination of precision for more precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(0), this.conditions.get(1)));
        assertTrue("Incorrect determination of precision for more precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(8), this.conditions.get(9)));
        assertFalse("Incorrect determination of precision for less precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(1), this.conditions.get(0)));
        assertFalse("Incorrect determination of precision for less precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(9), this.conditions.get(8)));
        assertTrue("Incorrect determination of precision for more precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(0), this.conditions.get(2)));
        assertTrue("Incorrect determination of precision for more precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(10), this.conditions.get(11)));
        assertFalse("Incorrect determination of precision for less precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(2), this.conditions.get(0)));
        assertFalse("Incorrect determination of precision for less precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(11), this.conditions.get(10)));
        assertFalse("Incorrect determination of precision for less precise condition", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(8), this.conditions.get(11)));
        assertFalse("Incorrect determination of precision for as precise conditions", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(1), this.conditions.get(2)));

        assertFalse("Incorrect determination of precision for condition with anat. entity as precise", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(5), this.conditions.get(1)));
        assertTrue("Incorrect determination of precision for condition with anat. entity as precise", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(0), this.conditions.get(4)));
        assertFalse("Incorrect determination of precision for condition with dev. stage as precise", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(1), this.conditions.get(1)));
        assertTrue("Incorrect determination of precision for condition with dev. stage as precise", 
                this.conditionUtils.isConditionMorePrecise(this.conditions.get(0), this.conditions.get(3)));
        
        AnatEntity anatEntity1 = new AnatEntity(this.conditions.get(0).getAnatEntityId());
        assertEquals("Incorrect AnatEntity retrieved", anatEntity1, this.conditionUtils.getAnatEntity(
                this.conditions.get(0).getAnatEntityId()));
        DevStage devStage1 = new DevStage(this.conditions.get(0).getDevStageId());
        assertEquals("Incorrect DevStage retrieved", devStage1, this.conditionUtils.getDevStage(
                this.conditions.get(0).getDevStageId()));
        
        //check that an Exception is correctly thrown if a condition used was not provided at instantiation
        try {
            this.conditionUtils.isConditionMorePrecise(this.conditions.get(0), 
                    new Condition("test1", "test2", "sp3"));
            //test fail
            fail("An exception should be thrown when a Condition was not provided at instantiation.");
        } catch (IllegalArgumentException e) {
            //test passed
        }
        
    }

    /**
     * Test {@link ConditionUtils#compare(Condition, Condition)}
     */
    @Test
    public void testCompareDifferentLoadings() {
        this.loadConditionUtils(true);
        this.testCompare();
        this.loadConditionUtils(false);
        this.testCompare();
    }
    /**
     * Test {@link ConditionUtils#compare(Condition, Condition)}
     */
    private void testCompare() {
        assertEquals("Incorrect comparison based on rels between Conditions", 1, 
                this.conditionUtils.compare(this.conditions.get(0), this.conditions.get(1)));
        assertEquals("Incorrect comparison based on rels between Conditions", -1, 
                this.conditionUtils.compare(this.conditions.get(1), this.conditions.get(0)));
        assertEquals("Incorrect comparison based on rels between Conditions", 1, 
                this.conditionUtils.compare(this.conditions.get(0), this.conditions.get(2)));
        assertEquals("Incorrect comparison based on rels between Conditions", -1, 
                this.conditionUtils.compare(this.conditions.get(2), this.conditions.get(0)));
        assertEquals("Incorrect comparison based on rels between Conditions", 0, 
                this.conditionUtils.compare(this.conditions.get(1), this.conditions.get(2)));
        assertEquals("Incorrect comparison based on rels between Conditions", 0, 
                this.conditionUtils.compare(this.conditions.get(2), this.conditions.get(1)));

        assertEquals("Incorrect comparison based on rels between Conditions", 0, 
                this.conditionUtils.compare(this.conditions.get(5), this.conditions.get(1)));
        assertEquals("Incorrect comparison based on rels between Conditions", 0, 
                this.conditionUtils.compare(this.conditions.get(1), this.conditions.get(5)));
        assertEquals("Incorrect comparison based on rels between Conditions", 1, 
                this.conditionUtils.compare(this.conditions.get(0), this.conditions.get(4)));
        assertEquals("Incorrect comparison based on rels between Conditions", -1, 
                this.conditionUtils.compare(this.conditions.get(4), this.conditions.get(0)));
        assertEquals("Incorrect comparison based on rels between Conditions", 0, 
                this.conditionUtils.compare(this.conditions.get(1), this.conditions.get(1)));
        assertEquals("Incorrect comparison based on rels between Conditions", 1, 
                this.conditionUtils.compare(this.conditions.get(0), this.conditions.get(3)));
        assertEquals("Incorrect comparison based on rels between Conditions", -1, 
                this.conditionUtils.compare(this.conditions.get(3), this.conditions.get(0)));
    }

    /**
     * Test the method {@link ConditionUtils#getDescendantConditions(Condition)}.
     */
    @Test
    public void shouldGetDescendantConditionsDifferentLoadings() {
        this.loadConditionUtils(true);
        this.shouldGetDescendantConditions();
        this.loadConditionUtils(false);
        this.shouldGetDescendantConditions();
    }
    /**
     * Test the method {@link ConditionUtils#getDescendantConditions(Condition)}.
     */
    private void shouldGetDescendantConditions() {
        Set<Condition> expectedDescendants = conditions.stream()
                .filter(e -> !e.equals(this.conditions.get(0)) && this.conditions.indexOf(e) <= 7)
                .collect(Collectors.toSet());
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(0)));
        
        expectedDescendants = new HashSet<>(Arrays.asList(this.conditions.get(1), this.conditions.get(5)));
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(3)));
        
        expectedDescendants = new HashSet<>(Arrays.asList(this.conditions.get(2), this.conditions.get(5)));
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(4)));
        
        expectedDescendants = new HashSet<>();
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(6)));
        
        expectedDescendants = new HashSet<>(Arrays.asList(this.conditions.get(9)));
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(8)));
        
        expectedDescendants = new HashSet<>(Arrays.asList(this.conditions.get(11)));
        assertEquals("Incorrect descendants retrieved", expectedDescendants, 
                this.conditionUtils.getDescendantConditions(this.conditions.get(10)));
    }

    /**
     * Test the method {@link ConditionUtils#getAncestorConditions(Condition, boolean)}.
     */
    @Test
    public void shouldGetAncestorConditionsDifferentLoadings() {
        this.loadConditionUtils(true);
        this.shouldGetAncestorConditions();
        this.loadConditionUtils(false);
        this.shouldGetAncestorConditions();
    }
    /**
     * Test the method {@link ConditionUtils#getAncestorConditions(Condition, boolean)}.
     */
    private void shouldGetAncestorConditions() {
        Set<Condition> expectedAncestors = new HashSet<>(Arrays.asList(this.conditions.get(0)));
        assertEquals("Incorrect ancestors retrieved", expectedAncestors, 
                this.conditionUtils.getAncestorConditions(this.conditions.get(6), false));
        
        expectedAncestors = new HashSet<>(Arrays.asList(this.conditions.get(0)));
        assertEquals("Incorrect ancestors retrieved", expectedAncestors, 
                this.conditionUtils.getAncestorConditions(this.conditions.get(3), false));

        expectedAncestors = new HashSet<>();
        assertEquals("Incorrect ancestors retrieved", expectedAncestors, 
                this.conditionUtils.getAncestorConditions(this.conditions.get(0), false));
        
        expectedAncestors = new HashSet<>(Arrays.asList(
                this.conditions.get(0), this.conditions.get(2), this.conditions.get(4)));
        assertEquals("Incorrect ancestors retrieved", expectedAncestors, 
                this.conditionUtils.getAncestorConditions(this.conditions.get(7), false));
    }
}
