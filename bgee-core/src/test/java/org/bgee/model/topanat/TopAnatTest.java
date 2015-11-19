package org.bgee.model.topanat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;
import org.bgee.model.BgeeProperties;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.expressiondata.Condition;
import org.bgee.model.expressiondata.baseelements.CallType;
import org.bgee.model.function.PentaFunction;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneService;
import org.bgee.model.topanat.TopAnatResults.TopAnatResultRow;
import org.bgee.model.topanat.exception.MissingParameterException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit tests for {@link TopAnatController}.
 * 
 * @author Mathieu Seppey
 * @version Bgee 13, June 2015
 * @since Bgee 13
 */
public class TopAnatTest {

    private TopAnatController topAnatController;

    private BgeeProperties props;

    private String hashKey = "24d69e8007c9e89000ec54f0f84ce5e938e37144";

    /**
     * This method inits every mock and real objects needed to run the tests
     * 
     * @throws IllegalStateException
     * @throws MissingParameterException
     */
    @Before
    public void initTest() throws IllegalStateException, MissingParameterException{

        // init the BgeeProperties
        System.setProperty(BgeeProperties.TOP_ANAT_RESULTS_WRITING_DIRECTORY_KEY, 
                System.getProperty("java.io.tmpdir"));
        System.setProperty(BgeeProperties.TOP_ANAT_R_WORKING_DIRECTORY_KEY, 
                System.getProperty("java.io.tmpdir"));
        this.props = BgeeProperties.getBgeeProperties();

        // Define the params for the analysis
        TopAnatParams.Builder topAnatParamsBuilder = new TopAnatParams.Builder(
                new HashSet<String>(Arrays.asList("G1","G2")),
                new HashSet<String>(Arrays.asList("G1","G2","G3","G4")),"999",
                CallType.Expression.EXPRESSED);

        topAnatParamsBuilder.fdrThreshold(1000d); // extreme value to produce results easily with false data
        topAnatParamsBuilder.pvalueThreshold(1d);
        TopAnatParams topAnatParams = topAnatParamsBuilder.build();
        // End of the params definition;

        // Mock objects and methods
        Gene mockGene1 = mock(Gene.class);
        Gene mockGene2 = mock(Gene.class);
        Gene mockGene3 = mock(Gene.class);
        Gene mockGene4 = mock(Gene.class);
        Gene mockGene5 = mock(Gene.class);
        ExpressionCall mockExpressionCall1 = mock(ExpressionCall.class);
        ExpressionCall mockExpressionCall2 = mock(ExpressionCall.class);
        ExpressionCall mockExpressionCall3 = mock(ExpressionCall.class);
        ExpressionCall mockExpressionCall4 = mock(ExpressionCall.class);
        ExpressionCall mockExpressionCall5 = mock(ExpressionCall.class);
        ServiceFactory mockServiceFactory = mock(ServiceFactory.class);
        GeneService mockGeneService = mock(GeneService.class);
        CallService mockCallService = mock(CallService.class);
        Condition mockCondition = mock(Condition.class);
        AnatEntityService mockAnatEntityService = mock(AnatEntityService.class);
        AnatEntity mockAnatEntity = mock(AnatEntity.class);
        Stream<AnatEntity> anatEntities = Arrays.asList(mockAnatEntity,mockAnatEntity,
                mockAnatEntity,mockAnatEntity,mockAnatEntity).stream();
        Map<String,Set<String>> anatEntitiesRelationships =
                new HashMap<String,Set<String>>();
        anatEntitiesRelationships.put("A1", new HashSet<String>(Arrays.asList("A2","A3")));
        anatEntitiesRelationships.put("A3", new HashSet<String>(Arrays.asList("A4")));        
        when(mockAnatEntityService.getAnatEntities(any(String.class))).thenReturn(anatEntities);
        when(mockAnatEntityService.getAnatEntitiesRelationships(any(String.class)))
        .thenReturn(anatEntitiesRelationships);
        when(mockGene1.getId()).thenReturn("G1");
        when(mockGene2.getId()).thenReturn("G2");
        when(mockGene3.getId()).thenReturn("G3");
        when(mockGene4.getId()).thenReturn("G4");
        when(mockGene5.getId()).thenReturn("G5");
        when(mockAnatEntity.getId()).thenReturn("A1").thenReturn("A2")
        .thenReturn("A3").thenReturn("A4").thenReturn("A5");
        when(mockAnatEntity.getName()).thenReturn("body").thenReturn("head")
        .thenReturn("hand").thenReturn("eye").thenReturn("finger");
        when(mockCallService.loadCalls(anyString(), any(Set.class))) // TODO be more specific here
        .thenReturn(Stream.of(mockExpressionCall1, mockExpressionCall2, mockExpressionCall3,
                mockExpressionCall4,mockExpressionCall5));    
        when(mockServiceFactory.getGeneService()).thenReturn(mockGeneService);
        when(mockServiceFactory.getCallService()).thenReturn(mockCallService);
        when(mockServiceFactory.getAnatEntityService()).thenReturn(mockAnatEntityService);
        when(mockGeneService
                .loadGenesByIdsAndSpeciesIds(any(),any())) // TODO be more specific here
        .thenReturn(Arrays.asList(mockGene1,mockGene2,mockGene3,mockGene4,mockGene5));
        when(mockExpressionCall1.getCondition()).thenReturn(mockCondition);
        when(mockExpressionCall2.getCondition()).thenReturn(mockCondition);
        when(mockExpressionCall3.getCondition()).thenReturn(mockCondition);
        when(mockExpressionCall4.getCondition()).thenReturn(mockCondition);
        when(mockExpressionCall5.getCondition()).thenReturn(mockCondition);
        when(mockExpressionCall1.getGeneId()).thenReturn("G1");
        when(mockExpressionCall2.getGeneId()).thenReturn("G2");
        when(mockExpressionCall3.getGeneId()).thenReturn("G3");
        when(mockExpressionCall4.getGeneId()).thenReturn("G4");
        when(mockExpressionCall5.getGeneId()).thenReturn("G5");
        when(mockAnatEntityService.getAnatEntities(any(String.class))).thenReturn(anatEntities);
        when(mockCondition.getAnatEntityId())
        .thenReturn("A1").thenReturn("A2").thenReturn("A3").thenReturn("A4").thenReturn("A4");      


        // non-mock TopAnatRManager, because without the R code doing its part, the part that
        // moves the tmp file crash, the part that reads the result crash...anything related with
        // the file system crashes.
        // XXX not sure if we should/can mock this part. 
        TopAnatRManager topAnatRManager = new TopAnatRManager(props, topAnatParams);

        // Supplier for TopAnatAnalysis
        PentaFunction<TopAnatParams, BgeeProperties, ServiceFactory, TopAnatRManager,
        TopAnatController, TopAnatAnalysis> 
        topAnatAnalysisSupplier;
        topAnatAnalysisSupplier = (p1, p2, p3, p4, p5) -> 
        new TopAnatAnalysis(
                topAnatParams,
                BgeeProperties.getBgeeProperties(),
                mockServiceFactory,
                topAnatRManager,
                this.topAnatController);

        // The TopAnatController that will be tested
        this.topAnatController = new TopAnatController(Arrays.asList(
                topAnatParams),
                BgeeProperties.getBgeeProperties(),
                mockServiceFactory,
                topAnatAnalysisSupplier);
    }

