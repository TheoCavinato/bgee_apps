package org.bgee.pipeline.expression.downloadfile;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO.AnatEntityTO;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO.AnatEntityTOResultSet;
import org.bgee.model.dao.api.anatdev.StageDAO;
import org.bgee.model.dao.api.anatdev.StageDAO.StageTO;
import org.bgee.model.dao.api.anatdev.StageDAO.StageTOResultSet;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO.DataState;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.ExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallParams;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.NoExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallParams;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTOResultSet;
import org.bgee.model.dao.api.ontologycommon.RelationDAO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO;
import org.bgee.model.dao.api.ontologycommon.RelationDAO.RelationTO.RelationType;
import org.bgee.model.dao.api.species.SpeciesDAO.SpeciesTO;
import org.bgee.model.dao.api.species.SpeciesDAO.SpeciesTOResultSet;
import org.bgee.model.dao.mysql.anatdev.MySQLAnatEntityDAO.MySQLAnatEntityTOResultSet;
import org.bgee.model.dao.mysql.anatdev.MySQLStageDAO.MySQLStageTOResultSet;
import org.bgee.model.dao.mysql.expressiondata.MySQLExpressionCallDAO.MySQLExpressionCallTOResultSet;
import org.bgee.model.dao.mysql.expressiondata.MySQLNoExpressionCallDAO.MySQLNoExpressionCallTOResultSet;
import org.bgee.model.dao.mysql.gene.MySQLGeneDAO.MySQLGeneTOResultSet;
import org.bgee.model.dao.mysql.ontologycommon.MySQLRelationDAO.MySQLRelationTOResultSet;
import org.bgee.model.dao.mysql.species.MySQLSpeciesDAO.MySQLSpeciesTOResultSet;
import org.bgee.pipeline.TestAncestor;
import org.bgee.pipeline.Utils;
import org.bgee.pipeline.expression.downloadfile.GenerateDiffExprFile.SingleSpDiffExprFileType;
import org.bgee.pipeline.expression.downloadfile.GenerateExprFile.SingleSpExprFileType;
import org.bgee.pipeline.expression.downloadfile.GenerateExprFile.ExpressionData;
import org.bgee.pipeline.expression.downloadfile.GenerateExprFile.ObservedData;
import org.junit.Test;
import org.supercsv.cellprocessor.constraint.IsElementOf;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.constraint.StrNotNullOrEmpty;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;

/**
 * Unit tests for {@link GenerateExprFile}
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 */
public class GenerateExprFileTest extends GenerateDownloadFileTest {

    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = LogManager.getLogger(GenerateExprFileTest.class.getName());

    public GenerateExprFileTest(){
        super();
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Test method {@link GenerateDownloadFile#convertToFileTypes(Collection, Class)} 
     * used with a {@code ExprFileType}.
     */
    @Test
    public void shouldConvertToFyleTypes() {
        //all expression file types
        Set<SingleSpExprFileType> exprExpectedFileTypes = new HashSet<SingleSpExprFileType>(
                EnumSet.of(SingleSpExprFileType.EXPR_SIMPLE, SingleSpExprFileType.EXPR_COMPLETE));
        assertEquals("Incorrect ExprFileTypes retrieved", exprExpectedFileTypes, 
                GenerateDownloadFile.convertToFileTypes(Arrays.asList(
                        SingleSpExprFileType.EXPR_SIMPLE.getStringRepresentation(), 
                        SingleSpExprFileType.EXPR_COMPLETE.getStringRepresentation()), 
                        SingleSpExprFileType.class));
        assertEquals("Incorrect ExprFileTypes retrieved", exprExpectedFileTypes, 
                GenerateDownloadFile.convertToFileTypes(Arrays.asList(
                        SingleSpExprFileType.EXPR_SIMPLE.name(), 
                        SingleSpExprFileType.EXPR_COMPLETE.name()), 
                        SingleSpExprFileType.class));
        
        //only one expression file type
        exprExpectedFileTypes = new HashSet<SingleSpExprFileType>(
                EnumSet.of(SingleSpExprFileType.EXPR_COMPLETE));
        assertEquals("Incorrect ExprFileTypes retrieved", exprExpectedFileTypes, 
                GenerateDownloadFile.convertToFileTypes(Arrays.asList(
                        SingleSpExprFileType.EXPR_COMPLETE.getStringRepresentation()), 
                        SingleSpExprFileType.class));
        assertEquals("Incorrect ExprFileTypes retrieved", exprExpectedFileTypes, 
                GenerateDownloadFile.convertToFileTypes(Arrays.asList(
                        SingleSpExprFileType.EXPR_COMPLETE.name()), 
                        SingleSpExprFileType.class));
        
        //test exceptions
        try {
            //existing FileType name, but incorrect type provided
            GenerateDownloadFile.convertToFileTypes(Arrays.asList(
                    SingleSpExprFileType.EXPR_COMPLETE.getStringRepresentation()), 
                    SingleSpDiffExprFileType.class);
            //test failed, exception not thrown as expected
            throw log.throwing(new AssertionError("IllegalArgumentException not thrown as expected"));
        } catch (IllegalArgumentException e) {
            //test passed
        }
        try {
            //non-existing FileType name
            GenerateDownloadFile.convertToFileTypes(Arrays.asList("whatever"), 
                    SingleSpExprFileType.class);
            //test failed, exception not thrown as expected
            throw log.throwing(new AssertionError("IllegalArgumentException not thrown as expected"));
        } catch (IllegalArgumentException e) {
            //test passed
        }
    }

    /**
     * Test {@link GenerateDownloadFile#generateSingleSpeciesFiles(List, List, String)},
     * which is the central method of the class doing all the job.
     */
    @Test
    public void shouldGenerateBasicExprFiles() throws IOException {

        Set<String> speciesIds = new HashSet<String>(Arrays.asList("11", "22")); 
        
        // First, we need a mock MySQLDAOManager, for the class to acquire mock DAOs. 
        MockDAOManager mockManager = new MockDAOManager();
        
        SpeciesTOResultSet mockSpeciesTORs = this.mockGetSpecies(mockManager, speciesIds);
        GeneTOResultSet mockGeneTORs = this.mockGetGenes(mockManager, speciesIds);
        AnatEntityTOResultSet mockAnatEntityTORs = this.mockGetAnatEntities(mockManager, speciesIds);
        StageTOResultSet mockStageTORs = this.mockGetStages(mockManager, speciesIds);
        
        // For each species, we need to mock getNonInformativeAnatEntities(), getExpressionCalls() 
        // and getNoExpressionCalls() (basic and global calls)
        
        //// Species 11
        speciesIds = new HashSet<String>(Arrays.asList("11")); 

        // Non informative anatomical entities
        MySQLAnatEntityTOResultSet mockAnatEntityRsSp11 = createMockDAOResultSet(
                Arrays.asList(
                        new AnatEntityTO("NonInfoAnatEnt1", null, null, null, null, null)),
                        MySQLAnatEntityTOResultSet.class);
        when(mockManager.mockAnatEntityDAO.getNonInformativeAnatEntitiesBySpeciesIds(speciesIds)).
                thenReturn(mockAnatEntityRsSp11);
        
        //stage relations
        MySQLRelationTOResultSet mockStageRelationRsSp11 = createMockDAOResultSet(
                Arrays.asList(
                        new RelationTO("Stage_id1", "ParentStage_id1"), 
                        new RelationTO("Stage_id2", "ParentStage_id2"), 
                        new RelationTO("ParentStage_id1", "ParentStage_id1"), 
                        new RelationTO("ParentStage_id2", "ParentStage_id2"), 
                        new RelationTO("Stage_id1", "Stage_id1"), 
                        new RelationTO("Stage_id2", "Stage_id2")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getStageRelationsBySpeciesIds(speciesIds, null)).
                thenReturn(mockStageRelationRsSp11);

        // Anatomical entity relations
        MySQLRelationTOResultSet mockOrganRelationRsSp11 = createMockDAOResultSet(
                Arrays.asList(
                        new RelationTO("Anat_id1", "Anat_id1"),
                        new RelationTO("Anat_id2", "Anat_id2"),
                        new RelationTO("Anat_id2", "Anat_id1"),
                        new RelationTO("Anat_id3", "Anat_id3"),
                        new RelationTO("Anat_id3", "Anat_id2"),
                        new RelationTO("NonInfoAnatEnt1", "NonInfoAnatEnt1")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getAnatEntityRelationsBySpeciesIds(speciesIds, 
                EnumSet.of(RelationType.ISA_PARTOF), null)).thenReturn(mockOrganRelationRsSp11);

        // Basic expression calls
        MySQLExpressionCallTOResultSet mockBasicExprRsSp11 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id1", 
                                DataState.NODATA, DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "ParentStage_id2", 
                                DataState.LOWQUALITY, DataState.HIGHQUALITY, DataState.LOWQUALITY,
                                DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id2", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),

                        new ExpressionCallTO(null, "ID2", "Anat_id1", "Stage_id2", 
                                DataState.NODATA, DataState.LOWQUALITY, DataState.NODATA, 
                                DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                        new ExpressionCallTO(null, "ID2", "Anat_id3", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.NODATA, 
                                DataState.HIGHQUALITY, DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                        new ExpressionCallTO(null, "ID2", "NonInfoAnatEnt1", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.NODATA, 
                                DataState.HIGHQUALITY, DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null)),
                        MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams basicExprParams11 = new ExpressionCallParams();
        basicExprParams11.addAllSpeciesIds(speciesIds);
        basicExprParams11.setIncludeSubstructures(false);
        basicExprParams11.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(basicExprParams11))).
                thenReturn(mockBasicExprRsSp11);
        
        // Global expression calls
        MySQLExpressionCallTOResultSet mockGlobalExprRsSp11 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id1", 
                                DataState.NODATA, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, DataState.LOWQUALITY, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
//                        Call will be propagated by GenerateDownloadFile
//                        new ExpressionCallTO(null, "ID1", "Anat_id1", "ParentStage_id1", 
//                                DataState.NODATA, DataState.LOWQUALITY, 
//                                DataState.LOWQUALITY, DataState.LOWQUALITY, true, true, 
//                                null/*ExpressionCallTO.OriginOfLine.SELF*/, 
//                                null/*ExpressionCallTO.OriginOfLine.DESCENT*/, false),
                          //this call had a BOTH stage OriginOfLine, it was decomposed into 
                          //two SELF calls in sub-stage and parent stage.
//                        new ExpressionCallTO(null, "ID1", "Anat_id1", "ParentStage_id2", 
//                                DataState.HIGHQUALITY, DataState.HIGHQUALITY, 
//                                DataState.LOWQUALITY, DataState.NODATA, true, true, 
//                                null/*ExpressionCallTO.OriginOfLine.SELF*/, 
//                                null/*ExpressionCallTO.OriginOfLine.BOTH*/, true),
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "ParentStage_id2", 
                                DataState.LOWQUALITY, DataState.HIGHQUALITY, 
                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
                        new ExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id2", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
                                
                        new ExpressionCallTO(null, "ID2", "Anat_id1", "Stage_id2", 
                                DataState.NODATA, DataState.LOWQUALITY, 
                                DataState.NODATA, DataState.NODATA, true, false,
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),                                
                        new ExpressionCallTO(null, "ID2", "Anat_id1", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
                                DataState.HIGHQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.BOTH,
                                null/*ExpressionCallTO.OriginOfLine.DESCENT*/, null/*false*/),
                        new ExpressionCallTO(null, "ID2", "Anat_id2", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.NODATA, 
                                DataState.HIGHQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.DESCENT,
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*false*/),
                        new ExpressionCallTO(null, "ID2", "Anat_id3", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.NODATA, 
                                DataState.HIGHQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
                        new ExpressionCallTO(null, "ID2", "NonInfoAnatEnt1", "ParentStage_id2", 
                                DataState.HIGHQUALITY, DataState.NODATA, 
                                DataState.HIGHQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/)),
                        MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams globalExprParams11 = new ExpressionCallParams();
        globalExprParams11.addAllSpeciesIds(speciesIds);
        globalExprParams11.setIncludeSubstructures(true);
        globalExprParams11.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(globalExprParams11))).
                thenReturn(mockGlobalExprRsSp11);

        // No-expression calls
        MySQLNoExpressionCallTOResultSet mockNoExprRsSp11 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id1", 
                                DataState.HIGHQUALITY, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, false, NoExpressionCallTO.OriginOfLine.SELF),
                                
                        new NoExpressionCallTO(null, "ID2", "Anat_id1", "Stage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, false, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID2", "Anat_id3", "ParentStage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, false, NoExpressionCallTO.OriginOfLine.SELF)),
                        MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams noExprParams11 = new NoExpressionCallParams();
        noExprParams11.addAllSpeciesIds(speciesIds);
        noExprParams11.setIncludeParentStructures(false);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(noExprParams11))).
                thenReturn(mockNoExprRsSp11);

