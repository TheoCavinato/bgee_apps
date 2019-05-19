package org.bgee.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.SearchResult;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.multispemapping.AnatEntitySimilarity;
import org.bgee.model.anatdev.multispemapping.AnatEntitySimilarityTaxonSummary;
import org.bgee.model.expressiondata.Condition;
import org.bgee.model.expressiondata.MultiGeneExprAnalysis;
import org.bgee.model.expressiondata.MultiGeneExprAnalysis.MultiGeneExprCounts;
import org.bgee.model.expressiondata.SingleSpeciesExprAnalysis;
import org.bgee.model.expressiondata.baseelements.SummaryCallType;
import org.bgee.model.expressiondata.multispecies.MultiSpeciesCondition;
import org.bgee.model.expressiondata.multispecies.MultiSpeciesExprAnalysis;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneBioType;
import org.bgee.model.species.Species;
import org.bgee.model.species.Taxon;
import org.bgee.view.ExpressionComparisonDisplay;
import org.bgee.view.ViewFactory;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller handling requests related to expression comparison pages. 
 *
 * @author  Valentine Rech de Laval
 * @version Bgee 14, May 2019
 * @since   Bgee 14, May 2019
 */
public class CommandExpressionComparison extends CommandParent {
    
    private final static Logger log = LogManager.getLogger(CommandExpressionComparison.class.getName());

    /**
     * Constructor
     *
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param viewFactory       A {@code ViewFactory} that provides the display type to be used.
     * @param serviceFactory    A {@code ServiceFactory} that provides bgee services.
     */
    public CommandExpressionComparison(HttpServletResponse response, RequestParameters requestParameters,
                                       BgeeProperties prop, ViewFactory viewFactory, ServiceFactory serviceFactory) {
        super(response, requestParameters, prop, viewFactory, serviceFactory);
    }

