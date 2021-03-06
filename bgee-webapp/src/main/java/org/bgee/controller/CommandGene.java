package org.bgee.controller;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.Call.ExpressionCall.ClusteringMethod;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneFilter;
import org.bgee.model.gene.GeneMatchResult;
import org.bgee.view.GeneDisplay;
import org.bgee.view.ViewFactory;

/**
 * Controller handling requests related to gene pages. 
 * 
 * @author  Philippe Moret
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @author  Julien Wollbrett
 * @version Bgee 14, Apr. 2019
 * @since   Bgee 13, Nov. 2015
 */
public class CommandGene extends CommandParent {

    private final static Logger log = LogManager.getLogger(CommandGene.class.getName());
    
    /**
     * Contains all information necessary to produce a view related to a {@code Gene}.
     * 
     * @author Frederic Bastian
     * @version Bgee 13, June 2016
     * @since   Bgee 13, Jan. 2016
     */
    public static class GeneResponse {
        private final Gene gene;
        private final LinkedHashMap<AnatEntity, List<ExpressionCall>> callsByAnatEntity;
        private final boolean includingAllRedundantCalls;
        private final Map<ExpressionCall, Integer> clusteringBestEachAnatEntity;
        private final Map<ExpressionCall, Integer> clusteringWithinAnatEntity;
        
        /**
         * @param gene                          See {@link #getGene()}.
         * @param includingAllRedundantCalls    See {@link #isIncludingAllRedundantCalls()}.
         * @param callsByAnatEntity             See {@link #getCallsByAnatEntity()}.
         * @param clusteringBestEachAnatEntity  See {@link #getClusteringBestEachAnatEntity()}.
         * @param clusteringWithinAnatEntity    See {@link #getClusteringWithinAnatEntity()}.
         */
        public GeneResponse(Gene gene, boolean includingAllRedundantCalls, 
                LinkedHashMap<AnatEntity, List<ExpressionCall>> callsByAnatEntity, 
                Map<ExpressionCall, Integer> clusteringBestEachAnatEntity, 
                Map<ExpressionCall, Integer> clusteringWithinAnatEntity) {
            this.gene = gene;
            this.includingAllRedundantCalls = includingAllRedundantCalls;
            //too boring to protect the Maps for this internal class...
            this.callsByAnatEntity = callsByAnatEntity;
            this.clusteringBestEachAnatEntity = clusteringBestEachAnatEntity;
            this.clusteringWithinAnatEntity = clusteringWithinAnatEntity;
        }

        /**
         * @return  The {@code Gene} which information are requested for. 
         */
        public Gene getGene() {
            return gene;
        }
        /**
         * @return  A {@code boolean} that is {@code true} if the information returned by 
         *          {@link #getCallsByAnatEntity()}, {@link #getClusteringBestEachAnatEntity()}, 
         *          and {@link #getClusteringWithinAnatEntity()}, were built by including all 
         *          redundant calls (see {@link #getRedundantExprCalls()}), {@code false} otherwise.
         */
        public boolean isIncludingAllRedundantCalls() {
            return includingAllRedundantCalls;
        }
        /**
         * @return  A {@code LinkedHashMap} where keys are {@code AnatEntity}s corresponding to 
         *          anat. entity, ordered based on the best rank in each anat. entity, 
         *          the associated value being a {@code List} of {@code ExpressionCall}s 
         *          in this anat. entity, ordered by their global mean rank.
         *          Redundant {@code ExpressionCall}s may or may not have been considered, 
         *          depending on {@link #isIncludingAllRedundantCalls()}.
         * @see #isIncludingAllRedundantCalls()
         */
        public LinkedHashMap<AnatEntity, List<ExpressionCall>> getCallsByAnatEntity() {
            return callsByAnatEntity;
        }
        /**
         * Returns a clustering of a set of {@code ExpressionCall}s generated by only considering  
         * the best {@code ExpressionCall} from each anatomical entity. 
         * Redundant {@code ExpressionCall}s may or may not have been considered, 
         * depending on {@link #isIncludingAllRedundantCalls()}.
         * 
         * @return      A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *              being the index of the group in which they are clustered, 
         *              based on their expression score. Group indexes are assigned in ascending 
         *              order of expression score, starting from 0. Only one best 
         *              {@code ExpressionCall} per anatomical entity is considered. 
         * @see #isIncludingAllRedundantCalls()
         */
        public Map<ExpressionCall, Integer> getClusteringBestEachAnatEntity() {
            return clusteringBestEachAnatEntity;
        }
        /**
         * Returns a clustering of {@code ExpressionCall}s clustered independently 
         * for each anatomical entity (so, {@code ExpressionCall}s associated to a same value 
         * in the returned {@code Map} might not be part of a same cluster). 
         * Redundant {@code ExpressionCall}s may or may not have been considered, 
         * depending on {@link #isIncludingAllRedundantCalls()}. 
         * 
         * @return      A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *              being the index of the group in which they are clustered, 
         *              based on their expression score. Group indexes are assigned in ascending 
         *              order of expression score, starting from 0. Clusters are independent 
         *              per anatomical entities. 
         * @see #isIncludingAllRedundantCalls()
         */
        public Map<ExpressionCall, Integer> getClusteringWithinAnatEntity() {
            return clusteringWithinAnatEntity;
        }
    }

