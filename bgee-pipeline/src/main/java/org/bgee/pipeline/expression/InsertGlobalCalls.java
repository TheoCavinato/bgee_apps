package org.bgee.pipeline.expression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.TransferObject;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO.DataState;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.ExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.ExpressionCallTOResultSet;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.GlobalExpressionToExpressionTO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallParams;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.GlobalNoExpressionToNoExpressionTO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.NoExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.NoExpressionCallTOResultSet;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallParams;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.pipeline.BgeeDBUtils;
import org.bgee.pipeline.CommandRunner;
import org.bgee.pipeline.MySQLDAOUser;


/**
 * Class responsible for inserting the global expression into the Bgee database.
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 */
public class InsertGlobalCalls extends MySQLDAOUser {

    /**
     * {@code Logger} of the class.
     */
    private final static Logger log = LogManager.getLogger(InsertGlobalCalls.class.getName());
    
    /**
     * A {@code String} that is the argument class for expression propagation.
     */
    public final static String EXPRESSION_ARG = "expression";

    /**
     * A {@code String} that is the argument class for no-expression propagation.
     */
    public final static String NOEXPRESSION_ARG = "no-expression";

    /**
     * Main method to insert global expression or no-expression in Bgee database. 
     * Parameters that must be provided in order in {@code args} are: 
     * <ol>
     * <li>A {@code String} defining whether the propagation is for expression or no-expression. 
     * If equals to {@code no-expression}, the propagation will be done for no-expression calls, 
     * if equals to  {@code expression}, the propagation will be done for expression calls.
     * <li> a list of NCBI species IDs (for instance, {@code 9606} for human) 
     * that will be used to propagate (non-)expression, separated by the 
     * {@code String} {@link CommandRunner#LIST_SEPARATOR}. If it is not provided, all species 
     * contained in database will be used.
     * </ol>
     * 
     * @param args           An {@code Array} of {@code String}s containing the requested parameters.
     * @throws DAOException  If an error occurred while inserting the data into the Bgee database.
     */
    public static void main(String[] args) throws DAOException {
        log.entry((Object[]) args);
        
        int expectedArgLengthWithoutSpecies = 1;
        int expectedArgLengthWithSpecies = 2;
    
        if (args.length != expectedArgLengthWithSpecies &&
                args.length != expectedArgLengthWithoutSpecies) {
            throw log.throwing(new IllegalArgumentException("Incorrect number of arguments " +
                    "provided, expected " + expectedArgLengthWithoutSpecies + " or " + 
                    expectedArgLengthWithSpecies + " arguments, " + args.length + 
                    " provided."));
        }
        
        boolean isNoExpression = args[0].equalsIgnoreCase(NOEXPRESSION_ARG);
        if (!isNoExpression && !args[0].equalsIgnoreCase(EXPRESSION_ARG)) {
            throw log.throwing(new IllegalArgumentException("Unrecognized argument: " + 
                    args[0]));
        }
        
        List<String> speciesIds = null;
        if (args.length == expectedArgLengthWithSpecies) {
            speciesIds = CommandRunner.parseListArgument(args[1]);    
        }
        
        InsertGlobalCalls insert = new InsertGlobalCalls();
        insert.insert(speciesIds, isNoExpression);
        
        log.exit();
    }

    /**
     * An {@code int} used to generate IDs of global expression or no-expression calls.
     */        
    private int globalId;

    /**
     * Default constructor. 
     */
    public InsertGlobalCalls() {
        this(null);
    }
    /**
     * Constructor providing the {@code MySQLDAOManager} that will be used by 
     * this object to perform queries to the database. This is useful for unit testing.
     * 
     * @param manager   the {@code MySQLDAOManager} to use.
     */
    public InsertGlobalCalls(MySQLDAOManager manager) {
        super(manager);
        this.globalId = 0;
    }