    @Override
    public void processRequest() throws Exception {
        log.entry();

        final List<String> userGeneList = Collections.unmodifiableList(Optional.ofNullable(
                this.requestParameters.getGeneList()).orElse(new ArrayList<>()));

        ExpressionComparisonDisplay display = viewFactory.getExpressionComparisonDisplay();

        if (userGeneList.isEmpty()) {
            display.displayExpressionComparisonHomePage();
            log.exit(); return;
        }
        if (userGeneList.size() == 1) {
            display.displayExpressionComparison("At least two Ensembl IDs should be provided.");
            log.exit(); return;
        }

        // FIXME to be removed when mock is removed
        SearchResult<String, Gene> searchResult = serviceFactory.getGeneService()
                .searchGenesByEnsemblIds(userGeneList);
        Set<Species> species = searchResult.getResults().stream()
                .map(Gene::getSpecies).collect(Collectors.toSet());

        if (species.isEmpty()) {
            display.displayExpressionComparison("No gene from species presents in Bgee are detected.");
            log.exit(); return;
        }

        AnatEntity anatEntity = new AnatEntity("UBERON:0002048", "lung", "AE desc");
        Species sp1 = new Species(9606, "human", null, "Homo", "sapiens", "version1", null, null, 123, null, null, 1);
        Species sp2 = new Species(10090, "mouse", null, "Mus", "musculus", "version1", null, null, null, null, null, 2);
        GeneBioType biotype = new GeneBioType("b");

        Gene g1 = new Gene("ID1", "name1", null, null, null, sp1, biotype, 1);
        Gene g2 = new Gene("ID2", "name2", null, null, null, sp1, biotype, 1);
        Gene g3 = new Gene("ID3", null, null, null, null, sp1, biotype, 1);
        Gene g4 = new Gene("ID4", "name4", null, null, null, sp1, biotype, 1);
        Gene g5 = new Gene("ID5", "name5", null, null, null, sp1, biotype, 1);
        Gene g6 = new Gene("ID6", "name6", null, null, null, sp1, biotype, 1);
        Gene g7 = new Gene("ID7", "name7", null, null, null, sp1, biotype, 1);
        
        Gene g8 = new Gene("ID8", "name7", null, null, null, sp2, biotype, 1);
        Gene g9 = new Gene("ID9", "name8", null, null, null, sp2, biotype, 1);
        Gene g10 = new Gene("ID10", "name10", null, null, null, sp2, biotype, 1);
        Gene g11 = new Gene("ID11", "name11", null, null, null, sp2, biotype, 1);
        Gene g12 = new Gene("ID12", "name12", null, null, null, sp2, biotype, 1);
        Gene g13 = new Gene("ID13", "name13", null, null, null, sp2, biotype, 1);
        Gene g14 = new Gene("ID14", "name14", null, null, null, sp2, biotype, 1);
        Gene g15 = new Gene("ID15", "name15", null, null, null, sp2, biotype, 1);
        Gene g16 = new Gene("ID16", "name16", null, null, null, sp2, biotype, 1);

        Map<SummaryCallType, Collection<Gene>> callTypeToGenes = new HashMap<>();

        if (species.size() == 1) {
//            SingleSpeciesExprAnalysis singleSpeciesExprAnalysis = serviceFactory.getCallService().loadMultiSpeciesExprAnalysis(userGeneList);
            Map<Condition, MultiGeneExprAnalysis.MultiGeneExprCounts> condToCounts = new HashMap<>();

            callTypeToGenes.put(SummaryCallType.ExpressionSummary.EXPRESSED,
                    Arrays.asList(g1, g2, g3));
            callTypeToGenes.put(SummaryCallType.ExpressionSummary.NOT_EXPRESSED,
                    Arrays.asList(g5 , g6, g7));
            
            MultiGeneExprCounts counts = new MultiGeneExprCounts(callTypeToGenes, Arrays.asList(g4));
            condToCounts.put(new Condition(anatEntity, null, sp1), counts);
            searchResult = new SearchResult<>(userGeneList,
                    Arrays.asList("ENSG11000125746", "ENSG11000125740"), searchResult.getResults());
            SingleSpeciesExprAnalysis singleSpeciesExprAnalysis = new SingleSpeciesExprAnalysis(
                    searchResult.getResults(), condToCounts);
            
            display.displayExpressionComparison(searchResult, singleSpeciesExprAnalysis);
            log.exit(); return;
        }

        callTypeToGenes.put(SummaryCallType.ExpressionSummary.EXPRESSED, Arrays.asList(g1, g2, g3, g4, g5, g6));
        callTypeToGenes.put(SummaryCallType.ExpressionSummary.NOT_EXPRESSED, Arrays.asList(g8 , g9, g10, g11, g12));

        Taxon taxon = new Taxon(10, null, null, "scientificName", 1, true);
        Set<AnatEntitySimilarityTaxonSummary> aeSimTaxonSummaries = Collections.singleton(
                new AnatEntitySimilarityTaxonSummary(taxon, true, true));

        AnatEntitySimilarity aeSim1 = new AnatEntitySimilarity(
                Arrays.asList(anatEntity), null, taxon, aeSimTaxonSummaries);
        
        Map<MultiSpeciesCondition, MultiGeneExprAnalysis.MultiGeneExprCounts> globalCondToCounts = new HashMap<>();
        
        MultiGeneExprCounts counts = new MultiGeneExprCounts(callTypeToGenes, 
                Arrays.asList(g7, g13, g14, g15, g16));
        globalCondToCounts.put(new MultiSpeciesCondition(aeSim1, null), counts);
        searchResult = new SearchResult<>(userGeneList,
                Arrays.asList("ENSMUSG10000027465"), searchResult.getResults());
        MultiSpeciesExprAnalysis multiSpeciesExprAnalysis = new MultiSpeciesExprAnalysis(
                searchResult.getResults(), globalCondToCounts);


//        MultiSpeciesExprAnalysis multiSpeciesExprAnalysis = serviceFactory.getMultiSpeciesCallService().loadMultiSpeciesExprAnalysis(userGeneList);
        display.displayExpressionComparison(searchResult, multiSpeciesExprAnalysis);
    }
}