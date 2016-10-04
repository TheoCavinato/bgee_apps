package org.bgee.model.ontology;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.NamedEntity;
import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.anatdev.TaxonConstraint;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationStatus;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTOResultSet;
import org.bgee.model.function.QuadriFunction;

/**
 * A {@link Service} to obtain {@link Ontology} and {@link MultiSpeciesOntology} objects.
 * Users should use the {@link ServiceFactory} to obtain {@code OntologyService}s.
 * 
 * @author  Valentine Rech de Laval
 * @author  Frederic Bastian
 * @version Bgee 13, Oct. 2016
 * @since   Bgee 13, Dec. 2015
 */
public class OntologyService extends Service {

    private static final Logger log = LogManager.getLogger(OntologyService.class.getName());

    /**
     * Constructs a {@code OntologyService}.
     * 
     * @param serviceFactory            The {@code ServiceFactory} to be used to obtain {@code Service}s 
     *                                  and {@code DAOManager}.
     * @throws IllegalArgumentException If {@code serviceFactory} is {@code null}.
     */
    public OntologyService(ServiceFactory serviceFactory) {
        super(serviceFactory);
    }
        
    /**
     * Retrieve the {@code Ontology} of {@code AnatEntity}s for the requested species. 
     * <p>
     * The returned {@code Ontology} contains only the selected anat. entities, and only the
     * relations between them with a {@code RelationType} {@code ISA_PARTOF} are included.
     * 
     * @param speciesId         A {@code String} that is the ID of species 
     *                          which to retrieve anat. entities for. Can be {@code null}.
     * @param anatEntityIds     A {@code Collection} of {@code String}s that are IDs of anat.
     *                          entity IDs to retrieve. Can be {@code null} or empty.
     * @return                  The {@code Ontology} of {@code AnatEntity}s for the requested species, 
     *                          anat. entity, relations types, and relation status.
     */
    public Ontology<AnatEntity> getAnatEntityOntology(String speciesId, Collection<String> anatEntityIds) {
        log.entry(speciesId, anatEntityIds);
        return log.exit(this.getAnatEntityOntology(speciesId, anatEntityIds,
                EnumSet.of(RelationType.ISA_PARTOF), false, false));
    }

    /**
     * Retrieve the {@code Ontology} of {@code AnatEntity}s for the requested species, anatomical entities,
     * relations types, and relation status. 
     * <p>
     * The returned {@code Ontology} contains ancestors and/or descendants of the selected anat. entities 
     * according to {@code getAncestors} and {@code getDescendants}, respectively. 
     * If both {@code getAncestors} and {@code getDescendants} are {@code false}, 
     * then only relations between the selected anat. entities are retrieved.
     * 
     * @param speciesId         A {@code String} that is the ID of species 
     *                          which to retrieve anat. entities for. Can be {@code null}.
     * @param anatEntityIds     A {@code Collection} of {@code String}s that are IDs of anat.
     *                          entity IDs to retrieve. Can be {@code null} or empty.
     * @param relationTypes     A {@code Collection} of {@code RelationType}s that are the relation
     *                          types allowing to filter the relations between elements
     *                          of the {@code Ontology}.
     * @param getAncestors      A {@code boolean} defining whether the ancestors of the selected 
     *                          anat. entities, and the relations leading to them, should be retrieved.
     * @param getDescendants    A {@code boolean} defining whether the descendants of the selected 
     *                          anat. entities, and the relations leading to them, should be retrieved.
     * @return                  The {@code Ontology} of {@code AnatEntity}s for the requested species, 
     *                          anat. entity, relations types, and relation status.
     */
    public Ontology<AnatEntity> getAnatEntityOntology(String speciesId, Collection<String> anatEntityIds, 
            Collection<RelationType> relationTypes, boolean getAncestors, boolean getDescendants) {
        log.entry(speciesId, anatEntityIds, getAncestors, getDescendants, relationTypes);
        
        return log.exit(this.getAnatEntityOntology(Arrays.asList(speciesId), anatEntityIds, 
                relationTypes, getAncestors, getDescendants)
                .getAsSingleSpeciesOntology(speciesId));
    }

