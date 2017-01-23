package org.bgee.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.CommandGene.GeneResponse;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.expressiondata.ConditionFilter;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.CallData.ExpressionCallData;
import org.bgee.model.expressiondata.CallFilter.ExpressionCallFilter;
import org.bgee.model.expressiondata.baseelements.DataPropagation;
import org.bgee.model.expressiondata.baseelements.CallType.Expression;
import org.bgee.model.species.Species;
import org.bgee.model.species.SpeciesService;
import org.bgee.view.ViewFactory;

/**
 * Controller handling requests related to anat.entity pages. 
 * 
 * @author Julien Wollbrett
 * @version Bgee 13, Jan. 2017
 * @since   Bgee 13, Jan. 2017
 */

public class CommandAnatEntity extends CommandParent{

	private final static Logger log = LogManager.getLogger(CommandAnatEntity.class.getName());
	
	 /**
     * Contains all information necessary to produce a view related to a {@code AnatEntity}.
     * 
     * @author Julien Wollbrett
     * @version Bgee 13, Jan. 2017
     * @since   Bgee 13, Jan. 2017
     */
	/**
     * Contains all information necessary to produce a view related to a {@code Gene}.
     * 
     * @author Frederic Bastian
     * @version Bgee 13, June 2016
     * @since   Bgee 13, Jan. 2016
     */
    public static class AnatEntityResponse {
    	
        private final AnatEntity anatEntity;
        private final List<ExpressionCall> exprCalls;
        
        /**
         * @param anatEntity                          See {@link #getAnatEntity()}.
         * @param exprCalls                     See {@link #getExprCalls()}.
         */ 
        
        public AnatEntityResponse(AnatEntity anatEntity, List<ExpressionCall> exprCalls) {
			this.anatEntity = anatEntity;
			this.exprCalls = exprCalls;
		}
        
        /**
         * @return  The {@code AnatEntity} which information are requested for. 
         */
        public AnatEntity getAnatEntity() {
			return anatEntity;
		}
        
        /**
         * @return  A {@code List} of {@code ExpressionCall}s, ranked by the normalized global rank 
         *          of the gene in the related {@code Condition}s.
         */
        public List<ExpressionCall> getExprCalls() {
			return exprCalls;
		}
 	}
	
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
	public CommandAnatEntity(HttpServletResponse response, RequestParameters requestParameters, BgeeProperties prop,
	        ViewFactory viewFactory, ServiceFactory serviceFactory) {
		super(response, requestParameters, prop, viewFactory, serviceFactory);
	}



	@Override
	public void processRequest() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	private GeneResponse buildGeneResponse(String anatEntityId) 
	        throws PageNotFoundException, IllegalStateException {
	//TODO 
		return log.exit(null);
	}
	
	/**
	 * Gets the {@code AnatEntity} instance from its id. 
	 * 
	 * @param geneId A {@code String} containing the AnatEntity id.
	 * @return       The {@code AnatEntiy} instance.
	 * @throws PageNotFoundException   If no {@code AnatEntity} could be found corresponding to {@code anatEntityId}.
	 */
	private AnatEntity getAnatEntity(String anatEntityId) throws PageNotFoundException {
	    log.entry(anatEntityId);
	    AnatEntity anatEntity = serviceFactory.getAnatEntityService().loadAnatEntityById(anatEntityId);
		if (anatEntity == null) {
		    throw log.throwing(new PageNotFoundException("No gene corresponding to " + anatEntity));
		}
		return log.exit(anatEntity);
	}
	
	/**
	 * Gets the {@code Set} of {@code Species} instance from their ids. 
	 * 
	 * @param speciesIds A {@code List} of {@code String} containing the Species ids.
	 * @return       The {@code Set} of {@code Species} instances.
	 */
	private Set<Species> getSpecies(List<String> speciesIds){
	    log.entry(speciesIds);
	    SpeciesService speciesService = serviceFactory.getSpeciesService();
	    Set<Species> speciesSet = speciesIds == null ? speciesService.loadSpeciesInDataGroups(false) : speciesService.loadSpeciesByIds(speciesIds, false);
		return log.exit(speciesSet);
	}
	
	/**
	 * Retrieves the sorted list of {@code ExpressionCall} associated to this anat. entity for selected species, 
	 * ordered by global mean rank.
	 * 
	 * @param anatEntity The {@code AnatEntity}
	 * @param speciesIds The {@code Set} of {@code Species} Ids.
	 * @return     The {@code HashMap} with {@code Species} Ids as keys and a {@code List} of {@code ExpressionCall} associated to this anat. entity, 
	 *             ordered by global mean rank as values.
	 */
	private HashMap<String,List<ExpressionCall>> getExpressions(AnatEntity anatEntity, Set<Species> speciesSet) {
	    log.entry(anatEntity);
	    LinkedHashMap<CallService.OrderingAttribute, Service.Direction> serviceOrdering = 
	                new LinkedHashMap<>();
		//The ordering is not essential here, because anyway we will need to order calls 
		//with an equal rank, based on the relations between their conditions, which is difficult 
		//to make in a query to the data source.
	    serviceOrdering.put(CallService.OrderingAttribute.GLOBAL_RANK, Service.Direction.ASC);
		CallService callService = serviceFactory.getCallService();
		Collection<ConditionFilter> condFilterCollection = new ArrayList<>();
		condFilterCollection.add(new ConditionFilter(Collections.singleton(anatEntity.getId()), null));
		HashMap<String, List<ExpressionCall>> expressionCallBySpecies = new HashMap<String, List<ExpressionCall>>();
		for(Species species:speciesSet){
			expressionCallBySpecies.put(species.getId(),
					callService.loadExpressionCalls(
					        species.getId(), 
			                new ExpressionCallFilter(null, condFilterCollection, new DataPropagation(), 
			                        Arrays.asList(new ExpressionCallData(Expression.EXPRESSED))), 
			                EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID, 
			                        CallService.Attribute.DEV_STAGE_ID, CallService.Attribute.CALL_DATA, 
			                        CallService.Attribute.GLOBAL_DATA_QUALITY, CallService.Attribute.GLOBAL_RANK), 
			                serviceOrdering)
			            .collect(Collectors.toList()));
		}
		return log.exit(expressionCallBySpecies);
	}
}

	
