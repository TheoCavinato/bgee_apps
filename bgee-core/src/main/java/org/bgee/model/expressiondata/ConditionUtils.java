package org.bgee.model.expressiondata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.ontology.Ontology;
import org.bgee.model.ontology.OntologyService;
import org.bgee.model.ontology.Ontology.RelationType;

/**
 * Class providing convenience operations on {@link Condition}s.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, Apr. 2016
 * @since   Bgee 13, Dec. 2015
 */
public class ConditionUtils {

    private static final Logger log = LogManager.getLogger(ConditionUtils.class.getName());
    
    /**
     * @see #getConditions()
     */
    private final Set<Condition> conditions;
    /**
     * A {@code ServiceFactory} allowing to acquire the {@code Service}s necessary to {@code ConditionUtils}.
     */
    private final ServiceFactory serviceFactory;
    
    /**
     * @see #getAnatEntityOntology()
     */
    private final Ontology<AnatEntity> anatEntityOnt;
    /**
     * @see #getDevStageOntology()
     */
    private final Ontology<DevStage> devStageOnt;
    
    /**
     * @see #isInferredAncestralConditions()
     */
    private final boolean inferAncestralConditions;
    
    /**
     * A {@code String} that is the ID of the species which the {@code Condition}s 
     * should be valid in.
     */
    //XXX: maybe this will become a Set<String> for multi-species management. We'll see later. 
    //We do not provide getter for now, we don't need it and the returned type might change.
    private final String speciesId;

    /**
     * @param speciesId         A {@code String} that is the ID of the species which the {@code Condition}s 
     *                          should be valid in.
     * @param conditions        A {@code Collection} of {@code Condition}s that will be managed 
     *                          by this {@code ConditionUtils}.
     * @param serviceFactory    A {@code ServiceFactory} to acquire {@code Service}s from.
     * @throws IllegalArgumentException If any of the arguments is {@code null} or empty, 
     *                                  or if the anat. entity or dev. stage of a {@code Condition} 
     *                                  does not exist in the requested species.
     */
    public ConditionUtils(String speciesId, Collection<Condition> conditions, 
            ServiceFactory serviceFactory) {
        this(speciesId, conditions, false, serviceFactory);
    }
    /**
     * Constructor accepting all required parameters. 
     * 
     * @param speciesId             A {@code String} that is the ID of the species which 
     *                              the {@code Condition}s should be valid in.
     * @param conditions            A {@code Collection} of {@code Condition}s that will be managed 
     *                              by this {@code ConditionUtils}.
     * @param inferAncestralConds   A {@code boolean} defining whether the ancestral conditions
     *                              should be inferred.
     * @param serviceFactory        A {@code ServiceFactory} to acquire {@code Service}s from.
     * @throws IllegalArgumentException If any of the arguments is {@code null} or empty, 
     *                                  or if the anat. entity or dev. stage of a {@code Condition} 
     *                                  does not exist in the requested species.
     */
    //XXX: we'll see what we'll do for multi-species later, for now we only accept a single species. 
    //I guess multi-species would need a separate class, e.g., MultiSpeciesConditionUtils.
    //TODO: unit test for ancestral condition inferrences
    //TODO: refactor this constructor, methods getAncestorConditions and getDescendantConditions
    public ConditionUtils(String speciesId, Collection<Condition> conditions, boolean inferAncestralConds, 
            ServiceFactory serviceFactory) throws IllegalArgumentException {
        log.entry(speciesId, conditions, inferAncestralConds, serviceFactory);
        if (StringUtils.isBlank(speciesId)) {
            throw log.throwing(new IllegalArgumentException("A species ID must be provided."));
        }
        if (conditions == null || conditions.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("Some conditions must be provided."));
        }
        if (serviceFactory == null) {
            throw log.throwing(new IllegalArgumentException("A ServiceFactory must be provided."));
        }
        
        this.speciesId = speciesId;
        this.inferAncestralConditions = inferAncestralConds;
        this.serviceFactory = serviceFactory;
        Set<Condition> tempConditions = new HashSet<>(conditions);
        
        Set<String> anatEntityIds = new HashSet<>();
        Set<String> devStageIds = new HashSet<>();
        for (Condition cond: tempConditions) {
            anatEntityIds.add(cond.getAnatEntityId());
            devStageIds.add(cond.getDevStageId());
        }
        