    /**
     * Inserts the global expression or no-expression calls into the Bgee database.
     * 
     * @param speciesIds       A {@code Set} of {@code String}s containing species IDs that will 
     *                         be used to propagate (non-)expression.
     * @param isNoExpression   A {@code boolean} defining whether we propagate expression or 
     *                         no-expression. If {@code true}, the propagation will be done for 
     *                         no-expression calls.
     * @throws DAOException    If an error occurred while inserting the data into the Bgee database.
     * @throws IllegalArgumentException If a species ID does not correspond to any species 
     *                                  in the Bgee data source.
     */
    //TODO: the previous version of Bgee didn't have any "BOTH" origin of line, 
    //the unique key was (geneId, anatEntityId, stageId, originOfLine), so that there was 
    //always a SELF origin of line for each entry. Not sure to remember why it was needed. 
    //Maybe it is needed and we should do the same? Or the "BOTH" solves the problem?
    public void insert(List<String> speciesIds, boolean isNoExpression) throws DAOException {
        log.entry(speciesIds, isNoExpression);

        try {
            // Get the maximum of global call IDs to get start index for new inserted global calls. 
            if (isNoExpression) {
                this.globalId = this.getNoExpressionCallDAO().getMaxNoExpressionCallId(true) + 1;
            } else {
                this.globalId = this.getExpressionCallDAO().getMaxExpressionCallId(true) + 1;
            }

            //get all species in Bgee even if some species IDs were provided, 
            //to check user input.
            List<String> speciesIdsToUse = BgeeDBUtils.checkAndGetSpeciesIds(speciesIds, 
                    this.getSpeciesDAO());
            
            //retrieve IDs of all anatomical entities allowed for no-expression call 
            //propagation, see loadAllowedAnatEntities method for details. 
            //This is done once for all species, as we want all no-expression calls 
            //to be propagated in the same way for any species.
            Set<String> anatEntityFilter = null;
            if (isNoExpression) {
                anatEntityFilter = this.loadAllowedAnatEntities();
            }

            for (String speciesId: speciesIdsToUse) {
                
                Set<String> speciesFilter = new HashSet<String>();
                speciesFilter.add(speciesId);
                
                if (isNoExpression) {
                    // For each no-expression row, propagate to allowed children.
                    Map<NoExpressionCallTO, Set<NoExpressionCallTO>> globalNoExprMap =
                            this.generateGlobalNoExpressionTOs(
                                    this.loadNoExpressionCallFromDb(speciesFilter), 
                                    BgeeDBUtils.getAnatEntityChildrenFromParents(speciesFilter, 
                                            this.getRelationDAO()), 
                                    anatEntityFilter);
                    
                    // Generate the globalNoExprToNoExprTOs.
                    Set<GlobalNoExpressionToNoExpressionTO> globalNoExprToNoExprTOs = 
                            this.generateGlobalCallToCallTOs(globalNoExprMap, 
                                    GlobalNoExpressionToNoExpressionTO.class);
                    
                    int nbInsertedNoExpressions = 0;
                    int nbInsertedGlobalNoExprToNoExpr = 0;
                    
                    this.startTransaction();
                    
                    log.info("Start inserting of global no-expression calls for {}...", speciesId);
                    nbInsertedNoExpressions += this.getNoExpressionCallDAO().
                            insertNoExpressionCalls(globalNoExprMap.keySet());
                    // Empty memory to free up some memory. We don't use clear() 
                    // because it empty ArgumentCaptor values in test in same time.
                    globalNoExprMap = new HashMap<NoExpressionCallTO, Set<NoExpressionCallTO>>();
                    log.info("Done inserting of global no-expression calls for {}.", speciesId);
                    
                    log.info("Start inserting of relation between a no-expression call " +
                            "and a global no-expression call for {}...", speciesId);
                    nbInsertedGlobalNoExprToNoExpr += this.getNoExpressionCallDAO().
                            insertGlobalNoExprToNoExpr(globalNoExprToNoExprTOs);
                    log.info("Done inserting of correspondances between a no-expression call " +
                            "and a global no-expression call for {}.", speciesId);

                    this.commit();
                    
                    log.info("Done inserting for {}: {} global no-expression calls inserted " +
                            "and {} correspondances inserted", speciesId, 
                            nbInsertedNoExpressions, nbInsertedGlobalNoExprToNoExpr);
                } else {
                    // For each expression row, propagate to parents.
                    Map<ExpressionCallTO, Set<ExpressionCallTO>> globalExprMap =
                            this.generateGlobalExpressionTOs(
                                    this.loadExpressionCallFromDb(speciesFilter), 
                                    BgeeDBUtils.getAnatEntityParentsFromChildren(speciesFilter, 
                                            this.getRelationDAO()));
                    
                    // Generate the globalExprToExprTOs.
                    Set<GlobalExpressionToExpressionTO> globalExprToExprTOs = 
                            this.generateGlobalCallToCallTOs(globalExprMap, 
                                    GlobalExpressionToExpressionTO.class);

                    int nbInsertedExpressions = 0;
                    int nbInsertedGlobalExprToExpr = 0;

                    this.startTransaction();
                    
                    log.info("Start inserting of global expression calls for {}...", speciesId);
                    nbInsertedExpressions += this.getExpressionCallDAO().
                            insertExpressionCalls(globalExprMap.keySet());
                    // Empty memory to free up some memory. We don't use clear() 
                    // because it empty ArgumentCaptor values in test in same time.
                    globalExprMap = new HashMap<ExpressionCallTO, Set<ExpressionCallTO>>();
                    log.info("Done inserting of global expression calls for {}.", speciesId);

                    log.info("Start inserting of relation between an expression call " +
                            "and a global expression call for {}...", speciesId);
                    nbInsertedGlobalExprToExpr += this.getExpressionCallDAO().
                            insertGlobalExpressionToExpression(globalExprToExprTOs);
                    log.info("Done inserting of correspondances between an expression call " +
                            "and a global expression call for {}.", speciesId);

                    this.commit();

                    log.info("Done inserting for {}: {} global expression calls inserted " +
                            "and {} correspondances inserted.", speciesId, 
                            nbInsertedExpressions, nbInsertedGlobalExprToExpr);
                }
            }            
        } finally {
            this.closeDAO();
        }

        log.exit();
    }