    /**
     * Constructor
     * 
     * @param response                  A {@code HttpServletResponse} that will be used to display the 
     *                                  page to the client
     * @param requestParameters         The {@code RequestParameters} that handles the parameters of the 
     *                                  current request.
     * @param prop                      A {@code BgeeProperties} instance that contains the properties
     *                                  to use.
     * @param viewFactory               A {@code ViewFactory} that provides the display type to be used.
     * @param serviceFactory            A {@code ServiceFactory} that provides bgee services.
     */
    public CommandGene(HttpServletResponse response, RequestParameters requestParameters,
                       BgeeProperties prop, ViewFactory viewFactory, ServiceFactory serviceFactory) {
        super(response, requestParameters, prop, viewFactory, serviceFactory);
    }

    @Override
    public void processRequest() throws Exception {
        log.entry();
        GeneDisplay display = viewFactory.getGeneDisplay();
        String geneId = requestParameters.getGeneId();
        Integer speciesId = requestParameters.getSpeciesId();
        String search = requestParameters.getQuery();

        if (StringUtils.isNotBlank(search)) {
            GeneMatchResult result = serviceFactory.getGeneMatchResultService(this.prop)
                    .searchByTerm(search, null, 0, 1000);
            display.displayGeneSearchResult(search, result);
            log.exit(); return;
        }

        if (geneId == null) {
            display.displayGeneHomePage();
            log.exit(); return;
        }

        // NOTE: we retrieve genes after the sanity check on geneId to avoid to throw an exception
        Set<Gene> genes = serviceFactory.getGeneService().loadGenesByEnsemblId(geneId, true);
        if (genes.size() == 0) {
            throw log.throwing(new PageNotFoundException("No gene corresponding to " + geneId));
        }

        if (genes.size() == 1 && speciesId != null) {
            //we want to avoid the use of 'species_id' parameter in URL if not necessary,
            //so if an Ensembl ID has an unique hit in Bgee, and there is a 'species_id'
            //in the URL, then we redirect to a page without 'species_id' in the URL,
            //for nicer URLs, and to avoid duplicated content.
            // XXX: we do not check that the user gives a bad species?
            RequestParameters url = new RequestParameters(
                    this.requestParameters.getUrlParametersInstance(), this.prop, false, "&");
            url.setPage(RequestParameters.PAGE_GENE);
            url.setGeneId(genes.iterator().next().getEnsemblGeneId());
            this.response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            this.response.addHeader("Location", url.getRequestURL());
            log.exit(); return;
        }

        Gene selectedGene = null;
        if (genes.size() == 1) {
            selectedGene = genes.iterator().next();
        } else if (genes.size() > 1)  {
            //if several gene IDs match, then we need to get the speciesId information,
            //otherwise we need to let the user choose the species he/she wants
            if (speciesId == null || speciesId <= 0) {
                display.displayGeneChoice(genes);
                log.exit(); return;
            }
            Set<Gene> speciesGenes = genes.stream()
                    .filter(g -> g.getSpecies().getId().equals(speciesId))
                    .collect(Collectors.toSet());
            if (speciesGenes.size() != 1) {
                throw log.throwing(new PageNotFoundException("No gene corresponding to "
                        + geneId + " for species " + speciesId));
            }
            selectedGene = speciesGenes.iterator().next();
        } else {
            throw log.throwing(new AssertionError("Impossible case"));
        }

        display.displayGene(this.buildGeneResponse(selectedGene));
        log.exit();
    }

    private GeneResponse buildGeneResponse(Gene gene) throws IllegalStateException {
        log.entry(gene);
        //retrieve calls with silver quality for one anat. entity and at least bronze quality
        //for the same anat. entity and a dev. stage
        LinkedHashMap<AnatEntity, List<ExpressionCall>> callsByAnatEntity = serviceFactory.getCallService()
                .loadCondCallsWithSilverAnatEntityCallsByAnatEntity(
                        new GeneFilter(gene.getSpecies().getId(), gene.getEnsemblGeneId()));
        if (callsByAnatEntity == null || callsByAnatEntity.isEmpty()) {
            log.debug("No calls for gene {}", gene.getEnsemblGeneId());
             return log.exit(new GeneResponse(gene, true, callsByAnatEntity, 
                     new HashMap<>(), new HashMap<>()));
        }
        
        //**************************************
        // Clustering, Building GeneResponse
        //**************************************
        return log.exit(this.buildGeneResponse(gene, callsByAnatEntity, true));
    }
    
