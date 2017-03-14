package org.bgee;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.Call.ExpressionCall.ClusteringMethod;
import org.bgee.model.expressiondata.CallData.ExpressionCallData;
import org.bgee.model.expressiondata.CallFilter.ExpressionCallFilter;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.expressiondata.ConditionFilter;
import org.bgee.model.expressiondata.baseelements.CallType.Expression;
import org.bgee.model.expressiondata.baseelements.DataPropagation;
import org.bgee.model.expressiondata.baseelements.DataQuality;
import org.bgee.model.expressiondata.baseelements.DataPropagation.PropagationState;
import org.bgee.model.expressiondata.baseelements.DataType;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneFilter;
import org.bgee.model.ontology.Ontology;
import org.bgee.model.ontology.Ontology.RelationType;
import org.bgee.model.species.Species;
import org.junit.Test;

public class TestTest extends TestAncestor {

	private final static Logger log = LogManager.getLogger(TestTest.class.getName());
	private final static boolean FILTER_ON_CLUSTERING_RESULTS = false;
	private final static boolean MANAGE_GENE_PROPAGATION = false;
	private final static int MAX_PROPAGATION_LEVEL = 4;
	private final static int CLUSTER_RANK_LIMIT = 0;
	private final static int LIMIT_NUMBER_OF_ANAT_ENTITIES = 2;
	private final static DataType DATATYPE = null;
	private final static PropagationState ANAT_ENTITY_PROPAGATION = PropagationState.SELF;
	private final static PropagationState DEV_STAGE_PROPAGATION = PropagationState.SELF;
	private final static String FILE_PATH = "src/test/data/";
	private final static Double CLUSTER_THRESHOLD=5.0;
	private final static int MAX_NUMBER_GROUP_SPECIFIC=7;
	private final static int HOUSEKEEPING_GENE_NUMBER = 170;
	private final static boolean HIGH_QUALITY = true;
	private final static boolean USE_ONTOLOGY_STRUCTURE_FOR_GROUP = true;

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Test
	public void testGetAnatEntity() {
		ServiceFactory factory = new ServiceFactory();
		log.debug(factory.getAnatEntityService().loadAnatEntityById("UBERON:0000178"));
		factory.close();
	}

	@Test
	public void testGetSpecies() {
		ServiceFactory factory = new ServiceFactory();
		// log.debug(factory.getGeneService().loadGeneById("ENSG00000244734"));
		Set<Species> speciesSet = factory.getSpeciesService().loadSpeciesInDataGroups(true);// loadSpeciesByIds(Collections.singleton("9606"),
																							// false));
		for (Species species : speciesSet) {
			log.debug(species.getId() + "\t" + species.getName());
		}
		factory.close();
	}
	