        // Global no-expression calls
        MySQLNoExpressionCallTOResultSet mockGlobalNoExprRsSp11 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "ID1", "Anat_id1", "Stage_id1", 
                                DataState.HIGHQUALITY, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, true, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID1", "Anat_id2", "Stage_id1", 
                                DataState.HIGHQUALITY, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, true, NoExpressionCallTO.OriginOfLine.PARENT),
                                
                        new NoExpressionCallTO(null, "ID2", "Anat_id1", "Stage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID2", "Anat_id2", "Stage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.PARENT),
                        new NoExpressionCallTO(null, "ID2", "Anat_id3", "ParentStage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID2", "Anat_id3", "Stage_id2", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.PARENT)),
                        MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams globalNoExprParams11 = new NoExpressionCallParams();
        globalNoExprParams11.addAllSpeciesIds(speciesIds);
        globalNoExprParams11.setIncludeParentStructures(true);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(globalNoExprParams11))).
                thenReturn(mockGlobalNoExprRsSp11);

        //// Species 22
        speciesIds = new HashSet<String>(Arrays.asList("22")); 

        // Non informative anatomical entities
        MySQLAnatEntityTOResultSet mockAnatEntityRsSp22 = createMockDAOResultSet(
                Arrays.asList(
                        new AnatEntityTO("NonInfoAnatEnt2", null, null, null, null, null)),
                        MySQLAnatEntityTOResultSet.class);
        when(mockManager.mockAnatEntityDAO.getNonInformativeAnatEntitiesBySpeciesIds(speciesIds)).
                thenReturn(mockAnatEntityRsSp22);

        //stage relations
        MySQLRelationTOResultSet mockStageRelationRsSp22 = createMockDAOResultSet(
                Arrays.asList(
                        new RelationTO("Stage_id5", "ParentStage_id5"), 
                        new RelationTO("ParentStage_id5", "ParentStage_id5"),
                        new RelationTO("Stage_id2", "Stage_id2"), 
                        new RelationTO("Stage_id5", "Stage_id5")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getStageRelationsBySpeciesIds(speciesIds, null)).
                thenReturn(mockStageRelationRsSp22);

        // Anatomical entity relations
        MySQLRelationTOResultSet mockOrganRelationRsSp22 = createMockDAOResultSet(
                Arrays.asList(
                        new RelationTO("Anat_id1", "Anat_id1"),
                        new RelationTO("Anat_id4", "Anat_id4"),
                        new RelationTO("Anat_id4", "Anat_id1"),
                        new RelationTO("Anat_id5", "Anat_id5"),
                        new RelationTO("Anat_id5", "Anat_id4")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getAnatEntityRelationsBySpeciesIds(speciesIds, 
                EnumSet.of(RelationType.ISA_PARTOF), null)).thenReturn(mockOrganRelationRsSp22);

        // Expression calls
        MySQLExpressionCallTOResultSet mockBasicExprRsSp22 = createMockDAOResultSet(
                Arrays.asList(
                        new ExpressionCallTO(null, "ID3", "Anat_id1", "Stage_id2", 
                                DataState.NODATA, DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.HIGHQUALITY, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                        new ExpressionCallTO(null, "ID3", "Anat_id5", "Stage_id2", 
                                DataState.LOWQUALITY, DataState.LOWQUALITY, DataState.HIGHQUALITY, 
                                DataState.LOWQUALITY, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null),
                                
                        new ExpressionCallTO(null, "ID5", "Anat_id4", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null)),
                        MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams basicExprParams22 = new ExpressionCallParams();
        basicExprParams22.addAllSpeciesIds(speciesIds);
        basicExprParams22.setIncludeSubstructures(false);
        basicExprParams22.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(basicExprParams22))).
                thenReturn(mockBasicExprRsSp22);

        // Global expression calls
        MySQLExpressionCallTOResultSet mockGlobalExprRsSp22 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "ID3", "Anat_id1", "Stage_id2", 
                                DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.HIGHQUALITY, DataState.HIGHQUALITY, true, false,
                                ExpressionCallTO.OriginOfLine.BOTH,  
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
                        new ExpressionCallTO(null, "ID3", "Anat_id4", "Stage_id2", 
                                DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, true, false, 
                                ExpressionCallTO.OriginOfLine.DESCENT,  
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*false*/),
                        new ExpressionCallTO(null, "ID3", "Anat_id5", "Stage_id2", 
                                DataState.LOWQUALITY, DataState.LOWQUALITY, 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF,  
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/),
                        new ExpressionCallTO(null, "ID5", "Anat_id1", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.DESCENT,  
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*false*/),
                          //this call will be obtained by propagation of the previous one
//                        new ExpressionCallTO(null, "ID5", "Anat_id1", "ParentStage_id5", 
//                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
//                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
//                                ExpressionCallTO.OriginOfLine.DESCENT,  
//                                null/*ExpressionCallTO.OriginOfLine.DESCENT*/, null/*false*/),
                        new ExpressionCallTO(null, "ID5", "Anat_id4", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF,  
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/)
                          //this call will be obtained by propagation of the previous one
//                        , new ExpressionCallTO(null, "ID5", "Anat_id4", "ParentStage_id5", 
//                                DataState.HIGHQUALITY, DataState.LOWQUALITY, 
//                                DataState.LOWQUALITY, DataState.NODATA, true, false, 
//                                ExpressionCallTO.OriginOfLine.SELF,  
//                                null/*ExpressionCallTO.OriginOfLine.DESCENT*/, null/*false*/)
                        ),
                        MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams globalExprParams22 = new ExpressionCallParams();
        globalExprParams22.addAllSpeciesIds(speciesIds);
        globalExprParams22.setIncludeSubstructures(true);
        globalExprParams22.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(globalExprParams22))).
                thenReturn(mockGlobalExprRsSp22);

        // No-expression calls
        MySQLNoExpressionCallTOResultSet mockBasicNoExprRsSp22 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "ID4", "Anat_id1", "Stage_id5", 
                                DataState.NODATA, DataState.HIGHQUALITY, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, false, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID4", "Anat_id4", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, false, NoExpressionCallTO.OriginOfLine.SELF),
                                
                        new NoExpressionCallTO(null, "ID5", "Anat_id4", "Stage_id5", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, false, NoExpressionCallTO.OriginOfLine.SELF)),
                        MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams basicNoExprParams22 = new NoExpressionCallParams();
        basicNoExprParams22.addAllSpeciesIds(speciesIds);
        basicNoExprParams22.setIncludeParentStructures(false);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(basicNoExprParams22))).
                thenReturn(mockBasicNoExprRsSp22);

        // Global no-expression calls
        MySQLNoExpressionCallTOResultSet mockGlobalNoExprRsSp22 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "ID4", "Anat_id1", "Stage_id5", 
                                DataState.NODATA, DataState.HIGHQUALITY, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID4", "Anat_id4", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.HIGHQUALITY, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.BOTH),
                        new NoExpressionCallTO(null, "ID4", "Anat_id5", "Stage_id5", 
                                DataState.HIGHQUALITY, DataState.HIGHQUALITY, DataState.HIGHQUALITY, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.PARENT),
                                
                        new NoExpressionCallTO(null, "ID5", "Anat_id4", "Stage_id5", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.SELF),
                        new NoExpressionCallTO(null, "ID5", "Anat_id5", "Stage_id5", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.PARENT)),
                        MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams globalNoExprParams22 = new NoExpressionCallParams();
        globalNoExprParams22.addAllSpeciesIds(speciesIds);
        globalNoExprParams22.setIncludeParentStructures(true);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(globalNoExprParams22))).
                thenReturn(mockGlobalNoExprRsSp22);

        String directory = testFolder.newFolder("tmpFolder").getPath();
        Set<SingleSpExprFileType> fileTypes = new HashSet<SingleSpExprFileType>(
                Arrays.asList(SingleSpExprFileType.EXPR_SIMPLE, SingleSpExprFileType.EXPR_COMPLETE)); 
        GenerateExprFile generate = new GenerateExprFile(mockManager, 
                Arrays.asList("11", "22"), fileTypes, directory);
        
        generate.generateExprFiles();
        
        String outputSimpleFile11 = new File(directory, "Genus11_species11_" + 
                SingleSpExprFileType.EXPR_SIMPLE + GenerateDownloadFile.EXTENSION).getAbsolutePath();
        String outputSimpleFile22 = new File(directory, "Genus22_species22_" + 
                SingleSpExprFileType.EXPR_SIMPLE + GenerateDownloadFile.EXTENSION).getAbsolutePath();
        String outputAdvancedFile11 = new File(directory, "Genus11_species11_" + 
                SingleSpExprFileType.EXPR_COMPLETE + GenerateDownloadFile.EXTENSION).getAbsolutePath();
        String outputAdvancedFile22 = new File(directory, "Genus22_species22_" + 
                SingleSpExprFileType.EXPR_COMPLETE + GenerateDownloadFile.EXTENSION).getAbsolutePath();

        assertExpressionFile(outputSimpleFile11, "11", true, 6);
        assertExpressionFile(outputAdvancedFile11, "11", false, 8);
        assertExpressionFile(outputSimpleFile22, "22", true, 5);
        assertExpressionFile(outputAdvancedFile22, "22", false, 9);

        // Verify that all ResultSet are closed.
        verify(mockSpeciesTORs).close();
        verify(mockGeneTORs).close();
        verify(mockAnatEntityTORs).close();
        verify(mockStageTORs).close();
        
        verify(mockAnatEntityRsSp11).close();
        verify(mockStageRelationRsSp11).close();
        verify(mockGlobalExprRsSp11).close();
        verify(mockGlobalNoExprRsSp11).close();
        
        verify(mockAnatEntityRsSp22).close();
        verify(mockStageRelationRsSp22).close();
        verify(mockGlobalExprRsSp22).close();
        verify(mockGlobalNoExprRsSp22).close();
        
        //check that the connection was closed at each species iteration
        verify(mockManager.mockManager, times(2)).releaseResources();

        // Verify that setAttributes are correctly called.
        verify(mockManager.mockAnatEntityDAO, times(2)).setAttributes(AnatEntityDAO.Attribute.ID);
        verify(mockManager.mockRelationDAO, times(4)).setAttributes(
                RelationDAO.Attribute.SOURCE_ID, RelationDAO.Attribute.TARGET_ID);
        verify(mockManager.mockExpressionCallDAO, times(4)).setAttributes(
                // All Attributes except ID, anat and stage OriginOfLines
                EnumSet.complementOf(EnumSet.of(ExpressionCallDAO.Attribute.ID, 
                        ExpressionCallDAO.Attribute.OBSERVED_DATA, 
                        ExpressionCallDAO.Attribute.STAGE_ORIGIN_OF_LINE)));
        verify(mockManager.mockNoExpressionCallDAO, times(4)).setAttributes(
                // All Attributes except ID
                EnumSet.complementOf(EnumSet.of(NoExpressionCallDAO.Attribute.ID)));
        
        verify(mockManager.mockAnatEntityDAO, times(1)).setAttributes(
                AnatEntityDAO.Attribute.ID, AnatEntityDAO.Attribute.NAME);
        verify(mockManager.mockGeneDAO, times(1)).setAttributes(
                GeneDAO.Attribute.ID, GeneDAO.Attribute.NAME);
        verify(mockManager.mockStageDAO, times(1)).setAttributes(
                StageDAO.Attribute.ID, StageDAO.Attribute.NAME);
    }

    /**
     * Test if exception is launch when resume equals to NODATA using 
     * {@link GenerateDownloadFile#generateSingleSpeciesFiles(List, List, String)},
     * which is the central method of the class doing all the job.
     */
    @Test
    public void shouldGenerateBasicExprFilesNoDataException() throws IOException {

        // First, we need a mock MySQLDAOManager, for the class to acquire mock DAOs. 
        MockDAOManager mockManager = new MockDAOManager();

        // Species 
        MySQLSpeciesTOResultSet mockSpeciesTORs33 = createMockDAOResultSet(
                Arrays.asList(new SpeciesTO("33", null, "Genus33", "species33", null, null, null, null)),
                MySQLSpeciesTOResultSet.class);
        when(mockManager.mockSpeciesDAO.getSpeciesByIds(
                new HashSet<String>(Arrays.asList("33")))).thenReturn(mockSpeciesTORs33);

        Set<String> speciesIds = new HashSet<String>(Arrays.asList("33")); 
        // Gene names
        MySQLGeneTOResultSet mockGeneTORs33 = createMockDAOResultSet(
                Arrays.asList(new GeneTO("IDX", "genNX", null)), MySQLGeneTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockGeneDAO.getGenesBySpeciesIds(speciesIds)). thenReturn(mockGeneTORs33);

        // Stage names
        MySQLStageTOResultSet mockStageTORs33 = createMockDAOResultSet(Arrays.asList(
                new StageTO("Stage_idX", "stageNX", null, null, null, null, null, null)),
                MySQLStageTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockStageDAO.getStagesBySpeciesIds(speciesIds)).thenReturn(mockStageTORs33);

        // Anatomical entity names
        MySQLAnatEntityTOResultSet mockAnatEntityTORs33 = createMockDAOResultSet(
                Arrays.asList(new AnatEntityTO("Anat_idX", "anatNameX", null, null, null, null)),
                MySQLAnatEntityTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockAnatEntityDAO.getAnatEntitiesBySpeciesIds(speciesIds)).
        thenReturn(mockAnatEntityTORs33);

        // Non informative anatomical entities
        MySQLAnatEntityTOResultSet mockAnatEntityRsSp33 = createMockDAOResultSet(
                Arrays.asList(new AnatEntityTO("NonInfoAnatEnt3", null, null, null, null, null)),
                MySQLAnatEntityTOResultSet.class);
        when(mockManager.mockAnatEntityDAO.getNonInformativeAnatEntitiesBySpeciesIds(speciesIds)).
        thenReturn(mockAnatEntityRsSp33);
        
        //stage relations
        MySQLRelationTOResultSet mockStageRelationRsSp33 = createMockDAOResultSet(
                Arrays.asList(new RelationTO("Stage_idX", "Stage_idX")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getStageRelationsBySpeciesIds(speciesIds, null)).
                thenReturn(mockStageRelationRsSp33);

        // Anatomical entity relations
        MySQLRelationTOResultSet mockOrganRelationRsSp33 = createMockDAOResultSet(
                Arrays.asList(new RelationTO("Anat_idX", "Anat_idX")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getAnatEntityRelationsBySpeciesIds(speciesIds, 
                EnumSet.of(RelationType.ISA_PARTOF), null)).thenReturn(mockOrganRelationRsSp33);

        // Expression calls
        MySQLExpressionCallTOResultSet mockBasicExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null)),
                MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams basicExprParams33 = new ExpressionCallParams();
        basicExprParams33.addAllSpeciesIds(speciesIds);
        basicExprParams33.setIncludeSubstructures(false);
        basicExprParams33.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(basicExprParams33))).
                thenReturn(mockBasicExprRsSp33);
        
        // Global expression calls
        MySQLExpressionCallTOResultSet mockGlobalExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, DataState.NODATA, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/)),
                MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams globalExprParams33 = new ExpressionCallParams();
        globalExprParams33.addAllSpeciesIds(speciesIds);
        globalExprParams33.setIncludeSubstructures(true);
        globalExprParams33.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(globalExprParams33))).
                thenReturn(mockGlobalExprRsSp33);

        // No-expression calls
        MySQLNoExpressionCallTOResultSet mockBasicNoExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, false, NoExpressionCallTO.OriginOfLine.SELF)),
                MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams basicNoExprParams33 = new NoExpressionCallParams();
        basicNoExprParams33.addAllSpeciesIds(speciesIds);
        basicNoExprParams33.setIncludeParentStructures(false);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(basicNoExprParams33))).
                thenReturn(mockBasicNoExprRsSp33);

        // Global no-expression calls
        MySQLNoExpressionCallTOResultSet mockGlobalNoExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, true, NoExpressionCallTO.OriginOfLine.SELF)),
                MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams globalNoExprParams33 = new NoExpressionCallParams();
        globalNoExprParams33.addAllSpeciesIds(speciesIds);
        globalNoExprParams33.setIncludeParentStructures(true);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(globalNoExprParams33))).
                thenReturn(mockGlobalNoExprRsSp33);

        try {
            String directory = testFolder.newFolder("tmpFolder").getPath();
            Set<SingleSpExprFileType> fileTypes = new HashSet<SingleSpExprFileType>(
                Arrays.asList(SingleSpExprFileType.EXPR_SIMPLE, SingleSpExprFileType.EXPR_COMPLETE)); 
            GenerateExprFile generate = new GenerateExprFile(mockManager, 
                Arrays.asList("33"), fileTypes, directory);
            generate.generateExprFiles();            
            // test failed
            throw new AssertionError("generateExprFiles did not throw " +
                "an IllegalStateException as expected");
        } catch (IllegalStateException e) {
            // test passed, do nothing
        }
        // Verify that all ResultSet are closed.
        verify(mockSpeciesTORs33).close();
        verify(mockGeneTORs33).close();
        verify(mockStageTORs33).close();
        verify(mockAnatEntityTORs33).close();
        verify(mockAnatEntityRsSp33).close();
        verify(mockStageRelationRsSp33).close();
        verify(mockGlobalExprRsSp33).close();
        verify(mockGlobalNoExprRsSp33).close();
        
        //check that the connection was closed at each species iteration
        verify(mockManager.mockManager, times(1)).releaseResources();

        // Verify that setAttributes are correctly called.
        verify(mockManager.mockAnatEntityDAO, times(1)).setAttributes(AnatEntityDAO.Attribute.ID);
        verify(mockManager.mockRelationDAO, times(2)).setAttributes(
                RelationDAO.Attribute.SOURCE_ID, RelationDAO.Attribute.TARGET_ID);
        verify(mockManager.mockExpressionCallDAO, times(2)).setAttributes(
                // All Attributes except ID, anat and stage OriginOfLines
                EnumSet.complementOf(EnumSet.of(ExpressionCallDAO.Attribute.ID, 
                        ExpressionCallDAO.Attribute.OBSERVED_DATA, 
                        ExpressionCallDAO.Attribute.STAGE_ORIGIN_OF_LINE)));
        verify(mockManager.mockNoExpressionCallDAO, times(2)).setAttributes(
                // All Attributes except ID
                EnumSet.complementOf(EnumSet.of(NoExpressionCallDAO.Attribute.ID)));
        
        verify(mockManager.mockAnatEntityDAO, times(1)).setAttributes(
                AnatEntityDAO.Attribute.ID, AnatEntityDAO.Attribute.NAME);
        verify(mockManager.mockGeneDAO, times(1)).setAttributes(
                GeneDAO.Attribute.ID, GeneDAO.Attribute.NAME);
        verify(mockManager.mockStageDAO, times(1)).setAttributes(
                StageDAO.Attribute.ID, StageDAO.Attribute.NAME);
    }
    
    /**
     * Test if exception is launch when an expression call and a no-expression call 
     * have data for the same data type using 
     * {@link GenerateDownloadFile#generateSingleSpeciesFiles(List, List, String)},
     * which is the central method of the class doing all the job.
     */
    @Test
    public void shouldGenerateBasicExprFilesDataConflictException() throws IOException {
        // First, we need a mock MySQLDAOManager, for the class to acquire mock DAOs. 
        MockDAOManager mockManager = new MockDAOManager();

        // Species 
        MySQLSpeciesTOResultSet mockSpeciesTORs33 = createMockDAOResultSet(
                Arrays.asList(new SpeciesTO("33", null, "Genus33", "species33", null, null, null, null)),
                MySQLSpeciesTOResultSet.class);
        when(mockManager.mockSpeciesDAO.getSpeciesByIds(
                new HashSet<String>(Arrays.asList("33")))).thenReturn(mockSpeciesTORs33);

        Set<String> speciesIds = new HashSet<String>(Arrays.asList("33")); 
        // Gene names
        MySQLGeneTOResultSet mockGeneTORs33 = createMockDAOResultSet(
                Arrays.asList(new GeneTO("IDX", "genNX", null)), MySQLGeneTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockGeneDAO.getGenesBySpeciesIds(speciesIds)). thenReturn(mockGeneTORs33);

        // Stage names
        MySQLStageTOResultSet mockStageTORs33 = createMockDAOResultSet(Arrays.asList(
                new StageTO("Stage_idX", "stageNX", null, null, null, null, null, null)),
                MySQLStageTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockStageDAO.getStagesBySpeciesIds(speciesIds)).thenReturn(mockStageTORs33);

        // Anatomical entity names
        MySQLAnatEntityTOResultSet mockAnatEntityTORs33 = createMockDAOResultSet(
                Arrays.asList(new AnatEntityTO("Anat_idX", "anatNameX", null, null, null, null)),
                MySQLAnatEntityTOResultSet.class);
        // The only Attributes requested should be ID and name, this will be checked 
        // at the end of the test
        when(mockManager.mockAnatEntityDAO.getAnatEntitiesBySpeciesIds(speciesIds)).
        thenReturn(mockAnatEntityTORs33);

        // Non informative anatomical entities
        MySQLAnatEntityTOResultSet mockAnatEntityRsSp33 = createMockDAOResultSet(
                Arrays.asList(new AnatEntityTO("NonInfoAnatEnt3", null, null, null, null, null)),
                MySQLAnatEntityTOResultSet.class);
        when(mockManager.mockAnatEntityDAO.getNonInformativeAnatEntitiesBySpeciesIds(speciesIds)).
        thenReturn(mockAnatEntityRsSp33);
        
        //stage relations
        MySQLRelationTOResultSet mockStageRelationRsSp33 = createMockDAOResultSet(
                Arrays.asList(new RelationTO("Stage_idX", "Stage_idX")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getStageRelationsBySpeciesIds(speciesIds, null)).
                thenReturn(mockStageRelationRsSp33);
        // Anatomical entity relations
        MySQLRelationTOResultSet mockOrganRelationRsSp33 = createMockDAOResultSet(
                Arrays.asList(new RelationTO("Anat_idX", "Anat_idX")),
                        MySQLRelationTOResultSet.class);
        when(mockManager.mockRelationDAO.getAnatEntityRelationsBySpeciesIds(speciesIds, 
                EnumSet.of(RelationType.ISA_PARTOF), null)).thenReturn(mockOrganRelationRsSp33);

        // Expression calls
        MySQLExpressionCallTOResultSet mockBasicExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, DataState.NODATA, false, false, 
                                ExpressionCallTO.OriginOfLine.SELF, null, null)),
                MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams basicExprParams33 = new ExpressionCallParams();
        basicExprParams33.addAllSpeciesIds(speciesIds);
        basicExprParams33.setIncludeSubstructures(false);
        basicExprParams33.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(basicExprParams33))).
                thenReturn(mockBasicExprRsSp33);

        // Global expression calls
        MySQLExpressionCallTOResultSet mockGlobalExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new ExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, DataState.LOWQUALITY, true, false, 
                                ExpressionCallTO.OriginOfLine.SELF, 
                                null/*ExpressionCallTO.OriginOfLine.SELF*/, null/*true*/)),
                                MySQLExpressionCallTOResultSet.class);
        ExpressionCallParams globalExprParams33 = new ExpressionCallParams();
        globalExprParams33.addAllSpeciesIds(speciesIds);
        globalExprParams33.setIncludeSubstructures(true);
        globalExprParams33.setIncludeSubStages(false);
        when(mockManager.mockExpressionCallDAO.getExpressionCalls(
                (ExpressionCallParams) TestAncestor.valueCallParamEq(globalExprParams33))).
                thenReturn(mockGlobalExprRsSp33);

        // No-expression calls
        MySQLNoExpressionCallTOResultSet mockBasicNoExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.NODATA, false, NoExpressionCallTO.OriginOfLine.SELF)),
                MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams basicNoExprParams33 = new NoExpressionCallParams();
        basicNoExprParams33.addAllSpeciesIds(speciesIds);
        basicNoExprParams33.setIncludeParentStructures(false);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(basicNoExprParams33))).
                thenReturn(mockBasicNoExprRsSp33);

        // Global no-expression calls
        MySQLNoExpressionCallTOResultSet mockGlobalNoExprRsSp33 = createMockDAOResultSet(
                // Attributes to fill: all except ID.
                Arrays.asList(
                        new NoExpressionCallTO(null, "IDX", "Anat_idX", "Stage_idX", 
                                DataState.NODATA, DataState.NODATA, DataState.NODATA, 
                                DataState.HIGHQUALITY, true, NoExpressionCallTO.OriginOfLine.SELF)),
                                MySQLNoExpressionCallTOResultSet.class);
        NoExpressionCallParams globalNoExprParams33 = new NoExpressionCallParams();
        globalNoExprParams33.addAllSpeciesIds(speciesIds);
        globalNoExprParams33.setIncludeParentStructures(true);
        when(mockManager.mockNoExpressionCallDAO.getNoExpressionCalls(
                (NoExpressionCallParams) TestAncestor.valueCallParamEq(globalNoExprParams33))).
                thenReturn(mockGlobalNoExprRsSp33);

        try {
            String directory = testFolder.newFolder("tmpFolder").getPath();
            Set<SingleSpExprFileType> fileTypes = new HashSet<SingleSpExprFileType>(
                    Arrays.asList(SingleSpExprFileType.EXPR_COMPLETE)); 
            GenerateExprFile generate = new GenerateExprFile(mockManager, 
                    Arrays.asList("33"), fileTypes, directory);
            generate.generateExprFiles();
            // test failed
            throw new AssertionError("generateExprFiles did not throw " +
                "an IllegalStateException as expected");
        } catch (IllegalStateException e) {
            // test passed, do nothing
        }
        // Verify that all ResultSet are closed.
        verify(mockSpeciesTORs33).close();
        verify(mockGeneTORs33).close();
        verify(mockStageTORs33).close();
        verify(mockAnatEntityTORs33).close();
        verify(mockAnatEntityRsSp33).close();
        verify(mockStageRelationRsSp33).close();
        verify(mockGlobalExprRsSp33).close();
        verify(mockGlobalNoExprRsSp33).close();
        
        //check that the connection was closed at each species iteration
        verify(mockManager.mockManager, times(1)).releaseResources();

        // Verify that setAttributes are correctly called.
        verify(mockManager.mockAnatEntityDAO, times(1)).setAttributes(AnatEntityDAO.Attribute.ID);
        verify(mockManager.mockRelationDAO, times(2)).setAttributes(
                RelationDAO.Attribute.SOURCE_ID, RelationDAO.Attribute.TARGET_ID);
        verify(mockManager.mockExpressionCallDAO, times(2)).setAttributes(
                // All Attributes except ID, anat and stage OriginOfLines
                EnumSet.complementOf(EnumSet.of(ExpressionCallDAO.Attribute.ID, 
                        ExpressionCallDAO.Attribute.OBSERVED_DATA, 
                        ExpressionCallDAO.Attribute.STAGE_ORIGIN_OF_LINE)));
        verify(mockManager.mockNoExpressionCallDAO, times(2)).setAttributes(
                // All Attributes except ID
                EnumSet.complementOf(EnumSet.of(NoExpressionCallDAO.Attribute.ID)));
        
        verify(mockManager.mockAnatEntityDAO, times(1)).setAttributes(
                AnatEntityDAO.Attribute.ID, AnatEntityDAO.Attribute.NAME);
        verify(mockManager.mockGeneDAO, times(1)).setAttributes(
                GeneDAO.Attribute.ID, GeneDAO.Attribute.NAME);
        verify(mockManager.mockStageDAO, times(1)).setAttributes(
                StageDAO.Attribute.ID, StageDAO.Attribute.NAME);
    }
    
    /**
     * Asserts that the simple expression/no-expression file is good.
     * <p>
     * Read given download file and check whether the file contents corresponds to what is expected. 
     * 
     * @param file              A {@code String} that is the path to the file were data was written 
     *                          as TSV.
     * @param isSimplifiedFile  A {@code String} defining the species ID.
     * @param expNbLines        An {@code Integer} defining the expected number of lines in 
     *                          {@code file}.
     * @throws IOException      If the file could not be used.
     */
    private void assertExpressionFile(String file, String speciesId, boolean isSimplified, 
            int expNbLines) throws IOException {
        log.entry(file, speciesId, isSimplified, expNbLines);
        
        try (ICsvMapReader mapReader = new CsvMapReader(new FileReader(file), Utils.TSVCOMMENTED)) {
            String[] headers = mapReader.getHeader(true);
            log.trace("Headers: {}", (Object[]) headers);

            // Check that the headers are what we expect
            String[] expecteds = new String[] { 
                    GenerateDownloadFile.GENE_ID_COLUMN_NAME, 
                    GenerateDownloadFile.GENE_NAME_COLUMN_NAME, 
                    GenerateDownloadFile.STAGE_ID_COLUMN_NAME, 
                    GenerateDownloadFile.STAGE_NAME_COLUMN_NAME,
                    GenerateDownloadFile.ANATENTITY_ID_COLUMN_NAME, 
                    GenerateDownloadFile.ANATENTITY_NAME_COLUMN_NAME,
                    GenerateExprFile.EXPRESSION_COLUMN_NAME,
                    GenerateDownloadFile.QUALITY_COLUMN_NAME};
            if (!isSimplified) {
                expecteds = new String[] { 
                        GenerateDownloadFile.GENE_ID_COLUMN_NAME, 
                        GenerateDownloadFile.GENE_NAME_COLUMN_NAME, 
                        GenerateDownloadFile.STAGE_ID_COLUMN_NAME, 
                        GenerateDownloadFile.STAGE_NAME_COLUMN_NAME,   
                        GenerateDownloadFile.ANATENTITY_ID_COLUMN_NAME, 
                        GenerateDownloadFile.ANATENTITY_NAME_COLUMN_NAME,
                        GenerateExprFile.EXPRESSION_COLUMN_NAME,
                        GenerateDownloadFile.QUALITY_COLUMN_NAME,
                        GenerateExprFile.INCLUDING_OBSERVED_DATA_COLUMN_NAME,
                        GenerateDownloadFile.AFFYMETRIX_DATA_COLUMN_NAME, 
                        GenerateDownloadFile.AFFYMETRIX_CALL_QUALITY_COLUMN_NAME, 
                        GenerateDownloadFile.AFFYMETRIX_OBSERVED_DATA_COLUMN_NAME, 
                        GenerateExprFile.EST_DATA_COLUMN_NAME, 
                        GenerateExprFile.EST_CALL_QUALITY_COLUMN_NAME, 
                        GenerateExprFile.EST_OBSERVED_DATA_COLUMN_NAME, 
                        GenerateExprFile.INSITU_DATA_COLUMN_NAME, 
                        GenerateExprFile.INSITU_CALL_QUALITY_COLUMN_NAME, 
                        GenerateExprFile.INSITU_OBSERVED_DATA_COLUMN_NAME, 
                        // GenerateExprFile.RELAXED_INSITU_DATA_COLUMN_NAME, 
                        // GenerateExprFile.RELAXED_INSITU_CALL_QUALITY_COLUMN_NAME, 
                        // GenerateExprFile.RELAXED_INSITU_OBSERVED_DATA_COLUMN_NAME,
                        GenerateDownloadFile.RNASEQ_DATA_COLUMN_NAME,
                        GenerateDownloadFile.RNASEQ_CALL_QUALITY_COLUMN_NAME,
                        GenerateDownloadFile.RNASEQ_OBSERVED_DATA_COLUMN_NAME};
            }
            assertArrayEquals("Incorrect headers", expecteds, headers);
            

            List<Object> expressionValues = new ArrayList<Object>();
            for (ExpressionData data : ExpressionData.values()) {
                expressionValues.add(data.getStringRepresentation());
            }

            List<Object> specificTypeQualities = new ArrayList<Object>();
            specificTypeQualities.add(DataState.HIGHQUALITY.getStringRepresentation());
            specificTypeQualities.add(DataState.LOWQUALITY.getStringRepresentation());
            specificTypeQualities.add(DataState.NODATA.getStringRepresentation());
            
            List<Object> resumeQualities = new ArrayList<Object>();
            resumeQualities.add(DataState.HIGHQUALITY.getStringRepresentation());
            resumeQualities.add(DataState.LOWQUALITY.getStringRepresentation());
            resumeQualities.add(GenerateDiffExprFile.NA_VALUE);
            
            List<Object> originValues = new ArrayList<Object>();
            for (ObservedData data : ObservedData.values()) {
                originValues.add(data.getStringRepresentation());
            }

            CellProcessor[] processors = null;
            if (isSimplified) {
                processors = new CellProcessor[] { 
                    new StrNotNullOrEmpty(), // gene ID
                    new NotNull(),           // gene Name
                    new StrNotNullOrEmpty(), // developmental stage ID
                    new StrNotNullOrEmpty(), // developmental stage name
                    new StrNotNullOrEmpty(), // anatomical entity ID
                    new StrNotNullOrEmpty(), // anatomical entity name
                    new IsElementOf(expressionValues),  // Expression
                    new IsElementOf(resumeQualities)};   // Call quality
            } else {
                processors = new CellProcessor[] { 
                    new StrNotNullOrEmpty(), // gene ID
                    new NotNull(),           // gene Name
                    new StrNotNullOrEmpty(), // developmental stage ID
                    new StrNotNullOrEmpty(), // developmental stage name
                    new StrNotNullOrEmpty(), // anatomical entity ID
                    new StrNotNullOrEmpty(), // anatomical entity name
                    new IsElementOf(expressionValues),      // Expression
                    new IsElementOf(resumeQualities),       // Call quality
                    new IsElementOf(originValues),          // Including observed data 
                    new IsElementOf(expressionValues),      // Affymetrix data
                    new IsElementOf(specificTypeQualities), // Affymetrix quality
                    new IsElementOf(originValues),          // Including Affymetrix data
                    new IsElementOf(expressionValues),      // EST data
                    new IsElementOf(specificTypeQualities), // EST quality
                    new IsElementOf(originValues),          // Including EST data
                    new IsElementOf(expressionValues),      // In Situ data
                    new IsElementOf(specificTypeQualities), // In Situ quality
                    new IsElementOf(originValues),          // Including in Situ data
                    new IsElementOf(expressionValues),      // RNA-seq data
                    new IsElementOf(specificTypeQualities), // RNA-seq quality
                    new IsElementOf(originValues)};         // Including RNA-seq data
            }

            Map<String, Object> rowMap;
            //TODO: add assertion tests to check correct order of the lines
            int nbLines = 0;
            while ((rowMap = mapReader.read(headers, processors)) != null ) {
                log.trace("Row: {}", rowMap);
                nbLines++;
                String geneId = (String) rowMap.get(headers[0]);
                String geneName = (String) rowMap.get(headers[1]);
                String stageId = (String) rowMap.get(headers[2]);
                String stageName = (String) rowMap.get(headers[3]);
                String anatEntityId = (String) rowMap.get(headers[4]);
                String anatEntityName = (String) rowMap.get(headers[5]);
                String resume = (String) rowMap.get(headers[6]);
                String quality = (String) rowMap.get(headers[7]);

                String affymetrixData = null, estData = null, inSituData = null, rnaSeqData = null, 
                        affymetrixQual= null, estQual = null, inSituQual = null, rnaSeqQual = null,
                        affymetrixObsData = null, estObsData = null, inSituObsData = null,
                        rnaSeqObsData = null, observedData = null;
                if (!isSimplified) {
                    observedData = (String) rowMap.get(headers[8]);
                    affymetrixData = (String) rowMap.get(headers[9]);
                    affymetrixQual = (String) rowMap.get(headers[10]);
                    affymetrixObsData = (String) rowMap.get(headers[11]);
                    estData = (String) rowMap.get(headers[12]);
                    estQual = (String) rowMap.get(headers[13]);
                    estObsData = (String) rowMap.get(headers[14]);
                    inSituData = (String) rowMap.get(headers[15]);
                    inSituQual = (String) rowMap.get(headers[16]);
                    inSituObsData = (String) rowMap.get(headers[17]);
                    rnaSeqData = (String) rowMap.get(headers[18]);
                    rnaSeqQual = (String) rowMap.get(headers[19]);
                    rnaSeqObsData = (String) rowMap.get(headers[20]);
                }

                if (speciesId.equals("11")) {
                    if (geneId.equals("ID1") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("Stage_id1")) {
                        this.assertCommonColumnRowEqual(geneId, "genN1", geneName,
                                "stageN1", stageName, "anatName1", anatEntityName, 
                                ExpressionData.HIGH_AMBIGUITY.getStringRepresentation(), resume,
                                GenerateDownloadFile.NA_VALUE, quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.NO_EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.EXPRESSION, rnaSeqData,
                                DataState.LOWQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID1") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("ParentStage_id1")) {
                        this.assertCommonColumnRowEqual(geneId, "genN1", geneName,
                                "parentstageN1", stageName, "anatName1", anatEntityName, 
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.LOWQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.NO_DATA, affymetrixData,
                            DataState.NODATA.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.EXPRESSION, estData,
                            DataState.LOWQUALITY.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.EXPRESSION, rnaSeqData,
                            DataState.LOWQUALITY.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID1") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("ParentStage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN1", geneName,
                                "parentstageN2", stageName, "anatName1", anatEntityName, 
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData, 
                                DataState.HIGHQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData, 
                                DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.NO_DATA, rnaSeqData,
                                DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                                ObservedData.NOT_OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID1") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN1", geneName,
                                "stageN2", stageName, "anatName1", anatEntityName, 
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.NO_DATA, rnaSeqData,
                                DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                                ObservedData.NOT_OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    // This call is no longer correct because it is filtered on the conditions 
                    //  } else if (geneId.equals("ID1") && anatEntityId.equals("Anat_id2") &&
                    //       stageId.equals("Stage_id1")) {
                    } else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id1") && 
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN2", geneName,
                                "stageN2", stageName, "anatName1", anatEntityName,
                                ExpressionData.HIGH_AMBIGUITY.getStringRepresentation(), resume,
                                GenerateDownloadFile.NA_VALUE, quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.NO_DATA, affymetrixData,
                                DataState.NODATA.getStringRepresentation(), affymetrixQual,
                                ObservedData.NOT_OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.NO_DATA, inSituData,
                                DataState.NODATA.getStringRepresentation(), inSituQual,
                                ObservedData.NOT_OBSERVED, inSituObsData,
                                ExpressionData.NO_EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id1") && 
                            stageId.equals("ParentStage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN2", geneName,
                                "parentstageN2", stageName, "anatName1", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                    ExpressionData.EXPRESSION, affymetrixData,
                                    DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                    ObservedData.NOT_OBSERVED, affymetrixObsData,
                                    ExpressionData.EXPRESSION, estData,
                                    DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                    ObservedData.NOT_OBSERVED, estObsData,
                                    ExpressionData.EXPRESSION, inSituData,
                                    DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                    ObservedData.NOT_OBSERVED, inSituObsData,
                                    ExpressionData.NO_DATA, rnaSeqData,
                                    DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                                    ObservedData.NOT_OBSERVED, rnaSeqObsData,
                                    ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id2") && 
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN2", geneName,
                                "stageN2", stageName, "anatName2", anatEntityName,
                                DataState.HIGHQUALITY.getStringRepresentation(), resume,
                                GenerateDownloadFile.NA_VALUE, quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.NO_DATA, affymetrixData,
                            DataState.NODATA.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.NO_DATA, estData,
                            DataState.NODATA.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.NO_DATA, inSituData,
                            DataState.NODATA.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_EXPRESSION, rnaSeqData,
                            DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id2") && 
                            stageId.equals("ParentStage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN2", geneName,
                                "parentstageN2", stageName, "anatName2", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.EXPRESSION, affymetrixData,
                            DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.NO_DATA, estData,
                            DataState.NODATA.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_DATA, rnaSeqData,
                            DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id3") && 
                            stageId.equals("ParentStage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN2", geneName,
                                "parentstageN2", stageName, "anatName3", anatEntityName,
                                ExpressionData.HIGH_AMBIGUITY.getStringRepresentation(), resume,
                                GenerateDownloadFile.NA_VALUE, quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.NO_DATA, estData,
                                DataState.NODATA.getStringRepresentation(), estQual,
                                ObservedData.NOT_OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.NO_EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    // This call is no longer correct because it is filtered on the conditions 
                    //} else if (geneId.equals("ID2") && anatEntityId.equals("Anat_id3") && 
                    //        stageId.equals("Stage_id2")) {
                    } else {
                        throw new IllegalArgumentException("Unexpected row: " + rowMap);
                    }
                } else if (speciesId.equals("22")){
                    if (geneId.equals("ID3") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN3", geneName,
                                "stageN2", stageName, "anatName1", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.LOWQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.NOT_OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID3") && anatEntityId.equals("Anat_id4") &&
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN3", geneName,
                                "stageN2", stageName, "anatName4", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.EXPRESSION, affymetrixData,
                            DataState.LOWQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.EXPRESSION, estData,
                            DataState.LOWQUALITY.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.EXPRESSION, rnaSeqData,
                            DataState.LOWQUALITY.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID3") && anatEntityId.equals("Anat_id5") &&
                            stageId.equals("Stage_id2")) {
                        this.assertCommonColumnRowEqual(geneId, "genN3", geneName,
                                "stageN2", stageName, "anatName5", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.LOWQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.EXPRESSION, rnaSeqData,
                                DataState.LOWQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID4") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("Stage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN4", geneName,
                                "stageN5", stageName, "anatName1", anatEntityName,
                                ExpressionData.NO_EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.NO_DATA, affymetrixData,
                                DataState.NODATA.getStringRepresentation(), affymetrixQual,
                                ObservedData.NOT_OBSERVED, affymetrixObsData,
                                ExpressionData.NO_DATA, estData,
                                DataState.NODATA.getStringRepresentation(), estQual,
                                ObservedData.NOT_OBSERVED, estObsData,
                                ExpressionData.NO_EXPRESSION, inSituData,
                                DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.NO_EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID4") && anatEntityId.equals("Anat_id4") &&
                            stageId.equals("Stage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN4", geneName,
                                "stageN5", stageName, "anatName4", anatEntityName,
                                ExpressionData.NO_EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.NO_EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.NO_DATA, estData,
                                DataState.NODATA.getStringRepresentation(), estQual,
                                ObservedData.NOT_OBSERVED, estObsData,
                                ExpressionData.NO_EXPRESSION, inSituData,
                                DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.NOT_OBSERVED, inSituObsData,
                                ExpressionData.NO_EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.NOT_OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID4") && anatEntityId.equals("Anat_id5") &&
                            stageId.equals("Stage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN4", geneName,
                                "stageN5", stageName, "anatName5", anatEntityName,
                                ExpressionData.NO_EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.NO_EXPRESSION, affymetrixData,
                            DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.NO_DATA, estData,
                            DataState.NODATA.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.NO_EXPRESSION, inSituData,
                            DataState.HIGHQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_EXPRESSION, rnaSeqData,
                            DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID5") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("Stage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN5", geneName,
                                "stageN5", stageName, "anatName1", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.EXPRESSION, affymetrixData,
                            DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.EXPRESSION, estData,
                            DataState.LOWQUALITY.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_DATA, rnaSeqData,
                            DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID5") && anatEntityId.equals("Anat_id1") &&
                            stageId.equals("ParentStage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN5", geneName,
                                "parentstageN5", stageName, "anatName1", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.EXPRESSION, affymetrixData,
                            DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.EXPRESSION, estData,
                            DataState.LOWQUALITY.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_DATA, rnaSeqData,
                            DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else if (geneId.equals("ID5") && anatEntityId.equals("Anat_id4") &&
                            stageId.equals("Stage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN5", geneName,
                                "stageN5", stageName, "anatName4", anatEntityName, 
                                //note that the ambiguity comes only from relaxedInSituData, 
                                //which are not yet implemented.
                                ExpressionData.HIGH_AMBIGUITY.getStringRepresentation(), resume,
                                GenerateDownloadFile.NA_VALUE, quality);
                        if (!isSimplified) {
                            this.assertCompleteExprColumnRowEqual(geneId, 
                                ExpressionData.EXPRESSION, affymetrixData,
                                DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                                ObservedData.OBSERVED, affymetrixObsData,
                                ExpressionData.EXPRESSION, estData,
                                DataState.LOWQUALITY.getStringRepresentation(), estQual,
                                ObservedData.OBSERVED, estObsData,
                                ExpressionData.EXPRESSION, inSituData,
                                DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                                ObservedData.OBSERVED, inSituObsData,
                                ExpressionData.NO_EXPRESSION, rnaSeqData,
                                DataState.HIGHQUALITY.getStringRepresentation(), rnaSeqQual,
                                ObservedData.OBSERVED, rnaSeqObsData,
                                ObservedData.OBSERVED, observedData);
                        }
                    } else if (geneId.equals("ID5") && anatEntityId.equals("Anat_id4") &&
                            stageId.equals("ParentStage_id5")) {
                        this.assertCommonColumnRowEqual(geneId, "genN5", geneName,
                                "parentstageN5", stageName, "anatName4", anatEntityName,
                                ExpressionData.EXPRESSION.getStringRepresentation(), resume,
                                DataState.HIGHQUALITY.getStringRepresentation(), quality);
                        if (isSimplified) {
                            throw new IllegalStateException("This triplet should not be present in the simple file");
                        }
                        this.assertCompleteExprColumnRowEqual(geneId, 
                            ExpressionData.EXPRESSION, affymetrixData,
                            DataState.HIGHQUALITY.getStringRepresentation(), affymetrixQual,
                            ObservedData.NOT_OBSERVED, affymetrixObsData,
                            ExpressionData.EXPRESSION, estData,
                            DataState.LOWQUALITY.getStringRepresentation(), estQual,
                            ObservedData.NOT_OBSERVED, estObsData,
                            ExpressionData.EXPRESSION, inSituData,
                            DataState.LOWQUALITY.getStringRepresentation(), inSituQual,
                            ObservedData.NOT_OBSERVED, inSituObsData,
                            ExpressionData.NO_DATA, rnaSeqData,
                            DataState.NODATA.getStringRepresentation(), rnaSeqQual,
                            ObservedData.NOT_OBSERVED, rnaSeqObsData,
                            ObservedData.NOT_OBSERVED, observedData);
                    } else {
                        throw new IllegalArgumentException("Unexpected row: " + rowMap);
                    }
                } else {
                    throw new IllegalStateException("Test of species ID " + speciesId + 
                            "not implemented yet");
                }
            }
            assertEquals("Incorrect number of lines in simple download file", expNbLines, nbLines);
        }
    }
    
    /**
     * Assert that specific complete file columns row are equal. It checks affymetrix data, 
     * EST data, <em>in Situ</em> data, and RNA-seq data columns. 
     * 
     * @param geneId            A {@code String} that is the gene ID of the row.
     * @param expAffyData       An {@code ExpressionData} that is the expected affymetrix data. 
     * @param affyData          A {@code String} that is the actual affymetrix data.
     * @param expESTData        An {@code ExpressionData} that is the expected EST data.
     * @param estData           A {@code String} that is the actual EST data.
     * @param expInSituData     An {@code ExpressionData} that is the expected <em>in Situ</em> data.
     * @param inSituData        A {@code String} that is the actual <em>in Situ</em> data.
     * @param expRNAseqData     An {@code ExpressionData} that is the expected RNA-seq data.
     * @param rnaSeqData        A {@code String} that is the actual RNA-seq data.
     */
    private void assertCompleteExprColumnRowEqual(String geneId, 
            ExpressionData expAffyData, String affyData, String expAffyQual, String affyQual, 
            ObservedData expAffyObsData, String affyObsData,
            ExpressionData expESTData, String estData, String expESTQual, String estQual, 
            ObservedData expESTObsData, String estObsData,
            ExpressionData expInSituData, String inSituData, String expInSituQual, String inSituQual, 
            ObservedData expInSituObsData, String inSituObsData, 
            ExpressionData expRNAseqData, String rnaSeqData, String expRNAseqQual, String rnaSeqQual, 
            ObservedData expRNAseqObsData, String rnaSeqObsData,
            ObservedData expObservedData, String observedData) {
        
        assertEquals("Incorrect Affymetrix data for " + geneId, 
            expAffyData.getStringRepresentation(), affyData);
        assertEquals("Incorrect Affymetrix quality for " + geneId, expAffyQual, affyQual);
        assertEquals("Incorrect Affymetrix observed data for " + geneId, 
                expAffyObsData.getStringRepresentation(), affyObsData);
        
        assertEquals("Incorrect EST data for " + geneId, 
                expESTData.getStringRepresentation(), estData);
        assertEquals("Incorrect EST quality for " + geneId, expESTQual, estQual);
        assertEquals("Incorrect EST observed data for " + geneId, 
                expESTObsData.getStringRepresentation(), estObsData);

        assertEquals("Incorrect in situ data for " + geneId, 
                expInSituData.getStringRepresentation(), inSituData);
        assertEquals("Incorrect in situ quality for " + geneId, expInSituQual, inSituQual);
        assertEquals("Incorrect in situ observed data for " + geneId, 
                expInSituObsData.getStringRepresentation(), inSituObsData);

        assertEquals("Incorrect RNA-seq data for " + geneId, 
                expRNAseqData.getStringRepresentation(), rnaSeqData);
        assertEquals("Incorrect RNA-seq quality for " + geneId, expRNAseqQual, rnaSeqQual);
        assertEquals("Incorrect RNA-seq observed data for " + geneId, 
                expRNAseqObsData.getStringRepresentation(), rnaSeqObsData);

        assertEquals("Incorrect observed data for " + geneId, 
                expObservedData.getStringRepresentation(), observedData);
    }


}
