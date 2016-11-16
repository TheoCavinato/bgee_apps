package org.bgee.model.ontology;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.bgee.model.ServiceFactory;
import org.bgee.model.TestAncestor;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.anatdev.DevStage;
import org.bgee.model.anatdev.DevStageService;
import org.bgee.model.anatdev.TaxonConstraint;
import org.bgee.model.anatdev.TaxonConstraintService;
import org.bgee.model.dao.api.DAOManager;
import org.bgee.model.dao.api.ontologycommon.RelationDAO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationStatus;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTOResultSet;
import org.bgee.model.species.Taxon;
import org.bgee.model.species.TaxonService;
import org.junit.Ignore;
import org.junit.Test;

/**
 * This class holds the unit tests for the {@code OntologyService} class.
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 13, Nov. 2016
 * @since   Bgee 13, Dec. 2015
 */
public class OntologyServiceTest extends TestAncestor {

    /**
     * Test the method{@link OntologyService#getAnatEntityOntology(
     * Collection, Collection, boolean, boolean, AnatEntityService)}.
     */
    @Test
    public void shouldGetAnatEntityOntology() {
        DAOManager managerMock = mock(DAOManager.class);
        RelationDAO relationDao = mock(RelationDAO.class);
        when(managerMock.getRelationDAO()).thenReturn(relationDao);
        
        Set<String> speciesIds = new HashSet<String>(Arrays.asList("11", "22", "44"));
        Set<String> anatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0002", "UBERON:0002p"));

        Set<String> sourceAnatEntityIds = new HashSet<String>(anatEntityIds);
        Set<String> targetAnatEntityIds = new HashSet<String>(anatEntityIds);
                
        Set<RelationTO.RelationType> daoRelationTypes1 = new HashSet<>(
                Arrays.asList(RelationTO.RelationType.ISA_PARTOF, RelationTO.RelationType.DEVELOPSFROM));

        Set<RelationTO.RelationType> daoRelationTypes23 = new HashSet<>(
                Arrays.asList(RelationTO.RelationType.ISA_PARTOF));

        Set<RelationStatus> relationStatus = EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE));
        // UBERON:0001 ------------------
        // | is_a       \ dev_from      |
        // UBERON:0002   UBERON:0002p   | is_a (indirect)
        // | is_a       / is_a          |
        // UBERON:0003 ------------------
        
        List<RelationTO> allRelations = Arrays.asList(
                new RelationTO("1", "UBERON:0002", "UBERON:0001", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("2", "UBERON:0002p", "UBERON:0001", RelationTO.RelationType.DEVELOPSFROM, RelationStatus.DIRECT),
                new RelationTO("3", "UBERON:0003", "UBERON:0002", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("4", "UBERON:0003", "UBERON:0002p", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("5", "UBERON:0003", "UBERON:0001", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT));

        List<RelationTO> relationTOs1 = Arrays.asList(allRelations.get(0), allRelations.get(1),
                allRelations.get(2), allRelations.get(3));
        RelationTOResultSet mockRelationRs1 = getMockResultSet(RelationTOResultSet.class, relationTOs1);
        when(relationDao.getAnatEntityRelations(
        		speciesIds, true, sourceAnatEntityIds, targetAnatEntityIds, true, 
        		daoRelationTypes1, relationStatus, null))
        	.thenReturn(mockRelationRs1);

        Set<String> newSourceAnatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0001", "UBERON:0003"));
        Set<String> newTargetAnatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0001", "UBERON:0003"));
        RelationTOResultSet mockRelationRs1b = getMockResultSet(RelationTOResultSet.class, allRelations);
        when(relationDao.getAnatEntityRelations(speciesIds, true, newSourceAnatEntityIds,
                newTargetAnatEntityIds, true, daoRelationTypes1, relationStatus, null))
            .thenReturn(mockRelationRs1b);

        List<RelationTO> relationTOs2 = Arrays.asList(allRelations.get(0), allRelations.get(1));
        RelationTOResultSet mockRelationRs2 = getMockResultSet(RelationTOResultSet.class, relationTOs2);
        when(relationDao.getAnatEntityRelations(speciesIds, true, sourceAnatEntityIds, null, true, 
        		daoRelationTypes23, relationStatus, null)).thenReturn(mockRelationRs2);
        
        RelationTOResultSet mockRelationRs2b = getMockResultSet(RelationTOResultSet.class, new ArrayList<>());
        when(relationDao.getAnatEntityRelations(speciesIds, true, new HashSet<String>(Arrays.asList("UBERON:0001")), 
                new HashSet<>(), true, daoRelationTypes23, relationStatus, null))
            .thenReturn(mockRelationRs2b);

        List<RelationTO> relationTOs3 = new ArrayList<>();
        RelationTOResultSet mockRelationRs3 = getMockResultSet(RelationTOResultSet.class, relationTOs3);
        when(relationDao.getAnatEntityRelations(
        		speciesIds, true, sourceAnatEntityIds, targetAnatEntityIds, false, 
        		daoRelationTypes23, relationStatus, null))
        	.thenReturn(mockRelationRs3);

        ServiceFactory serviceFactory = mock(ServiceFactory.class);
        when(serviceFactory.getDAOManager()).thenReturn(managerMock);
        AnatEntityService anatEntityService = mock(AnatEntityService.class);
        when(serviceFactory.getAnatEntityService()).thenReturn(anatEntityService);
        
        Set<AnatEntity> anatEntities1 = new HashSet<>(Arrays.asList(
        		new AnatEntity("1", "UBERON:0001", "desc1"),
        		new AnatEntity("2", "UBERON:0002", "desc2"),
        		new AnatEntity("3", "UBERON:0002p", "desc2p"),
        		new AnatEntity("4", "UBERON:0003", "desc3")));
        Stream<AnatEntity> anatEntityStream1 = anatEntities1.stream();
        Set<String> expAnatEntityIds1 = new HashSet<String>(
        		Arrays.asList("UBERON:0001", "UBERON:0002", "UBERON:0002p", "UBERON:0003"));
        when(anatEntityService.loadAnatEntities(speciesIds, true, expAnatEntityIds1))
        	.thenReturn(anatEntityStream1);

        Set<AnatEntity> anatEntities2 = new HashSet<>(Arrays.asList(
        		new AnatEntity("1", "UBERON:0001", "desc1"),
        		new AnatEntity("2", "UBERON:0002", "desc2"),
        		new AnatEntity("3", "UBERON:0002p", "desc2p")));
        Stream<AnatEntity> anatEntityStream2 = anatEntities2.stream();
        Set<String> expAnatEntityIds2 = new HashSet<String>(
        		Arrays.asList("UBERON:0001", "UBERON:0002", "UBERON:0002p"));
        when(anatEntityService.loadAnatEntities(speciesIds, true, expAnatEntityIds2))
        	.thenReturn(anatEntityStream2);

        Set<AnatEntity> anatEntities3 = new HashSet<>(Arrays.asList(
        		new AnatEntity("2", "UBERON:0002", "desc2"),
        		new AnatEntity("3", "UBERON:0002p", "desc2p")));
        Stream<AnatEntity> anatEntityStream3 = anatEntities3.stream();
        Set<String> expAnatEntityIds3 = new HashSet<String>(
        		Arrays.asList("UBERON:0002", "UBERON:0002p"));
        when(anatEntityService.loadAnatEntities(speciesIds, true, expAnatEntityIds3))
        	.thenReturn(anatEntityStream3);

        
        TaxonConstraintService tcService = mock(TaxonConstraintService.class);
        when(serviceFactory.getTaxonConstraintService()).thenReturn(tcService);

        Set<TaxonConstraint> taxonConstraints = new HashSet<>(Arrays.asList(
                // UBERON:0001 sp1/sp2/sp3 --------------------
                // |                    \                      |
                // UBERON:0002 sp1/sp2   UBERON:0002p sp2/sp3  | 
                // |                    /                      |
                // UBERON:0003 sp1/sp2 ------------------------
                new TaxonConstraint("UBERON:0001", null),
                new TaxonConstraint("UBERON:0002", "11"),
                new TaxonConstraint("UBERON:0002", "22"),
                new TaxonConstraint("UBERON:0002p", "22"),
                new TaxonConstraint("UBERON:0002p", "33"),
                new TaxonConstraint("UBERON:0003", "11"),
                new TaxonConstraint("UBERON:0003", "22")));
        // Note: we need to use thenReturn() twice because a stream can be use only once 
        when(tcService.loadAnatEntityTaxonConstraintBySpeciesIds(speciesIds))
            .thenReturn(taxonConstraints.stream()).thenReturn(taxonConstraints.stream())
            .thenReturn(taxonConstraints.stream()).thenReturn(taxonConstraints.stream())
            .thenReturn(taxonConstraints.stream()).thenReturn(taxonConstraints.stream());
        
        Set<TaxonConstraint> relationTaxonConstraints = new HashSet<>(Arrays.asList(
                // UBERON:0001 ------------------
                // | sp1/sp2   \ sp2            |
                // UBERON:0002   UBERON:0002p   | sp2/sp1 (indirect)
                // | sp1       / sp2            |
                // UBERON:0003 ------------------
                new TaxonConstraint("1", "11"),
                new TaxonConstraint("1", "22"),
                new TaxonConstraint("2", "22"),
                new TaxonConstraint("3", "11"),
                new TaxonConstraint("4", "22"),
                new TaxonConstraint("5", "11"),
                new TaxonConstraint("5", "22")));
        // Note: we need to use thenReturn() twice because a stream can be use only once 
        when(tcService.loadAnatEntityRelationTaxonConstraintBySpeciesIds(speciesIds))
            .thenReturn(relationTaxonConstraints.stream()).thenReturn(relationTaxonConstraints.stream())
            .thenReturn(relationTaxonConstraints.stream()).thenReturn(relationTaxonConstraints.stream())
            .thenReturn(relationTaxonConstraints.stream()).thenReturn(relationTaxonConstraints.stream());
        
        Set<RelationType> expRelationTypes1 = new HashSet<>(Arrays.asList(
                RelationType.ISA_PARTOF, RelationType.DEVELOPSFROM));
        
        Set<RelationType> expRelationTypes23 = new HashSet<>(Arrays.asList(RelationType.ISA_PARTOF));

        OntologyService service = new OntologyService(serviceFactory);

        OntologyBase<AnatEntity> expectedOntology1 = 
        		new MultiSpeciesOntology<>(speciesIds, anatEntities1, new HashSet<>(allRelations),
        		        taxonConstraints, relationTaxonConstraints,
        		        expRelationTypes1, serviceFactory, AnatEntity.class);
        assertEquals("Incorrect anatomical entity ontology",
                expectedOntology1, service.getAnatEntityOntology(speciesIds, anatEntityIds,
                		expRelationTypes1, true, true));
        
        OntologyBase<AnatEntity> expectedOntology2 = 
        		new MultiSpeciesOntology<>(speciesIds, anatEntities2, new HashSet<>(relationTOs2),
                        taxonConstraints, relationTaxonConstraints,
        		        expRelationTypes23, serviceFactory, AnatEntity.class);
        assertEquals("Incorrect anatomical entity ontology",
                expectedOntology2, service.getAnatEntityOntology(speciesIds, anatEntityIds,
                		expRelationTypes23, true, false));
        
        OntologyBase<AnatEntity> expectedOntology3 = 
        		new MultiSpeciesOntology<>(speciesIds, anatEntities3, new HashSet<>(relationTOs3),
                        taxonConstraints, relationTaxonConstraints,
        		        expRelationTypes23, serviceFactory, AnatEntity.class);
        assertEquals("Incorrect anatomical entity ontology",
                expectedOntology3, service.getAnatEntityOntology(speciesIds, anatEntityIds,
                		expRelationTypes23, false, false));
    }
    
    /**
     * Test the method{@link OntologyService#getAnatEntityOntology(
     * Collection, Collection, boolean, boolean, AnatEntityService)}.
     * <p>
     * Regression test: missed relations of inferred entities.
     */
    @Test
    public void shouldGetAnatEntityOntology_gettingAllRelations() {
        DAOManager managerMock = mock(DAOManager.class);
        RelationDAO relationDao = mock(RelationDAO.class);
        when(managerMock.getRelationDAO()).thenReturn(relationDao);
        
        Set<String> speciesIds = new HashSet<String>(Arrays.asList("11", "22", "44"));
        Set<String> anatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0004"));

        Set<String> sourceAnatEntityIds = new HashSet<String>(anatEntityIds);
        Set<String> targetAnatEntityIds = new HashSet<String>(anatEntityIds);

        Set<RelationTO.RelationType> daoRelationTypes = new HashSet<>(
                Arrays.asList(RelationTO.RelationType.ISA_PARTOF));

        Set<RelationStatus> relationStatus = EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE));

        // UBERON:0001 is_a UBERON:0002 is_a UBERON:0003 is_a UBERON:0004
        List<RelationTO> allRelations = Arrays.asList(
                new RelationTO("1", "UBERON:0002", "UBERON:0001", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("2", "UBERON:0003", "UBERON:0002", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("3", "UBERON:0003", "UBERON:0001", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT),     
                new RelationTO("4", "UBERON:0004", "UBERON:0003", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO("5", "UBERON:0004", "UBERON:0002", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT),
                new RelationTO("6", "UBERON:0004", "UBERON:0001", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        List<RelationTO> relationTOs1 = Arrays.asList(allRelations.get(3), allRelations.get(4), allRelations.get(5));

        RelationTOResultSet mockRelationRs1 = getMockResultSet(RelationTOResultSet.class, relationTOs1);
        when(relationDao.getAnatEntityRelations(
                speciesIds, true, sourceAnatEntityIds, targetAnatEntityIds, true, 
                daoRelationTypes, relationStatus, null))
            .thenReturn(mockRelationRs1);

        Set<String> newSourceAnatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0001", "UBERON:0002", "UBERON:0003"));
        Set<String> newTargetAnatEntityIds = new HashSet<String>(Arrays.asList("UBERON:0001", "UBERON:0002", "UBERON:0003"));
        List<RelationTO> relationTOs1b = Arrays.asList(allRelations.get(0), allRelations.get(1), allRelations.get(2));
        RelationTOResultSet mockRelationRs1b = getMockResultSet(RelationTOResultSet.class, relationTOs1b);
        when(relationDao.getAnatEntityRelations(
                speciesIds, true, newSourceAnatEntityIds, newTargetAnatEntityIds, true, 
                daoRelationTypes, relationStatus, null))
            .thenReturn(mockRelationRs1b);

        ServiceFactory serviceFactory = mock(ServiceFactory.class);
        when(serviceFactory.getDAOManager()).thenReturn(managerMock);
        AnatEntityService anatEntityService = mock(AnatEntityService.class);
        when(serviceFactory.getAnatEntityService()).thenReturn(anatEntityService);
        
        Set<AnatEntity> anatEntities = new HashSet<>(Arrays.asList(
                new AnatEntity("1", "UBERON:0001", "desc1"),
                new AnatEntity("2", "UBERON:0002", "desc2"),
                new AnatEntity("3", "UBERON:0003", "desc3"),
                new AnatEntity("4", "UBERON:0004", "desc4")));
        Stream<AnatEntity> anatEntityStream1 = anatEntities.stream();
        Set<String> expAnatEntityIds1 = new HashSet<String>(
                Arrays.asList("UBERON:0001", "UBERON:0002", "UBERON:0003", "UBERON:0004"));
        when(anatEntityService.loadAnatEntities(speciesIds, true, expAnatEntityIds1))
            .thenReturn(anatEntityStream1);

        TaxonConstraintService tcService = mock(TaxonConstraintService.class);
        when(serviceFactory.getTaxonConstraintService()).thenReturn(tcService);

        Set<TaxonConstraint> taxonConstraints = new HashSet<>(Arrays.asList(
                new TaxonConstraint("UBERON:0001", null),
                new TaxonConstraint("UBERON:0002", null),
                new TaxonConstraint("UBERON:0003", null),
                new TaxonConstraint("UBERON:0004", null)));
        // Note: we need to use thenReturn() twice because a stream can be use only once 
        when(tcService.loadAnatEntityTaxonConstraintBySpeciesIds(speciesIds))
            .thenReturn(taxonConstraints.stream()).thenReturn(taxonConstraints.stream());
        
        Set<TaxonConstraint> relationTaxonConstraints = new HashSet<>(Arrays.asList(
                new TaxonConstraint("1", null),
                new TaxonConstraint("2", null),
                new TaxonConstraint("3", null),
                new TaxonConstraint("4", null),
                new TaxonConstraint("5", null),
                new TaxonConstraint("6", null)));
        // Note: we need to use thenReturn() twice because a stream can be use only once 
        when(tcService.loadAnatEntityRelationTaxonConstraintBySpeciesIds(speciesIds))
            .thenReturn(relationTaxonConstraints.stream()).thenReturn(relationTaxonConstraints.stream());
        
        Set<RelationType> expRelationTypes = new HashSet<>(Arrays.asList(RelationType.ISA_PARTOF));

        OntologyService service = new OntologyService(serviceFactory);

        OntologyBase<AnatEntity> expectedOntology1 = 
                new MultiSpeciesOntology<>(speciesIds, anatEntities, new HashSet<>(allRelations),
                        taxonConstraints, relationTaxonConstraints,
                        expRelationTypes, serviceFactory, AnatEntity.class);
        assertEquals("Incorrect anatomical entity ontology",
                expectedOntology1, service.getAnatEntityOntology(speciesIds, anatEntityIds,
                        expRelationTypes, true, true));
    }

    /**
     * Test the method 
     * {@link OntologyService#getDevStageOntology(Collection, boolean, boolean, DevStageService)}.
     */
    @Test
    public void shouldGetStageOntology() {
        DAOManager managerMock = mock(DAOManager.class);
        RelationDAO relationDao = mock(RelationDAO.class);
        when(managerMock.getRelationDAO()).thenReturn(relationDao);
        
        Set<String> speciesIds = new HashSet<String>(Arrays.asList("11", "22", "44"));
        Set<String> stageIds = new HashSet<String>(Arrays.asList("Stage_id1", "Stage_id2"));

        Set<String> sourceStageIds = new HashSet<String>(stageIds);
        Set<String> targetStageIds = new HashSet<String>(stageIds);
                
        // Stage_id1 -----------------
        // | is_a       \ is_a        |
        // Stage_id2     Stage_id2p   | is_a (indirect)
        // | is_a       / is_a        |
        // Stage_id3 -----------------

        Set<RelationStatus> relationStatus = EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE));

        List<RelationTO> allRelationTOs = Arrays.asList(
        		new RelationTO(null, "Stage_id2", "Stage_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        		new RelationTO(null, "Stage_id2p", "Stage_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        		new RelationTO(null, "Stage_id3", "Stage_id2", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        		new RelationTO(null, "Stage_id3", "Stage_id2p", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
        		new RelationTO(null, "Stage_id3", "Stage_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        List<RelationTO> relationTOs1 = Arrays.asList(allRelationTOs.get(0),
                allRelationTOs.get(1), allRelationTOs.get(2), allRelationTOs.get(4));
        RelationTOResultSet mockRelationRs1 = getMockResultSet(RelationTOResultSet.class, relationTOs1);
        when(relationDao.getStageRelations(speciesIds, true, sourceStageIds, targetStageIds,
        		true, relationStatus, null))
        	.thenReturn(mockRelationRs1);
        
        Set<String> newSourceStageIds = new HashSet<String>(Arrays.asList("Stage_id2p", "Stage_id3"));
        Set<String> newTargetStageIds = new HashSet<String>(Arrays.asList("Stage_id2p", "Stage_id3"));
        List<RelationTO> relationTOs1b = Arrays.asList(allRelationTOs.get(1),
                allRelationTOs.get(2), allRelationTOs.get(3), allRelationTOs.get(4));
        RelationTOResultSet mockRelationRs1b = getMockResultSet(RelationTOResultSet.class, relationTOs1b);
        when(relationDao.getStageRelations(speciesIds, true, newSourceStageIds, newTargetStageIds,
                true, relationStatus, null))
            .thenReturn(mockRelationRs1b);

        List<RelationTO> relationTOs2 = Arrays.asList(allRelationTOs.get(0),
                allRelationTOs.get(1), allRelationTOs.get(2), allRelationTOs.get(4));
        RelationTOResultSet mockRelationRs2 = getMockResultSet(RelationTOResultSet.class, relationTOs2);
        when(relationDao.getStageRelations(speciesIds, true, null, targetStageIds,
        		true, relationStatus, null))
        	.thenReturn(mockRelationRs2);
        List<RelationTO> relationTOs2b = Arrays.asList(allRelationTOs.get(3));
        RelationTOResultSet mockRelationRs2b = getMockResultSet(RelationTOResultSet.class, relationTOs2b);
        when(relationDao.getStageRelations(speciesIds, true, new HashSet<>(), 
                new HashSet<String>(Arrays.asList("Stage_id2p", "Stage_id3")), true, relationStatus, null))
            .thenReturn(mockRelationRs2b);
        
        List<RelationTO> relationTOs3 = Arrays.asList(
        		new RelationTO(null, "Stage_id2", "Stage_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        RelationTOResultSet mockRelationRs3 = getMockResultSet(RelationTOResultSet.class, relationTOs3);
        when(relationDao.getStageRelations(speciesIds, true, sourceStageIds, targetStageIds,
        		false, relationStatus, null))
        	.thenReturn(mockRelationRs3);

        ServiceFactory serviceFactory = mock(ServiceFactory.class);
        when(serviceFactory.getDAOManager()).thenReturn(managerMock);
        DevStageService devStageService = mock(DevStageService.class);
        when(serviceFactory.getDevStageService()).thenReturn(devStageService);

        Set<DevStage> devStages1 = new HashSet<>(Arrays.asList(
        		new DevStage("1", "Stage_id1", "desc1", 0, 0, 0, false, false),
        		new DevStage("2", "Stage_id2", "desc2", 0, 0, 0, false, false),
        		new DevStage("3", "Stage_id2p", "desc2p", 0, 0, 0, false, false),
        		new DevStage("4", "Stage_id3", "desc3", 0, 0, 0, false, false)));
        Set<String> expStageIds1 = new HashSet<String>(
        		Arrays.asList("Stage_id1", "Stage_id2", "Stage_id2p", "Stage_id3"));
        when(devStageService.loadDevStages(speciesIds, true, expStageIds1))
            .thenReturn(devStages1.stream()).thenReturn(devStages1.stream());

        Set<DevStage> devStages2 = new HashSet<>(Arrays.asList(
        		new DevStage("1", "Stage_id1", "desc1", 0, 0, 0, false, false),
        		new DevStage("2", "Stage_id2", "desc2", 0, 0, 0, false, false),
                new DevStage("3", "Stage_id2p", "desc2p", 0, 0, 0, false, false),
        		new DevStage("4", "Stage_id3", "desc3", 0, 0, 0, false, false)));
        Set<String> expStageIds2 = new HashSet<String>(
        		Arrays.asList("Stage_id1", "Stage_id2", "Stage_id3"));
        when(devStageService.loadDevStages(speciesIds, true, expStageIds2))
            .thenReturn(devStages2.stream()).thenReturn(devStages2.stream());

        Set<DevStage> devStages3 = new HashSet<>(Arrays.asList(
        		new DevStage("1", "Stage_id1", "desc1", 0, 0, 0, false, false),
        		new DevStage("2", "Stage_id2", "desc2", 0, 0, 0, false, false)));
        Set<String> expStageIds3 = new HashSet<String>(Arrays.asList("Stage_id1", "Stage_id2"));
        when(devStageService.loadDevStages(speciesIds, true, expStageIds3)).thenReturn(devStages3.stream());

        TaxonConstraintService tcService = mock(TaxonConstraintService.class);
        when(serviceFactory.getTaxonConstraintService()).thenReturn(tcService);
        Set<TaxonConstraint> stageTCs = 
                // stage1 sp1/sp2 -------
                // |               \     \    
                // stage2 sp1/sp2   |     stage2p sp2
                // |               /      | 
                // stage3 sp1             stage3p sp2
                new HashSet<>(Arrays.asList(
                        new TaxonConstraint("stage1", null),
                        new TaxonConstraint("stage2", null),
                        new TaxonConstraint("stage2p", "sp2"),
                        new TaxonConstraint("stage3", "sp1"),
                        new TaxonConstraint("stage3p", "sp2")));

        // Note: we need to use thenReturn() twice because a stream can be use only once
        when(tcService.loadDevStageTaxonConstraintBySpeciesIds(speciesIds))
            .thenReturn(stageTCs.stream()).thenReturn(stageTCs.stream())
            .thenReturn(stageTCs.stream()).thenReturn(stageTCs.stream())
            .thenReturn(stageTCs.stream()).thenReturn(stageTCs.stream());

        Set<RelationType> expRelationTypes = new HashSet<>();
        expRelationTypes.add(RelationType.ISA_PARTOF);

        OntologyService service = new OntologyService(serviceFactory);

        MultiSpeciesOntology<DevStage> expectedOntology1 = 
        		new MultiSpeciesOntology<>(speciesIds, devStages1, new HashSet<>(allRelationTOs),
        		        stageTCs, null,
        		        expRelationTypes, serviceFactory, DevStage.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology1,
        		service.getDevStageOntology(speciesIds, stageIds, true, true));

        MultiSpeciesOntology<DevStage> expectedOntology2 = 
        		new MultiSpeciesOntology<>(speciesIds, devStages2, new HashSet<>(allRelationTOs),
        		        stageTCs, null,
        		        expRelationTypes, serviceFactory, DevStage.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology2,
        		service.getDevStageOntology(speciesIds, stageIds, false, true));

        MultiSpeciesOntology<DevStage> expectedOntology3 = 
        		new MultiSpeciesOntology<>(speciesIds, devStages3, new HashSet<>(relationTOs3),
        		        stageTCs, null,
        		        expRelationTypes, serviceFactory, DevStage.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology3, 
        		service.getDevStageOntology(speciesIds, stageIds, false, false));
    }
    
    /**
     * Test the method 
     * {@link OntologyService#getTaxonOntology(java.util.Collection, java.util.Collection, boolean, boolean)}.
     */
    @Test
    @Ignore("An error was found in recovery of taxonomy with parameters. The method was disabled")
    public void shouldGetTaxonOntology() {
        
        DAOManager managerMock = mock(DAOManager.class);
        RelationDAO relationDao = mock(RelationDAO.class);
        when(managerMock.getRelationDAO()).thenReturn(relationDao);
        
        Set<String> speciesIds = new HashSet<String>();
        speciesIds.addAll(Arrays.asList("11", "22", "44"));
        Set<String> taxonIds = new HashSet<String>();
        taxonIds.addAll(Arrays.asList("tax_id1", "tax_id2"));

        Set<String> sourceTaxonIds = new HashSet<String>();
        sourceTaxonIds.addAll(taxonIds);
        Set<String> targetTaxonIds = new HashSet<String>();
        targetTaxonIds.addAll(taxonIds);
                
        // Tax_id1 --------------
        // | is_a    \ is_a      |
        // Tax_id2     Tax_id2p  | is_a (indirect)
        // | is_a    / is_a      |
        // Tax_id3 --------------

        Set<RelationTO.RelationType> daoRelationTypes = new HashSet<>();
        daoRelationTypes.add(RelationTO.RelationType.ISA_PARTOF);
        
        List<RelationTO> relationTOs1 = Arrays.asList(
                new RelationTO(null, "tax_id2", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO(null, "tax_id2p", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO(null, "tax_id3", "tax_id2", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO(null, "tax_id3", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        RelationTOResultSet mockRelationRs1 = getMockResultSet(RelationTOResultSet.class, relationTOs1);
        when(relationDao.getTaxonRelations(sourceTaxonIds, targetTaxonIds,
                true, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
            .thenReturn(mockRelationRs1);

        sourceTaxonIds = new HashSet<>(Arrays.asList("tax_id3", "tax_id2p"));
        targetTaxonIds = new HashSet<>(Arrays.asList("tax_id3", "tax_id2p"));
        List<RelationTO> relationTOs1b = Arrays.asList(
            new RelationTO(null, "tax_id2p", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
            new RelationTO(null, "tax_id3", "tax_id2p", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        RelationTOResultSet mockRelationRs1b = getMockResultSet(RelationTOResultSet.class, relationTOs1b);
        when(relationDao.getTaxonRelations(sourceTaxonIds, targetTaxonIds,
                true, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
            .thenReturn(mockRelationRs1b);

        targetTaxonIds = new HashSet<>(Arrays.asList("tax_id1", "tax_id2"));
        List<RelationTO> relationTOs2 = Arrays.asList(
                new RelationTO(null, "tax_id2", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
                new RelationTO(null, "tax_id3", "tax_id2", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        RelationTOResultSet mockRelationRs2 = getMockResultSet(RelationTOResultSet.class, relationTOs2);
        when(relationDao.getTaxonRelations(null, targetTaxonIds,
                true, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
            .thenReturn(mockRelationRs2);
        
        targetTaxonIds = new HashSet<>(Arrays.asList("tax_id3"));
        List<RelationTO> relationTOs2b = Arrays.asList(
            new RelationTO(null, "tax_id3", "tax_id2", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT),
            new RelationTO(null, "tax_id3", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.INDIRECT));
        RelationTOResultSet mockRelationRs2b = getMockResultSet(RelationTOResultSet.class, relationTOs2b);
        when(relationDao.getTaxonRelations(new HashSet<>(), targetTaxonIds,
                true, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
            .thenReturn(mockRelationRs2b);

        sourceTaxonIds = new HashSet<>(Arrays.asList("tax_id1", "tax_id2"));
        targetTaxonIds = new HashSet<>(Arrays.asList("tax_id1", "tax_id2"));
        List<RelationTO> relationTOs3 = Arrays.asList(
                new RelationTO(null, "tax_id2", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        RelationTOResultSet mockRelationRs3 = getMockResultSet(RelationTOResultSet.class, relationTOs3);
        when(relationDao.getTaxonRelations(sourceTaxonIds, targetTaxonIds,
                false, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
            .thenReturn(mockRelationRs3);
        
        List<RelationTO> relationTOs3b = Arrays.asList(
            new RelationTO(null, "tax_id2", "tax_id1", RelationTO.RelationType.ISA_PARTOF, RelationStatus.DIRECT));
        RelationTOResultSet mockRelationRs3b = getMockResultSet(RelationTOResultSet.class, relationTOs3b);
        when(relationDao.getTaxonRelations(sourceTaxonIds, targetTaxonIds,
            false, EnumSet.complementOf(EnumSet.of(RelationStatus.REFLEXIVE)), null))
        .thenReturn(mockRelationRs3b);

        ServiceFactory serviceFactory = mock(ServiceFactory.class);
        when(serviceFactory.getDAOManager()).thenReturn(managerMock);
        TaxonService taxonService = mock(TaxonService.class);
        when(serviceFactory.getTaxonService()).thenReturn(taxonService);

        Set<Taxon> taxa1 = new HashSet<>(Arrays.asList(
                new Taxon("1", "tax_id1", "desc1"),
                new Taxon("2", "tax_id2", "desc2"),
                new Taxon("3", "tax_id2p", "desc2p"),
                new Taxon("4", "tax_id3", "desc3")));
        Set<Taxon> taxa2 = new HashSet<>(Arrays.asList(
                new Taxon("1", "tax_id1", "desc1"),
                new Taxon("2", "tax_id2", "desc2"),
                new Taxon("4", "tax_id3", "desc3")));
        Set<Taxon> taxa3 = new HashSet<>(Arrays.asList(
                new Taxon("1", "tax_id1", "desc1"),
                new Taxon("2", "tax_id2", "desc2")));
        when(taxonService.loadTaxa(speciesIds, true)).thenReturn(taxa1.stream())
        .thenReturn(taxa2.stream()).thenReturn(taxa3.stream());

        Set<RelationType> expRelationTypes = new HashSet<>();
        expRelationTypes.add(RelationType.ISA_PARTOF);

        OntologyService service = new OntologyService(serviceFactory);

        Set<RelationTO> rel1 = new HashSet<>();
        rel1.addAll(relationTOs1);
        rel1.addAll(relationTOs1b);
        MultiSpeciesOntology<Taxon> expectedOntology1 = 
                new MultiSpeciesOntology<>(speciesIds, taxa1, rel1,
                    null, new HashSet<>(), expRelationTypes, serviceFactory, Taxon.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology1,
                service.getTaxonOntology(speciesIds, taxonIds, true, true));

        Set<RelationTO> rel2 = new HashSet<>();
        rel2.addAll(relationTOs2);
        rel2.addAll(relationTOs2b);
        MultiSpeciesOntology<Taxon> expectedOntology2 = 
                new MultiSpeciesOntology<>(speciesIds, taxa2, rel2,
                    null, new HashSet<>(), expRelationTypes, serviceFactory, Taxon.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology2,
                service.getTaxonOntology(speciesIds, taxonIds, false, true));

        Set<RelationTO> rel3 = new HashSet<>();
        rel3.addAll(relationTOs3);
        rel3.addAll(relationTOs3b);
        MultiSpeciesOntology<Taxon> expectedOntology3 = 
                new MultiSpeciesOntology<>(speciesIds, taxa3, rel3,
                    null, new HashSet<>(), expRelationTypes, serviceFactory, Taxon.class);
        assertEquals("Incorrect dev. stage ontology", expectedOntology3, 
                service.getTaxonOntology(speciesIds, taxonIds, false, false));
    }
}
