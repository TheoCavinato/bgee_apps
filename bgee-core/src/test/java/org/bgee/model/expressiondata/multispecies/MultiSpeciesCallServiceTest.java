package org.bgee.model.expressiondata.multispecies;

import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;
import org.bgee.model.TestAncestor;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.multispemapping.AnatEntitySimilarity;
import org.bgee.model.anatdev.multispemapping.AnatEntitySimilarityService;
import org.bgee.model.anatdev.multispemapping.AnatEntitySimilarityTaxonSummary;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.CallFilter.ExpressionCallFilter;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.expressiondata.Condition;
import org.bgee.model.expressiondata.ConditionFilter;
import org.bgee.model.expressiondata.baseelements.SummaryCallType;
import org.bgee.model.expressiondata.baseelements.SummaryCallType.ExpressionSummary;
import org.bgee.model.expressiondata.baseelements.SummaryQuality;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneBioType;
import org.bgee.model.gene.GeneFilter;
import org.bgee.model.species.Species;
import org.bgee.model.species.Taxon;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class holds the unit tests for the {@code MultiSpeciesCallService} class.
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Mar. 2019
 * @since   Bgee 14, Nov. 2016
 */
public class MultiSpeciesCallServiceTest extends TestAncestor {
    
    /**
     * Test the method
     * {@link MultiSpeciesCallService#loadSimilarityExpressionCalls(int, Collection, ConditionFilter, boolean)}.
     */
    @Test
    public void shouldLoadMultiSpeciesCalls() {
        // Initialize mocks
        ServiceFactory serviceFactory = mock(ServiceFactory.class);
        CallService callService = mock(CallService.class);
        when(serviceFactory.getCallService()).thenReturn(callService);
        AnatEntitySimilarityService aeSimService = mock(AnatEntitySimilarityService.class);
        when(serviceFactory.getAnatEntitySimilarityService()).thenReturn(aeSimService);

        int taxonId = 10;
        Taxon taxon = new Taxon(taxonId, null, null, "scientificName", 1, true);
        
        int speciesId1 = 1;
        int speciesId2 = 2;
        Species species1 = new Species(speciesId1);
        Species species2 = new Species(speciesId2);

        String anatEntityId1a = "anatEntityId1a";
        String anatEntityId1b = "anatEntityId1b";
        String anatEntityId2a = "anatEntityId2a";
        AnatEntity anatEntity1a = new AnatEntity(anatEntityId1a);
        AnatEntity anatEntity1b = new AnatEntity(anatEntityId1b);
        AnatEntity anatEntity2a = new AnatEntity(anatEntityId2a);
        
        Gene gene1 = new Gene("gene1a", species1, new GeneBioType("biotype1"));
        Gene gene2a = new Gene("gene2a", species2, new GeneBioType("biotype1"));
        Gene gene2b = new Gene("gene2b", species2, new GeneBioType("biotype1"));
        Set<String> sp1GenesIds = Collections.singleton(gene1.getEnsemblGeneId());
        GeneFilter geneFilter1 = new GeneFilter(speciesId1, sp1GenesIds);
        Set<GeneFilter> geneFilters = Collections.singleton(geneFilter1);

        // aeSim1
        ExpressionCall call1 = new ExpressionCall(gene1, new Condition(anatEntity1a,  null, species1),
                null, ExpressionSummary.EXPRESSED, null, null, null);
        ExpressionCall call2 = new ExpressionCall(gene1, new Condition(anatEntity2a,  null, species1),
                null, ExpressionSummary.NOT_EXPRESSED, null, null, null);
        ExpressionCall call4 = new ExpressionCall(gene2a, new Condition(anatEntity2a, null, species2),
                null, ExpressionSummary.EXPRESSED, null, null, null);
        // aeSim2
        ExpressionCall call3 = new ExpressionCall(gene1, new Condition(anatEntity1b,  null, species1),
                null, ExpressionSummary.EXPRESSED, null, null, null);
        ExpressionCall call5 = new ExpressionCall(gene2b, new Condition(anatEntity1b, null, species2),
                null, ExpressionSummary.NOT_EXPRESSED, null, null, null);

        Set<AnatEntitySimilarityTaxonSummary> aeSimTaxonSummaries = Collections.singleton(
                new AnatEntitySimilarityTaxonSummary(taxon, true, true));
        AnatEntitySimilarity aeSim1 = new AnatEntitySimilarity(
                Arrays.asList(anatEntity1a, anatEntity2a), null, taxon, aeSimTaxonSummaries);
        AnatEntitySimilarity aeSim2 = new AnatEntitySimilarity(
                Arrays.asList(anatEntity1b), null, taxon, aeSimTaxonSummaries);
        
        boolean onlyTrusted = true;
        when(aeSimService.loadPositiveAnatEntitySimilarities(taxonId, onlyTrusted))
                .thenReturn(new HashSet<>(Arrays.asList(aeSim1, aeSim2)));

        ConditionFilter providedCondFilter = new ConditionFilter(new HashSet<>(
                Arrays.asList(anatEntityId1a, anatEntityId1b)), null);
        ConditionFilter usedCondFilter = new ConditionFilter(new HashSet<>(
                Arrays.asList(anatEntityId1a, anatEntityId1b, anatEntityId2a)), null);

        Map<SummaryCallType.ExpressionSummary, SummaryQuality> qualityFilter =
                new HashMap<>();
        qualityFilter.put(SummaryCallType.ExpressionSummary.EXPRESSED, SummaryQuality.BRONZE);
        qualityFilter.put(SummaryCallType.ExpressionSummary.NOT_EXPRESSED, SummaryQuality.BRONZE);

        ExpressionCallFilter usedCallFilter = new ExpressionCallFilter(
                qualityFilter, geneFilters, Collections.singleton(usedCondFilter), null, null, null, null);

        LinkedHashMap<CallService.OrderingAttribute, Service.Direction> serviceOrdering =
                new LinkedHashMap<>();
        serviceOrdering.put(CallService.OrderingAttribute.GENE_ID, Service.Direction.ASC);

        when(callService.loadExpressionCalls(usedCallFilter,
                EnumSet.of(CallService.Attribute.GENE, CallService.Attribute.ANAT_ENTITY_ID,
                        CallService.Attribute.CALL_TYPE),
                serviceOrdering))
                .thenReturn(Stream.of(call1, call2, call3, call4, call5));

        MultiSpeciesCallService service = new MultiSpeciesCallService(serviceFactory);

        Stream<SimilarityExpressionCall> similarityExpressionCallStream = 
                service.loadSimilarityExpressionCalls(taxonId, Collections.singleton(geneFilter1),
                        providedCondFilter, onlyTrusted);

        Set<SimilarityExpressionCall> simCalls = similarityExpressionCallStream.collect(Collectors.toSet());

        assertNotNull(simCalls);
        assertEquals(4, simCalls.size());

        for (SimilarityExpressionCall simCall : simCalls) {
            if (simCall.getGene().equals(gene1)) {
                if (simCall.getMultiSpeciesCondition().getAnatSimilarity().getAllAnatEntities().contains(anatEntity1a)) {
                    assertEquals(new SimilarityExpressionCall(
                                    gene1, new MultiSpeciesCondition(aeSim1, null),
                                    Arrays.asList(call1, call2), ExpressionSummary.EXPRESSED),
                            simCall);
                } else {
                    assertEquals(new SimilarityExpressionCall(
                                    gene1, new MultiSpeciesCondition(aeSim2, null),
                                    Arrays.asList(call3), ExpressionSummary.EXPRESSED),
                            simCall);
                }
            } else if (simCall.getGene().equals(gene2a)) {
                assertEquals(new SimilarityExpressionCall(
                                gene2a, new MultiSpeciesCondition(aeSim1, null),
                                Arrays.asList(call4), ExpressionSummary.EXPRESSED),
                        simCall);
            } else {
                assertEquals(new SimilarityExpressionCall(
                                gene2b, new MultiSpeciesCondition(aeSim2, null),
                                Arrays.asList(call5), ExpressionSummary.NOT_EXPRESSED),
                        simCall);
            }
        }

//    /**
//     * Test the method {@link MultiSpeciesCallService#loadMultiSpeciesExpressionCalls(Gene, Collection)}.
//     */
//    @Test
//    @Ignore("Test ignored until it is re-implemented following many modifications.")
//    public void shouldLoadMultiSpeciesExpressionCalls() {
//        // Initialize mocks
//        ServiceFactory serviceFactory = mock(ServiceFactory.class);
//        // OntologyService to get ordered relevant taxa from the gene taxa
//        OntologyService ontService = mock(OntologyService.class);
//        when(serviceFactory.getOntologyService()).thenReturn(ontService);
//        // GeneService to retrieve homologous organ groups
//        GeneService geneService = mock(GeneService.class);
//        when(serviceFactory.getGeneService()).thenReturn(geneService);
//        // AnatEntityService to retrieve anat. entity similarities
//        AnatEntityService anatEntityService = mock(AnatEntityService.class);
//        when(serviceFactory.getAnatEntityService()).thenReturn(anatEntityService);
//        // DevStageService to retrieve dev. stage similarities
//        DevStageService devStageService = mock(DevStageService.class);
//        when(serviceFactory.getDevStageService()).thenReturn(devStageService);
//        // CallService to retrieve propagated and reconciled calls
//        CallService callService = mock(CallService.class);
//        when(serviceFactory.getCallService()).thenReturn(callService);
//        
//        String taxId1 = "taxId1";
//        Taxon taxon1 = new Taxon(taxId1);
//        String taxId2 = "taxId2";
//        Taxon taxon2 = new Taxon(taxId2);
//        String taxId10 = "taxId10";
//        Taxon taxon10 = new Taxon(taxId10);
//        String taxId100 = "taxId100";
//        Taxon taxon100 = new Taxon(taxId100);
//        // tax100--
//        // |        \
//        // tax10     \
//        // |     \    \
//        // tax1  tax2  tax3
//        // |     |     |
//        // sp1   sp2   sp3
//
//        String spId1 = "spId1";
//        String spId2 = "spId2";
//        String spId3 = "spId3";
//        Set<String> speciesIds = new HashSet<String>(Arrays.asList(spId1, spId2, spId3)); 
//
//        @SuppressWarnings("unchecked")
//        MultiSpeciesOntology<Taxon> taxonOnt = mock(MultiSpeciesOntology.class);
//        when(ontService.getTaxonOntology()).thenReturn(taxonOnt);
//        when(taxonOnt.getElements()).thenReturn(new HashSet<>(Arrays.asList(taxon1, taxon2)));
//        when(taxonOnt.getElement(taxId1)).thenReturn(taxon1);
//        when(taxonOnt.getOrderedAncestors(taxon1)).thenReturn(Arrays.asList(taxon10, taxon100));
//        
//        Map<String, Set<String>> omaToGeneIds1 = new HashMap<>();
//        omaToGeneIds1.put("oma1", new HashSet<>(Arrays.asList("sp1g1", "sp2g1")));
//        when(geneService.getOrthologs(taxId10, speciesIds)).thenReturn(omaToGeneIds1);
//        Map<String, Set<String>> omaToGeneIds2 = new HashMap<>();
//        omaToGeneIds2.put("oma2", new HashSet<>(Arrays.asList("sp1g1", "sp2g1", "sp3g1")));
//        when(geneService.getOrthologs(taxId100, speciesIds)).thenReturn(omaToGeneIds2);
//
//        AnatEntitySimilarity aeSim1 = new AnatEntitySimilarity("aeSim1", new HashSet<>(Arrays.asList("aeId1", "aeId2")));
//        AnatEntitySimilarity aeSim2 = new AnatEntitySimilarity("aeSimA", new HashSet<>(Arrays.asList("aeId3", "aeId4")));
//        when(anatEntityService.loadAnatEntitySimilarities(taxId10, speciesIds, true))
//            .thenReturn(new HashSet<>(Arrays.asList(aeSim1)));
//        when(anatEntityService.loadAnatEntitySimilarities(taxId100, speciesIds, true))
//            .thenReturn(new HashSet<>(Arrays.asList(aeSim1, aeSim2)));
//
//        DevStageSimilarity dsSim1 = new DevStageSimilarity("dsSim1", new HashSet<>(Arrays.asList("dsId1", "dsId2")));
//        DevStageSimilarity dsSim2 = new DevStageSimilarity("dsSim2", new HashSet<>(Arrays.asList("dsId3", "dsId4")));
//        DevStageSimilarity dsSim2b = new DevStageSimilarity("dsSim2b", new HashSet<>(Arrays.asList("dsId3", "dsId4", "dsId5")));
//        when(devStageService.loadDevStageSimilarities(taxId10, speciesIds))
//            .thenReturn(new HashSet<>(Arrays.asList(dsSim1, dsSim2)));
//        when(devStageService.loadDevStageSimilarities(taxId100, speciesIds))
//            .thenReturn(new HashSet<>(Arrays.asList(dsSim1, dsSim2b)));
//
//        LinkedHashMap<CallService.OrderingAttribute, Direction> orderAttrs = new LinkedHashMap<>();
//        orderAttrs.put(CallService.OrderingAttribute.GENE_ID, Direction.ASC);
//        
//        Set<String> orthologousGeneIds10 = new HashSet<>();
//        orthologousGeneIds10.addAll(omaToGeneIds1.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
//        Set<String> anatEntityIds10 = new HashSet<>();
//        anatEntityIds10.addAll(aeSim1.getAnatEntityIds());
//        Set<String> devStageIds10 = new HashSet<>();
//        devStageIds10.addAll(dsSim1.getDevStageIds());
//        devStageIds10.addAll(dsSim2.getDevStageIds());
//
//        ExpressionCallFilter callFilter10 = new ExpressionCallFilter(
//                new GeneFilter(orthologousGeneIds10), 
//                new HashSet<>(Arrays.asList(new ConditionFilter(anatEntityIds10, devStageIds10))),
//                ExpressionSummary.EXPRESSED);
//
//        Set<String> orthologousGeneIds100 = new HashSet<>();
//        orthologousGeneIds100.addAll(omaToGeneIds1.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
//        orthologousGeneIds100.addAll(omaToGeneIds2.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
//        Set<String> anatEntityIds100 = new HashSet<>();
//        anatEntityIds100.addAll(aeSim1.getAnatEntityIds());
//        anatEntityIds100.addAll(aeSim2.getAnatEntityIds());
//        Set<String> devStageIds100 = new HashSet<>();
//        devStageIds100.addAll(dsSim1.getDevStageIds());
//        devStageIds100.addAll(dsSim2b.getDevStageIds());
//        ExpressionCallFilter callFilter100 = new ExpressionCallFilter(
//                new GeneFilter(orthologousGeneIds100), 
//                new HashSet<>(Arrays.asList(new ConditionFilter(anatEntityIds100, devStageIds100))),
//                ExpressionSummary.EXPRESSED);
//
//        // aeSim1 - dsSim1
//        ExpressionCall call1 = new ExpressionCall("sp1g1", new Condition("aeId1", "dsId1", spId1),
//                false, ExpressionSummary.EXPRESSED, SummaryQuality.GOLD, null,
////                Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DataType.AFFYMETRIX)),
//                null);
//        // aeSim1 - dsSim1
//        ExpressionCall call2 = new ExpressionCall("sp2g1", new Condition("aeId2", "dsId2", spId2),
//            true, ExpressionSummary.NOT_EXPRESSED, SummaryQuality.GOLD, null,
////            Arrays.asList(new ExpressionCallData(Expression.NOT_EXPRESSED, DataType.RNA_SEQ)),
//            null);
//        // aeSim1 - dsSim2
//        ExpressionCall call3 = new ExpressionCall("sp1g1", new Condition("aeId1", "dsId4", spId1),
//                false,
//                ExpressionSummary.EXPRESSED, SummaryQuality.GOLD, null,
////                Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DataType.AFFYMETRIX)),
//                null);
//        // aeSim2 - dsSim2 & dsSim2b
//        ExpressionCall call4 = new ExpressionCall("sp1g1", new Condition("aeId4", "dsId3", spId1),
//            true, ExpressionSummary.EXPRESSED, SummaryQuality.GOLD, null,
////            Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DataType.RNA_SEQ)),
//            null);
//        // aeSim2 - dsSim2 & dsSim2b
//        ExpressionCall call5 = new ExpressionCall("sp2g1", new Condition("aeId3", "dsId4", spId2),
//            true, ExpressionSummary.EXPRESSED, SummaryQuality.SILVER, null,
////            Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DataType.IN_SITU)),
//            null);
//        // aeSim2 - dsSim2b
//        ExpressionCall call6 = new ExpressionCall("sp3g1", new Condition("aeId4", "dsId5", spId3),
//            true, ExpressionSummary.EXPRESSED, SummaryQuality.SILVER, null,
////            Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DataType.IN_SITU)),
//            null);
//        
//        when(callService.loadExpressionCalls(spId1, callFilter10, null, orderAttrs))
//            .thenReturn(Arrays.asList(call1, call3).stream());
//        when(callService.loadExpressionCalls(spId2, callFilter10, null, orderAttrs))
//            .thenReturn(Arrays.asList(call2).stream());
//        when(callService.loadExpressionCalls(spId3, callFilter10, null, orderAttrs))
//            .thenReturn(Stream.empty());
//
//        when(callService.loadExpressionCalls(spId1, callFilter100, null, orderAttrs))
//            .thenReturn(Arrays.asList(call1, call3, call4).stream());
//        when(callService.loadExpressionCalls(spId2, callFilter100, null, orderAttrs))
//            .thenReturn(Arrays.asList(call2, call5).stream());
//        when(callService.loadExpressionCalls(spId3, callFilter100, null, orderAttrs))
//            .thenReturn(Arrays.asList(call6).stream());
//        
//        MultiSpeciesCallService analysisService = new MultiSpeciesCallService(serviceFactory);
//        Gene gene = new Gene("sp1g1", spId1, null, null, new Species(spId1, null, null, null, null, null, taxId1, null, null));
//        Map<String, Set<MultiSpeciesCall<ExpressionCall>>> actual = 
//            analysisService.loadMultiSpeciesExpressionCalls(gene, speciesIds);
//
//        LinkedHashMap<String, Set<MultiSpeciesCall<ExpressionCall>>> expected = new LinkedHashMap<>();
//        
//        Set<MultiSpeciesCall<ExpressionCall>> multiSpCalls1 = new HashSet<>();
//        multiSpCalls1.add(new MultiSpeciesCall<>(aeSim1, dsSim1, taxId10, "oma1", orthologousGeneIds10,
//            new HashSet<>(Arrays.asList(call1, call2)), null, serviceFactory));
//        multiSpCalls1.add(new MultiSpeciesCall<>(aeSim1, dsSim2, taxId10, "oma1", orthologousGeneIds10,
//            new HashSet<>(Arrays.asList(call3)), null, serviceFactory));
//        expected.put(taxId10, multiSpCalls1);
//
//        Set<MultiSpeciesCall<ExpressionCall>> multiSpCalls100 = new HashSet<>();
//        multiSpCalls100.add(new MultiSpeciesCall<>(aeSim1, dsSim1, taxId100, "oma2", orthologousGeneIds100,
//            new HashSet<>(Arrays.asList(call1, call2)), null, serviceFactory));
//        multiSpCalls100.add(new MultiSpeciesCall<>(aeSim1, dsSim2b, taxId100, "oma2", orthologousGeneIds100,
//            new HashSet<>(Arrays.asList(call3)), null, serviceFactory));
//        multiSpCalls100.add(new MultiSpeciesCall<>(aeSim2, dsSim2b, taxId100, "oma2", orthologousGeneIds100, 
//            new HashSet<>(Arrays.asList(call4, call5, call6)), null, serviceFactory));
//        expected.put(taxId100, multiSpCalls100);
//
//        assertEquals("Incorrect multi-species expression calls", new ArrayList<>(expected.keySet()),
//            new ArrayList<>(actual.keySet()));
//        assertEquals("Incorrect multi-species expression calls for tax ID 10",
//            expected.get(taxId10), actual.get(taxId10));
//        assertEquals("Incorrect multi-species expression calls for tax ID 100",
//            expected.get(taxId100), actual.get(taxId100));
    }