    /**
     * Check that all files are written on the disk
     * @throws IOException 
     */
    @Test
    public void testFiles() throws IOException{

        this.topAnatController.proceedToTopAnatAnalyses().forEach(System.out::println);

        // Check that all files have been written with a correct content
        assertEquals("1ed02eea41d11997727d90da27875ae81bcc2c4e",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_AnatEntitiesNames_999.tsv"
                                ))));

        assertEquals("17703d9e6c20e89f4bb2fc63192651db6258d45e",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_AnatEntitiesRelationships_999.tsv"
                                ))));

        assertEquals("d537d769d2185fcb67b19b2c0dbe0495e2abe014",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_functions.R"
                                ))));

        assertEquals("51b7d3a7c6c7b75f59b559753e6407062f02fa68",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_RScript_"+this.hashKey+".R"
                                ))));

        assertEquals("33916dc23856ae306e90f13bc7bb4056fa98ca3c",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_Params_"+this.hashKey+".txt"
                                ))));

        assertEquals("d9862c3a086ef3465eabc0a30d791986f85c9dfa",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_"+this.hashKey+".tsv"
                                ))));

        assertEquals("549fbc7c643b5e06ebecb4099777988858e103ca",
                DigestUtils.sha1Hex(
                        Files.readAllBytes(Paths.get(
                                this.props.getTopAnatResultsWritingDirectory()
                                +"/topAnat_GeneToAnatEntities_"+this.hashKey+".tsv"
                                ))));

        // For the following files, the hash constantly change. Just check it is there
        assertTrue(Files.exists(Paths.get(
                this.props.getTopAnatResultsWritingDirectory()
                +"/topAnat_PDF_"+this.hashKey+".pdf"
                )));

        assertTrue(Files.exists(Paths.get(
                this.props.getTopAnatResultsWritingDirectory()
                +"/topAnat_"+this.hashKey+".R_console"
                )));

        assertTrue(Files.exists(Paths.get(
                this.props.getTopAnatResultsWritingDirectory()
                +"/topAnat_"+this.hashKey+".zip"
                )));

    }

    /**
     * Test the behavior of areAnalysesDone()
     * @throws IOException 
     */
    @Test
    public void testAreAnalysesDone(){
        assertFalse(this.topAnatController.areAnalysesDone());
        this.topAnatController.proceedToTopAnatAnalyses().forEach(System.out::println);
        assertTrue(this.topAnatController.areAnalysesDone());  
    }

    /**
     * Test that the results are correctly parsed from the file on the disk
     * @throws IOException 
     */
    @Test
    public void testParseResult() throws IOException{
        // Run the analysis to ensure that all needed files are created
        this.topAnatController.proceedToTopAnatAnalyses().forEach(System.out::println);
        // Delete the result file and replace it with a new one
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_"+this.hashKey+".tsv").delete();  
        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(this.props.getTopAnatResultsWritingDirectory()
                        +"topAnat_"+this.hashKey+".tsv")))) {
            out.println("OrganId\tOrganName\tAnnotated\tSignificant\tExpected\tfoldEnrichment\tp\tfdr");
            out.println("A1\tbody\t5\t7\t9\t11\t1\t1");
            out.println("A2\thead\t6\t8\t10\t12\t0.0001\t5E-03");
        }
        // Rerun the analysis to access the results
        // And assess that the results correspond to the fake file just written.
        // It also proves that the new analysis did not replace an existing one
        List<TopAnatResultRow> results = 
                this.topAnatController.proceedToTopAnatAnalyses().findFirst().get().getRows();
        TopAnatResultRow first  = results.get(0);
        TopAnatResultRow second  = results.get(1);
        assertEquals("A1",first.getAnatEntitiesId());
        assertEquals("A2",second.getAnatEntitiesId());
        assertEquals("body",first.getAnatEntitiesName());
        assertEquals("head",second.getAnatEntitiesName());
        assertEquals("5.0",String.valueOf(first.getAnnotated()));
        assertEquals("6.0",String.valueOf(second.getAnnotated()));
        assertEquals("7.0",String.valueOf(first.getSignificant()));
        assertEquals("8.0",String.valueOf(second.getSignificant()));
        assertEquals("9.0",String.valueOf(first.getExpected()));
        assertEquals("10.0",String.valueOf(second.getExpected()));
        assertEquals("11.0",String.valueOf(first.getEnrich()));
        assertEquals("12.0",String.valueOf(second.getEnrich()));
        assertEquals("1.0",String.valueOf(first.getPval()));
        assertEquals("1.0E-4",String.valueOf(second.getPval()));
        assertEquals("1.0",String.valueOf(first.getFdr()));
        assertEquals("0.005",String.valueOf(second.getFdr()));
        
    }


    /**
     * Clean all files written on the disk.
     * Restore the default system properties
     */
    @After
    public void clean(){
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"/topAnat_AnatEntitiesNames_999.tsv").delete();  
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"/topAnat_AnatEntitiesRelationships_999.tsv").delete();
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_functions.R").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_RScript_"+this.hashKey+".R").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_Params_"+this.hashKey+".txt").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_"+this.hashKey+".tsv").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_PDF_"+this.hashKey+".pdf").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_GeneToAnatEntities_"+this.hashKey+".tsv").delete();        
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_"+this.hashKey+".zip").delete();
        new File(this.props.getTopAnatResultsWritingDirectory()
                +"topAnat_"+this.hashKey+".R_console").delete(); 
        System.clearProperty(BgeeProperties.TOP_ANAT_RESULTS_WRITING_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.TOP_ANAT_R_WORKING_DIRECTORY_KEY);

    }

}
