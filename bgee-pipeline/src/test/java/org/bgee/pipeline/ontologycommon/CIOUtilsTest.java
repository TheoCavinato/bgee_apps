package org.bgee.pipeline.ontologycommon;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.pipeline.TestAncestor;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owltools.graph.OWLGraphWrapper;

/**
 * Unit tests for {@link CIOUtils}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class CIOUtilsTest extends TestAncestor {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = LogManager.getLogger(CIOUtilsTest.class.getName());
    
    /**
     * Default Constructor. 
     */
    public CIOUtilsTest() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Test correct loading of the CIO at instantiation of {@code CIOUtils}, and check 
     * that the code is in sync with the CIO version used.
     * @throws OBOFormatParserException
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    @Test
    public void shouldLoadCIO() throws OBOFormatParserException, OWLOntologyCreationException, 
    IOException {
        OWLOntology ont = OntologyUtils.loadOntology(CIOUtilsTest.class.
                getResource("/ontologies/confidence_information_ontology.owl").getFile());
        OWLGraphWrapper wrapper = new OWLGraphWrapper(ont);
        CIOUtils utils = new CIOUtils(wrapper);
        
        Set<OWLClass> allClasses = new HashSet<OWLClass>();
        assertEquals("single evidence conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.SINGLE_EVIDENCE_CONF_ID), 
                utils.getSingleEvidenceConf());
        allClasses.add(utils.getSingleEvidenceConf());
        assertEquals("rejected term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.REJECTED_TERM_ID), 
                utils.getRejectedTerm());
        allClasses.add(utils.getRejectedTerm());
        assertEquals("multiple evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.MULTIPLE_EVIDENCE_CONF_ID), 
                utils.getMultipleEvidenceConf());
        allClasses.add(utils.getMultipleEvidenceConf());
        assertEquals("congruent same type evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.CONGRUENT_SAME_TYPE_EVIDENCE_CONF_ID), 
                utils.getCongruentSameTypeEvidenceConf());
        allClasses.add(utils.getCongruentSameTypeEvidenceConf());
        assertEquals("congruent multiple types evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.CONGRUENT_MULTIPLE_TYPES_EVIDENCE_CONF_ID), 
                utils.getCongruentMultipleTypesEvidenceConf());
        allClasses.add(utils.getCongruentMultipleTypesEvidenceConf());
        assertEquals("weakly conflicting same type evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.WEAK_CONFLICT_SAME_TYPE_EVIDENCE_CONF_ID), 
                utils.getWeakConflictSameTypeEvidenceConf());
        allClasses.add(utils.getWeakConflictSameTypeEvidenceConf());
        assertEquals("weakly conflicting multiple types evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.WEAK_CONFLICT_MULTIPLE_TYPES_EVIDENCE_CONF_ID), 
                utils.getWeakConflictMultipleTypesEvidenceConf());
        allClasses.add(utils.getWeakConflictMultipleTypesEvidenceConf());
        assertEquals("strong conflicting same type evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.STRONG_CONFLICT_SAME_TYPE_EVIDENCE_CONF_ID), 
                utils.getStrongConflictSameTypeEvidenceConf());
        allClasses.add(utils.getStrongConflictSameTypeEvidenceConf());
        assertEquals("strong conflicting multiple types evidence lines conf. not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.STRONG_CONFLICT_MULTIPLE_TYPES_EVIDENCE_CONF_ID), 
                utils.getStrongConflictMultipleTypesEvidenceConf());
        allClasses.add(utils.getStrongConflictMultipleTypesEvidenceConf());
        assertEquals("high conf. level not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.HIGH_CONF_LEVEL_ID), 
                utils.getHighConfLevel());
        allClasses.add(utils.getHighConfLevel());
        assertEquals("medium conf. level not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.MEDIUM_CONF_LEVEL_ID), 
                utils.getMediumConfLevel());
        allClasses.add(utils.getMediumConfLevel());
        assertEquals("low conf. level not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.LOW_CONF_LEVEL_ID), 
                utils.getLowConfLevel());
        allClasses.add(utils.getLowConfLevel());
        assertEquals("congruant concordance term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.CONGRUENT_CONCORDANCE_ID), 
                utils.getCongruentConcordance());
        allClasses.add(utils.getCongruentConcordance());
        assertEquals("weakly conflicting concordance term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.WEAKLY_CONFLICTING_CONCORDANCE_ID), 
                utils.getWeaklyConflictingConcordance());
        allClasses.add(utils.getWeaklyConflictingConcordance());
        assertEquals("strongly conflicting concordance term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.STRONGLY_CONFLICTING_CONCORDANCE_ID), 
                utils.getStronglyConflictingConcordance());
        allClasses.add(utils.getStronglyConflictingConcordance());
        assertEquals("same type evidence concordance term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.SAME_TYPE_EVIDENCE_CONCORDANCE_ID), 
                utils.getSameTypeEvidenceConcordance());
        allClasses.add(utils.getSameTypeEvidenceConcordance());
        assertEquals("different types evidence concordance term not retrieved.", 
                wrapper.getOWLClassByIdentifier(CIOUtils.DIFFERENT_TYPES_EVIDENCE_CONCORDANCE_ID), 
                utils.getDifferentTypesEvidenceConcordance());
        allClasses.add(utils.getDifferentTypesEvidenceConcordance());
        //check there is no equal classes
        assertEquals("Incorrect number of classes", 17, allClasses.size());
        
        Set<OWLClass> expectedValidBranches = new HashSet<OWLClass>(Arrays.asList(
                wrapper.getOWLClassByIdentifier(CIOUtils.SINGLE_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.CONGRUENT_SAME_TYPE_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.CONGRUENT_MULTIPLE_TYPES_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.WEAK_CONFLICT_SAME_TYPE_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.WEAK_CONFLICT_MULTIPLE_TYPES_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.STRONG_CONFLICT_SAME_TYPE_EVIDENCE_CONF_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.STRONG_CONFLICT_MULTIPLE_TYPES_EVIDENCE_CONF_ID)));
        assertEquals("Invalid valid branches", expectedValidBranches, utils.getValidBranches());
        
        List<OWLClass> expectedConfLevels = Arrays.asList(
                wrapper.getOWLClassByIdentifier(CIOUtils.LOW_CONF_LEVEL_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.MEDIUM_CONF_LEVEL_ID), 
                wrapper.getOWLClassByIdentifier(CIOUtils.HIGH_CONF_LEVEL_ID));
        assertEquals("Invalid conf. levels", expectedConfLevels, utils.getOrderedConfidenceLevels());
    }
}