    @Test
    @Ignore("Add a quick and dirty integration test in MultiSpeciesCallServiceTest using the actual database; to be reverted later. Commit b520e0ca")
    public void makeIntegrationTest() {
        try (ServiceFactory serviceFactory = new ServiceFactory()) {
            getLogger().info("SIMILARITY: {}", serviceFactory.getAnatEntitySimilarityService()
                    .loadPositiveAnatEntitySimilarities(7742, false)
            .stream().map(s -> s.getSourceAnatEntities().stream()
                    .map(ae -> ae.getName()).collect(Collectors.joining(", ")))
            .collect(Collectors.joining("\n")));

            MultiSpeciesCallService callService = serviceFactory.getMultiSpeciesCallService();
            GeneFilter geneFilter1 = new GeneFilter(9606, Arrays.asList("ENSG00000244734", "ENSG00000130208"));
            GeneFilter geneFilter2 = new GeneFilter(10090, Arrays.asList("ENSMUSG00000052187", "ENSMUSG00000040564"));
            callService.loadSimilarityExpressionCalls(7742, Arrays.asList(geneFilter1, geneFilter2), null, false)
            .forEach(c -> getLogger().info(c.getGene().getName() + " - "
            + c.getMultiSpeciesCondition().getAnatSimilarity().getAllAnatEntities().stream()
            .map(ae -> ae.getName()).collect(Collectors.joining(", "))));
        }
    }
}