        OntologyService ontService = this.serviceFactory.getOntologyService();
        this.anatEntityOnt = ontService.getAnatEntityOntology(Arrays.asList(this.speciesId), 
                anatEntityIds, EnumSet.of(RelationType.ISA_PARTOF), 
                inferAncestralConds? true: false, false, this.serviceFactory.getAnatEntityService());
        this.devStageOnt = ontService.getDevStageOntology(Arrays.asList(this.speciesId), 
                devStageIds, inferAncestralConds? true: false, false, 
                this.serviceFactory.getDevStageService());

        if (inferAncestralConds) {
            Set<Condition> ancConditions = tempConditions.stream().flatMap(cond -> {
                Set<String> ancStageIds = this.devStageOnt.getAncestors(
                        this.devStageOnt.getElement(cond.getDevStageId()))
                    .stream().map(e -> e.getId()).collect(Collectors.toSet());
                ancStageIds.add(cond.getDevStageId());
                
                Set<String> ancAnatEntityIds = this.anatEntityOnt.getAncestors(
                            this.anatEntityOnt.getElement(cond.getAnatEntityId()))
                        .stream().map(e -> e.getId()).collect(Collectors.toSet());
                ancAnatEntityIds.add(cond.getAnatEntityId());
                
                return ancAnatEntityIds.stream().flatMap(ancAnatEntityId -> 
                    ancStageIds.stream().map(ancStageId -> new Condition(ancAnatEntityId, ancStageId))
                ).filter(ancCond -> !cond.equals(ancCond));
                
            }).collect(Collectors.toSet());
            
            tempConditions.addAll(ancConditions);
        }
        this.conditions = Collections.unmodifiableSet(tempConditions);