	@Test
	public void testTissueSpecificityBasedOnRankComparison() throws PageNotFoundException, IOException{
		Instant startTime = Instant.now();
		String speciesID = "10090";
//		Set<String> anatEntitiesIDs = Collections.singleton("UBERON:0000955");//,"UBERON:0000473","UBERON:0000956","UBERON:0000473","UBERON:0003889","UBERON:0002107","UBERON:0001987","UBERON:0001132","UBERON:0002108","UBERON:0002106","UBERON:0000473","UBERON:0014895","UBERON:0000014","UBERON:0001301","UBERON:0002113","UBERON:0002371","UBERON:0000029","UBERON:0001043","UBERON:0002369","UBERON:0002046","UBERON:0001154","UBERON:0002349","UBERON:0002372","UBERON:0002048","UBERON:0001052","UBERON:0002367","UBERON:0001013","UBERON:0001155","UBERON:0000945","UBERON:0000002","UBERON:0002110","UBERON:0000310","UBERON:0000998","UBERON:0000992","UBERON:0001295","UBERON:0001135","UBERON:0001044","UBERON:0001264","UBERON:0001255"));
		//test housekeeping genes with high level anat entity element anatomical structure (UBERON:0000061)
		Set<String> anatEntitiesIDs = new HashSet<String>(Collections.singleton("UBERON:0000061"));
		for(String anatEntityID:anatEntitiesIDs){	
			ServiceFactory serviceFactory = new ServiceFactory();
			//an ontology containing parents and descendants terms linked by IS_A or PART_OF relationships
			Ontology<AnatEntity> linkedElementsOntology = serviceFactory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(speciesID)), new ArrayList<String>(Collections.singleton(anatEntityID)), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), true, true, serviceFactory.getAnatEntityService());
			//retrieve anatEntity object
			AnatEntity anatEntity = serviceFactory.getAnatEntityService().loadAnatEntityById(anatEntityID);
			if (anatEntity == null) {
				serviceFactory.close();
				throw log.throwing(new PageNotFoundException("No anatEntity corresponding to " + anatEntityID));
			}
			//retrieve species object
			Species species = serviceFactory.getSpeciesService()
					.loadSpeciesByIds(Collections.singleton(speciesID), false).iterator().next();
			if (species == null) {
				serviceFactory.close();
				throw log.throwing(new PageNotFoundException("No species corresponding to " + speciesID));
			}
			CallService callService = serviceFactory.getCallService();
			//define condition filter (use anatEntityID)
			Collection<ConditionFilter> condFilterCollection = new ArrayList<>();
			condFilterCollection.add(new ConditionFilter(Collections.singleton(anatEntity.getId()), null));
			List<ExpressionCall> expressionCallWithBestRank = null;
			if(HIGH_QUALITY){
				expressionCallWithBestRank = new ArrayList<ExpressionCall>(
				retrieveExprCallsForAnatEntities(callService, species.getId(), condFilterCollection)
				.filter(s -> s.getSummaryQuality().compareTo(DataQuality.HIGH) == 0)
				//filter on best ranked expression call for each gene. Don't take into account development stage
				.collect(Collectors.toMap(p -> p.getGeneId(), p -> p, (v1, v2) -> v1, LinkedHashMap::new))
				.values());
			}else{
				expressionCallWithBestRank = new ArrayList<ExpressionCall>(
						retrieveExprCallsForAnatEntities(callService, species.getId(), condFilterCollection)
						//filter on best ranked expression call for each gene. Don't take into account development stage
						.collect(Collectors.toMap(p -> p.getGeneId(), p -> p, (v1, v2) -> v1, LinkedHashMap::new))
						.values());
			}
			log.debug("Size of map with only best ranked expression call for each gene : "+ expressionCallWithBestRank.size());
			Map<String,Gene> genes = serviceFactory.getGeneService().loadGenesByIdsAndSpeciesIds(
					expressionCallWithBestRank.stream().map(p -> p.getGeneId()).collect(Collectors.toList()),
					Collections.singletonList(speciesID)).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
			if (genes == null) {
				serviceFactory.close();
				throw log.throwing(new PageNotFoundException("No species corresponding to " + speciesID));
			}
			List<String> tissueEnhanced = new ArrayList<>();
			List<String> housekeeper = new ArrayList<>();
			List<String> undefined = new ArrayList<>();
			List<String> grouppecific = new ArrayList<>();
			List<String> tissuepecific = new ArrayList<>();
			genes.values().parallelStream().forEach(gene -> {
				LinkedHashMap<CallService.OrderingAttribute, Service.Direction> serviceOrdering = new LinkedHashMap<>();
				serviceOrdering.put(CallService.OrderingAttribute.GLOBAL_RANK, Service.Direction.ASC);
				// list of expression call, ordered by anat entity
				LinkedHashMap<String, ExpressionCall> callsByAnatEntityId = null;
				if(HIGH_QUALITY){
				callsByAnatEntityId = callService
						.loadExpressionCalls(gene.getSpeciesId(),
								new ExpressionCallFilter(new GeneFilter(gene.getId()), null, new DataPropagation(ANAT_ENTITY_PROPAGATION,DEV_STAGE_PROPAGATION),
										Arrays.asList(new ExpressionCallData(Expression.EXPRESSED,DATATYPE))),
								EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID,
										CallService.Attribute.DEV_STAGE_ID, CallService.Attribute.GLOBAL_DATA_QUALITY, CallService.Attribute.GLOBAL_RANK),
								serviceOrdering)
						.filter(s -> s.getSummaryQuality().compareTo(DataQuality.HIGH) == 0)
						// return best ranked expression call for each anat entity. Don't take into account devStage
						.collect(Collectors.toMap(p -> p.getCondition().getAnatEntityId(), p -> p, (v1, v2) -> v1,
								LinkedHashMap::new));
				}else{
					callsByAnatEntityId = callService
							.loadExpressionCalls(gene.getSpeciesId(),
									new ExpressionCallFilter(new GeneFilter(gene.getId()), null, new DataPropagation(ANAT_ENTITY_PROPAGATION,DEV_STAGE_PROPAGATION),
											Arrays.asList(new ExpressionCallData(Expression.EXPRESSED,DATATYPE))),
									EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID,
											CallService.Attribute.DEV_STAGE_ID, CallService.Attribute.GLOBAL_DATA_QUALITY, CallService.Attribute.GLOBAL_RANK),
									serviceOrdering)
							// return best ranked expression call for each anat entity. Don't take into account devStage
							.collect(Collectors.toMap(p -> p.getCondition().getAnatEntityId(), p -> p, (v1, v2) -> v1,
									LinkedHashMap::new));
				}
				log.debug(callsByAnatEntityId.get(anatEntityID));
				List<ExpressionCall> calls = new ArrayList<>(callsByAnatEntityId.values());
				ExpressionCall anatEntityCall = callsByAnatEntityId.get(anatEntityID);
				if(calls!=null&&!calls.isEmpty()){//&&anatEntityCall!=null){
					//if we only have one anat. entity with gene expression, we considerate it tissue specific with a ratio of Integer.MAX_VALUE
					if(calls.size()==1){
						tissuepecific.add(anatEntityCall.getGeneId() + "\t" + gene.getName() + "\t" + anatEntityCall.getFormattedGlobalMeanRank() + "\t" + Integer.MAX_VALUE
								+ "\t" + gene.getDescription() + "\t" +anatEntityCall.getSummaryQuality());
					}else{
						boolean findCategory = false;
						//remove all elements coming from the parent/descendant ontology of the selected anat. entity
						calls = calls.stream()
						.filter(s -> linkedElementsOntology.getElement(s.getCondition().getAnatEntityId())==null||s.getCondition().getAnatEntityId().equals(anatEntityID))
						.collect(Collectors.toList());
						ExpressionCall firstCall = calls.get(0);
						// search for tissue specific genes
						if(calls.size()==1){
							tissuepecific.add(firstCall.getGeneId() + "\t" + gene.getName() + "\t" + firstCall.getFormattedGlobalMeanRank() + "\t" + Integer.MAX_VALUE
									+ "\t" + gene.getDescription()+ "\t" +anatEntityCall.getSummaryQuality());
						}else{
							log.debug("calls size : "+calls.size());
							BigDecimal ratio = null;
							boolean findAnatEntity = false;
							if(firstCall.equals(anatEntityCall)){
								findAnatEntity=true;
								ratio = calls.get(1).getGlobalMeanRank().divide(firstCall.getGlobalMeanRank(),1, RoundingMode.HALF_UP);
								if(ratio.compareTo(new BigDecimal(5))>0){
									log.debug("compare "+firstCall.getGeneId()+ " -> "+anatEntityCall.getGeneId()+" -> "+firstCall.getGlobalMeanRank());
									tissuepecific.add(firstCall.getGeneId() + "\t" + gene.getName() + "\t" + firstCall.getFormattedGlobalMeanRank() + "\t" + ratio
											+ "\t" + gene.getDescription()+ "\t" +firstCall.getSummaryQuality());
									findCategory = true;
									firstCall.getSummaryQuality();
								}
							}
							//search for group specific genes
							if(!findCategory){
								int currentAnatEntityPosition = 1;
								ratio = null;
								while(currentAnatEntityPosition< MAX_NUMBER_GROUP_SPECIFIC){
									ExpressionCall currentAnatEntity = calls.get(currentAnatEntityPosition);
									if(USE_ONTOLOGY_STRUCTURE_FOR_GROUP){
										final Ontology <AnatEntity> currentAnatEntityOntology = serviceFactory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(speciesID)), new ArrayList<String>(Collections.singleton(currentAnatEntity.getCondition().getAnatEntityId())), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), true, true, serviceFactory.getAnatEntityService());
										calls = calls.stream()
												.filter(s -> currentAnatEntityOntology.getElement(s.getCondition().getAnatEntityId())==null||s.getCondition().getAnatEntityId().equals(anatEntityID)||s.getCondition().getAnatEntityId().equals(currentAnatEntity.getCondition().getAnatEntityId()))
												.collect(Collectors.toList());
									}
									
									if(calls.size()>(currentAnatEntityPosition+1)){
										ExpressionCall nextAnatEntity= calls.get(currentAnatEntityPosition+1);
										if(!findAnatEntity){
											if(currentAnatEntity.equals(anatEntityCall)){
												findAnatEntity=true;
											}
										}
										if(findAnatEntity){
											BigDecimal currentRatio = nextAnatEntity.getGlobalMeanRank().divide(currentAnatEntity.getGlobalMeanRank(),1, RoundingMode.HALF_UP);
											if(currentRatio.compareTo(new BigDecimal(5))>=0&&(ratio==null||currentRatio.compareTo(ratio)>0)){
												ratio=currentRatio;
												findCategory = true;
											}
										}
									}else{
										if(ratio == null){ // if there is less than 7 anat. entities associated to this gene, this gene is tag as group specific.
											ratio = new BigDecimal(Integer.MAX_VALUE);
											findCategory = true;
											findAnatEntity=true;
											currentAnatEntityPosition = MAX_NUMBER_GROUP_SPECIFIC;
										}else{
											findCategory = true;
											currentAnatEntityPosition = MAX_NUMBER_GROUP_SPECIFIC;
										}
									}
									currentAnatEntityPosition++;
								}
								if(findCategory){
									grouppecific.add(gene.getId() + "\t" + gene.getName() + "\t" + anatEntityCall.getFormattedGlobalMeanRank() + "\t" + ratio
											+ "\t" + gene.getDescription()+ "\t" +anatEntityCall.getSummaryQuality());
								}
							}
							if(!findCategory){
								String outputString = testTissueEnhanced(calls, anatEntityCall, gene);
								
								if(calls.size()>HOUSEKEEPING_GENE_NUMBER&&calls.get(calls.size()/2).getGlobalMeanRank().compareTo(calls.get(0).getGlobalMeanRank().multiply(new BigDecimal(10))) < 0){
									housekeeper.add(gene.getId() + "\t" + gene.getName() + "\t" + anatEntityCall.getFormattedGlobalMeanRank() +  "\t" + gene.getDescription());
								}else if(outputString != null){
									tissueEnhanced.add(outputString);
								}else{
									undefined.add(gene.getId() + "\t" + gene.getName() + "\t" + anatEntityCall.getFormattedGlobalMeanRank() +  "\t" + gene.getDescription());
								}
							}
						}
					}
				}
			});
			writeOutputTissueSpe("TISSUESPE_", tissuepecific, species.getName(), anatEntity.getName(),true);
			writeOutputTissueSpe("GROUPSPE_", grouppecific, species.getName(), anatEntity.getName(),true);
			writeOutputTissueSpe("UNDEFINED_", undefined, species.getName(), anatEntity.getName(),false);
			writeOutputTissueSpe("TISSUEENHANCED_", tissueEnhanced, species.getName(), anatEntity.getName(),false);
			writeOutputTissueSpe("HOUSEKEEPER", housekeeper, species.getName(), anatEntity.getName(),false);
			serviceFactory.close();
			Instant endTime = Instant.now();
			log.debug("execution time : "+Duration.between(startTime, endTime).getSeconds());
		}
	}

	private String testGroupSpecificity(List<ExpressionCall> calls,Gene gene, String anatEntityId, ServiceFactory factory){
//		ServiceFactory factory = new ServiceFactory();
		int groupNumber = 2;
		while (groupNumber <= MAX_NUMBER_GROUP_SPECIFIC){
			
			//need at least groupNumber+1 elements in order to compare rank of groupNumber element to rank of element goroupNumber+1
			if(calls.size()>groupNumber+2){
				ExpressionCall currentCall = calls.get(groupNumber-1);
				ExpressionCall nextCall = calls.get(groupNumber);
				log.debug(currentCall.getCondition().getAnatEntityId()+"\t"+gene.getSpeciesId());
				Ontology<AnatEntity> currentCallOntology = factory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(gene.getSpeciesId())), new ArrayList<String>(Collections.singleton(currentCall.getCondition().getAnatEntityId())), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), true, true, factory.getAnatEntityService());
				calls = calls.stream().filter(s -> currentCallOntology.getElement(s.getCondition().getAnatEntityId())==null||s.getCondition().getAnatEntityId().equals(anatEntityId)).collect(Collectors.toList());
				if(currentCall.getGlobalMeanRank().multiply(new BigDecimal(CLUSTER_THRESHOLD)).compareTo(nextCall.getGlobalMeanRank()) < 0){
//					factory.close();
					return currentCall.getGeneId() + "\t" + gene.getName() + "\t" + currentCall.getFormattedGlobalMeanRank() + "\t" + calls.get(groupNumber).getGlobalMeanRank().divide(currentCall.getGlobalMeanRank(),1, RoundingMode.HALF_UP)
							+ "\t" + gene.getDescription();
				}
			}
			groupNumber++;
		}