    /**
     * Retrieve the {@code MultiSpeciesOntology} of {@code AnatEntity}s for the requested species 
     * and anatomical entities. 
     * <p>
     * The returned {@code MultiSpeciesOntology} contains only the selected anat. entities, 
     * and only the relations between them with a {@code RelationType} {@code ISA_PARTOF} are included.
     * 
     * @param speciesIds        A {@code Collection} of {@code String}s that are IDs of species 
     *                          which to retrieve anat. entities for. If several IDs are provided, 
     *                          anat. entities existing in any of them will be retrieved. 
     *                          Can be {@code null} or empty.
     * @param anatEntityIds     A {@code Collection} of {@code String}s that are IDs of anat.
     *                          entity IDs to retrieve. Can be {@code null} or empty.
     * @return                  The {@code MultiSpeciesOntology} of {@code AnatEntity}s for the requested species 
     *                          and anat. entity.
     */
    public MultiSpeciesOntology<AnatEntity> getAnatEntityOntology(Collection<String> speciesIds, 
            Collection<String> anatEntityIds) {
        log.entry(speciesIds, anatEntityIds);
        return log.exit(this.getAnatEntityOntology(speciesIds, anatEntityIds,
                EnumSet.of(RelationType.ISA_PARTOF), false, false));
    }

    /**
     * Retrieve the {@code MultiSpeciesOntology} of {@code AnatEntity}s for the requested species, 
     * anatomical entities, relations types, and relation status. 
     * <p>
     * The returned {@code MultiSpeciesOntology} contains ancestors and/or descendants of the selected anat. entities 
     * according to {@code getAncestors} and {@code getDescendants}, respectively. 
     * If both {@code getAncestors} and {@code getDescendants} are {@code false}, 
     * then only relations between the selected anat. entities are retrieved.
     * 
     * @param speciesIds        A {@code Collection} of {@code String}s that are IDs of species 
     *                          which to retrieve anat. entities for. If several IDs are provided, 
     *                          anat. entities existing in any of them will be retrieved. 
     *                          Can be {@code null} or empty.
     * @param anatEntityIds     A {@code Collection} of {@code String}s that are IDs of anat.
     *                          entity IDs to retrieve. Can be {@code null} or empty.
     * @param relationTypes     A {@code Collection} of {@code RelationType}s that are the relation
     *                          types allowing to filter the relations between elements
     *                          of the {@code MultiSpeciesOntology}.
     * @param getAncestors      A {@code boolean} defining whether the ancestors of the selected 
     *                          anat. entities, and the relations leading to them, should be retrieved.
     * @param getDescendants    A {@code boolean} defining whether the descendants of the selected 
     *                          anat. entities, and the relations leading to them, should be retrieved.
     * @return                  The {@code MultiSpeciesOntology} of {@code AnatEntity}s for the requested species, 
     *                          anat. entity, relations types, and relation status.
     */
    public MultiSpeciesOntology<AnatEntity> getAnatEntityOntology(Collection<String> speciesIds, 
            Collection<String> anatEntityIds, Collection<RelationType> relationTypes, 
            boolean getAncestors, boolean getDescendants) {
        log.entry(speciesIds, anatEntityIds, getAncestors, getDescendants, relationTypes);
        
        Set<RelationTO> rels = this.getAnatEntityRelationTOs(speciesIds, anatEntityIds,
                relationTypes, getAncestors, getDescendants);
        Set<TaxonConstraint> relationTaxonConstraints = getServiceFactory().getTaxonConstraintService()
                    .loadAnatEntityRelationTaxonConstraintBySpeciesIds(speciesIds)
                    .collect(Collectors.toSet());
        Set<TaxonConstraint> taxonConstraints = getServiceFactory().getTaxonConstraintService()
                    .loadAnatEntityTaxonConstraintBySpeciesIds(speciesIds)
                    .collect(Collectors.toSet());
        return log.exit(new MultiSpeciesOntology<AnatEntity>(speciesIds, 
                this.getServiceFactory().getAnatEntityService()
                    .loadAnatEntities(speciesIds, true, this.getRequestedEntityIds(anatEntityIds, rels))
                    .collect(Collectors.toSet()), 
                rels, taxonConstraints, relationTaxonConstraints, relationTypes,
                this.getServiceFactory(), AnatEntity.class));
    }
    
    /**
     * Retrieve the {@code Ontology} of {@code DevStage}s for the requested species and 
     * developmental stages IDs.
     * <p>
     * The returned {@code Ontology} contains only {@code DevStage}s corresponding to 
     * the provided dev. stages IDs, and only the relations between them 
     * with a {@code RelationType} {@code ISA_PARTOF} are included. 
     * 
     * @param speciesId         A {@code String} that is the ID of species which to retrieve 
     *                          dev. stages for. Can be {@code null}.
     * @param devStageIds       A {@code Collection} of {@code String}s that are dev. stages IDs
     *                          of the {@code Ontology} to retrieve. Can be {@code null} or empty.
     * @return                  The {@code Ontology} of the {@code DevStage}s for the requested species 
     *                          and dev. stages.
     */
    public Ontology<DevStage> getDevStageOntology(String speciesId, Collection<String> devStageIds) {
        log.entry(speciesId, devStageIds);
        return this.getDevStageOntology(Arrays.asList(speciesId), devStageIds, false, false)
                .getAsSingleSpeciesOntology(speciesId);
    }