        this.checkEntityExistence(devStageIds, this.devStageOnt);
        this.checkEntityExistence(anatEntityIds, this.anatEntityOnt);
    }
    
    /**
     * Check that all elements in {@code entityIds} are present in {@code ont}, 
     * and only them.
     * 
     * @param entityIds A {@code Set} of {@code String}s that are the IDs of the entities 
     *                  to check for existence in {@code ont}.
     * @param ont       An {@code Ontology} that should contain all elements with their ID 
     *                  in {@code entityIds}.
     * @throws IllegalArgumentException If some elements in {@code entityIds} are not present in 
     *                                  {@code ont}.
     */
    private void checkEntityExistence(Set<String> entityIds, Ontology<?> ont) throws IllegalArgumentException {
        log.entry(entityIds, ont);
        
        Set<String> recognizedEntityIds = ont.getElements().stream()
                .map(e -> e.getId()).collect(Collectors.toSet());
        if (!recognizedEntityIds.containsAll(entityIds)) {
            Set<String> unrecognizedIds = new HashSet<>(entityIds);
            unrecognizedIds.removeAll(recognizedEntityIds);
            throw log.throwing(new IllegalArgumentException("Some entities do not exist "
                    + "in the requested species (" + this.speciesId + "): " + unrecognizedIds));
        }
        
        log.exit();
    }
    
    /**
     * Determines whether the second condition is more precise than the first condition. 
     * "More precise" means that the anatomical structure of {@code secondCond} would be a descendant 
     * of the anatomical structure of {@code firstCond}, and the developmental stage 
     * of {@code secondCond} would be a descendant of the developmental stage of {@code firstCond}.
     * 
     * @param firstCond     The first {@code Condition} to be checked for relations to {@code secondCond}. 
     * @param secondCond    The second {@code Condition} to be checked for relations to {@code firstCond}. 
     * @return              {@code true} if {@code secondCond} is more precise than {@code firstCond}.
     * @throws IllegalArgumentException If one of the provided {@code Condition}s is not registered 
     *                                  to this {@code ConditionUtils}.
     */
    public boolean isConditionMorePrecise(Condition firstCond, Condition secondCond) throws IllegalArgumentException {
        log.entry(firstCond, secondCond);
        if (!this.getConditions().contains(firstCond) || !this.getConditions().contains(secondCond)) {
            throw log.throwing(new IllegalArgumentException("Some of the provided conditions "
                    + "are not registered to this ConditionUtils. First condition: " + firstCond 
                    + " - Second condition: " + secondCond));
        }
        if (firstCond.equals(secondCond)) {
            return log.exit(false);
        }
        
        //Of note, computations are three times faster when checking stages before anat. entities. 
        
        if (!firstCond.getDevStageId().equals(secondCond.getDevStageId()) && 
                !this.devStageOnt.getAncestors(
                        this.devStageOnt.getElement(secondCond.getDevStageId()))
                .contains(this.devStageOnt.getElement(firstCond.getDevStageId()))) {
            return log.exit(false);
        }
        
        if (!firstCond.getAnatEntityId().equals(secondCond.getAnatEntityId()) && 
                !this.anatEntityOnt.getAncestors(
                        this.anatEntityOnt.getElement(secondCond.getAnatEntityId()))
                .contains(this.anatEntityOnt.getElement(firstCond.getAnatEntityId()))) {
            return log.exit(false);
        }
        
        return log.exit(true);
    }
    
    //TODO: unit tests
    //TODO: refactor this method, constructor and getDescendantConditions
    /**
     * Get all the {@code Conditions} that are less precise than {@code cond}, 
     * among the {@code Condition}s provided at instantiation. 
     * 
     * @param cond          A {@code Condition} for which we want to retrieve ancestors {@code Condition}s.
     * @param directRelOnly A {@code boolean} defining whether only direct parents 
     *                      or children of {@code element} should be returned.
     * @return              A {@code Set} of {@code Condition}s that are ancestors of {@code cond}.
     * @throws IllegalArgumentException If {@code cond} is not registered to this {@code ConditionUtils}.
     */
    public Set<Condition> getAncestorConditions(Condition cond, boolean directRelOnly) 
            throws IllegalArgumentException {
        log.entry(cond, directRelOnly);
        if (!this.getConditions().contains(cond)) {
            throw log.throwing(new IllegalArgumentException("The provided condition "
                    + "is not registered to this ConditionUtils: " + cond));
        }
        
        Set<String> devStageIds = this.devStageOnt.getAncestors(
                    this.devStageOnt.getElement(cond.getDevStageId()), directRelOnly)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        devStageIds.add(cond.getDevStageId());
        
        Set<String> anatEntityIds = this.anatEntityOnt.getAncestors(
                    this.anatEntityOnt.getElement(cond.getAnatEntityId()), directRelOnly)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        anatEntityIds.add(cond.getAnatEntityId());
        
        return log.exit(this.conditions.stream()
                .filter(e -> !e.equals(cond) && 
                             devStageIds.contains(e.getDevStageId()) && 
                             anatEntityIds.contains(e.getAnatEntityId()))
                .collect(Collectors.toSet()));
    }

    /**
     * Get all the {@code Conditions} that are more precise than {@code cond}, 
     * among the {@code Condition}s provided at instantiation. 
     * 
     * @param cond  A {@code Condition} for which we want to retrieve descendant {@code Condition}s.
     * @return      A {@code Set} of {@code Condition}s that are descendants of {@code cond}.
     * @throws IllegalArgumentException If {@code cond} is not registered to this {@code ConditionUtils}.
     */
    public Set<Condition> getDescendantConditions(Condition cond) {
        log.entry(cond);
        return log.exit(this.getDescendantConditions(cond, false));
    }
    /**
     * Get all the {@code Conditions} that are more precise than {@code cond}, 
     * among the {@code Condition}s provided at instantiation. 
     * 
     * @param cond          A {@code Condition} for which we want to retrieve descendant {@code Condition}s.
     * @param directRelOnly A {@code boolean} defining whether only direct parents 
     *                      or children of {@code element} should be returned.
     * @return              A {@code Set} of {@code Condition}s that are descendants of {@code cond}.
     * @throws IllegalArgumentException If {@code cond} is not registered to this {@code ConditionUtils}.
     */
    public Set<Condition> getDescendantConditions(Condition cond, boolean directRelOnly) {
        log.entry(cond, directRelOnly);
        if (!this.getConditions().contains(cond)) {
            throw log.throwing(new IllegalArgumentException("The provided condition "
                    + "is not registered to this ConditionUtils: " + cond));
        }
        
        Set<String> devStageIds = this.devStageOnt.getDescendants(
                    this.devStageOnt.getElement(cond.getDevStageId()), directRelOnly)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        devStageIds.add(cond.getDevStageId());
        
        Set<String> anatEntityIds = this.anatEntityOnt.getDescendants(
                    this.anatEntityOnt.getElement(cond.getAnatEntityId()), directRelOnly)
                .stream().map(e -> e.getId()).collect(Collectors.toSet());
        anatEntityIds.add(cond.getAnatEntityId());
        
        return log.exit(this.conditions.stream()
                .filter(e -> !e.equals(cond) && 
                             devStageIds.contains(e.getDevStageId()) && 
                             anatEntityIds.contains(e.getAnatEntityId()))
                .collect(Collectors.toSet()));
    }
    
    /**
     * Retrieve an {@code AnatEntity} present in a {@code Condition} provided at instantiation, 
     * based on its ID.
     * 
     * @param anatEntityId  A {@code String} that is the ID of the {@code AnatEntity} to retrieve.
     * @return              The corresponding {@code AnatEntity}. {@code null} if no corresponding 
     *                      {@code AnatEntity} was present in the {@code Condition}s provided 
     *                      at instantiation.
     */
    public AnatEntity getAnatEntity(String anatEntityId) {
        log.entry(anatEntityId);
        return log.exit(this.getAnatEntityOntology().getElement(anatEntityId));
    }
    /**
     * Retrieve an {@code AnatEntity} from a {@code Condition}.
     * 
     * @param condition     The {@code Condition} which to retrieve the ID of the requested 
     *                      {@code AnatEntity} from.
     * @return              The {@code AnatEntity} corresponding to the ID provided by {@code condition}. 
     * @throws IllegalArgumentException If {@code condition} was not part of the {@code Condition}s 
     *                                  provided at instantiation of this {@code ConditionUtils}.
     */
    public AnatEntity getAnatEntity(Condition condition) {
        log.entry(condition);
        if (!this.conditions.contains(condition)) {
            throw log.throwing(new IllegalArgumentException("Unrecognized condition: " + condition));
        }
        return log.exit(this.getAnatEntityOntology().getElement(condition.getAnatEntityId()));
    }
    /**
     * Retrieve a {@code DevStage} present in a {@code Condition} provided at instantiation, 
     * based on its ID.
     * 
     * @param devStageId    A {@code String} that is the ID of the {@code DevStage} to retrieve.
     * @return              The corresponding {@code DevStage}. {@code null} if no corresponding 
     *                      {@code DevStage} was present in the {@code Condition}s provided 
     *                      at instantiation.
     */
    public DevStage getDevStage(String devStageId) {
        log.entry(devStageId);
        return log.exit(this.getDevStageOntology().getElement(devStageId));
    }
    /**
     * Retrieve a {@code DevStage} from a {@code Condition}.
     * 
     * @param condition     The {@code Condition} which to retrieve the ID of the requested 
     *                      {@code DevStage} from.
     * @return              The {@code DevStage} corresponding to the ID provided by {@code condition}. 
     * @throws IllegalArgumentException If {@code condition} was not part of the {@code Condition}s 
     *                                  provided at instantiation of this {@code ConditionUtils}.
     */
    public DevStage getDevStage(Condition condition) {
        log.entry(condition);
        if (!this.conditions.contains(condition)) {
            throw log.throwing(new IllegalArgumentException("Unrecognized condition: " + condition));
        }
        return log.exit(this.getDevStageOntology().getElement(condition.getDevStageId()));
    }
    
    //*********************************
    //  GETTERS/SETTERS
    //*********************************
    /**
     * @return  The {@code Set} of {@code Condition}s to be considered for operations on this {@code ConditionUtils}.
     */
    public Set<Condition> getConditions() {
        return conditions;
    }
    /**
     * @return  An {@code Ontology} of {@code AnatEntity}s used to infer relations between {@code Condition}s. 
     *          Contains only {@code AnatEntity}s and relations for entities present 
     *          in the {@code Condition}s provided at instantiation.
     * @see #getDevStageOntology()
     */
    public Ontology<AnatEntity> getAnatEntityOntology() {
        return anatEntityOnt;
    }
    /**
     * @return  An {@code Ontology} of {@code DevStage}s used to infer relations between {@code Condition}s. 
     *          Contains only {@code DevStage}s and relations for entities present 
     *          in the {@code Condition}s provided at instantiation.
     * @see #getAnatEntityOntology()
     */
    public Ontology<DevStage> getDevStageOntology() {
        return devStageOnt;
    }
    /** 
     * @return  The {@code boolean} defining whether the ancestral conditions should be inferred.
     */
    public boolean isInferredAncestralConditions() {
        return this.inferAncestralConditions;
    }
    
}
