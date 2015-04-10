package org.bgee.pipeline.annotations;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.pipeline.TestAncestor;
import org.bgee.pipeline.annotations.SimilarityAnnotation;
import org.bgee.pipeline.annotations.SimilarityAnnotation.CuratorAnnotationBean;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

/**
 * Unit tests for {@link SimilarityAnnotation}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class SimilarityAnnotationTest extends TestAncestor {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(SimilarityAnnotationTest.class.getName());
    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();
    
    /**
     * Default Constructor. 
     */
    public SimilarityAnnotationTest() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
//    /**
//     * Test {@link SimilarityAnnotation#extractAnnotations()}.
//     */
//    @Test
//    public void shouldExtractAnnotations() throws ParseException, 
//        FileNotFoundException, IOException {
//        
//        List<Map<String, Object>> expectedAnnots = new ArrayList<Map<String, Object>>();
//        
//        Map<String, Object> row1 = new HashMap<String, Object>();
//        row1.put(SimilarityAnnotation.ENTITY_COL_NAME, "entity1");
//        row1.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, "entityName1");
//        row1.put(SimilarityAnnotation.QUALIFIER_COL_NAME, null);
//        row1.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:1");
//        row1.put(SimilarityAnnotation.HOM_NAME_COL_NAME, "HOMName1");
//        row1.put(SimilarityAnnotation.REF_COL_NAME, "myRef:1");
//        row1.put(SimilarityAnnotation.REF_TITLE_COL_NAME, "myRefTitle1");
//        row1.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:1");
//        row1.put(SimilarityAnnotation.ECO_NAME_COL_NAME, "ECOName1");
//        row1.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:1");
//        row1.put(SimilarityAnnotation.CONF_NAME_COL_NAME, "CONFName1");
//        row1.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        row1.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, "taxon:1");
//        row1.put(SimilarityAnnotation.SUPPORT_TEXT_COL_NAME, "blabla1");
//        row1.put(SimilarityAnnotation.ASSIGN_COL_NAME, "bgee1");
//        row1.put(SimilarityAnnotation.CURATOR_COL_NAME, "me1");
//        row1.put(SimilarityAnnotation.DATE_COL_NAME, 
//                new SimpleDateFormat("yyyy-MM-dd").parse("1984-01-01"));
//        expectedAnnots.add(row1);
//        
//        Map<String, Object> row2 = new HashMap<String, Object>();
//        row2.put(SimilarityAnnotation.ENTITY_COL_NAME, "entity2");
//        row2.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, null);
//        row2.put(SimilarityAnnotation.QUALIFIER_COL_NAME, "NOT");
//        row2.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:2");
//        row2.put(SimilarityAnnotation.HOM_NAME_COL_NAME, null);
//        row2.put(SimilarityAnnotation.REF_COL_NAME, "myRef:2");
//        row2.put(SimilarityAnnotation.REF_TITLE_COL_NAME, null);
//        row2.put(SimilarityAnnotation.ECO_COL_NAME, null);
//        row2.put(SimilarityAnnotation.ECO_NAME_COL_NAME, null);
//        row2.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:2");
//        row2.put(SimilarityAnnotation.CONF_NAME_COL_NAME, null);
//        row2.put(SimilarityAnnotation.TAXON_COL_NAME, 2);
//        row2.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, null);
//        row2.put(SimilarityAnnotation.SUPPORT_TEXT_COL_NAME, null);
//        row2.put(SimilarityAnnotation.ASSIGN_COL_NAME, "bgee2");
//        row2.put(SimilarityAnnotation.CURATOR_COL_NAME, null);
//        row2.put(SimilarityAnnotation.DATE_COL_NAME, null);
//        expectedAnnots.add(row2);
//        
//        assertEquals(expectedAnnots, new SimilarityAnnotation().extractAnnotations(
//                this.getClass().getResource("/similarity_annotations/similarity2.tsv").getFile(), true));
//    }
//    
//    /**
//     * Test {@link SimilarityAnnotation#generateReleaseData(List, Map, Set, OWLGraphWrapper, 
//            OWLGraphWrapper, OWLGraphWrapper, OWLGraphWrapper, OWLGraphWrapper)}
//     */
//    @Test
//    public void shouldGenerateReleaseData() throws NoSuchMethodException, 
//        SecurityException, IllegalAccessException, IllegalArgumentException, 
//        InvocationTargetException, UnknownOWLOntologyException, OWLOntologyCreationException, 
//        OBOFormatParserException, IOException {
//        SimilarityAnnotation sim = new SimilarityAnnotation();
//        Method method = sim.getClass().getDeclaredMethod("generateReleaseData", 
//                List.class, Map.class, Set.class, OWLGraphWrapper.class, 
//                OWLGraphWrapper.class, OWLGraphWrapper.class, OWLGraphWrapper.class, 
//                OWLGraphWrapper.class);
//        method.setAccessible(true);
//        
//        Map<String, Set<Integer>> taxonConstraints = new HashMap<String, Set<Integer>>();
//        taxonConstraints.put("UBERON:0000001", new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//        taxonConstraints.put("UBERON:0000002", new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//        taxonConstraints.put("UBERON:0000003", new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//        Set<Integer> taxonIds = new HashSet<Integer>(Arrays.asList(1, 2, 3));
//
//        OWLGraphWrapper uberonOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/fake_uberon.obo").getPath()));
//        OWLGraphWrapper taxOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/fake_taxonomy.obo").getPath()));
//        OWLGraphWrapper ecoOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/eco.obo").getPath()));
//        OWLGraphWrapper homOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/homology_ontology.obo").getPath()));
//        OWLGraphWrapper confOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/conf_information.obo").getPath()));
//        
//        
//        List<Map<String, Object>> annotations = new ArrayList<Map<String, Object>>();
//        List<Map<String, Object>> expectedAnnots = new ArrayList<Map<String, Object>>();
//        Map<String, Object> annotSingle = new HashMap<String, Object>();
//        annotSingle.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:0000002");
//        annotSingle.put(SimilarityAnnotation.TAXON_COL_NAME, 2);
//        annotSingle.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:3");
//        annotSingle.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000005");
//        annotSingle.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000005");
//        annotSingle.put(SimilarityAnnotation.REF_COL_NAME, "REF:1 title1");
//        Map<String, Object> expectedAnnotSingle = new HashMap<String, Object>(annotSingle);
//        expectedAnnotSingle.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, "uberon 2");
//        expectedAnnotSingle.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, "taxon 2");
//        expectedAnnotSingle.put(SimilarityAnnotation.ECO_NAME_COL_NAME, "eco 3");
//        expectedAnnotSingle.put(SimilarityAnnotation.REF_COL_NAME, "REF:1");
//        expectedAnnotSingle.put(SimilarityAnnotation.REF_TITLE_COL_NAME, "title1");
//        expectedAnnotSingle.put(SimilarityAnnotation.HOM_NAME_COL_NAME, "parallelism");
//        expectedAnnotSingle.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//                "low confidence assertion from single evidence");
//        expectedAnnotSingle.put(SimilarityAnnotation.LINE_TYPE_COL_NAME, 
//                SimilarityAnnotation.RAW_LINE);
//        
//        Map<String, Object> annot1 = new HashMap<String, Object>();
//        annot1.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:0000001");
//        annot1.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot1.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:1");
//        annot1.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annot1.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000003");
//        annot1.put(SimilarityAnnotation.REF_COL_NAME, "REF:2 title2");
//        Map<String, Object> expectedAnnot1 = new HashMap<String, Object>(annot1);
//        expectedAnnot1.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, "uberon 1");
//        expectedAnnot1.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, "taxon 1");
//        expectedAnnot1.put(SimilarityAnnotation.ECO_NAME_COL_NAME, "eco 1");
//        expectedAnnot1.put(SimilarityAnnotation.HOM_NAME_COL_NAME, "historical homology");
//        expectedAnnot1.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//                "high confidence assertion from single evidence");
//        expectedAnnot1.put(SimilarityAnnotation.REF_COL_NAME, "REF:2");
//        expectedAnnot1.put(SimilarityAnnotation.REF_TITLE_COL_NAME, "title2");
//        expectedAnnot1.put(SimilarityAnnotation.LINE_TYPE_COL_NAME, 
//                SimilarityAnnotation.RAW_LINE);
//        
//        Map<String, Object> annot2 = new HashMap<String, Object>();
//        annot2.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:0000001");
//        annot2.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot2.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:1");
//        annot2.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annot2.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000003");
//        annot2.put(SimilarityAnnotation.REF_COL_NAME, "REF:3 title3");
//        Map<String, Object> expectedAnnot2 = new HashMap<String, Object>(annot2);
//        expectedAnnot2.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, "uberon 1");
//        expectedAnnot2.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, "taxon 1");
//        expectedAnnot2.put(SimilarityAnnotation.ECO_NAME_COL_NAME, "eco 1");
//        expectedAnnot2.put(SimilarityAnnotation.HOM_NAME_COL_NAME, "historical homology");
//        expectedAnnot2.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//                "high confidence assertion from single evidence");
//        expectedAnnot2.put(SimilarityAnnotation.REF_COL_NAME, "REF:3");
//        expectedAnnot2.put(SimilarityAnnotation.REF_TITLE_COL_NAME, "title3");
//        expectedAnnot2.put(SimilarityAnnotation.LINE_TYPE_COL_NAME, 
//                SimilarityAnnotation.RAW_LINE);
//        
//        Map<String, Object> generatedAnnot = new HashMap<String, Object>(expectedAnnot1);
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000017");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000017")));
//        generatedAnnot.put(SimilarityAnnotation.LINE_TYPE_COL_NAME, 
//                SimilarityAnnotation.SUMMARY_LINE);
//        generatedAnnot.put(SimilarityAnnotation.QUALIFIER_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ASSIGN_COL_NAME, 
//                SimilarityAnnotation.BGEE_ASSIGNMENT);
//        generatedAnnot.put(SimilarityAnnotation.REF_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.REF_TITLE_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ECO_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ECO_NAME_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.SUPPORT_TEXT_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.CURATOR_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.DATE_COL_NAME, null);
//        
//        annotations.add(annot2);
//        annotations.add(annot1);
//        annotations.add(annotSingle);
//        expectedAnnots.add(expectedAnnotSingle);
//        expectedAnnots.add(expectedAnnot1);
//        expectedAnnots.add(expectedAnnot2);
//        expectedAnnots.add(generatedAnnot);
//        
//        assertEquals(expectedAnnots, 
//                method.invoke(sim, annotations, taxonConstraints, taxonIds, uberonOntWrapper, 
//                taxOntWrapper, ecoOntWrapper, homOntWrapper, confOntWrapper));
//    }
//    
//    
//    /**
//     * Test {@link SimilarityAnnotation#addGeneratedAnnotations(Collection, 
//     * OWLGraphWrapper, OWLGraphWrapper)}
//     */
//    @Test
//    public void shouldAddGeneratedAnnotations() throws NoSuchMethodException, 
//        SecurityException, IllegalAccessException, IllegalArgumentException, 
//        InvocationTargetException, UnknownOWLOntologyException, OWLOntologyCreationException, 
//        OBOFormatParserException, IOException {
//        SimilarityAnnotation sim = new SimilarityAnnotation();
//        Method method = sim.getClass().getDeclaredMethod("addGeneratedAnnotations", 
//                Collection.class, OWLGraphWrapper.class, OWLGraphWrapper.class);
//        method.setAccessible(true);
//        
//        OWLGraphWrapper ecoOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/eco.obo").getPath()));
//        OWLGraphWrapper confOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/conf_information.obo").getPath()));
//        
//        
//        Collection<Map<String, Object>> annotations = new HashSet<Map<String, Object>>();
//        Map<String, Object> annotSingle = new HashMap<String, Object>();
//        annotSingle.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:2");
//        annotSingle.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annotSingle.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:1");
//        annotSingle.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annotSingle.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000004");
//        annotations.add(annotSingle);
//        Map<String, Object> annot1 = new HashMap<String, Object>();
//        annot1.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:1");
//        annot1.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot1.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:1");
//        annot1.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annot1.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000004");
//        annotations.add(annot1);
//        Map<String, Object> annot2 = new HashMap<String, Object>();
//        annot2.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:1");
//        annot2.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot2.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:2");
//        annot2.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annot2.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000003");
//        annotations.add(annot2);
//        
//        Collection<Map<String, Object>> expectedAnnots = 
//                new HashSet<Map<String, Object>>(annotations);
//        Map<String, Object> generatedAnnot = new HashMap<String, Object>();
//        generatedAnnot.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:1");
//        generatedAnnot.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        generatedAnnot.put(SimilarityAnnotation.ECO_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000017");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000017")));
//        generatedAnnot.put(SimilarityAnnotation.LINE_TYPE_COL_NAME, 
//                SimilarityAnnotation.SUMMARY_LINE);
//        generatedAnnot.put(SimilarityAnnotation.QUALIFIER_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ASSIGN_COL_NAME, 
//                SimilarityAnnotation.BGEE_ASSIGNMENT);
//        generatedAnnot.put(SimilarityAnnotation.REF_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.REF_TITLE_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ECO_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ECO_NAME_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.SUPPORT_TEXT_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.CURATOR_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.DATE_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.ENTITY_NAME_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.HOM_NAME_COL_NAME, null);
//        generatedAnnot.put(SimilarityAnnotation.TAXON_NAME_COL_NAME, null);
//        expectedAnnots.add(generatedAnnot);
//        
//        method.invoke(sim, annotations, ecoOntWrapper, confOntWrapper);
//        assertEquals(expectedAnnots, annotations);
//        
//        
//        annotations = new HashSet<Map<String, Object>>();
//        annotations.add(annotSingle);
//        annotations.add(annot1);
//        annot2.put(SimilarityAnnotation.QUALIFIER_COL_NAME, 
//                SimilarityAnnotation.NEGATE_QUALIFIER);
//        annotations.add(annot2);
//        expectedAnnots = new HashSet<Map<String, Object>>(annotations);
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000020");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000020")));
//        expectedAnnots.add(generatedAnnot);
//        method.invoke(sim, annotations, ecoOntWrapper, confOntWrapper);
//        assertEquals(expectedAnnots, annotations);
//        
//        
//        annotations = new HashSet<Map<String, Object>>();
//        annotations.add(annotSingle);
//        annot1.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:3");
//        annotations.add(annot1);
//        annotations.add(annot2);
//        expectedAnnots = new HashSet<Map<String, Object>>(annotations);
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000010");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000010")));
//        expectedAnnots.add(generatedAnnot);
//        method.invoke(sim, annotations, ecoOntWrapper, confOntWrapper);
//        assertEquals(expectedAnnots, annotations);
//        
//        
//        annotations = new HashSet<Map<String, Object>>();
//        annotations.add(annotSingle);
//        annotations.add(annot1);
//        annot2.put(SimilarityAnnotation.QUALIFIER_COL_NAME, null);
//        annotations.add(annot2);
//        expectedAnnots = new HashSet<Map<String, Object>>(annotations);
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000012");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000012")));
//        expectedAnnots.add(generatedAnnot);
//        method.invoke(sim, annotations, ecoOntWrapper, confOntWrapper);
//        assertEquals(expectedAnnots, annotations);
//        
//        
//        annotations = new HashSet<Map<String, Object>>();
//        annotations.add(annotSingle);
//        annot1.put(SimilarityAnnotation.QUALIFIER_COL_NAME, 
//                SimilarityAnnotation.NEGATE_QUALIFIER);
//        annotations.add(annot1);
//        annot2.put(SimilarityAnnotation.QUALIFIER_COL_NAME, 
//                SimilarityAnnotation.NEGATE_QUALIFIER);
//        annotations.add(annot2);
//        expectedAnnots = new HashSet<Map<String, Object>>(annotations);
//        generatedAnnot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000012");
//        generatedAnnot.put(SimilarityAnnotation.CONF_NAME_COL_NAME, 
//            confOntWrapper.getLabel(confOntWrapper.getOWLClassByIdentifier("CONF:0000012")));
//        generatedAnnot.put(SimilarityAnnotation.QUALIFIER_COL_NAME, 
//                SimilarityAnnotation.NEGATE_QUALIFIER);
//        expectedAnnots.add(generatedAnnot);
//        method.invoke(sim, annotations, ecoOntWrapper, confOntWrapper);
//        assertEquals(expectedAnnots, annotations);
//    }
//    
//    /**
//     * Test {@link SimilarityAnnotation#checkAnnotation(Map, Map, Set, OWLGraphWrapper, 
//     * OWLGraphWrapper, OWLGraphWrapper} and {@link SimilarityAnnotation#verifyErrors()}.
//     */
//    @Test
//    public void shouldCheckAnnotation() throws NoSuchMethodException, 
//        SecurityException, IllegalAccessException, IllegalArgumentException, 
//        InvocationTargetException, OWLOntologyCreationException, 
//        UnknownOWLOntologyException, OBOFormatParserException, IOException {
//        SimilarityAnnotation sim = new SimilarityAnnotation();
//        Method methodCheck = sim.getClass().getDeclaredMethod("checkAnnotation", 
//                Map.class, Map.class, Set.class, OWLGraphWrapper.class, 
//                OWLGraphWrapper.class, OWLGraphWrapper.class);
//        methodCheck.setAccessible(true);
//        Method methodVerify = sim.getClass().getDeclaredMethod("verifyErrors");
//        methodVerify.setAccessible(true);
//        
//        OWLGraphWrapper ecoOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/eco.obo").getPath()));
//        OWLGraphWrapper homOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/homology_ontology.obo").getPath()));
//        OWLGraphWrapper confOntWrapper = new OWLGraphWrapper(OntologyUtils.loadOntology(
//                this.getClass().getResource("/similarity_annotations/conf_information.obo").getPath()));
//        
//        Map<String, Set<Integer>> taxonConstraints = new HashMap<String, Set<Integer>>();
//        taxonConstraints.put("UBERON:1", new HashSet<Integer>(Arrays.asList(1, 2, 3)));
//        taxonConstraints.put("UBERON:2", new HashSet<Integer>(Arrays.asList(1, 3)));
//        Set<Integer> taxonIds = new HashSet<Integer>(Arrays.asList(1, 2, 3));
//        
//        //first, check that when everything is fine, nothing happens
//        Map<String, Object> annot = new HashMap<String, Object>();
//        annot.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:1");
//        annot.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot.put(SimilarityAnnotation.REF_COL_NAME, "PMID:1");
//        //annot.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:0000067");
//        annot.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        annot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000003");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        methodVerify.invoke(sim);
//        
//        //now, check that the verifyErrors method will spot potential problem
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:3");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:1");
//        annot.put(SimilarityAnnotation.TAXON_COL_NAME, 4);
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.ENTITY_COL_NAME, "UBERON:2");
//        annot.put(SimilarityAnnotation.TAXON_COL_NAME, 2);
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.TAXON_COL_NAME, 1);
//        annot.put(SimilarityAnnotation.ECO_COL_NAME, "ECO:100");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.ECO_COL_NAME, null);
//        annot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:100");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.CONF_COL_NAME, "CONF:0000003");
//        annot.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:100");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        try {
//            methodVerify.invoke(sim);
//            //test failed, should have thrown an IllegalArgumenException
//            throw new AssertionError("An exception should have been thrown");
//        } catch (InvocationTargetException e) {
//            //test passed
//            assertEquals(IllegalStateException.class, e.getCause().getClass());
//        }
//        
//        //final check, verify that everything is fine
//        sim = new SimilarityAnnotation();
//        annot.put(SimilarityAnnotation.HOM_COL_NAME, "HOM:0000007");
//        methodCheck.invoke(sim, annot, taxonConstraints, taxonIds, ecoOntWrapper, 
//                homOntWrapper, confOntWrapper);
//        methodVerify.invoke(sim);
//    }
//    
    
    /**
     * Test {@link SimilarityAnnotation.CuratorAnnotationBean#setRefId(String)}, 
     * which extract ref IDs and titles from Strings mixing both.
     */
    @Test
    public void shouldGetRefIdFromRefColValue() {
        CuratorAnnotationBean bean = new CuratorAnnotationBean();
        
        String expectedId = "ID:1";
        String expectedTitle = "my great title";
        bean.setRefId("ID:1 my great title");
        assertEquals(expectedId, bean.getRefId());
        assertEquals(expectedTitle, bean.getRefTitle());

        bean.setRefId("ID:1 \"my great title\"");
        assertEquals(expectedId, bean.getRefId());
        assertEquals(expectedTitle, bean.getRefTitle());

        bean.setRefId(" ID:1 ");
        assertEquals(expectedId, bean.getRefId());
        assertNull(bean.getRefTitle());

        bean.setRefId(" ID:1 regression\"test\"");
        assertEquals(expectedId, bean.getRefId());
        assertEquals("regression\"test", bean.getRefTitle());
        
        bean.setRefId("");
        assertNull(bean.getRefId());
        assertNull(bean.getRefTitle());
        
        bean.setRefId(null);
        assertNull(bean.getRefId());
        assertNull(bean.getRefTitle());
    }
    
    /**
     * Test the method {@link SimilarityAnnotation#extractTaxonIdsToFile(String, String)}
     */
    @Test
    public void shouldExtractTaxonIdsToFile() throws FileNotFoundException, IOException {
        String tempFile = testFolder.newFile("taxonIdsOutput.txt").getPath();
        SimilarityAnnotation.extractTaxonIdsToFile(
                this.getClass().getResource("/similarity_annotations/similarity.tsv").getFile(), 
                tempFile);
        Set<Integer> retrievedIds = new HashSet<Integer>();
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                retrievedIds.add(Integer.parseInt(line));
            }
        }
        assertEquals("Incorrect number of lines in file", 3, lineCount);
        Set<Integer> expectedIds = new HashSet<Integer>(Arrays.asList(7742, 40674, 1294634));
        assertEquals("Incorrect taxon IDs retrieved from generated file", 
                expectedIds, retrievedIds);
    }
    
    /**
     * Test the method {@link SimilarityAnnotation#getAnatEntitiesWithNoTransformationOf(String, String)}
     */
    @Test
    public void shouldGetAnatEntitiesWithNoTransformationOf() 
            throws UnknownOWLOntologyException, IllegalArgumentException, 
            FileNotFoundException, OWLOntologyCreationException, 
            OBOFormatParserException, IOException {
        
        ParserWrapper parserWrapper = new ParserWrapper();
        parserWrapper.setCheckOboDoc(false);
        try (OWLGraphWrapper fakeOntology = new OWLGraphWrapper(parserWrapper.parse(
            this.getClass().getResource("/similarity_annotations/fake_uberon.obo").getFile()))) {
        
            Set<OWLClass> expectedClasses = new HashSet<OWLClass>(
                    Arrays.asList(fakeOntology.getOWLClassByIdentifier("UBERON:0000001")));
            
            assertEquals(
                "Incorrect anatomical entities with no transformation_of relations identified", 
                expectedClasses, SimilarityAnnotation.getAnatEntitiesWithNoTransformationOf(
                this.getClass().getResource("/similarity_annotations/similarity.tsv").getFile(), 
                this.getClass().getResource("/similarity_annotations/fake_uberon.obo").getFile()));
        }
    }
    
    /**
     * Test the method {@link 
     * SimilarityAnnotation#writeAnatEntitiesWithNoTransformationOfToFile(String, String, String)}
     * @throws OBOFormatParserException 
     * @throws OWLOntologyCreationException 
     * @throws IllegalArgumentException 
     * @throws UnknownOWLOntologyException 
     */
    @Test
    public void shouldExtractAnatEntitiesWithNoTransformationOfToFile() 
            throws FileNotFoundException, IOException, UnknownOWLOntologyException, 
            IllegalArgumentException, OWLOntologyCreationException, OBOFormatParserException {
        String tempFile = testFolder.newFile("anatEntitiesNoTransfOfOutput.txt").getPath();
        SimilarityAnnotation.writeAnatEntitiesWithNoTransformationOfToFile(
                this.getClass().getResource("/similarity_annotations/similarity.tsv").getFile(), 
                this.getClass().getResource("/similarity_annotations/fake_uberon.obo").getFile(), 
                tempFile);
        Set<String> retrievedEntities = new HashSet<String>();
        int lineCount = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(tempFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                //skip the first line that is supposed to be a header line
                lineCount++;
                if (lineCount == 1) {
                    continue;
                }
                retrievedEntities.add(line);
            }
        }
        //we should have 2 lines: one header line, and one line with data
        assertEquals("Incorrect number of lines in file", 2, lineCount);
        Set<String> expectedEntities = new HashSet<String>(Arrays.asList("UBERON:0000001\tuberon 1\t" +
        		"develops from: UBERON:0000003 uberon 3"));
        assertEquals("Incorrect anatomical entities IDs retrieved from generated file", 
                expectedEntities, retrievedEntities);
    }
    
}