//		factory.close();
		return null;
	}
	
	private String testTissueEnhanced(List<ExpressionCall> calls, ExpressionCall anatEntityCall, Gene gene){
		BigDecimal average = calls
	            .stream()
	            .map(s -> s.getGlobalMeanRank())
	            .reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(calls.size()),1, RoundingMode.HALF_UP);
		log.debug(" mean : "+average+", anat. entity rank : "+anatEntityCall.getGlobalMeanRank()+" for "+gene.getId());
		BigDecimal anatEntityRank = anatEntityCall.getGlobalMeanRank();
		if(average.compareTo(anatEntityRank.multiply(new BigDecimal(CLUSTER_THRESHOLD)))>0){
			return gene.getId() + "\t" + gene.getName() + "\t" + anatEntityCall.getFormattedGlobalMeanRank() + "\t" + gene.getDescription();
		}
		return null;
		
	}
	
	private void writeOutputTissueSpe(String filePrefix, List<String> output, String speciesName, String anatEntityName, boolean tissueSpeColumn) throws IOException{
		if(!output.isEmpty()){
			String columnsNames = "gene_ID" + "\t" + "gene_name" + "\t" + "rank_value" + "\t";
			columnsNames += tissueSpeColumn ? "tissue spe"+ "\t" + "gene_definition" : "gene_definition";
			output.add(0,columnsNames);
			String fileName = FILE_PATH + filePrefix + "_" + speciesName.replace(" ", "_") + "_"
					+ anatEntityName.replace(" ", "_");//+"_"+CLUSTER_THRESHOLD.toString()+ "_"+ANAT_ENTITY_PROPAGATION+"_"+DEV_STAGE_PROPAGATION;
			if(DATATYPE!=null){
				fileName +="_"+DATATYPE.toString();
			}if(HIGH_QUALITY){
				fileName +="_"+"HIGH";
			}if(USE_ONTOLOGY_STRUCTURE_FOR_GROUP){
				fileName +="_"+"DES";
			}
			fileName+=".tsv";
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)))) {
				writer.write(String.join("\n", output));
			} catch (IOException e){
				throw log.throwing(new IOException("Can't write output file "+fileName+" \n"+e.getMessage()));
			}			
		}
	}

	
	private Boolean filterOnClusters(Gene gene, String anatEntityId, CallService callService, Ontology<AnatEntity> descendants) {
		LinkedHashMap<CallService.OrderingAttribute, Service.Direction> serviceOrdering = new LinkedHashMap<>();
		serviceOrdering.put(CallService.OrderingAttribute.GLOBAL_RANK, Service.Direction.ASC);
		// list of expression call, ordered by anat entity
		LinkedHashMap<String, ExpressionCall> callsByAnatEntityId = callService
				.loadExpressionCalls(gene.getSpeciesId(),
						new ExpressionCallFilter(new GeneFilter(gene.getId()), null, new DataPropagation(ANAT_ENTITY_PROPAGATION,DEV_STAGE_PROPAGATION),
								Arrays.asList(new ExpressionCallData(Expression.EXPRESSED,DATATYPE))),
						EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID,
								CallService.Attribute.DEV_STAGE_ID, CallService.Attribute.GLOBAL_DATA_QUALITY, CallService.Attribute.GLOBAL_RANK),
						serviceOrdering)
				// return best ranked expression call for each anat entity
				// mapped by anat entity
				.collect(Collectors.toMap(p -> p.getCondition().getAnatEntityId(), p -> p, (v1, v2) -> v1,
						LinkedHashMap::new));
		// define clustering method
		Function<List<ExpressionCall>, Map<ExpressionCall, Integer>> clusteringFunction = getClusteringFunction();
		// Store a clustering of ExpressionCalls, by considering only one best ExpressionCall from each anatomical entity (anat entity part of first cluster of gene expression)
		Map<String, Integer> clusteringBestEachAnatEntity = clusteringFunction
				.apply(new ArrayList<ExpressionCall>(callsByAnatEntityId.values()))
				.entrySet().stream()
				//filter on cluster number
				.filter(map -> map.getValue() <= CLUSTER_RANK_LIMIT)
				//create a Map like anatEntityID -> cluster rank
				.collect(Collectors.toMap(e -> e.getKey().getCondition().getAnatEntityId(), e -> e.getValue()));
		if(MANAGE_GENE_PROPAGATION){
			clusteringBestEachAnatEntity = clusteringBestEachAnatEntity.entrySet().stream().filter(s -> descendants.getElement(s.getKey())==null||s.getKey().equals(anatEntityId)).collect(Collectors.toMap(s -> s.getKey(), s -> s.getValue()));
		}
		if (clusteringBestEachAnatEntity.containsKey(anatEntityId)) {
			log.debug("number of anat entities in first cluster : "+clusteringBestEachAnatEntity.size()+" for gene "+gene.getId());
			if (clusteringBestEachAnatEntity.size() < LIMIT_NUMBER_OF_ANAT_ENTITIES){
				return log.exit(true);
			}
		}
		log.debug("gene "+gene.getId()+" is expressed in selected anat entity but not more expressed than in other anat entities");
		return log.exit(false);
	}
	
	
	/**
	 * Method used to test different filter approach for the creation of the anat entity page.
	 * These filters are :
	 * - Return all genes for one anat. entity
	 * - Return all genes for one anat. entity but only when this anat entity is present in the first X clusters of expression for this gene AND when this cluster don’t contain more than Y anat. entities
	 * - previous one + test different data propagation for anat. entities (not currently used)
	 * - previous one + test different data propagation for development stage (not currently used)
	 * - previous one + filter on data type (i.e IN_SITU)
	 * - previous one + take into account arborescence of the anat. entity ontology in order to remove all descendant elements from the cluster list
	 * 
	 * @throws PageNotFoundException
	 * @throws IOException
	 */
	@Test
	public void testAnatEntityWithFilter() throws PageNotFoundException, IOException {
		//init all parameters of anat entity retrieval
		Instant startTime = Instant.now();
		//species and anat. entities to retrieve
		List<String> speciesIDs = Collections.singletonList("9606");
		List<String> anatEntitiesIDs = Collections.singletonList("UBERON:0002114");
// 		List<String> speciesIDs = Arrays.asList("9606","10090");
//		List<String> anatEntitiesIDs =
//		Arrays.asList("UBERON:0000451","UBERON:0002021","UBERON:0001954","UBERON:0001898","UBERON:2007003");
//		List<String> speciesIDs =
//		Arrays.asList("9606",/**"10090,"*/"8364","7955","9598","7227","6239");
//		List<String> anatEntitiesIDs =
//		Arrays.asList("UBERON:0000955","UBERON:0002037","UBERON:0002107","UBERON:0000948","UBERON:0000178");
		//loop over all species and anat. entities
		for (String speciesID : speciesIDs) {
			for (String anatEntityID : anatEntitiesIDs) {
				ServiceFactory serviceFactory = new ServiceFactory();
				//create ontology with all descendants of the anatEntity
				Ontology<AnatEntity> descendantsOntology = serviceFactory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(speciesID)), new ArrayList<String>(Collections.singleton(anatEntityID)), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), true, true, serviceFactory.getAnatEntityService());
				log.debug("species_ID : " + speciesID + " & anatEntity_ID : " + anatEntityID);
				//retrieve anatEntity object
				AnatEntity anatEntity = serviceFactory.getAnatEntityService().loadAnatEntityById(anatEntityID);
				if (anatEntity == null) {
					serviceFactory.close();
					throw log.throwing(new PageNotFoundException("No anatEntity corresponding to " + anatEntityID));
				}
				//retrieve species object
				Species species = serviceFactory.getSpeciesService()
						.loadSpeciesByIds(Collections.singleton(speciesID), false).iterator().next();
				if (species == null) {
					serviceFactory.close();
					throw log.throwing(new PageNotFoundException("No species corresponding to " + speciesID));
				}
				CallService callService = serviceFactory.getCallService();
				//define condition filter (use anatEntityID)
				Collection<ConditionFilter> condFilterCollection = new ArrayList<>();
				condFilterCollection.add(new ConditionFilter(Collections.singleton(anatEntity.getId()), null));
				// return list of best ranked ExpressionCall for each gene
				List<ExpressionCall> expressionCallWithBestRank = 
						new ArrayList<ExpressionCall>(
				retrieveExprCallsForAnatEntities(callService, species.getId(), condFilterCollection)
				//filter on best ranked expression call for each gene
						.collect(Collectors.toMap(p -> p.getGeneId(), p -> p, (v1, v2) -> v1, LinkedHashMap::new))
						.values());
				log.debug("expression call Map with only first expression call size : "+ expressionCallWithBestRank.size());
				//retrieve list of all genes expressed in this anatEntity
				Map<String,Gene> genes = serviceFactory.getGeneService().loadGenesByIdsAndSpeciesIds(
						expressionCallWithBestRank.stream().map(p -> p.getGeneId()).collect(Collectors.toList()),
						Collections.singletonList(speciesID)).stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
				if (genes == null) {
					serviceFactory.close();
					throw log.throwing(new PageNotFoundException("No species corresponding to " + speciesID));
				}
				log.debug("finish uniqueCalls");
				log.debug("redundant expression calls number " + expressionCallWithBestRank.size());

				// format output
				String output = "gene_ID" + "\t" + "gene_name" + "\t" + "rank_value" + "\t" + "gene_definition\n";
				int outputInitialLength = output.length();
				for (ExpressionCall call : expressionCallWithBestRank) {
					Gene gene = genes.get(call.getGeneId());
					//filter or not on anat entity expression for the gene (use or not cluster of gene expression)
					if(!FILTER_ON_CLUSTERING_RESULTS){
						output += call.getGeneId() + "\t" + gene.getName() + "\t" + call.getFormattedGlobalMeanRank()
								+ "\t" + gene.getDescription() + "\n";
					}else if(filterOnClusters(gene, anatEntityID, callService,descendantsOntology)){
						output += call.getGeneId() + "\t" + gene.getName() + "\t" + call.getFormattedGlobalMeanRank()
						+ "\t" + gene.getDescription() + "\n";
					}
				}
				// write output file
				writeOutputFile(outputInitialLength, output, species.getName(), anatEntity.getName());
				serviceFactory.close();
			}
		}
		Instant endTime = Instant.now();
		log.debug("execution time : "+Duration.between(startTime, endTime).getSeconds());
	}
	
	@Test
	public void testGeneRankPropagation() throws PageNotFoundException, IOException {
		//init all parameters of anat entity retrieval
		Instant startTime = Instant.now();
		//species and anat. entities to retrieve
		String speciesID = "9606";
		String anatEntityID = "UBERON:0000955";
		ServiceFactory serviceFactory = new ServiceFactory();
		//create ontology with all descendants of the anatEntity
		Ontology<AnatEntity> descendantsOntology = serviceFactory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(speciesID)), new ArrayList<String>(Collections.singleton(anatEntityID)), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), false, true, serviceFactory.getAnatEntityService());
		//allows to retrieve descendants and relations between them
		//TODO remove this line after merging. New version of bgee doesn't reauire it
		descendantsOntology = serviceFactory.getOntologyService().getAnatEntityOntology(new ArrayList<String>(Collections.singleton(speciesID)), descendantsOntology.getElements().stream().map(p -> p.getId()).collect(Collectors.toList()), new ArrayList<RelationType>(Collections.singleton(RelationType.ISA_PARTOF)), false, false, serviceFactory.getAnatEntityService());
		if (descendantsOntology == null) {
			serviceFactory.close();
			throw log.throwing(new PageNotFoundException("No ontology of descendants corresponding to " + anatEntityID +" for species " +speciesID));
		}
		//retrieve anatEntity object
		AnatEntity anatEntity = serviceFactory.getAnatEntityService().loadAnatEntityById(anatEntityID);
		if (anatEntity == null) {
			serviceFactory.close();
			throw log.throwing(new PageNotFoundException("No anatEntity corresponding to " + anatEntityID));
		}
		CallService callService = serviceFactory.getCallService();
		Set<AnatEntity> propagatedAnatEntities = manageGeneRankPropagation(anatEntity, descendantsOntology);
		//define condition filter (use anatEntityID)
		Collection<ConditionFilter> condFilterCollection = new ArrayList<>();
		condFilterCollection.add(new ConditionFilter(propagatedAnatEntities.stream().map(p -> p.getId()).collect(Collectors.toList()), null));
		log.debug("time before expression call "+Duration.between(startTime, Instant.now()).getSeconds());
		Map<String,List<ExpressionCall>> expressionCallWithBestRank = retrieveExprCallsForAnatEntities(callService, speciesID, condFilterCollection).collect(Collectors.groupingBy(ExpressionCall::getGeneId));
		log.debug(expressionCallWithBestRank.size());
		Instant endTime = Instant.now();
		log.debug("execution time : "+Duration.between(startTime, endTime).getSeconds());

	}
	
	private Stream<ExpressionCall> retrieveExprCallsForAnatEntities(CallService callService, String speciesId, Collection<ConditionFilter> condFilterCollection){
		//define ordering approach
		LinkedHashMap<CallService.OrderingAttribute, Service.Direction> serviceOrdering = new LinkedHashMap<>();
		serviceOrdering.put(CallService.OrderingAttribute.GLOBAL_RANK, Service.Direction.ASC);
		return callService.loadExpressionCalls(speciesId,
				new ExpressionCallFilter(null, condFilterCollection, new DataPropagation(ANAT_ENTITY_PROPAGATION,DEV_STAGE_PROPAGATION), 
						Arrays.asList(new ExpressionCallData(Expression.EXPRESSED, DATATYPE))),
				EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID,
						CallService.Attribute.DEV_STAGE_ID, CallService.Attribute.GLOBAL_DATA_QUALITY, CallService.Attribute.GLOBAL_RANK),
				serviceOrdering);
	}
	
	private Set<AnatEntity> manageGeneRankPropagation(AnatEntity anatEntity, Ontology<AnatEntity> descendantsOntology){
		Set<AnatEntity> propagationAnatEntities = new HashSet<AnatEntity>();
		int currentPropagationLevel = 0;
		Set<AnatEntity> newPropagations = new HashSet<AnatEntity>();
		Set<AnatEntity> temp = new HashSet<AnatEntity>();
		newPropagations.add(anatEntity);
		while (currentPropagationLevel < MAX_PROPAGATION_LEVEL){
			if(newPropagations.size()==0){
				currentPropagationLevel = MAX_PROPAGATION_LEVEL;
			}else{
//				log.debug("prop level "+currentPropagationLevel);
				for(AnatEntity anat:newPropagations){
					temp.addAll(descendantsOntology.getDescendants(anat, true));	
//					log.debug(descendantsOntology.getElements().size()+" -> "+anat.getId()+" -> "+temp.size()+" -> "+newPropagations.size()+" -> "+propagationAnatEntities.size());
				}
				if(++currentPropagationLevel==MAX_PROPAGATION_LEVEL){
//					log.debug("ended for loop and max prop");
					propagationAnatEntities.addAll(temp);
					temp.clear();
				}else{
					if(currentPropagationLevel!=1){
						propagationAnatEntities.addAll(newPropagations);
					}
//					log.debug("ended for loop and not max prop");
					newPropagations.clear();
					newPropagations.addAll(temp);
					temp.clear();
				}
			}
			
		}
		log.debug("end of ontology propagation. It returned : "+propagationAnatEntities.size()+" elements");
		return propagationAnatEntities;
	}
	
	private void writeOutputFile(int outputInitialLength, String output, String speciesName, String anatEntityName) throws IOException{
		if(output.length() > outputInitialLength){
			String fileName = FILE_PATH + speciesName.replace(" ", "_") + "_"
					+ anatEntityName.replace(" ", "_");// + "_"+ANAT_ENTITY_PROPAGATION+"_"+DEV_STAGE_PROPAGATION;
			if(DATATYPE!=null){
				fileName +="_"+DATATYPE.toString();
			}
			if(FILTER_ON_CLUSTERING_RESULTS){
				fileName +="_"+(CLUSTER_RANK_LIMIT+1)+"_"+(LIMIT_NUMBER_OF_ANAT_ENTITIES)+"_"+CLUSTER_THRESHOLD.toString();
			}if(MANAGE_GENE_PROPAGATION){
				fileName +="_DES";
			}
			fileName+=".tsv";
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)))) {
				writer.write(output);
			} catch (IOException e){
				throw log.throwing(new IOException("Can't write output file "+fileName+" \n"+e.getMessage()));
			}			
		}
	}
	
	

	private Function<List<ExpressionCall>, Map<ExpressionCall, Integer>> getClusteringFunction()
			throws IllegalStateException {
		log.entry();
		// init clustering method and clustering threshold with default values
		String clusteringMethod = BgeeProperties.GENE_SCORE_CLUSTERING_METHOD_DEFAULT;
		Double ClusteringThreshold = CLUSTER_THRESHOLD;//BgeeProperties.GENE_SCORE_CLUSTERING_THRESHOLD_DEFAULT;
		if (clusteringMethod == null) {
			throw log.throwing(new IllegalStateException("No clustering method specified."));
		}
		// Distance threshold
		if (ClusteringThreshold == null ||
		// we don't want negative nor near-zero values
				ClusteringThreshold < 0.000001) {
			throw log.throwing(new IllegalStateException("A clustering method was specified, "
					+ "but no distance threshold or incorrect threshold value assigned."));
		}
		try {
			// find clustering method
			final ClusteringMethod method = ClusteringMethod.valueOf(clusteringMethod.trim());

			// define clustering function
			log.debug("Using clustering method {} with distance threshold {}", method, ClusteringThreshold);
			return log.exit(
					callList -> ExpressionCall.generateMeanRankScoreClustering(callList, method, ClusteringThreshold));
		} catch (IllegalArgumentException e) {
			throw log.throwing(
					new IllegalStateException("No custering method corresponding to " + clusteringMethod.trim()));
		}
	}
	
	
//	private Set<AnatEntity> manageDescendants (List<AnatEntity> parent,ServiceFactory factory, String speciesId){
//		Ontology<AnatEntity> descendantEntities = factory.getOntologyService().getAnatEntityOntology(Collections.singleton(speciesId), Collections.singleton(parent.getId()), Collections.singleton(RelationType.ISA_PARTOF), false, true, factory.getAnatEntityService());
//		descendantEntities.
//		return null;
//	}

}
