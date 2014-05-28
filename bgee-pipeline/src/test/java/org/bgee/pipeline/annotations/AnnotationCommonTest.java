package org.bgee.pipeline.annotations;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.pipeline.OntologyUtils;
import org.bgee.pipeline.TestAncestor;
import org.bgee.pipeline.Utils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;

/**
 * Unit tests for {@link AnnotationCommon}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class AnnotationCommonTest extends TestAncestor {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(AnnotationCommonTest.class.getName());
    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();
    
    /**
     * Default Constructor. 
     */
    public AnnotationCommonTest() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Test {@link AnnotationCommon#parseMultipleEntitiesColumn(String)}
     */
    @Test
    public void shouldParseMultipleAnatEntitiesColumn() {
        List<String> expectedList = Arrays.asList("ID:1", "ID:2", "ID:3");
        assertEquals(expectedList, 
                AnnotationCommon.parseMultipleEntitiesColumn("ID:2|ID:3|ID:1"));
        assertEquals(expectedList, 
                AnnotationCommon.parseMultipleEntitiesColumn("ID:2,ID:3,ID:1"));
        assertEquals(Arrays.asList("ID:1"), 
                AnnotationCommon.parseMultipleEntitiesColumn(" ID:1 "));
    }
    
    /**
     * Test {@link AnnotationCommon#getTermsToColumnValue(List)}
     */
    @Test
    public void shouldGetTermsToColumnValue() {
        String expectedTerm = "ID:2" + AnnotationCommon.DEFAULT_ENTITY_SEPARATOR + 
                "ID:1" + AnnotationCommon.DEFAULT_ENTITY_SEPARATOR + "ID:3";
        assertEquals(expectedTerm, AnnotationCommon.getTermsToColumnValue(
                        Arrays.asList("ID:2", "ID:1", "ID:3")));
    }
    
    /**
     * Test the method {@link AnnotationCommon#extractAnatEntityIdsFromFile(String, boolean)}
     */
    @Test
    public void shouldExtractAnatEntityIdsFromFile() throws FileNotFoundException, IOException {
        //first, test with single anatomical entity annotations
        Set<String> expectedIds = new HashSet<String>(Arrays.asList("ID:1", "ID:3", "ID:100", 
                "ALT_ID:0000006"));
        assertEquals("Incorrect anatomical entity IDs extract from file", expectedIds, 
                AnnotationCommon.extractAnatEntityIdsFromFile(this.getClass().getResource(
                        "/annotations/annotations_extract_ids.tsv").getFile(), 
                true));
        
        //now, test for multiple anatomical entities annotations
        expectedIds = new HashSet<String>(Arrays.asList("UBERON:0000001", 
                "UBERON:0000002", "UBERON:0000003", "UBERON:0000004", "UBERON:0000005"));
        assertEquals("Incorrect anatomical entity IDs extract from file", expectedIds, 
                AnnotationCommon.extractAnatEntityIdsFromFile(this.getClass().getResource(
                        "/annotations/similarity_extract_ids.tsv").getFile(), 
                false));
    }
    
    /**
     * Test the method {@link AnnotationCommon#filterUberonSimplificationInfo(Set, Set, Set, String)}
     * @throws IOException 
     * @throws OBOFormatParserException 
     * @throws OWLOntologyCreationException 
     */
    @Test
    public void shouldFilterUberonSimplificationInfo() throws IOException, 
        OWLOntologyCreationException, OBOFormatParserException {
        
        String infoFileName = "uberon_info_file.tsv";
        String infoFile = this.getClass().getResource(
                "/annotations/" + infoFileName).getFile();
        String singleEntityAnnotFile = this.getClass().getResource(
                "/annotations/annotations_extract_ids.tsv").getFile();
        String multipleEntitiesAnnotFile = this.getClass().getResource(
                "/annotations/similarity_extract_ids.tsv").getFile();
        
        AnnotationCommon.filterUberonSimplificationInfo(
                OntologyUtils.loadOntology(this.getClass().getResource(
                "/annotations/xRefForFiltering.obo").getFile()), 
                new HashSet<String>(Arrays.asList(infoFile)), 
                new HashSet<String>(Arrays.asList(singleEntityAnnotFile)), 
                new HashSet<String>(Arrays.asList(multipleEntitiesAnnotFile)), 
                testFolder.getRoot().toString());
        
        //read filtered info file for the unit test
        try (ICsvMapReader mapReader = 
                new CsvMapReader(new FileReader(
                        Paths.get(testFolder.getRoot().toString(), infoFileName).toFile()), 
                        Utils.TSVCOMMENTED)) {
            
            String[] header = mapReader.getHeader(true);
            String[] expectedHeader = new String[3];
            expectedHeader[0] = "fake column";
            expectedHeader[1] = "Uberon ID";
            expectedHeader[2] = "Uberon name";
            assertArrayEquals("Incorrect header in filtered file", expectedHeader, header);
            
            Map<String, String> row;
            int i = 0;
            while( (row = mapReader.read(header)) != null ) {
                Map<String, String> expectedRow = new HashMap<String, String>();
                //getRowNumber == 1 corresponds to the header line
                if (mapReader.getRowNumber() == 2) {
                    expectedRow.put(header[0], "whatever");
                    expectedRow.put(header[1], "ID:1");
                    expectedRow.put(header[2], "name_1");
                } else if (mapReader.getRowNumber() == 3) {
                    expectedRow.put(header[0], "still_whatever");
                    expectedRow.put(header[1], "ID:100");
                    expectedRow.put(header[2], "name_100");
                } else if (mapReader.getRowNumber() == 4) {
                    expectedRow.put(header[0], null);
                    expectedRow.put(header[1], "UBERON:0000001");
                    expectedRow.put(header[2], null);
                } else if (mapReader.getRowNumber() == 5) {
                    expectedRow.put(header[0], null);
                    expectedRow.put(header[1], "UBERON:0000005");
                    expectedRow.put(header[2], null);
                } else if (mapReader.getRowNumber() == 6) {
                    //this one is kept thanks to its XRef (it is its XRef that is used 
                    //in annotation file, not its main ID)
                    expectedRow.put(header[0], "XRef filtering");
                    expectedRow.put(header[1], "UBERON:0000006");
                    expectedRow.put(header[2], "name_uberon_6");
                } else {
                    throw new AssertionError("Incorrect number of rows in filtered file: " + 
                            mapReader.getRowNumber());
                }
                assertEquals("Incorrect row in filtered info file", expectedRow, row);
                i++;
            }
            assertEquals("Incorrect number of rows in filtered info file", 5, i);
        }
    }

}