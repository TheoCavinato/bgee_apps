package org.bgee.model.gene;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.CommonService;
import org.bgee.model.SearchResult;
import org.bgee.model.ServiceFactory;
import org.bgee.model.dao.api.EntityTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneNameSynonymDAO.GeneNameSynonymTO;
import org.bgee.model.dao.api.gene.GeneXRefDAO;
import org.bgee.model.dao.api.gene.GeneXRefDAO.GeneXRefTO;
import org.bgee.model.source.Source;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.species.Species;
import org.bgee.model.species.SpeciesService;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * A {@link org.bgee.model.Service} to obtain {@link Gene} objects. Users should use the
 * {@link org.bgee.model.ServiceFactory ServiceFactory} to obtain {@code GeneService}s.
 * 
 * @author  Philippe Moret
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Apr. 2019
 * @since   Bgee 13, Sept. 2015
 */
public class GeneService extends CommonService {
    private static final Logger log = LogManager.getLogger(GeneService.class.getName());

    private final SpeciesService speciesService;
    private final GeneDAO geneDAO;

    /**
     * @param serviceFactory            The {@code ServiceFactory} to be used to obtain {@code Service}s 
     *                                  and {@code DAOManager}.
     * @throws IllegalArgumentException If {@code serviceFactory} is {@code null}.
     */
    public GeneService(ServiceFactory serviceFactory) {
        super(serviceFactory);
        this.speciesService = this.getServiceFactory().getSpeciesService();
        this.geneDAO = this.getDaoManager().getGeneDAO();
    }
    
    /**
     * Retrieve {@code Gene}s based on the provided {@code GeneFiter}.
     * <p>
     * This method won't load synonyms and cross-references in {@code Gene}s, to avoid memory problems.
     * 
     * @param filter        A {@code GeneFilter}s allowing to filter the {@code Gene}s to retrieve.
     * @return              A {@code Stream} of matching {@code Gene}s.
     */
    public Stream<Gene> loadGenes(GeneFilter filter) {
        log.entry(filter);
        return log.exit(this.loadGenes(Collections.singleton(filter)));
    }