    /**
     * Retrieve the {@code Ontology} of {@code DevStage}s for the requested species, dev. stage IDs,
     * and relation status. 
     * <p>
     * The returned {@code Ontology} contains ancestors and/or descendants according to
     * {@code getAncestors} and {@code getDescendants}, respectively. 
     * If both {@code getAncestors} and {@code getDescendants} are {@code false}, 
     * then only relations between provided developmental stages are considered.
     * 
     * @param speciesId         A {@code String} that is the ID of species which to retrieve 
     *                          dev. stages for. Can be {@code null}.
     * @param devStageIds       A {@code Collection} of {@code String}s that are dev. stages IDs
     *                          of the {@code Ontology} to retrieve. Can be {@code null} or empty.
     * @param getAncestors      A {@code boolean} defining whether the ancestors of the selected 
     *                          dev. stages, and the relations leading to them, should be retrieved.
     * @param getDescendants    A {@code boolean} defining whether the descendants of the selected 
     *                          dev. stages, and the relations leading to them, should be retrieved.
     * @return                  The {@code Ontology} of the {@code DevStage}s for the requested species, 
     *                          dev. stages, and relation status. 
     */
    public Ontology<DevStage> getDevStageOntology(String speciesId, Collection<String> devStageIds, 
            boolean getAncestors, boolean getDescendants) {
        log.entry(speciesId, devStageIds, getAncestors, getDescendants);
        return log.exit(getDevStageOntology(Arrays.asList(speciesId), devStageIds, getAncestors, 
                getDescendants).getAsSingleSpeciesOntology(speciesId));
    }

    /**
     * Retrieve the {@code MultiSpeciesOntology} of {@code DevStage}s for the requested species and 
     * developmental stages IDs.
     * <p>
     * The returned {@code MultiSpeciesOntology} contains only {@code DevStage}s corresponding to 
     * the provided dev. stages IDs, and only the relations between them 
     * with a {@code RelationType} {@code ISA_PARTOF} are included. 
     * 
     * @param speciesIds        A {@code Collection} of {@code String}s that are IDs of species 
     *                          which to retrieve dev. stages for. If several IDs are provided, 
     *                          dev. stages existing in any of them will be retrieved. 
     *                          Can be {@code null} or empty.
     * @param devStageIds       A {@code Collection} of {@code String}s that are dev. stages IDs
     *                          of the {@code MultiSpeciesOntology} to retrieve. Can be {@code null} or empty.
     * @return                  The {@code MultiSpeciesOntology} of the {@code DevStage}s for the requested species 
     *                          and dev. stages.
     */
    public MultiSpeciesOntology<DevStage> getDevStageOntology(Collection<String> speciesIds, 
            Collection<String> devStageIds) {
        log.entry(speciesIds, devStageIds);
        return this.getDevStageOntology(speciesIds, devStageIds, false, false);
    }

    /**
     * Retrieve the {@code MultiSpeciesOntology} of {@code DevStage}s for the requested species, dev. stage IDs,
     * and relation status. 
     * <p>
     * The returned {@code MultiSpeciesOntology} contains ancestors and/or descendants according to
     * {@code getAncestors} and {@code getDescendants}, respectively. 
     * If both {@code getAncestors} and {@code getDescendants} are {@code false}, 
     * then only relations between provided developmental stages are considered.
     * 
     * @param speciesIds        A {@code Collection} of {@code String}s that are IDs of species 
     *                          which to retrieve dev. stages for. If several IDs are provided, 
     *                          dev. stages existing in any of them will be retrieved. 
     *                          Can be {@code null} or empty.
     * @param devStageIds       A {@code Collection} of {@code String}s that are dev. stages IDs
     *                          of the {@code MultiSpeciesOntology} to retrieve. Can be {@code null} or empty.
     * @param getAncestors      A {@code boolean} defining whether the ancestors of the selected 
     *                          dev. stages, and the relations leading to them, should be retrieved.
     * @param getDescendants    A {@code boolean} defining whether the descendants of the selected 
     *                          dev. stages, and the relations leading to them, should be retrieved.
     * @return                  The {@code MultiSpeciesOntology} of the {@code DevStage}s for the requested species, 
     *                          dev. stages, and relation status. 
     */
    public MultiSpeciesOntology<DevStage> getDevStageOntology(Collection<String> speciesIds, 
            Collection<String> devStageIds, boolean getAncestors, boolean getDescendants) {
        log.entry(speciesIds, devStageIds, getAncestors, getDescendants);
        
        Set<RelationTO> rels = this.getDevStageRelationTOs(speciesIds, devStageIds, 
                getAncestors, getDescendants);

        Set<TaxonConstraint> taxonConstraints = getServiceFactory().getTaxonConstraintService()
                .loadDevStageTaxonConstraintBySpeciesIds(speciesIds)
                .collect(Collectors.toSet());
        //there is no relation IDs for nested set models, so no TaxonConstraints. 
        //Relations simply exist if both the source and target of the relations 
        //exists in the targeted species.
        return log.exit(new MultiSpeciesOntology<DevStage>(speciesIds, 
                this.getServiceFactory().getDevStageService()
                .loadDevStages(speciesIds, true, this.getRequestedEntityIds(devStageIds, rels))
                .collect(Collectors.toSet()), 
                rels, taxonConstraints, null, EnumSet.of(RelationType.ISA_PARTOF), this.getServiceFactory(), DevStage.class));
    }
    