    /**
     * Retrieves all expression calls for given species, present into the Bgee database.
     * 
     * @param speciesIds        A {@code Set} of {@code String}s that are the IDs of species 
     *                          allowing to filter the expression calls to use.
     * @return                  A {@code List} of {@code ExpressionCallTO}s containing all 
     *                          expression calls of the given species.
     * @throws DAOException     If an error occurred while getting the data from the Bgee database.
     */
    public List<ExpressionCallTO> loadExpressionCallFromDb(Set<String> speciesIds)
            throws DAOException {
        log.entry(speciesIds);

        log.info("Start retrieving expression calls for the species IDs {}...", speciesIds);

        ExpressionCallDAO dao = this.getExpressionCallDAO();

        ExpressionCallParams params = new ExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);

        ExpressionCallTOResultSet rsExpressionCalls = dao.getExpressionCalls(params);

        List<ExpressionCallTO> exprTOs = rsExpressionCalls.getAllTOs();
        //no need for a try with resource or a finally, the insert method will close everything 
        //at the end in any case.
        // No need to close the ResultSet, it's done by getAllTOs().
        log.info("Done retrieving expression calls, {} calls found", exprTOs.size());

        return log.exit(exprTOs);        
    }

    /**
     * Retrieves all no-expression calls of provided species, present into the Bgee database.
     * 
     * @param speciesIds    A {@code Set} of {@code String}s that are the IDs of species 
     *                      allowing to filter the genes to consider.
     * @return              A {@code List} of {@code NoExpressionCallTO}s containing all 
     *                      no-expression calls for the provided species.
     * @throws DAOException If an error occurred while getting the data from the Bgee database.
     */
    public List<NoExpressionCallTO> loadNoExpressionCallFromDb(Set<String> speciesIds)
            throws DAOException {
        log.entry(speciesIds);
        
        log.info("Start retrieving no-expression calls for the species IDs {}...", speciesIds);

        NoExpressionCallDAO dao = this.getNoExpressionCallDAO();

        NoExpressionCallParams params = new NoExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);

        NoExpressionCallTOResultSet rsNoExpressionCalls = dao.getNoExpressionCalls(params);

        List<NoExpressionCallTO> noExprTOs = rsNoExpressionCalls.getAllTOs();
        //no need for a try with resource or a finally, the insert method will close everything 
        //at the end in any case.
        // No need to close the ResultSet, it's done by getAllTOs().
        
        log.info("Done retrieving no-expression calls, {} calls found", noExprTOs.size());

        return log.exit(noExprTOs);        
    }
    
    /**
     * Retrieves all anatomical entity IDs allowed for no-expression call propagation. 
     * This method will retrieve the IDs of anatomical entities having at least 
     * an expression call, or a no-expression call, as well as the IDs of all 
     * anatomical entities with evolutionary relations to them (for instance, homology, 
     * or analogy), and the IDs of all the parents by is_a/part_of relation of 
     * all these anatomical entities.
     * <p>
     * These method is called by {@link #insert(List, boolean)} when the second argument 
     * is {@code true} (no-expression call propagation). The reason is that no-expression 
     * calls are propagated from parents to children, yet we do not want to propagate 
     * to all possible terms, as this would generate too many global no-expression calls. 
     * Instead, we restrain propagation to terms with data, or worthing a comparison 
     * to terms with data, or leading to these terms (for a consistent graph propagation).
     * 
     * @return              A {@code Set} of {@code String}s containing anatomical entity IDs
     *                      allowed to be used for no-expression call propagation.
     * @throws DAOException If an error occurred while getting the data from the Bgee data source.
     */
    private Set<String> loadAllowedAnatEntities() throws DAOException {
        log.entry();
        
        log.info("Start retrieving anat entities all for no-expression call propagation...");
        Set<String> allowedAnatEntities = new HashSet<String>();
        
        log.debug("Retrieving anat entities with expression calls...");
        ExpressionCallDAO exprDao = this.getExpressionCallDAO();
        exprDao.setAttributes(ExpressionCallDAO.Attribute.ANATENTITYID);
        //we do not query global expression calls, because this way we can launch 
        //the propagation of global expression and global no-expression calls independently. 
        //We will propagate expression calls thanks to relations between anatomical terms.
        //params.setIncludeSubstructures(true);
        ExpressionCallTOResultSet rsExpressionCalls = 
                exprDao.getExpressionCalls(new ExpressionCallParams());
        while (rsExpressionCalls.next()) {
            String anatEntityId = rsExpressionCalls.getTO().getAnatEntityId();
            log.trace("Anat. entity with expression calls: {}", anatEntityId);
            allowedAnatEntities.add(anatEntityId);
        }
        //no need for a try with resource or a finally, the insert method will close everything 
        //at the end in any case.
        rsExpressionCalls.close();
        
        log.debug("Retrieving anat entities with no-expression calls...");
        NoExpressionCallDAO noExprDao = this.getNoExpressionCallDAO();
        noExprDao.setAttributes(NoExpressionCallDAO.Attribute.ANATENTITYID);
        NoExpressionCallTOResultSet rsNoExpressionCalls = noExprDao.getNoExpressionCalls(
                new NoExpressionCallParams());
        while (rsNoExpressionCalls.next()) {
            String anatEntityId = rsNoExpressionCalls.getTO().getAnatEntityId();
            log.trace("Anat. entity with no-expression calls: {}", anatEntityId);
            allowedAnatEntities.add(anatEntityId);
        }
        rsNoExpressionCalls.close();
        
        //TODO: retrieve anat. entities related by an evolutionary relation, 
        //once this information will be inserted into Bgee

        log.debug("Retrieving parents of anat entities allowed so far...");
        Map<String, Set<String>> parentsFromChildren = 
                BgeeDBUtils.getAnatEntityParentsFromChildren(null, this.getRelationDAO());
        Set<String> ancestorIds = new HashSet<String>();
        for (String anatEntityId: allowedAnatEntities) {
            ancestorIds.addAll(parentsFromChildren.get(anatEntityId));
        }
        allowedAnatEntities.addAll(ancestorIds);

        log.info("Done retrieving anat entities for no-expression call propagation, {} entities allowed: {}", 
                allowedAnatEntities.size());
    
        return log.exit(allowedAnatEntities);        
    }

    /**
     * Generate the global expression calls from given expression calls filling the return 
     * {@code Map} associating each generated global expression calls to expression calls used 
     * to generate it.
     * <p>
     * First, the method fills the map with generic global expression calls as key (only gene ID, 
     * anatomical entity ID, and stage ID are defined) with all expression calls used to generate 
     * the global expression call. Second, it updates the global expression calls calling
     * {@link #updateGlobalExpressions()}.
     * 
     * @param expressionTOs         A {@code List} of {@code ExpressionCallTO}s containing 
     *                              all expression calls to propagate.
     * @param parentsFromChildren   A {@code Map} where keys are IDs of anatomical entities 
     *                              that are sources of a relation, the associated value 
     *                              being a {@code Set} of {@code String}s that are 
     *                              the IDs of their associated targets. 
     * @return              A {@code Map} associating generated global expression calls to 
     *                      expression calls used to generate it.
     */
    private Map<ExpressionCallTO, Set<ExpressionCallTO>> generateGlobalExpressionTOs(
            List<ExpressionCallTO> expressionTOs, 
            Map<String, Set<String>> parentsFromChildren) {
        log.entry(expressionTOs, parentsFromChildren);
                
        Map<ExpressionCallTO, Set<ExpressionCallTO>> mapGlobalExpr = 
                new HashMap<ExpressionCallTO, Set<ExpressionCallTO>>();

        for (ExpressionCallTO exprCallTO : expressionTOs) {
            log.trace("Propagation for expression call: {}", exprCallTO);
            //the relations include a reflexive relation, where sourceId == targetId, 
            //this will allow to also include the actual not-propagated calls
            for (String parentId : parentsFromChildren.get(exprCallTO.getAnatEntityId())) {
                log.trace("Propagation of the current expression to parent: {}" + parentId);
                // Set ID to null to be able to compare keys of the map on 
                // gene ID, anatomical entity ID, and stage ID.
                // Add propagated expression call (same gene ID and stage ID 
                // but with anatomical entity ID of the current relation target ID).
                ExpressionCallTO propagatedExpression = new ExpressionCallTO(
                        null, 
                        exprCallTO.getGeneId(),
                        parentId,
                        exprCallTO.getStageId(),
                        DataState.NODATA,      
                        DataState.NODATA,
                        DataState.NODATA,
                        DataState.NODATA,
                        false,
                        false,
                        ExpressionCallTO.OriginOfLine.SELF);
                
                log.trace("Add the propagated expression: " + propagatedExpression);
                Set<ExpressionCallTO> curExprAsSet = mapGlobalExpr.get(propagatedExpression);
                if (curExprAsSet == null) {
                    curExprAsSet = new HashSet<ExpressionCallTO>();
                    mapGlobalExpr.put(propagatedExpression, curExprAsSet);
                }
                curExprAsSet.add(exprCallTO);
            }
        }

        this.updateGlobalExpressions(mapGlobalExpr);
        
        return log.exit(mapGlobalExpr);        
    }

    /**
     * Generate the global no-expression calls from given no-expression calls filling the return
     * {@code Map} associating each generated global no-expression calls to no-expression calls 
     * used to generate it.
     * <p>
     * First, the method fills the map associating generic global no-expression calls as key 
     * (only gene ID, anatomical entity ID, and stage ID are defined) with all no-expression calls 
     * used to generate the global no-expression call. Second, it updates the global no-expression
     * calls calling {@link #updateGlobalNoExpressions()}.
     * 
     * @param noExpressionTOs           A {@code List} of {@code NoExpressionCallTO}s containing 
     *                                  all no-expression calls to be propagated.
     * @param childrenFromParents        A {@code Map} where keys are IDs of anatomical entities 
     *                                  that are targets of a relation, the associated value 
     *                                  being a {@code Set} of {@code String}s that are 
     *                                  the IDs of their associated sources. 
     * @param filteredAnatEntities      A {@code Set} of {@code String}s that are the IDs of 
     *                                  anatomical entities allowing filter calls to propagate.
     * @return                          A {@code Map} associating generated global no-expression 
     *                                  calls to no-expression calls used to generate it.
     */
    private Map<NoExpressionCallTO, Set<NoExpressionCallTO>>
            generateGlobalNoExpressionTOs(List<NoExpressionCallTO> noExpressionTOs, 
                    Map<String, Set<String>> childrenFromParents, 
                    Set<String> filteredAnatEntities) {
        log.entry(noExpressionTOs, childrenFromParents, filteredAnatEntities);

        Map<NoExpressionCallTO, Set<NoExpressionCallTO>> mapGlobalNoExpr = 
                new HashMap<NoExpressionCallTO, Set<NoExpressionCallTO>>();

        for (NoExpressionCallTO noExprCallTO : noExpressionTOs) {
            log.trace("Propagation for no-expression call: {}", noExprCallTO);
            //the relations include a reflexive relation, where sourceId == targetId, 
            //this will allow to also include the actual not-propagated calls
            for (String childId : childrenFromParents.get(noExprCallTO.getAnatEntityId())) {
                log.trace("Propagation of the current no-expression to child: {}", childId);

                // Add propagated no-expression call (same gene ID and stage ID 
                // but with anatomical entity ID of the current relation source ID) 
                // only in anatomical entities having at least a global expression call.
                if (childId.equals(noExprCallTO.getAnatEntityId()) || //reflexive relation
                            filteredAnatEntities.contains(childId)) {
                    // Set ID to null to be able to compare keys of the map on 
                    // gene ID, anatomical entity ID, and stage ID.
                    NoExpressionCallTO propagatedExpression = new NoExpressionCallTO(
                            null, 
                            noExprCallTO.getGeneId(),
                            childId,
                            noExprCallTO.getStageId(),
                            DataState.NODATA,      
                            DataState.NODATA,
                            DataState.NODATA,
                            DataState.NODATA,
                            false, 
                            NoExpressionCallTO.OriginOfLine.SELF);

                    log.trace("Add the propagated no-expression: " + propagatedExpression);
                    Set<NoExpressionCallTO> curExprAsSet = mapGlobalNoExpr.get(propagatedExpression);
                    if (curExprAsSet == null) {
                       curExprAsSet = new HashSet<NoExpressionCallTO>();
                       mapGlobalNoExpr.put(propagatedExpression, curExprAsSet);
                    }
                    curExprAsSet.add(noExprCallTO);
                }
            }
        }

        this.updateGlobalNoExpressions(mapGlobalNoExpr);

        return log.exit(mapGlobalNoExpr);        
    }

    /**
     * Update global expression calls of the given {@code Map} taking account {@code DataType}s and 
     * anatomical entity IDs of calls to generate {@code OrigineOfLine} of global calls.
     * <p>
     * The provided {@code Map} will be modified.
     * 
     * @param globalMap     A {@code Map} associating generated global expression calls to 
     *                      expression calls to be updated.
     */
    private void updateGlobalExpressions(Map<ExpressionCallTO, Set<ExpressionCallTO>> globalMap) {
        log.entry(globalMap);
        
        // Create a Set from keySet to be able to modify globalMap.
        Set<ExpressionCallTO> tmpGlobalCalls = new HashSet<ExpressionCallTO>(globalMap.keySet());

        for (ExpressionCallTO globalCall: tmpGlobalCalls) {
            // Remove generic global call which contains only 
            // gene ID, anatomical entity ID, and stage ID
            Set<ExpressionCallTO> calls = globalMap.remove(globalCall);

            log.trace("Update global expression calls: {}; with: {}", globalCall, calls);
            
            // Define the best DataType of the global call according to all calls
            // and get anatomical entity IDs to be able to define OriginOfLine later.
            DataState affymetrixData = DataState.NODATA, estData = DataState.NODATA, 
                    inSituData = DataState.NODATA, rnaSeqData = DataState.NODATA;
            Set<String> anatEntityIDs = new HashSet<String>();
            for (ExpressionCallTO call: calls) {
                affymetrixData = getBestDataState(affymetrixData, call.getAffymetrixData());
                estData = getBestDataState(estData, call.getESTData());
                inSituData = getBestDataState(inSituData, call.getInSituData());
                rnaSeqData = getBestDataState(rnaSeqData, call.getRNASeqData());
                anatEntityIDs.add(call.getAnatEntityId());
            }

            // Define the OriginOfLine of the global expression call according to all calls
            ExpressionCallTO.OriginOfLine origin = ExpressionCallTO.OriginOfLine.DESCENT;
            if (anatEntityIDs.contains(globalCall.getAnatEntityId())) {
                if (anatEntityIDs.size() == 1) {
                    origin = ExpressionCallTO.OriginOfLine.SELF;
                } else {
                    origin = ExpressionCallTO.OriginOfLine.BOTH;
                }
            }
            ExpressionCallTO updatedGlobalCall =
                    new ExpressionCallTO(String.valueOf(this.globalId++), 
                            globalCall.getGeneId(), globalCall.getAnatEntityId(), 
                            globalCall.getStageId(), 
                            affymetrixData, estData, inSituData, rnaSeqData, true,
                            globalCall.isIncludeSubStages(), origin);

            log.trace("Updated global expression call: " + updatedGlobalCall);

            // Add the updated global expression call
            globalMap.put(updatedGlobalCall, calls);
        } 
        
        log.exit();
    }

    /**
     * Update global no-expression calls of the given {@code Map} taking account {@code DataType}s 
     * and anatomical entity IDs of calls to generate {@code OrigineOfLine} of global calls. 
     * <p>
     * The provided {@code Map} will be modified.
     * 
     * @param globalMap     A {@code Map} associating generated global no-expression calls to 
     *                      no-expression calls to be updated.
     */
    private void updateGlobalNoExpressions(
            Map<NoExpressionCallTO, Set<NoExpressionCallTO>> globalMap) {
        log.entry(globalMap);
        
        // Create a Set from keySet to be able to modify globalMap.
        Set<NoExpressionCallTO> tmpGlobalCalls = new HashSet<NoExpressionCallTO>(globalMap.keySet());

        for (NoExpressionCallTO globalCall: tmpGlobalCalls) {
            // Remove generic global call which contains only 
            // gene ID, anatomical entity ID, and stage ID
            Set<NoExpressionCallTO> calls = globalMap.remove(globalCall);

            log.trace("Update global no-expression calls: {}; with: {}", globalCall, calls);
            
            // Define the best DataType of the global call according to all calls
            // and get anatomical entity IDs to be able to define OriginOfLine later.
            DataState affymetrixData = DataState.NODATA, relaxedinSituData = DataState.NODATA,
                    inSituData = DataState.NODATA, rnaSeqData = DataState.NODATA;
            Set<String> anatEntityIDs = new HashSet<String>();
            for (NoExpressionCallTO call: calls) {
                affymetrixData = getBestDataState(affymetrixData, call.getAffymetrixData());
                inSituData = getBestDataState(inSituData, call.getInSituData());
                relaxedinSituData = getBestDataState(relaxedinSituData, call.getRelaxedInSituData());
                rnaSeqData = getBestDataState(rnaSeqData, call.getRNASeqData());
                anatEntityIDs.add(call.getAnatEntityId());
            }

            // Define the OriginOfLine of the global no-expression call according to all calls
            NoExpressionCallTO.OriginOfLine origin = NoExpressionCallTO.OriginOfLine.PARENT;
            if (anatEntityIDs.contains(globalCall.getAnatEntityId())) {
                if (anatEntityIDs.size() == 1) {
                    origin = NoExpressionCallTO.OriginOfLine.SELF;
                } else {
                    origin = NoExpressionCallTO.OriginOfLine.BOTH;
                }
            }

            NoExpressionCallTO updatedGlobalCall =
                    new NoExpressionCallTO(String.valueOf(this.globalId++), 
                            globalCall.getGeneId(), globalCall.getAnatEntityId(), 
                            globalCall.getStageId(), 
                            affymetrixData, inSituData, relaxedinSituData, rnaSeqData,
                            true, origin);

            log.trace("Updated global no-expression call: " + updatedGlobalCall);

            // Add the updated global no-expression call
            globalMap.put(updatedGlobalCall, calls);
        }
        
        log.exit();
    }

    /**
     * Get the best {@code DataState} between two {@code DataState}s.
     * 
     * @param dataState1    A {@code DataState} to be compare to {@code dataState2}.
     * @param dataState2    A {@code DataState} to be compare to {@code dataState1}.
     * @return              The best {@code DataState} between {@code dataState1} 
     *                      and {@code dataState2}.
     */
    private DataState getBestDataState(DataState dataState1, DataState dataState2) {
        log.entry(dataState1, dataState2);
        
        if (dataState1.ordinal() < dataState2.ordinal()) {
            return log.exit(dataState2);
        }
        
        return log.exit(dataState1);
    }

    /**
     * Generate the {@code Set} of {@code V}s associating generated global calls to calls, 
     * according the given {@code Map}. Values will be casted to the same type as {@code type}. 
     * Are currently supported (for {@code V}): {@code GlobalExpressionToExpressionTO.class}, 
     * {@code GlobalNoExpressionToNoExpressionTO.class}.
     * 
     * @param globalExprMap A {@code Map} associating generated global calls to calls to be 
     *                      inserted into Bgee database.
     * @param type          The desired returned type of values.
     * @return              A {@code Set} of {@code V}s containing associations between global calls 
     *                      to calls to be inserted into the Bgee database.
     * @param <T>           A {@code CallTO} type parameter.
     * @param <V>           A {@code TransferObject} type parameter.
     */
    private <T extends CallTO, V extends TransferObject> Set<V> generateGlobalCallToCallTOs(
            Map<T, Set<T>> globalExprMap, Class<V> type) {
        log.entry(globalExprMap, type);
        
        Set<V> globalExprToExprTOs = new HashSet<V>();
        for (Entry<T, Set<T>> entry: globalExprMap.entrySet()) {
            for (T expression : entry.getValue()) {
                if (type.equals(GlobalExpressionToExpressionTO.class)) {
                    globalExprToExprTOs.add(type.cast(new GlobalExpressionToExpressionTO(
                            expression.getId(), entry.getKey().getId())));
                } else if (type.equals(GlobalNoExpressionToNoExpressionTO.class)) {
                    globalExprToExprTOs.add(type.cast(new GlobalNoExpressionToNoExpressionTO(
                            expression.getId(), entry.getKey().getId())));
                } else {
                    throw log.throwing(new IllegalArgumentException("There is no propagation " +
                        "implemented for TransferObject " + expression.getClass() + "."));
                }
            }
        }

        return log.exit(globalExprToExprTOs);
    }
}
