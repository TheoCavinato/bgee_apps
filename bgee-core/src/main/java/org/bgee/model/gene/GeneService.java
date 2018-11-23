package org.bgee.model.gene;

import java.security.Provider.Service;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.CommonService;
import org.bgee.model.ServiceFactory;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneNameSynonymDAO.GeneNameSynonymTO;
import org.bgee.model.dao.api.gene.HierarchicalGroupDAO.HierarchicalNodeToGeneTOResultSet;
import org.bgee.model.species.Species;
import org.bgee.model.species.SpeciesService;

/**
 * A {@link Service} to obtain {@link Gene} objects. Users should use the
 * {@link org.bgee.model.ServiceFactory ServiceFactory} to obtain {@code GeneService}s.
 * 
 * @author  Philippe Moret
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Sep. 2018
 * @since   Bgee 13, Sep. 2015
 */
public class GeneService extends CommonService {
    private static final Logger log = LogManager.getLogger(GeneService.class.getName());
    
//    public class OrthologousGroupSpliterator<U extends OrthologousGeneGroup>
//    extends Spliterators.AbstractSpliterator<U> {
//
//    	private final Map<Integer, HierarchicalNodeTO> groupMap;
//    	private final HierarchicalGroupToGeneTOResultSet rs;
//    	
//    	private OrthologousGroupSpliterator(Map<Integer, HierarchicalNodeTO> groupMap, Map<Integer, Gene> geneMap,
//    			HierarchicalGroupToGeneTOResultSet rs) {
//    		
//    	}
//        @Override
//        public boolean tryAdvance(Consumer<? super U> action) {
//        	
//
//        	Map<Integer, Set<Integer>> nodeIdToGeneIds = new HashMap<>();
//        	Integer previousNodeId = null;
//            while (true) {
//            	boolean hasNext = rs.next();
//            	if (!hasNext) {
//            		return false;
//            	}
//            	
//            	HierarchicalGroupToGeneTO nodeToGeneTO = rs.getTO();
//            	Set<Integer> geneIds = nodeIdToGeneIds.computeIfAbsent(nodeToGeneTO.getNodeId(), k -> new HashSet<>());
//            	geneIds.add(nodeToGeneTO.getBgeeGeneId());
//            	
//            	previousNodeId = nodeToGeneTO.getNodeId();
//            }
//        	
//        	
//        	OrthologousGeneGroup group = null;
//        	action.accept((U) group);
//        }
//    }
    
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
        
