package org.bgee.model.expressiondata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.TestAncestor;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.gene.Gene;
import org.bgee.model.species.Species;
import org.junit.Test;

/**
 * Unit tests for {@link ExpressionCall}.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @author  Julien Wollbrett
 * @version Bgee 14, Aug. 2018
 * @since   Bgee 13, June 2016
 */
//FIXME: to reactivate
public class ExpressionCallTest extends TestAncestor {
    private final static Logger log = LogManager.getLogger(ExpressionCallTest.class.getName());
    
    @Override
    protected Logger getLogger() {
        return log;
    } 
    
    /**
     * Test {@link #ExpressionCall#generateMeanRankScoreClustering(Collection, ClusteringMethod, double)}.
     */
    @Test
    public void shouldGenerateMeanRankScoreClustering() {
        
        Species spe1 = new Species(1);
        Gene g1 = new Gene("gene1", spe1);
        AnatEntity anat1 = new AnatEntity("anat1");
        AnatEntity anat2 = new AnatEntity("anat2");
        AnatEntity anat3 = new AnatEntity("anat3");
        DevStage stage1 = new DevStage("stage1");
        DevStage stage2 = new DevStage("stage2");
        DevStage stage3 = new DevStage("stage3");
        Condition cond1 = new Condition(anat1, stage1, spe1);
        Condition cond2 = new Condition(anat1, stage2, spe1);
        Condition cond3 = new Condition(anat1, stage3, spe1);
        Condition cond4 = new Condition(anat2, stage1, spe1);
        Condition cond5 = new Condition(anat2, stage2, spe1);
        Condition cond6 = new Condition(anat2, stage3, spe1);
        Condition cond7 = new Condition(anat3, stage1, spe1);
        Condition cond8 = new Condition(anat3, stage2, spe1);


        //we don't bother to retrieve exact score thresholds etc, we just create calls 
        //very obvious to cluster by Canderra distance
        ExpressionCall c1 = new ExpressionCall(g1, cond1, null, null, null, null, new BigDecimal("1.25"), new BigDecimal("1.25"));
        ExpressionCall c2 = new ExpressionCall(g1, cond2, null, null, null, null, new BigDecimal("1.27"), new BigDecimal("1.27"));
        ExpressionCall c3 = new ExpressionCall(g1, cond3, null, null, null, null, new BigDecimal("10000"), new BigDecimal("10000"));
        ExpressionCall c4 = new ExpressionCall(g1, cond4, null, null, null, null, new BigDecimal("20000"), new BigDecimal("20000"));
        ExpressionCall c5 = new ExpressionCall(g1, cond5, null, null, null, null, new BigDecimal("40000"), new BigDecimal("40000"));
        ExpressionCall c6 = new ExpressionCall(g1, cond6, null, null, null, null, new BigDecimal("40010"), new BigDecimal("40010"));
        ExpressionCall c7 = new ExpressionCall(g1, cond7, null, null, null, null, new BigDecimal("70000"), new BigDecimal("70000"));
        ExpressionCall c8 = new ExpressionCall(g1, cond8, null, null, null, null, new BigDecimal("70010"), new BigDecimal("70010"));
        //we'd like to incorrectly order the calls, but there is a method signature accepting a List...
        Set<ExpressionCall> toCluster = new HashSet<>(Arrays.asList(c5, c1, c7, c3, c6, c8, c4, c2));
        Map<ExpressionCall, Integer> expectedClusters = new HashMap<>();
        expectedClusters.put(c1, 0);
        expectedClusters.put(c2, 0);
        expectedClusters.put(c3, 1);
        expectedClusters.put(c4, 2);
        expectedClusters.put(c5, 3);
        expectedClusters.put(c6, 3);
        expectedClusters.put(c7, 4);
        expectedClusters.put(c8, 4);
        
        for (ExpressionCall.ClusteringMethod method: ExpressionCall.ClusteringMethod.values()) {
            log.debug("Testing clustering method: " + method.name());

            assertEquals("Incorrect clustering of expression scores for method: " + method.name(), 
                    expectedClusters, 
                    ExpressionCall.generateMeanRankScoreClustering(toCluster, method, 
                            (method.isDistanceMeasureAboveOne()? 1.5: 0.1)));
        }
    }
    