    /**
     * Continue the building of a {@code GeneResponse}, by taking care of the steps 
     * of grouping of {@code ExpressionCall}s per anatomical entity, and of clustering. 
     * 
     * @param gene                     The requested {@code Gene}.
     * @param callsByAnatEntity        A {@code LinkedHashMap} where values are {@code ExpressionCall}s sorted using the  
     *                                 {@link ExpressionCall#filterAndOrderCallsByRank(Collection, ConditionGraph)}
     *                                 and keys correspond to {@code AnatEntity}s
     * @param filterRedundantCalls     A {@code boolean} defining whether redundant calls 
     *                                 should be filtered for the grouping and clustering steps.
     * @return                         A built {@code GeneResponse}.
     */
    private GeneResponse buildGeneResponse(Gene gene, LinkedHashMap<AnatEntity, 
            List<ExpressionCall>> callsByAnatEntity, boolean filterRedundantCalls) {
        log.entry(gene, callsByAnatEntity, filterRedundantCalls);

        long startFilteringTimeInMs = System.currentTimeMillis();

        //*********************
        // Clustering
        //*********************
        //define clustering method
        Function<List<ExpressionCall>, Map<ExpressionCall, Integer>> clusteringFunction = 
                getClusteringFunction();
        
        //Store a clustering of ExpressionCalls, by considering only one best ExpressionCall 
        //from each anatomical entity.
        Map<ExpressionCall, Integer> clusteringBestEachAnatEntity = clusteringFunction.apply(
                callsByAnatEntity.values().stream()
                         //store the best call from each anat. entity 
                        .map(callList -> callList.get(0))
                        //in the order of the sorted List of ExpressionCalls
                        .collect(Collectors.toList()));
        
        //store a clustering, independent for each anatomical entity, of the ExpressionCalls 
        //of an anatomical entity
        Map<ExpressionCall, Integer> clusteringWithinAnatEntity = callsByAnatEntity.values().stream()
                .flatMap(callList -> clusteringFunction.apply(callList).entrySet().stream())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
        
        log.debug("Total clustering of calls performed in {} ms", 
                System.currentTimeMillis() - startFilteringTimeInMs);
        
        //*********************
        // Build GeneResponse
        //*********************
        return log.exit(new GeneResponse(gene, !filterRedundantCalls, callsByAnatEntity,
                clusteringBestEachAnatEntity, clusteringWithinAnatEntity));
    }
    
    /**
     * Return the {@code Function} corresponding to the clustering method to used, 
     * based on the properties {@link BgeeProperties#getGeneScoreClusteringMethod()} 
     * and {@link BgeeProperties#getGeneScoreClusteringThreshold()}. The {@code Function}
     * will trigger a call to {@link ExpressionCall#generateMeanRankScoreClustering(
     * List, ClusteringMethod, double)}.
     * 
     * @return     A {@code Function} accepting a {@code List} of {@code ExpressionCall}s 
     *             as input, and returns a {@code Map} corresponding to the clustering as output.
     * @throws IllegalStateException   If {@link #prop} does not provide properties 
     *                                 allowing to parameterize the clustering function.
     * @see ExpressionCall#generateMeanRankScoreClustering(List, ClusteringMethod, double)
     */
    private Function<List<ExpressionCall>, Map<ExpressionCall, Integer>> getClusteringFunction() 
            throws IllegalStateException {
        log.entry();
        if (this.prop.getGeneScoreClusteringMethod() == null) {
            throw log.throwing(new IllegalStateException("No clustering method specified."));
        }
        //Distance threshold
        if (this.prop.getGeneScoreClusteringThreshold() == null || 
                //we don't want negative nor near-zero values
                this.prop.getGeneScoreClusteringThreshold() < 0.000001) {
            throw log.throwing(new IllegalStateException("A clustering method was specified, "
                    + "but no distance threshold or incorrect threshold value assigned."));
        }
        try {
            //find clustering method
            final ClusteringMethod method = ClusteringMethod.valueOf(
                    this.prop.getGeneScoreClusteringMethod().trim());
            
            //define clustering function
            log.debug("Using clustering method {} with distance threshold {}", method, 
                    this.prop.getGeneScoreClusteringThreshold());
            return log.exit(
                    callList -> ExpressionCall.generateMeanRankScoreClustering(callList, method, 
                            this.prop.getGeneScoreClusteringThreshold()));
        } catch (IllegalArgumentException e) {
            throw log.throwing(new IllegalStateException("No clustering method corresponding to "
                    + this.prop.getGeneScoreClusteringMethod().trim()));
        }
    }
}