    /**
     * Retrieve {@code Gene}s based on the provided {@code GeneFiter}s.
     * <p>
     * This method won't load synonyms and cross-references in {@code Gene}s, to avoid memory problems.
     * 
     * @param filters       A {@code Collection} of {@code GeneFilter}s allowing to filter
     *                      the {@code Gene}s to retrieve.
     * @return              A {@code Stream} of matching {@code Gene}s.
     */
    public Stream<Gene> loadGenes(Collection<GeneFilter> filters) {
        log.entry(filters);
        
        Set<GeneFilter> clonedFilters = filters == null? new HashSet<>(): new HashSet<>(filters);
        Map<Integer, Set<String>> filtersToMap = clonedFilters.stream()
                .collect(Collectors.toMap(f -> f.getSpeciesId(), f -> new HashSet<>(f.getEnsemblGeneIds()),
                        (s1, s2) -> {s1.addAll(s2); return s1;}));

        //retrieve the Species requested in GeneFilters
        Map<Integer, Species> speciesMap = this.speciesService.loadSpeciesMap(filtersToMap.keySet(), false);
        if (!speciesMap.keySet().containsAll(filtersToMap.keySet())) {
            Set<Integer> unrecognizedSpeciesIds = new HashSet<>(filtersToMap.keySet());
            unrecognizedSpeciesIds.removeAll(speciesMap.keySet());
            throw log.throwing(new IllegalArgumentException(
                    "GeneFilters contain unrecognized species IDs: " + unrecognizedSpeciesIds));
        }
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));
        
        // We want to return a Stream without iterating the GeneTOs first,
        // so we won't load synonyms

        return log.exit(mapGeneTOStreamToGeneStream(
                this.geneDAO.getGenesBySpeciesAndGeneIds(filtersToMap).stream(),
                speciesMap, null, null, null, geneBioTypeMap));
    }

    /**
     * Loads {@code Gene}s from an Ensembl gene ID. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     * <p>
     * This method will load synonyms and cross-references in {@code Gene}s.
     * 
     * @param ensemblGeneId A {@code String} that is the Ensembl ID of genes to retrieve.
     * @return              A {@code Set} of matching {@code Gene}s.
     */
    public Set<Gene> loadGenesByEnsemblId(String ensemblGeneId) {
        log.entry(ensemblGeneId);
        return log.exit(this.loadGenesByEnsemblId(ensemblGeneId, false));
    }

    /**
     * Loads {@code Gene}s from Ensembl gene IDs. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     * <p>
     * This method won't load synonyms and cross-references in {@code Gene}s, to avoid memory problems.
     * 
     * @param ensemblGeneIds    A {@code Collection} of {@code String}s that are the Ensembl IDs
     *                          of genes to retrieve.
     * @return                  A {@code Stream} of matching {@code Gene}s.
     */
    public Stream<Gene> loadGenesByEnsemblIds(Collection<String> ensemblGeneIds) {
        log.entry(ensemblGeneIds);
        return log.exit(this.loadGenesByEnsemblIds(ensemblGeneIds, false));
    }
    /**
     * Retrieves genes from their Ensembl IDs as a {@code SearchResult}. {@code SearchResult} allows
     * also to retrieve the IDs that were <strong>not</strong> found in the database.
     * The {@code Gene}s retrieved are the same as a call to {@link #loadGenesByEnsemblIds(Collection)}
     * would return (except that this method does not accept {@code null} or empty argument).
     *
     * @param ensemblGeneIds    A {@code Collection} of {@code String}s that are the Ensembl IDs
     *                          of genes to retrieve. Cannot be {@code null} or emty, otherwise
     *                          an {@code IllegalArgumentException} is thrown.
     * @return                  A {@code SearchResult} holding the requested IDs, the {@code Gene}s
     *                          retrieved, and the IDs that could not be found in Bgee.
     * @see #loadGenesByEnsemblIds(Collection)
     * @throws IllegalArgumentException If {@code ensemblGeneIds} is {@code null} or empty.
     */
    public SearchResult<String, Gene> searchGenesByEnsemblIds(Collection<String> ensemblGeneIds)
    throws IllegalArgumentException {
        log.entry(ensemblGeneIds);
        if (ensemblGeneIds == null || ensemblGeneIds.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("Some genes must be provided"));
        }
        Set<String> clonedGeneIds = new HashSet<>(ensemblGeneIds);
        Set<Gene> genes = this.loadGenesByEnsemblIds(clonedGeneIds).collect(Collectors.toSet());
        Set<String> geneIdsFound = genes.stream().map(g -> g.getEnsemblGeneId())
                .collect(Collectors.toSet());
        Set<String> geneIdsNotFound = new HashSet<>(clonedGeneIds);
        geneIdsNotFound.removeAll(geneIdsFound);
        return log.exit(new SearchResult<>(clonedGeneIds, geneIdsNotFound, genes));
    }

    /**
     * Loads {@code Gene}s from an Ensembl gene ID. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     * <p>
     * This method will load synonyms and cross-references in {@code Gene}s.
     *
     * @param ensemblGeneId     A {@code String} that is the Ensembl ID of genes to retrieve.
     * @param withSpeciesInfo   A {@code boolean}s defining whether data sources of the species
     *                          is retrieved or not.
     * @return                  A {@code Set} of matching {@code Gene}s.
     */
    public Set<Gene> loadGenesByEnsemblId(String ensemblGeneId, boolean withSpeciesInfo) {
        log.entry(ensemblGeneId, withSpeciesInfo);
        if (StringUtils.isBlank(ensemblGeneId)) {
            throw log.throwing(new IllegalArgumentException("No gene ID can be blank."));
        }
        
        //we expect very few results from a single Ensembl ID, so we don't preload all species
        //in database as for method 'loadGenesByEnsemblIds'
        Set<GeneTO> geneTOs = this.geneDAO
                .getGenesByEnsemblGeneIds(Collections.singleton(ensemblGeneId))
                .stream().collect(Collectors.toSet());
        //In case the ID provided was incorrect/doesn't match any gene in Bgee
        if (geneTOs == null || geneTOs.isEmpty()) {
            return log.exit(new HashSet<>());
        }
        final Map<Integer, Species> speciesMap = Collections.unmodifiableMap(loadSpeciesMap(geneTOs, withSpeciesInfo));
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));
        // we expect very few results from a single Ensembl ID, so we preload synonyms and x-refs
        // from database
        Map<Integer, Set<String>> synonymMap = loadSynonymsByBgeeGeneIds(geneTOs);
        Map<Integer, Set<GeneXRefTO>> xRefsMap = loadXrefTOsByBgeeGeneIds(geneTOs);
        //We load all sources, to be able to retrieve the Ensembl and Ensembl metazoa sources anyway
        final Map<Integer, Source> sourceMap = getServiceFactory().getSourceService()
                .loadSourcesByIds(null);
        
        return log.exit(mapGeneTOStreamToGeneStream(geneTOs.stream(), speciesMap, synonymMap,
                xRefsMap, sourceMap, geneBioTypeMap)
                .collect(Collectors.toSet()));
    }

    /**
     * Loads {@code Gene}s from Ensembl gene IDs. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     * <p>
     * This method won't load synonyms and cross-references in {@code Gene}s, to avoid memory problems.
     *
     * @param ensemblGeneIds    A {@code Collection} of {@code String}s that are the Ensembl IDs
     *                          of genes to retrieve.
     * @param withSpeciesInfo   A {@code boolean}s defining whether data sources of the species
     *                          is retrieved or not.
     * @return                  A {@code Stream} of matching {@code Gene}s.
     */
    public Stream<Gene> loadGenesByEnsemblIds(Collection<String> ensemblGeneIds, boolean withSpeciesInfo) {
        log.entry(ensemblGeneIds, withSpeciesInfo);
        if (ensemblGeneIds != null && ensemblGeneIds.stream().anyMatch(id -> StringUtils.isBlank(id))) {
            throw log.throwing(new IllegalArgumentException("No gene ID can be blank."));
        }

        //we need to get the Species genes belong to, in order to instantiate Gene objects.
        //we don't have access to the species ID information before getting the GeneTOs,
        //and we want to return a Stream without iterating the GeneTOs first,
        //so we load all species in database
        final Map<Integer, Species> speciesMap = Collections.unmodifiableMap(this.speciesService.loadSpeciesMap(null, withSpeciesInfo));
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));

        // As we want to return a Stream without iterating the GeneTOs first,
        // so we won't load synonyms
        
        return log.exit(mapGeneTOStreamToGeneStream(
                this.geneDAO.getGenesByEnsemblGeneIds(ensemblGeneIds).stream(),
                speciesMap, null, null, null, geneBioTypeMap));
    }

    /**
     * Get the orthologies for a given taxon.
     * 
     * @param taxonId      An {@code Integer that is the ID of taxon for which
     *                      to retrieve the orthology groups.
     * @param speciesIds    A {@code Set} of {@code Integer}s that are the IDs of species to be
     *                      considered. If {@code null}, all species available for the taxon are used.
     * @return              The {@code Map} where keys are {@code Integer}s corresponding to 
     *                      OMA Node IDs, the associated value being a {@code Set} of {@code Integer}s
     *                      corresponding to their {@code Gene}.
     */
    public Map<Integer, Set<Gene>> getOrthologs(Integer taxonId, Set<Integer> speciesIds) {
        log.entry(taxonId, speciesIds);
        throw log.throwing(new UnsupportedOperationException("To implement"));
//        HierarchicalNodeToGeneTOResultSet resultSet = getDaoManager().getHierarchicalGroupDAO()
//                .getOMANodeToGene(taxonId, speciesIds);
//
//        final Set<Integer> clnSpId =  speciesIds == null? new HashSet<>():
//                Collections.unmodifiableSet(new HashSet<>(speciesIds));
//        
//        final Map<Integer, Species> speciesMap = getSpeciesMap(clnSpId, false);
//
//        final Map<Integer, Gene> geneMap = Collections.unmodifiableMap(this.geneDAO
//            .getGenesBySpeciesIds(speciesIds).stream()
//                .collect(Collectors.toMap(
//                    gTO -> gTO.getId(),
//                    gTO -> mapGeneTOToGene(gTO, speciesMap.get(gTO.getSpeciesId())))));
//
//        Map<Integer, Set<Gene>> results = resultSet.stream()
//                .collect(Collectors.groupingBy(hg -> hg.getNodeId()))
//                .entrySet().stream()
//                .collect(Collectors.toMap(
//                        e -> e.getKey(), 
//                        e -> e.getValue().stream()
//                            .map(to -> geneMap.get(to.getBgeeGeneId())).collect(Collectors.toSet())));
//        return log.exit(results);
    }

    /**
     * 
     * @param taxonIds		A {@code Collection} of {@code Integer} corresponding to taxon Ids for which
     * 						orthologous genes should be returned.
     * @param speciesIds	A {@code Collection} of {@code Integer} corresponding to species Ids for which
     * 						orthologous genes should be returned. If null orthologous genes are returned for
     * 						all species. 
     * @param geneFilter	A {@code GeneFilter} corresponding to starting genes for
     * 						which orthologous genes should be returned.
     * @return				A A {@code Stream} of {@code OrthologousGeneGroup}
     */
    public Stream<OrthologousGeneGroup> getOrthologs(Collection<Integer> taxonIds,
    		Collection<Integer> speciesIds, GeneFilter geneFilter) {
        log.entry(taxonIds, speciesIds, geneFilter);
        throw log.throwing(new UnsupportedOperationException("To implement"));
//        if(geneFilter == null || geneFilter.getSpeciesId() == null || geneFilter.getSpeciesId() == 0){
//        	log.throwing(new IllegalArgumentException("No species id provided for the starting genes."));
//        }
//        final Collection<Integer> clnSpIds =  speciesIds == null? new HashSet<>():
//            Collections.unmodifiableCollection(speciesIds);
//        final Collection<Integer> clnTaxonIds =  taxonIds == null? new HashSet<>():
//            Collections.unmodifiableCollection(taxonIds);
//        // return OrthologousGeneGroup without genes by OMA node Id
//        Map <Integer, OrthologousGeneGroup> geneGroupsByOMANodes = getDaoManager().getHierarchicalGroupDAO()
//            .getOMANodesFromStartingGenes(clnTaxonIds, geneFilter.getSpeciesId(),
//            		geneFilter.getEnsemblGeneIds())
//            .getAllTOs().stream().collect(Collectors.toMap(
//            		nTO -> nTO.getId(), 
//            		nTO -> new OrthologousGeneGroup(nTO.getTaxonId(),
//            				nTO.getOMAGroupId(), nTO.getTaxonId(), null)));
//        //return all gene ids by OMA node id
//        final Map<Integer, Set<Integer>> bgeeGeneIdsByNodeId = new HashMap<>();
//    	getDaoManager().getHierarchicalGroupDAO()
//    			.getGenesByNodeFromNodes(geneGroupsByOMANodes.keySet(), clnSpIds).getAllTOs()
//    			.stream().forEach(e -> {
//    				if(bgeeGeneIdsByNodeId.containsKey(e.getNodeId())){
//    					bgeeGeneIdsByNodeId.get(e.getNodeId()).add(e.getBgeeGeneId());
//    				}else{
//    					bgeeGeneIdsByNodeId.put(e.getNodeId(), new HashSet<>());
//    					bgeeGeneIdsByNodeId.get(e.getNodeId()).add(e.getBgeeGeneId());
//    				}
//    			}
//    			);
////    			.stream().map(hgToG -> hgToG.getBgeeGeneId())
////				.collect(Collectors.groupingBy(hgToG -> hgToG.getNodeId()));
//    	//XXX need to develop methods allowing to retrieve Genes by their bgeegeneId or 
//    	// add genes to OrthologousGeneGroups
//    	Map<Integer, Species> speciesMap = getSpeciesMap(new HashSet<>(speciesIds), true);
//    	geneGroupsByOMANodes.entrySet().stream().forEach(e -> {
//    		geneGroupsByOMANodes.get(e.getKey()).getGenes().addAll(
//    				mapGeneTOStreamToGeneStream(this.geneDAO.getGenesByIds(
//    		    			bgeeGeneIdsByNodeId.get(e.getKey())).stream(),speciesMap)
//    				.collect(Collectors.toSet()));
//    	});
//    	return geneGroupsByOMANodes.entrySet().stream().map(e -> e.getValue());
    }

    /**
     * Retrieve {@code Gene}s for a given set of IDs (gene IDs or any cross-reference IDs).
     * <p>
     * This method won't load synonyms and cross-references in {@code Gene}s, to avoid memory problems.
     * 
     * @param mixedGeneIDs  A {@code Collection} of {@code String}s that are IDs (gene IDs or any 
     *                      cross-reference IDs) for which to return the gene IDs.
     * @param withSpeciesInfo   A {@code boolean}s defining whether data sources of the species
     *                          is retrieved or not.
     * @return              The {@code Stream} of {@code Entry}s where keys are 
     *                      provided gene or any cross-reference IDs, the associated value being a
     *                      {@code Set} of {@code Gene}s they correspond to. 
     */
    public Stream<Entry<String, Set<Gene>>> loadGenesByAnyId(Collection<String> mixedGeneIDs,
                                                             boolean withSpeciesInfo) {
        log.entry(mixedGeneIDs, withSpeciesInfo);

        Set<String> clnMixedGeneIDs = mixedGeneIDs == null? new HashSet<>(): new HashSet<>(mixedGeneIDs);

        //we need to get the Species genes belong to, in order to instantiate Gene objects.
        //we don't have access to the species ID information before getting the GeneTOs,
        //and we want to return a Stream without iterating the GeneTOs first,
        //so we load all species in database
        final Map<Integer, Species> speciesMap = this.speciesService.loadSpeciesMap(null, withSpeciesInfo);
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));

        // Get mapping between given IDs and Bgee gene IDs
        final Map<String, Set<Integer>> mapMixedIdToBgeeGeneIds = 
                this.loadMappingXRefIdToBgeeGeneIds(clnMixedGeneIDs);
        final Set<Integer> bgeeGeneIDs = mapMixedIdToBgeeGeneIds.values().stream()
                .flatMap(Set::stream).collect(Collectors.toSet());
        
        // Get Genes from Bgee gene Ids
        final Map<Integer, Gene> geneMap = Collections.unmodifiableMap(getDaoManager().getGeneDAO()
                .getGenesByBgeeIds(bgeeGeneIDs).stream()
                .collect(Collectors.toMap(
                        EntityTO::getId,
                        gTO -> mapGeneTOToGene(gTO, speciesMap.get(gTO.getSpeciesId()), null, null,
                                geneBioTypeMap.get(gTO.getGeneBioTypeId()))
                )));

        // Build mapping between given IDs and genes
        Map<String, Set<Gene>> mapAnyIdToGenes = mapMixedIdToBgeeGeneIds.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().stream().map(geneMap::get).collect(Collectors.toSet())));


        // Retrieve Genes that were not found using cross-reference IDs (i.e. Ensembl IDs)
        Set<String> notXRefIds = clnMixedGeneIDs.stream()
                    .filter(id -> !mapMixedIdToBgeeGeneIds.containsKey(id))
                    .collect(Collectors.toSet());
        mapAnyIdToGenes.putAll(this.loadGenesByEnsemblIds(notXRefIds, withSpeciesInfo)
                .collect(Collectors.groupingBy(Gene::getEnsemblGeneId,
                        Collectors.mapping(x -> x, Collectors.toSet()))));

        // Add provided IDs with no gene found
        clnMixedGeneIDs.removeAll(mapAnyIdToGenes.keySet());
        if (!clnMixedGeneIDs.isEmpty()) {
            for (String clnMixedGeneID : clnMixedGeneIDs) {
                mapAnyIdToGenes.put(clnMixedGeneID, new HashSet<>());
            }
        }
        // Build mapping between given IDs and genes
        return log.exit(mapAnyIdToGenes.entrySet().stream());
    }

    public Set<GeneBioType> loadGeneBioTypes() {
        log.entry();
        return log.exit(this.geneDAO.getGeneBioTypes()
                .stream().map(to -> mapGeneBioTypeTOToGeneBioType(to))
                .collect(Collectors.toSet()));
    }
    
    /**
     * Retrieve the mapping between a given set of cross-reference IDs. 
     * and Bgee gene IDs of the data source.
     * 
     * @param ids   A {@code Collection} of {@code String}s that are cross-reference IDs
     *              for which to return the mapping. If empty or {@code null}, an empty {@code Map}
     *              will be returned (and not all Bgee gene IDs in the database)
     * @return      The {@code Map} where keys are {@code String}s corresponding to provided IDs,
     *              and values are {@code Set} of {@code Integers}s that are the associated Bgee gene IDs.
     */
    private Map<String, Set<Integer>> loadMappingXRefIdToBgeeGeneIds(Collection<String> ids) {
        log.entry(ids);
        if (ids == null || ids.isEmpty()) {
            return log.exit(new HashMap<>());
        }
        
        Map<String, Set<Integer>> xRefIdToGeneIds = getDaoManager().getGeneXRefDAO()
                .getGeneXRefsByXRefIds(ids, Arrays.asList(GeneXRefDAO.Attribute.BGEE_GENE_ID, 
                        GeneXRefDAO.Attribute.XREF_ID)).stream()
                .collect(Collectors.groupingBy(GeneXRefTO::getXRefId,
                        Collectors.mapping(GeneXRefTO::getBgeeGeneId, Collectors.toSet())));
        return log.exit(xRefIdToGeneIds);
    }

    /**
     * @param geneTOs   A {@code Collection} of {@code GeneTO}s representing the genes for which
     *                  we want to retrieve the {@code Species}. If empty or {@code null}, an empty {@code Map}
     *                  will be returned (and not all {@code Species} in Bgee)
     * @return          A {@code Map} where keys are {@code Integer}s that are species IDs,
     *                  corresponding to the provided {@code GeneTO}s, the associated value
     *                  being the corresponding {@code Species}.
     */
    private Map<Integer, Species> loadSpeciesMap(Collection<GeneTO> geneTOs, boolean withSpeciesInfo) {
        log.entry(geneTOs, withSpeciesInfo);
        if (geneTOs == null || geneTOs.isEmpty()) {
            return log.exit(new HashMap<>());
        }
        Set<Integer> speciesIds = geneTOs.stream()
                .map(gTO -> {
                    if (gTO.getSpeciesId() == null) {
                        throw log.throwing(new IllegalArgumentException(
                                "The provided GeneTOs must contain species IDs"));
                    }
                    return gTO.getSpeciesId();
                })
                .collect(Collectors.toSet());
        return log.exit(this.speciesService.loadSpeciesMap(speciesIds, withSpeciesInfo));
    }

    /**
     * @param geneTOs   A {@code Collection} of {@code GeneTO}s representing the genes for which
     *                  we want to retrieve synonyms. If empty or {@code null}, an empty {@code Map}
     *                  will be returned (and not all synonyms for any gene in Bgee)
     * @return          A {@code Map} where keys are {@code Integer}s that are internal Bgee gene IDs,
     *                  corresponding to the provided {@code GeneTO}s, the associated value
     *                  being a {@code Set} of {@code String}s that are the corresponding synonyms.
     */
    private Map<Integer, Set<String>> loadSynonymsByBgeeGeneIds(Collection<GeneTO> geneTOs) {
        log.entry(geneTOs);
        if (geneTOs == null || geneTOs.isEmpty()) {
            return log.exit(new HashMap<>());
        }
        Set<Integer> bgeeGeneIds = geneTOs.stream()
                .map(gTO -> {
                    if (gTO.getId() == null) {
                        throw log.throwing(new IllegalArgumentException(
                                "The provided GeneTOs must contain Bgee gene IDs"));
                    }
                    return gTO.getId();
                })
                .collect(Collectors.toSet());
        return log.exit(this.getDaoManager().getGeneNameSynonymDAO()
                .getGeneNameSynonyms(bgeeGeneIds).stream()
                .collect(Collectors.groupingBy(GeneNameSynonymTO::getBgeeGeneId,
                        Collectors.mapping(GeneNameSynonymTO::getGeneNameSynonym, Collectors.toSet()))));
    }

    /**
     * @param geneTOs   A {@code Collection} of {@code GeneTO}s representing the genes for which
     *                  we want to retrieve XRef. If empty or {@code null}, an empty {@code Map}
     *                  will be returned (and not all XRefs for any gene in Bgee)
     * @return          A {@code Map} where keys are {@code Integer}s that are internal Bgee gene IDs,
     *                  corresponding to the provided {@code GeneTO}s, the associated value
     *                  being a {@code Set} of {@code GeneXRefTO}s that are the corresponding XRefs.
     */
    private Map<Integer, Set<GeneXRefTO>> loadXrefTOsByBgeeGeneIds(Collection<GeneTO> geneTOs) {
        log.entry(geneTOs);
        if (geneTOs == null || geneTOs.isEmpty()) {
            return log.exit(new HashMap<>());
        }

        Set<Integer> bgeeGeneIds = geneTOs.stream()
                .map(gTO -> {
                    if (gTO.getId() == null) {
                        throw log.throwing(new IllegalArgumentException(
                                "The provided GeneTOs must contain Bgee gene IDs"));
                    }
                    return gTO.getId();
                })
                .collect(Collectors.toSet());

        Set<GeneXRefTO> xRefTOs = this.getDaoManager().getGeneXRefDAO()
                .getGeneXRefsByBgeeGeneIds(bgeeGeneIds, null).stream()
                .collect(Collectors.toSet());

        return log.exit(xRefTOs.stream()
                .collect(Collectors.groupingBy(GeneXRefTO::getBgeeGeneId,
                        Collectors.toSet())));
    }

    private static GeneXRef mapGeneXRefTOToXRef(GeneXRefTO to, Map<Integer, Source> sourceMap,
            GeneTO geneTO, Map<Integer, Species> speciesMap) {
        log.entry(to, sourceMap, geneTO, speciesMap);
        return log.exit(new GeneXRef(to.getXRefId(), to.getXRefName(), sourceMap.get(to.getDataSourceId()), 
                geneTO.getGeneId(), speciesMap.get(geneTO.getSpeciesId()).getScientificName()));
    }

    private static Set<GeneXRef> getGeneXRefs(GeneTO to, Map<Integer, Set<GeneXRefTO>> xrefTOs,
            Map<Integer, Source> sourceMap, Map<Integer, Species> speciesMap) {
        log.entry(to, xrefTOs, sourceMap, speciesMap);
        if (sourceMap == null) {
            return log.exit(null);
        }
        Set<GeneXRef> xrefs = xrefTOs == null || xrefTOs.isEmpty()? new HashSet<>():
            Optional.ofNullable(xrefTOs.get(to.getId())).orElse(new HashSet<>()).stream()
                .map(xrefTO -> mapGeneXRefTOToXRef(xrefTO, sourceMap, to, speciesMap))
                .collect(Collectors.toSet());
        //We add the source genomic database to the XRef
        Species species = speciesMap.get(to.getSpeciesId());
        //(but only if the genome used is indeed the genome of the species considered,
        //and not the genome of a closely related species)
        if (species == null) {
            throw log.throwing(new IllegalArgumentException(
                    "Species Map does not contain species " + to.getSpeciesId()));
        }
        if (species.getId().equals(species.getGenomeSpeciesId())) {
            xrefs.add(new GeneXRef(to.getGeneId(), to.getName(), species.getGenomeSource(), to.getGeneId(),
                species.getScientificName()));
        }
        return log.exit(xrefs);
    }

    private static Stream<Gene> mapGeneTOStreamToGeneStream(Stream<GeneTO> geneTOStream,
            Map<Integer, Species> speciesMap, Map<Integer, Set<String>> synonyms,
            Map<Integer, Set<GeneXRefTO>> xrefTOs, Map<Integer, Source> sourceMap,
            Map<Integer, GeneBioType> geneBioTypeMap) {
        log.entry(geneTOStream, speciesMap, synonyms, xrefTOs, sourceMap, geneBioTypeMap);
        return log.exit(geneTOStream.map(to -> mapGeneTOToGene(to, speciesMap.get(to.getSpeciesId()),
                synonyms == null ? null : synonyms.get(to.getId()),
                        getGeneXRefs(to, xrefTOs, sourceMap, speciesMap),
                geneBioTypeMap.get(to.getGeneBioTypeId()))));
    }
}