        return log.exit(mapGeneTOStreamToGeneStream(
                this.geneDAO.getGenesBySpeciesAndGeneIds(filtersToMap).stream(),
                speciesMap, geneBioTypeMap));
    }

    /**
     * Loads {@code Gene}s from an Ensembl gene ID. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     * 
     * @param geneIds   A {@code String} that is the Ensembl ID of genes to retrieve.
     * @return          A {@code Set} of matching {@code Gene}s.
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
     * 
     * @param geneIds   A {@code Collection} of {@code String}s that are the Ensembl IDs
     *                  of genes to retrieve.
     * @return          A {@code Stream} of matching {@code Gene}s.
     */
    public Stream<Gene> loadGenesByEnsemblIds(Collection<String> ensemblGeneIds) {
        log.entry(ensemblGeneIds);
        return log.exit(this.loadGenesByEnsemblIds(ensemblGeneIds, false));
    }

    /**
     * Loads {@code Gene}s from an Ensembl gene ID. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     *
     * @param geneIds           A {@code String} that is the Ensembl ID of genes to retrieve.
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
        final Map<Integer, Species> speciesMap = Collections.unmodifiableMap(loadSpeciesMapFromGeneTOs(geneTOs, withSpeciesInfo));
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));
        
        return log.exit(mapGeneTOStreamToGeneStream(geneTOs.stream(), speciesMap, geneBioTypeMap)
                .collect(Collectors.toSet()));
    }

    /**
     * Loads {@code Gene}s from Ensembl gene IDs. Please note that in Bgee a same Ensembl gene ID
     * can correspond to several {@code Gene}s, belonging to different species. This is because
     * in Bgee, the genome of a species can be used for another closely-related species.
     * For instance, in Bgee the chimpanzee genome is used for analyzing bonobo data.
     * For unambiguous retrieval of {@code Gene}s, see {@link #loadGenes(Collection)}.
     *
     * @param geneIds           A {@code Collection} of {@code String}s that are the Ensembl IDs
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

        return log.exit(mapGeneTOStreamToGeneStream(
                this.geneDAO.getGenesByEnsemblGeneIds(ensemblGeneIds).stream(),
                speciesMap, geneBioTypeMap));
    	
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
     * Search the genes by name, id and synonyms.
     * @param term A {@code String} containing the query 
     * @return A {@code List} of results (ordered).
     */
    public List<GeneMatch> searchByTerm(final String term) {
        log.entry(term);
        
        List<GeneTO> geneTOs = this.geneDAO.getGeneBySearchTerm(term, null, 1, 100).stream().collect(Collectors.toList());
        
        // if result is empty, return an empty list
        if (geneTOs.isEmpty()) {
            return log.exit(new LinkedList<>());
        }
        
        final Map<Integer, Species> speciesMap = Collections.unmodifiableMap(loadSpeciesMapFromGeneTOs(geneTOs, false));
        final Map<Integer, GeneBioType> geneBioTypeMap = Collections.unmodifiableMap(loadGeneBioTypeMap(this.geneDAO));
        Set<Integer> bgeeGeneIds = geneTOs.stream().map(GeneTO::getId).collect(Collectors.toSet());
        
        final Map<Integer, List<String>> synonymMap = getDaoManager().getGeneNameSynonymDAO()
                .getGeneNameSynonyms(bgeeGeneIds).stream()
                .collect(Collectors.groupingBy(GeneNameSynonymTO::getBgeeGeneId, 
                        Collectors.mapping(GeneNameSynonymTO::getGeneNameSynonym, Collectors.toList())));

        return log.exit(geneTOs.stream()
            .map(g -> geneMatch(g, term, synonymMap.get(g.getId()), speciesMap.get(g.getSpeciesId()),
                    geneBioTypeMap.get(g.getGeneBioTypeId())))
            .collect(Collectors.toList()));
    }

    public Set<GeneBioType> loadGeneBioTypes() {
        log.entry();
        return log.exit(this.geneDAO.getGeneBioTypes()
                .stream().map(to -> mapGeneBioTypeTOToGeneBioType(to))
                .collect(Collectors.toSet()));
    }

    private GeneMatch geneMatch(final GeneTO geneTO, final String term,
            final List<String> synonymList, final Species species, final GeneBioType geneBioType) {
        log.entry(geneTO, term, synonymList, species, geneBioType);
        Gene gene = mapGeneTOToGene(geneTO, species, geneBioType);
        // if the gene name or id match there is no synonym
        if (geneTO.getName().toLowerCase().contains(term.toLowerCase())
                || String.valueOf(geneTO.getGeneId()).contains(term)) {
            return log.exit(new GeneMatch(gene, null));
        }

        // otherwise we fetch synonym and find the first match
        List<String> synonyms = synonymList.stream().
                filter(s -> s.toLowerCase().contains(term.toLowerCase()))
                .collect(Collectors.toList());
                
        if (synonyms.size() < 1) {
            throw new IllegalStateException("The term should match either the gene id/name "
                    + "or one of its synonyms. Term: " + term + " GeneTO;" + geneTO);
        }
        return log.exit(new GeneMatch(gene, synonyms.get(0)));
    }

    private Map<Integer, Species> loadSpeciesMapFromGeneTOs(Collection<GeneTO> geneTOs, boolean withSpeciesInfo) {
        log.entry(geneTOs, withSpeciesInfo);
        Set<Integer> speciesIds = geneTOs.stream().map(GeneTO::getSpeciesId).collect(Collectors.toSet());
        return log.exit(this.speciesService.loadSpeciesMap(speciesIds, withSpeciesInfo));
    }
    
    private static Stream<Gene> mapGeneTOStreamToGeneStream(Stream<GeneTO> geneTOStream,
            Map<Integer, Species> speciesMap, Map<Integer, GeneBioType> geneBioTypeMap) {
        log.entry(geneTOStream, speciesMap, geneBioTypeMap);
        return log.exit(geneTOStream.map(to -> mapGeneTOToGene(to, speciesMap.get(to.getSpeciesId()),
                geneBioTypeMap.get(to.getGeneBioTypeId()))));
    }
}