    private Set<RelationTO> getAnatEntityRelationTOs(Collection<String> speciesIds, Collection<String> entityIds, 
            Collection<RelationType> relationTypes, boolean getAncestors, boolean getDescendants) {
        QuadriFunction<Set<String>, Set<String>, Boolean, Set<RelationStatus>, RelationTOResultSet> fun = (s, t, b, r) ->
            getDaoManager().getRelationDAO().getAnatEntityRelations(
                speciesIds, true, s, t, b, 
                relationTypes.stream()
                .map(OntologyBase::convertRelationType)
                .collect(Collectors.toCollection(() -> 
                EnumSet.noneOf(RelationTO.RelationType.class))), 
                r, 
                null);
        return log.exit(getRelationTOs(fun, entityIds, getAncestors, getDescendants));
    }
    
    private Set<RelationTO> getDevStageRelationTOs(Collection<String> speciesIds, Collection<String> entityIds, 
            boolean getAncestors, boolean getDescendants) {
        QuadriFunction<Set<String>, Set<String>, Boolean, Set<RelationStatus>, RelationTOResultSet> fun = (s, t, b, r) ->
        getDaoManager().getRelationDAO().getStageRelations(
                speciesIds, true, s, t, b, r, null);
        return log.exit(getRelationTOs(fun, entityIds, getAncestors, getDescendants));
    }
    