    /**
     * Test {@link ExpressionCall.RankComparator}.
     */
    @Test
    public void testRankComparator() {
        
        //These calls and conditions allow a regression test for management of equal ranks
        //cond2 and cond3 will be considered more precise than cond1, and unrelated to each other. 
        //It is important for the test that cond1 would be sorted first alphabetically. 
        Condition cond1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), new Species(1));
        Condition cond2 = new Condition(new AnatEntity("Anat2"), new DevStage("stage1"), new Species(1));
        Condition cond3 = new Condition(new AnatEntity("Anat3"), new DevStage("stage1"), new Species(1));
        //we mock the ConditionGraph used to compare Conditions
        ConditionGraph condGraph = mock(ConditionGraph.class);
        Set<Condition> allConds = new HashSet<>(Arrays.asList(cond1, cond2, cond3));
        when(condGraph.getConditions()).thenReturn(allConds);
        when(condGraph.compare(cond1, cond1)).thenReturn(0);
        when(condGraph.compare(cond2, cond2)).thenReturn(0);
        when(condGraph.compare(cond3, cond3)).thenReturn(0);
        when(condGraph.compare(cond1, cond2)).thenReturn(1);
        when(condGraph.compare(cond2, cond1)).thenReturn(-1);
        when(condGraph.compare(cond1, cond3)).thenReturn(1);
        when(condGraph.compare(cond3, cond1)).thenReturn(-1);
        when(condGraph.compare(cond2, cond3)).thenReturn(0);
        when(condGraph.compare(cond3, cond2)).thenReturn(0);

        ExpressionCall c1 = new ExpressionCall(null, null, null, null, null, null, new BigDecimal("1.25"), new BigDecimal("1.25"));

        //test ordering by geneId
        ExpressionCall c2 = new ExpressionCall(new Gene("ID0", new Species(1)), null, null, null,
            null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        //Test ordering based on Conditions
        //anat2 will be considered more precise than anat1, and as precise as anat3, 
        //the order between anat2 and anat3 should be based on their attributes
        ExpressionCall c3 = new ExpressionCall(new Gene("ID1", new Species(1)), cond2, null, null, 
                null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        //anat3 will be considered more precise than anat1, so we will have different ordering 
        //whether we consider the relations between Conditions, or only attributes of Conditions
        ExpressionCall c4 = new ExpressionCall(new Gene("ID1", new Species(1)), cond3, null, null, 
                null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        ExpressionCall c5 = new ExpressionCall(new Gene("ID1", new Species(1)), cond1, null, null, 
                null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        //test null Conditions last
        ExpressionCall c6 = new ExpressionCall(new Gene("ID1", new Species(1)), null, null, null,
            null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        //test null geneID last
        ExpressionCall c7 = new ExpressionCall(null, null, null, null, null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        //test equal ExpressionCalls
        ExpressionCall c8 = new ExpressionCall(null, null, null, null, null, null, new BigDecimal("1.27"), new BigDecimal("1.25"));
        
        ExpressionCall c9 = new ExpressionCall(null, null, null, null, null, null, new BigDecimal("10000"), new BigDecimal("1.25"));
        ExpressionCall c10 = new ExpressionCall(null, null, null, null, null, null, new BigDecimal("40000"), new BigDecimal("1.25"));
        
        List<ExpressionCall> toSort = Arrays.asList(c10, c2, c1, c6, c8, c4, c9, c7, c3, c5);
        List<ExpressionCall> expectedResult = Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c7, c9, c10);
        Collections.sort(toSort, new ExpressionCall.RankComparator(condGraph));
        
        assertEquals("Incorrect sorting of ExpressionCalls based on their rank and relations", 
                expectedResult, toSort);
        
        //ordering without taking the relations between Conditions into account
        expectedResult = Arrays.asList(c1, c2, c5, c3, c4, c6, c7, c7, c9, c10);
        Collections.sort(toSort, new ExpressionCall.RankComparator());
        assertEquals("Incorrect sorting of ExpressionCalls based on their rank without relations", 
                expectedResult, toSort);
    }
    
    @Test
    public void testBgeeRankDistance() {
        ExpressionCall.BgeeRankDistance measure = new ExpressionCall.BgeeRankDistance();
        
        double score1 = 12;
        double score2 = 33;
        double expected = Math.pow(score2, 1.03)/score1;
        assertEquals("Incorrect BgeeRankDistance computed", expected, 
                measure.compute(new double[]{score1}, new double[]{score2}), 0.0000001);
        
    }
    
    /**
     * Test for {@link ExpressionCall#identifyRedundantCalls(Collection, ConditionGraph)}.
     */
    @Test
    public void shouldIdentifyRedundantCalls() {
      //These calls and conditions allow a regression test for management of equal ranks
        //cond2 and cond3 will be considered more precise than cond1, and unrelated to each other
        Species spe1 = new Species(1);
        Condition cond1 = new Condition(new AnatEntity("Anat1"), new DevStage("stage1"), spe1);
        Condition cond2 = new Condition(new AnatEntity("Anat2"), new DevStage("stage1"), spe1);
        Condition cond3 = new Condition(new AnatEntity("Anat3"), new DevStage("stage1"), spe1);
        //we mock the ConditionGraph used to compare Conditions
        ConditionGraph condGraph = mock(ConditionGraph.class);
        Set<Condition> allConds = new HashSet<>(Arrays.asList(cond1, cond2, cond3));
        when(condGraph.getConditions()).thenReturn(allConds);
        when(condGraph.compare(cond1, cond1)).thenReturn(0);
        when(condGraph.compare(cond2, cond2)).thenReturn(0);
        when(condGraph.compare(cond3, cond3)).thenReturn(0);
        when(condGraph.compare(cond1, cond2)).thenReturn(1);
        when(condGraph.compare(cond2, cond1)).thenReturn(-1);
        when(condGraph.compare(cond1, cond3)).thenReturn(1);
        when(condGraph.compare(cond3, cond1)).thenReturn(-1);
        when(condGraph.compare(cond2, cond3)).thenReturn(0);
        when(condGraph.compare(cond3, cond2)).thenReturn(0);
        when(condGraph.getDescendantConditions(cond1)).thenReturn(new HashSet<>(Arrays.asList(cond2, cond3)));
        when(condGraph.getDescendantConditions(cond2)).thenReturn(new HashSet<>());
        when(condGraph.getDescendantConditions(cond3)).thenReturn(new HashSet<>());
        
        
        //Nothing too complicated with gene ID1, c3 is redundant
        ExpressionCall c1 = new ExpressionCall(new Gene("ID1", new Species(1)), cond3, null, null,
            null, null, new BigDecimal("1.25000"), new BigDecimal("1.25000"));
        ExpressionCall c2 = new ExpressionCall(new Gene("ID1", new Species(1)), cond2, null, null,
            null, null, new BigDecimal("2.0"), new BigDecimal("2.0"));
        ExpressionCall c3 = new ExpressionCall(new Gene("ID1", new Species(1)), cond1, null, null,
            null, null, new BigDecimal("3.00"), new BigDecimal("3.00"));
        //for gene ID2 we test identification with equal ranks and relations between conditions. 
        //c4 is redundant because less precise condition
        ExpressionCall c4 = new ExpressionCall(new Gene("ID2", new Species(1)), cond1, null, null,
            null, null, new BigDecimal("1.250"), new BigDecimal("1.250"));
        ExpressionCall c5 = new ExpressionCall(new Gene("ID2", new Species(1)), cond3, null, null,
            null, null, new BigDecimal("1.25000"), new BigDecimal("1.25000"));
        ExpressionCall c6 = new ExpressionCall(new Gene("ID2", new Species(1)), cond2, null, null,
            null, null, new BigDecimal("1.25"), new BigDecimal("1.25"));
        //for gene ID3 we test identification with equal ranks and relations between conditions. 
        //nothing redundant
        ExpressionCall c7 = new ExpressionCall(new Gene("ID3", new Species(1)), cond1, null, null,
            null, null, new BigDecimal("1"), new BigDecimal("1"));
        ExpressionCall c8 = new ExpressionCall(new Gene("ID3", new Species(1)), cond3, null, null,
            null, null, new BigDecimal("1.25000"), new BigDecimal("1.25000"));
        ExpressionCall c9 = new ExpressionCall(new Gene("ID3", new Species(1)), cond2, null, null,
            null, null, new BigDecimal("1.25"), new BigDecimal("1.25"));
        
        Set<ExpressionCall> withRedundancy = new HashSet<>(Arrays.asList(c1, c2, c3, c4, c5, c6, c7, c8, c9));
        Set<ExpressionCall> expectedRedundants = new HashSet<>(Arrays.asList(c3, c4));
        assertEquals("Incorrect redundant calls identified", 
                expectedRedundants, ExpressionCall.identifyRedundantCalls(withRedundancy, condGraph));
    }
    
    /**
     * Test {@link ExpressionCall#getFormattedGlobalMeanRank()}
     */
    @Test
    public void shouldFormatRankScore() {
        ExpressionCall c = new ExpressionCall(null, null, null, null, null, null,
                new BigDecimal("2"), new BigDecimal("2"));
        assertEquals("Incorrect score formatting", "2.00", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null,
                new BigDecimal("2.23"), new BigDecimal("2.23"));
        assertEquals("Incorrect score formatting", "2.23", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null,
                new BigDecimal("20"), new BigDecimal("20"));
        assertEquals("Incorrect score formatting", "20.0", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null,
                new BigDecimal("20.23"), new BigDecimal("20.23"));
        assertEquals("Incorrect score formatting", "20.2", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("200"), new BigDecimal("200"));
        assertEquals("Incorrect score formatting", "200", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("200.23"), new BigDecimal("200.23"));
        assertEquals("Incorrect score formatting", "200", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("1000"), new BigDecimal("1000"));
        assertEquals("Incorrect score formatting", "1.00e3", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("2000"), new BigDecimal("2000"));
        assertEquals("Incorrect score formatting", "2.00e3", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("2000.23"), new BigDecimal("2000.23"));
        assertEquals("Incorrect score formatting", "2.00e3", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("20000"), new BigDecimal("20000"));
        assertEquals("Incorrect score formatting", "2.00e4", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("20000.23"), new BigDecimal("20000.23"));
        assertEquals("Incorrect score formatting", "2.00e4", c.getFormattedGlobalMeanRank());
        c = new ExpressionCall(null, null, null, null, null, null, 
                new BigDecimal("20100.23"), new BigDecimal("20100.23"));
        assertEquals("Incorrect score formatting", "2.01e4", c.getFormattedGlobalMeanRank());
    }
}