    /**
     * Convenience method to retrieve {@code RelationTO}s for any {@code OntologyElement} type. 
     * 
     * @param elementType           A {@code Class<T>} that is the type of the elements 
     *                              for which to retrieve {@code RelationTO}s.
     * @param speciesIds            A {@code Collection} of {@code String}s that are IDs of species 
     *                              which to retrieve relations for. If several IDs are provided, 
     *                              relations valid in any of them will be retrieved. 
     *                              Can be {@code null} or empty.
     * @param entityIds             A {@code Collection} of {@code String}s that are IDs of 
     *                              entities to retrieve relations for. Can be {@code null} or empty.
     * @param relationTypes         A {@code Collection} of {@code RelationType}s that are the relation
     *                              types allowing to filter the relations to retrieve.
     * @param getAncestors          A {@code boolean} defining whether the ancestors of the selected 
     *                              entities, and the relations leading to them, should be retrieved.
     * @param getDescendants        A {@code boolean} defining whether the descendants of the selected 
     *                              entities, and the relations leading to them, should be retrieved.
     * @return                      A {@code Set} of {@code RelationTO}s that are relations between 
     *                              requested entities, and potentially also to their ancestors 
     *                              and/or their descendants.
     * @param <T>                   The type of elements for which to retrieve {@code RelationTO}s.
     */
    private <T extends NamedEntity & OntologyElement<T>> Set<RelationTO> getRelationTOs(
            QuadriFunction<Set<String>, Set<String>, Boolean, Set<RelationStatus>, RelationTOResultSet> relationRetrievalFun, 
            Collection<String> entityIds, boolean getAncestors, boolean getDescendants) {
        log.entry(relationRetrievalFun, entityIds, getAncestors, getDescendants);
        
        final Set<String> filteredEntities = Collections.unmodifiableSet(
                entityIds == null? new HashSet<>(): new HashSet<>(entityIds));
    
        // Currently, we use all non reflexive relations.
        //Warning: we absolutely need to retrieve indirect relations in case getAncestors is true 
        //or getDescendants is true
        Set<RelationStatus> relationStatus = EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE));
        
        //by default, include all ancestors and descendants of selected entities
        Set<String> sourceIds = filteredEntities;
        Set<String> targetIds = filteredEntities;
        boolean sourceOrTarget = true;
        if (!getAncestors && !getDescendants) {
            //request only relations between selected entities (constraints both sources and targets 
            //of considered relations to be one of the selected entities).
            sourceOrTarget = false;
        } else if (!getAncestors) {
            //to not get ancestors, we don't select relations where selected entities are sources
            sourceIds = null;
        } else if (!getDescendants) {
            //opposite if we don't want the descendants
            targetIds = null;
        }
        log.trace("sourceIds: {} - targetIds: {} - sourceOrTarget: {} - relationStatus: {}", 
                sourceIds, targetIds, sourceOrTarget, relationStatus);
        
        Set<RelationTO> relations = new HashSet<>();
        relations.addAll(relationRetrievalFun.apply(sourceIds, targetIds, sourceOrTarget, relationStatus)
                    .getAllTOs());
        //if it is requested to infer entities,  
        if (getAncestors || getDescendants) {
            assert sourceOrTarget: "Incorrect source/target condition status: sourceOrTarget should be true";
        
            Set<String> newSourceIds = new HashSet<>();
            Set<String> newTargetIds = new HashSet<>();
            if (getAncestors) {
                log.debug("get targets IDs of retrieved relations that become new source IDs");
                //get targets IDs of retrieved relations that become new source IDs
                newSourceIds.addAll(relations.stream().map(r -> r.getTargetId()).collect(Collectors.toSet()));
            }
            if (getDescendants) {
                log.debug("get source IDs of retrieved relations that become new target IDs");
                //get source IDs of retrieved relations that become new target IDs
                newTargetIds.addAll(relations.stream().map(r -> r.getSourceId()).collect(Collectors.toSet()));
            }
            if (getAncestors && getDescendants) {
                log.debug("getAncestors && getDescendants");
                // if we infer ancestors and descendants, we need to retrieve all relations of 
                // retrieved ancestors and descendants.
                newTargetIds.addAll(relations.stream().map(r -> r.getTargetId()).collect(Collectors.toSet()));
                newSourceIds.addAll(relations.stream().map(r -> r.getSourceId()).collect(Collectors.toSet()));
            }

            //Query only if new terms have been discovered
            if (!newSourceIds.isEmpty() || !newTargetIds.isEmpty()) {
                if (!newSourceIds.isEmpty()) newSourceIds.removeAll(sourceIds);
                if (!newTargetIds.isEmpty()) newTargetIds.removeAll(targetIds);
                log.debug("relationRetrievalFun={}, newSourceIds={}, newTargetIds={}",
                        relationRetrievalFun, newSourceIds, newTargetIds);
                relations.addAll(relationRetrievalFun.apply(newSourceIds, newTargetIds, 
                        sourceOrTarget, relationStatus).getAllTOs());
            }
        }
        
        return log.exit(relations);
    }
    /**
     * Convenience method to retrieve IDs of {@code OntologyElement}s to load, 
     * based on requested {@code OntologyElement} IDs and relations 
     * leading to other {@code OntologyElement}s
     * 
     * @param entityIds             A {@code Collection} of {@code String}s that are IDs of 
     *                              requested {@code OntologyElement}s. Can be {@code null} or empty.
     * @param relations             A {@code Collection} of {@code RelationTO}s that are relations 
     *                              between {@code OntologyElement}s.
     * @return                      A {@code Set} of {@code String}s that are IDs of 
     *                              {@code OntologyElement}s to load.
     */
    private Set<String> getRequestedEntityIds(Collection<String> entityIds, Collection<RelationTO> relations) {
        log.entry(entityIds, relations);
        //we retrieve objects corresponding to all the requested entities, 
        //plus their ancestors/descendants depending on the parameters. 
        //We cannot simply use the retrieved relations, as some entities 
        //might have no relations according to the requested parameters
        Set<String> requestedEntityIds = entityIds == null? new HashSet<>(): new HashSet<>(entityIds);
        //Warning: if filteredEntities is empty, then all entities are requested 
        //and we should not restrain the entities using the relations
        if (!requestedEntityIds.isEmpty()) {
            requestedEntityIds.addAll(relations.stream()
                    .flatMap(rel -> Stream.of(rel.getSourceId(), rel.getTargetId()))
                    .collect(Collectors.toSet()));
        }
        return log.exit(requestedEntityIds);
    }
}
