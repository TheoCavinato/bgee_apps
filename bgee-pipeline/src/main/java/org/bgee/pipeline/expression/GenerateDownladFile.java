package org.bgee.pipeline.expression;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO.AnatEntityTO;
import org.bgee.model.dao.api.anatdev.AnatEntityDAO.AnatEntityTOResultSet;
import org.bgee.model.dao.api.anatdev.StageDAO;
import org.bgee.model.dao.api.anatdev.StageDAO.StageTO;
import org.bgee.model.dao.api.anatdev.StageDAO.StageTOResultSet;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO.DataState;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.ExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.ExpressionCallParams;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.NoExpressionCallTO;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallDAO.NoExpressionCallTOResultSet;
import org.bgee.model.dao.api.expressiondata.NoExpressionCallParams;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTOResultSet;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.pipeline.BgeeDBUtils;
import org.bgee.pipeline.CommandRunner;
import org.bgee.pipeline.Utils;
import org.supercsv.cellprocessor.constraint.IsElementOf;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;


/**
 * Class responsible to generate TSV download files (simple and complete files) 
 * from the Bgee database. 
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 */
public class GenerateDownladFile extends CallUser {
    
    /**
     * {@code Logger} of the class.
     */
    private final static Logger log = LogManager.getLogger(GenerateDownladFile.class.getName());
        
    /**
     * A {@code String} that is the name of the column containing gene IDs, in the download file.
     */
    public final static String GENE_ID_COLUMN_NAME = "Gene ID";

    /**
     * A {@code String} that is the name of the column containing gene names, in the download file.
     */
    public final static String GENE_NAME_COLUMN_NAME = "Gene name";

    /**
     * A {@code String} that is the name of the column containing developmental stage IDs, 
     * in the download file.
     */
    public final static String STAGE_ID_COLUMN_NAME = "Developmental stage ID";

    /**
     * A {@code String} that is the name of the column containing developmental stage names, 
     * in the download file.
     */
    public final static String STAGE_NAME_COLUMN_NAME = "Developmental stage name";

    /**
     * A {@code String} that is the name of the column containing anatomical entity IDs, 
     * in the download file.
     */
    public final static String ANATENTITY_ID_COLUMN_NAME = "Anatomical entity ID";

    /**
     * A {@code String} that is the name of the column containing anatomical entity names, 
     * in the download file.
     */
    public final static String ANATENTITY_NAME_COLUMN_NAME = "Anatomical entity name";

    /**
     * A {@code String} that is the name of the column containing expression/no-expression found 
     * with Affymetrix experiment, in the download file.
     */
    public final static String AFFYMETRIXDATA_COLUMN_NAME = "Affymetrix data";

    /**
     * A {@code String} that is the name of the column containing whether the Affymetrix data 
     * is inferred or not.
     */
    public final static String AFFYMETRIX_ORIGIN_COLUMN_NAME = "Affymetrix data inferred";

    /**
     * A {@code String} that is the name of the column containing expression/no-expression found 
     * with EST experiment, in the download file.
     */
    public final static String ESTDATA_COLUMN_NAME = "EST data";

    /**
     * A {@code String} that is the name of the column containing whether the EST data 
     * is inferred or not.
     */
    public final static String EST_ORIGIN_COLUMN_NAME = "EST data inferred";

    /**
     * A {@code String} that is the name of the column containing expression/no-expression found 
     * with <em>in situ</em> experiment, in the download file.
     */
    public final static String INSITUDATA_COLUMN_NAME = "In situ data";

    /**
     * A {@code String} that is the name of the column containing whether the <em>in situ</em> data 
     * is inferred or not.
     */
    public final static String INSITU_ORIGIN_COLUMN_NAME = "In situ inferred";

    /**
     * A {@code String} that is the name of the column containing expression/no-expression found 
     * with relaxed <em>in situ</em> experiment, in the download file.
     */
    public final static String RELAXEDINSITUDATA_COLUMN_NAME = "Relaxed in situ data";
    
    /**
     * A {@code String} that is the name of the column containing whether the relaxed 
     * <em>in situ</em> data is inferred or not.
     */
    public final static String RELAXEDINSITU_ORIGIN_COLUMN_NAME = "Relaxed in situ inferred";
    
    /**
     * A {@code String} that is the name of the column containing expression/no-expression found 
     * with RNA-Seq experiment, in the download file.
     */
    public final static String RNASEQDATA_COLUMN_NAME = "RNA-Seq data";

    /**
     * A {@code String} that is the name of the column containing whether the RNA-Seq data
     * is inferred or not.
     */
    public final static String RNASEQ_ORIGIN_COLUMN_NAME = "RNA-Seq data inferred";

    /**
     * A {@code String} that is the name of the column containing merged expression/no-expression 
     * from different data types, in the download file.
     */
    public final static String EXPRESSION_COLUMN_NAME = "Expression/No-expression";

    /**
     * A {@code String} that is the name of the column containing merged differential expressions 
     * from different data types, in the download file.
     */
    public final static String DIFFEXPRESSION_COLUMN_NAME = "Over/Under-expression";

    /**
     * A {@code String} that is the argument class for generate presence/absence of expression 
     * download simple file.
     */
    public final static String EXPR_SIMPLE = "expr-simple";

    /**
     * A {@code String} that is the argument class for generate presence/absence of expression 
     * download complete file.
     */
    public final static String EXPR_COMPLETE = "expr-complete"; 

    /**
     * A {@code String} that is the argument class for generate differential expression 
     * download simple file.
     */
    public final static String DIFFEXPR_SIMPLE = "diffexpr-simple";

    /**
     * A {@code String} that is the argument class for generate differential expression 
     * download complete file.
     */
   public final static String DIFFEXPR_COMPLETE = "diffexpr-complete";

   public final static List<String> ALL_FILE_TYPES = 
           Arrays.asList(EXPR_SIMPLE, EXPR_COMPLETE, DIFFEXPR_SIMPLE, DIFFEXPR_COMPLETE);
   
   /**
    * A {@code String} that is the extension of download files to be generated.
    */
   public final static String EXTENSION = ".tsv";
   
   /**
    * An {@code Enum} used to define, for each data type (Affymetrix, RNA-Seq, ...), the 
    * expression/no-expression of the call.
    * <ul>
    * <li>{@code NODATA}:         no data from the associated data type allowed to produce the call.
    * <li>{@code NOEXPRESSION}:   no-expression was detected from the associated data type.
    * <li>{@code LOWEXPRESSION}:  low expression was detected from the associated data type.
    * <li>{@code HIGHEXPRESSION}: high expression was detected from the associated data type.
    * <li>{@code LOWAMBIGUITY}:   different data types are not coherent with a no-expression call 
    *                             inferred (for instance, Affymetrix data reveals an low expression 
    *                             while <em>in situ</em> data reveals an inferred no-expression).
    * <li>{@code HIGHAMBIGUITY}:  different data types are not coherent without at least an inferred
    *                             no-expression call (for instance, Affymetrix data reveals a low 
    *                             expression while <em>in situ</em> data reveals a no-expression 
    *                             without been inferred).
    * </ul>
    * 
    * @author Valentine Rech de Laval
    * @version Bgee 13
    * @since Bgee 13
    */
   public enum ExpressionData {
       NODATA("no data"), NOEXPRESSION("absent high quality"), 
       LOWQUALITY("expression low quality"), HIGHQUALITY("expression high quality"), 
       LOWAMBIGUITY("low ambiguity"), HIGHAMBIGUITY("high ambiguity");

       private final String stringRepresentation;

       /**
        * Constructor providing the {@code String} representation of this {@code ExpressionData}.
        * 
        * @param stringRepresentation  A {@code String} corresponding to
        *                              this {@code ExpressionData}.
        */
       private ExpressionData(String stringRepresentation) {
           this.stringRepresentation = stringRepresentation;
       }

       public String getStringRepresentation() {
           return this.stringRepresentation;
       }

       public String toString() {
           return this.getStringRepresentation();
       }
   }

   /**
    * An {@code Enum} used to define, for each data type (Affymetrix, RNA-Seq, ...), the 
    * differential expression of the call.
    * <ul>
    * <li>{@code NODATA}:           no data from the associated data type allowed to produce the call.
    * <li>{@code OVEREXPRESSED}:    over-expression was detected from the associated data type.
    * <li>{@code UNDEREXPRESSED}:   under-expression was detected from the associated data type.
    * <li>{@code NOTDIFFEXPRESSED}: not differential expression was detected from the associated data type.
    * </ul>
    * 
    * @author Valentine Rech de Laval
    * @version Bgee 13
    * @since Bgee 13
    */
   public enum DiffExpressionData {
       NODATA("no data"), OVEREXPRESSED("over-expression"), UNDEREXPRESSED("under-expression"), 
       NOTDIFFEXPRESSED("no diff expression");
       
       private final String stringRepresentation;
       
       /**
        * Constructor providing the {@code String} representation of this {@code DiffCallType}.
        * 
        * @param stringRepresentation  A {@code String} corresponding to this {@code DiffCallType}.
        */
       private DiffExpressionData(String stringRepresentation) {
           this.stringRepresentation = stringRepresentation;
       }

       public String getStringRepresentation() {
           return this.stringRepresentation;
       }

       public String toString() {
           return this.getStringRepresentation();
       }
   }
 
   /**
    * An {@code Enum} used to define.
    * <ul>
    * <li>{@code INFERRED}:     defined by inferred data.
    * <li>{@code BOTH}:         defined by inferred and no-inferred data.
    * <li>{@code NOTINFERRED}:  defined by no-inferred data.
    * <li>{@code NODATA}:       no data.
    * </ul>
    * 
    * @author Valentine Rech de Laval
    * @version Bgee 13
    * @since Bgee 13
    */
   public enum Origin {
       INFERRED("yes"), BOTH("both"), NOTINFERRED("no"), NODATA("-");
       
       private final String stringRepresentation;
       
       /**
        * Constructor providing the {@code String} representation of this {@code InferredWords}.
        * 
        * @param stringRepresentation  A {@code String} corresponding to this {@code InferredWords}.
        */
       private Origin(String stringRepresentation) {
           this.stringRepresentation = stringRepresentation;
       }

       public String getStringRepresentation() {
           return this.stringRepresentation;
       }

       public String toString() {
           return this.getStringRepresentation();
       }
   }
   
   /**
     * Default constructor. 
     */
    public GenerateDownladFile() {
        this(null);
    }

    /**
     * Constructor providing the {@code MySQLDAOManager} that will be used by 
     * this object to perform queries to the database. This is useful for unit testing.
     * 
     * @param manager   the {@code MySQLDAOManager} to use.
     */
    public GenerateDownladFile(MySQLDAOManager manager) {
        super(manager);
    }

    /**
     * Main method to trigger the generate TSV download files (simple and complete files) from Bgee 
     * database. Parameters that must be provided in order in {@code args} are: 
     * <ol>
     * <li> a list of NCBI species IDs (for instance, {@code 9606} for human) that will be used to 
     * generate download files, separated by the {@code String} {@link CommandRunner#LIST_SEPARATOR}.
     * If it is not provided, all species contained in database will be used.
     * <li> a list of files types that will be generated ({@code EXPR_SIMPLE}, {@code EXPR_COMPLETE}, 
     * {@code DIFFEXPR_SIMPLE}, and {@code DIFFEXPR_SIMPLE}), separated by the 
     * {@code String} {@link CommandRunner#LIST_SEPARATOR}.
     * <li>the directory path that will be used to generate download files. So 
     * it must finish with {@code /}
     * </ol>
     * 
     * @param args          An {@code Array} of {@code String}s containing the requested parameters.
     * @throws IOException  If some files could not be used.
     * @throws UnsupportedOperationException If in the given {@code ExpressionCallParams},
     *                                        {@code isIncludeSubStages} is set to {@code true},
     *                                        because it is not implemented yet.
     */
    public static void main(String[] args) throws IOException, UnsupportedOperationException {
        log.entry((Object[]) args);

        // TODO Manage with multi-species!
        
        int expectedArgLengthSingleSpecies = 3; // species list and file types to be generated
        if (args.length != expectedArgLengthSingleSpecies) {
            throw log.throwing(new IllegalArgumentException(
                    "Incorrect number of arguments provided, expected " + 
                    expectedArgLengthSingleSpecies + " arguments, " + args.length + " provided."));
        }

        List<String> speciesIds = CommandRunner.parseListArgument(args[0]);
        
        List<String> fileTypes = CommandRunner.parseListArgument(args[1]);    
        
        String directory = args[2];
        
        GenerateDownladFile generate = new GenerateDownladFile();
        generate.generateSingleSpeciesFiles(speciesIds, fileTypes, directory);
        
        log.exit();
    }
    
    /**
     * Generate single species files according the given {@code List} of species IDs 
     * in the given directory. 
     * 
     * @param speciesIds    A {@code List} of {@code String}s that are the IDs of species for which
     *                      files are generated.
     * @param fileTypes     A {@code List} of {@code String}s containing file types to be generated.
     * @param directory     A {@code String} that is the directory path directory to store the 
     *                      generated files. 
     * @throws IOException  If an error occurred while trying to write generated files.
     * @throws UnsupportedOperationException If in the given {@code ExpressionCallParams},
     *                                        {@code isIncludeSubStages} is set to {@code true},
     *                                        because it is not implemented yet.
     */
    public void generateSingleSpeciesFiles(List<String> speciesIds, List<String> fileTypes, 
            String directory) throws IOException, UnsupportedOperationException { 
        log.entry(speciesIds, fileTypes, directory);
        
        // Get all species in Bgee even if some species IDs were provided, to check user input.
        List<String> speciesIdsToUse = BgeeDBUtils.checkAndGetSpeciesIds(
                speciesIds, this.getSpeciesDAO()); 

        if (fileTypes.isEmpty()) {
            // If no file types are given by user, we set all file types
            fileTypes = ALL_FILE_TYPES;
        } else if (!ALL_FILE_TYPES.containsAll(fileTypes)) {
            List<String> debugFileTypes = new ArrayList<String>(fileTypes);
            debugFileTypes.removeAll(ALL_FILE_TYPES);
            throw log.throwing(new IllegalArgumentException(
                    "Some file types could not be generated: " + debugFileTypes));
        }

        for (String speciesId: speciesIdsToUse) {
            log.trace("Start generation of download files for the species {}", speciesId);
            
            if (fileTypes.contains(DIFFEXPR_SIMPLE) || fileTypes.contains(DIFFEXPR_COMPLETE)) {
                this.generateDiffExprRows(speciesId);
            }

            if (fileTypes.contains(EXPR_SIMPLE) || fileTypes.contains(EXPR_COMPLETE)) {
                log.trace("Retrieve data from data source for absence/presence of expression filesfor the species {}",
                        speciesId);
                Set<String> speciesFilter = new HashSet<String>();
                speciesFilter.add(speciesId);

                // Load non-informative anatomical entities
                List<String> nonInformativesAnatEntities = this.loadNonInformativeAnatEntities(speciesFilter);

                // Load basic expression calls
                List<ExpressionCallTO> exprTOs = 
                        this.loadBasicExprCallsFromDb(speciesFilter);

                // Load basic no-expression calls
                List<NoExpressionCallTO> noExprTOs = 
                        this.loadBasicNoExprCallsFromDb(speciesFilter, nonInformativesAnatEntities);

                List<ExpressionCallTO> globalExprTOs = new ArrayList<ExpressionCallTO>(); 
                if (fileTypes.contains(EXPR_COMPLETE)) {
                    // Load global expression calls
                    globalExprTOs = this.loadGlobalExprCallsFromDb(speciesFilter);
                    log.debug("globalExprTOs size {}", globalExprTOs.size());
                }
                
                // Load global no-expression
                List<NoExpressionCallTO> globalNoExprTOs = new ArrayList<NoExpressionCallTO>(); 
                globalNoExprTOs = 
                        this.loadGlobalNoExprCallsFromDb(speciesFilter, nonInformativesAnatEntities);

                List<Map<String, String>> exprSimpleFile = null;
                if (fileTypes.contains(EXPR_SIMPLE)) {
                    log.trace("Start generation of data for simple file for the species {}",
                            speciesId);
                    // Note that this Collection is a List, otherwise a global expression call 
                    // could be seen as equal to a basic expression call. 
                    List<CallTO> allCallTOs = new ArrayList<CallTO>();
                    allCallTOs.addAll(exprTOs);
                    allCallTOs.addAll(noExprTOs);
                    allCallTOs.addAll(globalNoExprTOs);
                    exprSimpleFile = this.mergeCallsFromSortedMap(
                            this.groupAndOrderByGeneAnatEntityStage(allCallTOs));
                    log.trace("Done generation of data for simple file for the species {}",
                            speciesId);
                }
                
                List<Map<String, String>> exprAdvancedFile = null;
                if (fileTypes.contains(EXPR_COMPLETE)) {
                    log.trace("Start generation of data for advanced file for the species {}",
                            speciesId);
                    // Note that this Collection is a List, otherwise a global expression call 
                    // could be seen as equal to a basic expression call. 
                    List<CallTO> allCallTOs = new ArrayList<CallTO>();
                    allCallTOs.addAll(exprTOs);
                    allCallTOs.addAll(globalExprTOs);
                    allCallTOs.addAll(noExprTOs);
                    allCallTOs.addAll(globalNoExprTOs);

                    exprAdvancedFile = this.mergeCallsFromSortedMap(
                            this.groupAndOrderByGeneAnatEntityStage(allCallTOs));
                    log.trace("Done generation of data for advanced file for the species {}", 
                            speciesId);
                }
                
                // Add gene, stage and anatomical entity names
                this.addGeneNames(exprSimpleFile, exprAdvancedFile, speciesId);
                this.addStageNames(exprSimpleFile, exprAdvancedFile, speciesId);
                this.addAnatEntityNames(exprSimpleFile, exprAdvancedFile, speciesId);
                
                if (fileTypes.contains(EXPR_SIMPLE)) {
                    this.createDownloadFiles(exprSimpleFile, 
                            directory + speciesId + "_" + EXPR_SIMPLE + EXTENSION, true, false);
                }  
                if (fileTypes.contains(EXPR_COMPLETE)) {
                    this.createDownloadFiles(exprAdvancedFile, 
                            directory + speciesId + "_" + EXPR_COMPLETE + EXTENSION, false, false);
                }
            }
        }
        log.exit();
    }

    /**
     * Retrieves non-informative anatomical entities for given species, 
     * present into the Bgee database.
     * 
     * @param speciesIds        A {@code Set} of {@code String}s that are the IDs of species 
     *                          allowing to filter the non-informative anatomical entities to use.
     * @return                  A {@code List} of {@code String}s containing all 
     *                          non-informative anatomical entities of the given species.
     * @throws DAOException     If an error occurred while getting the data from the Bgee database.
     */
    private List<String> loadNonInformativeAnatEntities(Set<String> speciesIds) 
            throws DAOException {
        log.entry(speciesIds);
    
        log.info("Start retrieving non-informative anatomical entities for the species IDs {}...",
                speciesIds);
    
        AnatEntityDAO dao = this.getAnatEntityDAO();
        dao.setAttributes(AnatEntityDAO.Attribute.ID);
        
        log.debug("DAOMOCK: " + dao);
        List<String> anatEntities = new ArrayList<String>();
        try (AnatEntityTOResultSet rs = dao.getNonInformativeAnatEntities(speciesIds)) {
            while (rs.next()) {
                anatEntities.add(rs.getTO().getId());
            }
        }
        
        log.info("Done retrieving non-informative anatomical entities, {} entities found",
                anatEntities.size());
    
        return log.exit(anatEntities);        
    }

    /**
     * Retrieves all basic expression calls for given species, present into the Bgee database.
     * <p>
     * We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs, neither
     * INCLUDESUBSTAGES.
     * 
     * @param speciesIds                    A {@code Set} of {@code String}s that are the IDs of 
     *                                      species allowing to filter the basic expression calls 
     *                                      to use.
     * @return                              A {@code List} of {@code ExpressionCallTO}s containing 
     *                                      all basic expression calls of the given species.
     * @throws DAOException     If an error occurred while getting the data from the Bgee database.
     * @throws UnsupportedOperationException If in the given {@code ExpressionCallParams},
     *                                        {@code isIncludeSubStages} is set to {@code true},
     *                                        because it is not implemented yet.
     */
    private List<ExpressionCallTO> loadBasicExprCallsFromDb(Set<String> speciesIds)
                    throws DAOException, UnsupportedOperationException {
        log.entry(speciesIds);
    
        log.info("Start retrieving basic expression calls for the species IDs {}...", speciesIds);
    
        ExpressionCallDAO dao = this.getExpressionCallDAO();
        // We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
        // We don't need INCLUDESUBSTAGES. 
        dao.setAttributes(ExpressionCallDAO.Attribute.GENEID, 
                ExpressionCallDAO.Attribute.STAGEID, ExpressionCallDAO.Attribute.ANATENTITYID, 
                ExpressionCallDAO.Attribute.AFFYMETRIXDATA, ExpressionCallDAO.Attribute.ESTDATA,
                ExpressionCallDAO.Attribute.INSITUDATA, ExpressionCallDAO.Attribute.RNASEQDATA,
                ExpressionCallDAO.Attribute.INCLUDESUBSTRUCTURES);
         
        ExpressionCallParams params = new ExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);
        params.setUseAnatDescendants(false);
    
        List<ExpressionCallTO> exprTOs = dao.getExpressionCalls(params).getAllTOs();

        log.info("Done retrieving basic expression calls, {} calls found", exprTOs.size());
        
        return log.exit(exprTOs); 
    }

    /**
     * Retrieves all basic no-expression calls for given species not in a non-informative anatomical 
     * entity, present into the Bgee database.
     * <p>
     * We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
     * 
     * @param speciesIds                    A {@code Set} of {@code String}s that are the IDs of 
     *                                      species allowing to filter the basic no-expression 
     *                                      calls to use.
     * @param nonInformativesAnatEntities   A {@code List} of {@code String}s that are the 
     *                                      non-informative anatomical entities.
     * @return                              A {@code List} of {@code NoExpressionCallTO}s containing 
     *                                      all basic expression calls of the given species.
     * @throws DAOException If an error occurred while getting the data from the Bgee database.
     */
    private List<NoExpressionCallTO> loadBasicNoExprCallsFromDb(
            Set<String> speciesIds, List<String> nonInformativesAnatEntities) throws DAOException {
        log.entry(speciesIds, nonInformativesAnatEntities);
    
        log.info("Start retrieving basic no-expression calls for the species IDs {}...", speciesIds);
    
        NoExpressionCallDAO dao = this.getNoExpressionCallDAO();
        // We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
        // We don't need ORIGINOFLINE. 
        dao.setAttributes(NoExpressionCallDAO.Attribute.GENEID, 
                NoExpressionCallDAO.Attribute.DEVSTAGEID, NoExpressionCallDAO.Attribute.ANATENTITYID, 
                NoExpressionCallDAO.Attribute.AFFYMETRIXDATA, NoExpressionCallDAO.Attribute.INSITUDATA, 
                NoExpressionCallDAO.Attribute.RNASEQDATA, 
                NoExpressionCallDAO.Attribute.INCLUDEPARENTSTRUCTURES);
    
        NoExpressionCallParams params = new NoExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);
        params.setIncludeParentStructures(false);
    
        try (NoExpressionCallTOResultSet rsNoExpr = dao.getNoExpressionCalls(params)) {
            List<NoExpressionCallTO> noExprTOs = new ArrayList<NoExpressionCallTO>();
            while (rsNoExpr.next()) {
                NoExpressionCallTO to = rsNoExpr.getTO();
                if (!nonInformativesAnatEntities.contains(to.getAnatEntityId())) {
                    noExprTOs.add(to);
                }
            }
            log.info("Done retrieving basic no-expression calls, {} calls found", noExprTOs.size());
            return log.exit(noExprTOs); 
        } 
    }

    /**
     * Retrieves all global expression calls for given species, present into the Bgee database.
     * <p>
     * We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs, neither
     * INCLUDESUBSTAGES.
     * 
     * @param speciesIds                    A {@code Set} of {@code String}s that are the IDs of 
     *                                      species allowing to filter the global expression calls 
     *                                      to use.
     * @return                              A {@code List} of {@code ExpressionCallTO}s containing 
     *                                      all global expression calls of the given species.
     * @throws DAOException     If an error occurred while getting the data from the Bgee database.
     * @throws UnsupportedOperationException If in the given {@code ExpressionCallParams},
     *                                        {@code isIncludeSubStages} is set to {@code true},
     *                                        because it is not implemented yet.
     */
    private List<ExpressionCallTO> loadGlobalExprCallsFromDb(Set<String> speciesIds) 
                    throws DAOException, UnsupportedOperationException {
        log.entry(speciesIds);
    
        log.info("Start retrieving global expression calls for the species IDs {}...", speciesIds);
    
        ExpressionCallDAO dao = this.getExpressionCallDAO();
        // We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
        // We don't need INCLUDESUBSTAGES. 
        dao.setAttributes(ExpressionCallDAO.Attribute.GENEID, 
                ExpressionCallDAO.Attribute.STAGEID, ExpressionCallDAO.Attribute.ANATENTITYID, 
                ExpressionCallDAO.Attribute.AFFYMETRIXDATA, ExpressionCallDAO.Attribute.ESTDATA,
                ExpressionCallDAO.Attribute.INSITUDATA, ExpressionCallDAO.Attribute.RNASEQDATA,
                ExpressionCallDAO.Attribute.INCLUDESUBSTRUCTURES);
        
        ExpressionCallParams params = new ExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);
        params.setIncludeSubstructures(true);
    
        List<ExpressionCallTO> globalExprTOs = dao.getExpressionCalls(params).getAllTOs();

        log.info("Done retrieving global expression calls, {} calls found", globalExprTOs.size());
        
        return log.exit(globalExprTOs); 
    }

    /**
     * Retrieves all global no-expression calls for given species in a non-informative anatomical 
     * entity, present into the Bgee database.
     * <p>
     * We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
     * 
     * @param speciesIds                    A {@code Set} of {@code String}s that are the IDs of 
     *                                      species allowing to filter the global no-expression 
     *                                      calls to use.
     * @param nonInformativesAnatEntities   A {@code List} of {@code String}s that are the 
     *                                      non-informative anatomical entities.
     * @return                              A {@code List} of {@code NoExpressionCallTO}s containing 
     *                                      all global no-expression calls of the given species.
     * @throws DAOException     If an error occurred while getting the data from the Bgee database.
     */
    private List<NoExpressionCallTO> loadGlobalNoExprCallsFromDb(
            Set<String> speciesIds, List<String> nonInformativesAnatEntities) throws DAOException {
        log.entry(speciesIds, nonInformativesAnatEntities);
    
        log.info("Start retrieving global no-expression calls for the species IDs {}...", speciesIds);
    
        NoExpressionCallDAO dao = this.getNoExpressionCallDAO();
        // We don't retrieve ID to be able to compare calls on gene, stage and anatomical IDs.
        // We don't need INCLUDEPARENTSTRUCTURES. 
        dao.setAttributes(NoExpressionCallDAO.Attribute.GENEID, 
                NoExpressionCallDAO.Attribute.DEVSTAGEID, NoExpressionCallDAO.Attribute.ANATENTITYID, 
                NoExpressionCallDAO.Attribute.AFFYMETRIXDATA, NoExpressionCallDAO.Attribute.INSITUDATA, 
                NoExpressionCallDAO.Attribute.RNASEQDATA, 
                NoExpressionCallDAO.Attribute.INCLUDEPARENTSTRUCTURES);
    
        NoExpressionCallParams params = new NoExpressionCallParams();
        params.addAllSpeciesIds(speciesIds);
        params.setIncludeParentStructures(true);
        
        try (NoExpressionCallTOResultSet rsGlobalNoExpr = dao.getNoExpressionCalls(params)) {
            List<NoExpressionCallTO> globalNoExprTOs = new ArrayList<NoExpressionCallTO>();
            while (rsGlobalNoExpr.next()) {
                NoExpressionCallTO to = rsGlobalNoExpr.getTO();
                if (!nonInformativesAnatEntities.contains(to.getAnatEntityId())) {
                    globalNoExprTOs.add(to);
                }
            }
            log.info("Done retrieving basic no-expression calls, {} calls found", globalNoExprTOs.size());
            return log.exit(globalNoExprTOs); 
        } 
    }

    /**
     * Merges basic and global calls provided in a {@code SortedMap} where keys are {@code CallTO}s 
     * providing the information of gene-anat.entity-stage, the associated values being 
     * {@code Collection} of {@code CallTO}s with the corresponding gene-anat.entity-stage, and
     * fill a {@code List} of {@code Map}s where keys are file column names and values are data 
     * associated to the column name.
     *  
     * @param allCalls  A {@code SortedMap} where keys are {@code CallTO}s providing 
     *                  the information of gene-anat.entity-stage, the associated values 
     *                  being {@code Collection} of {@code CallTO}s with the corresponding 
     *                  gene-anat.entity-stage. {@code Entry}s are ordered according to 
     *                  the natural ordering of the IDs of the gene, anat. entity, and stage, 
     *                  in that order.
     * @return          A {@code List} of {@code Map}s where keys are file column names and 
     *                  values are data associated to the column name.
     */
    private List<Map<String, String>> mergeCallsFromSortedMap(SortedMap<CallTO, Collection<CallTO>> allCalls) {
        log.entry(allCalls);
        
        List<Map<String, String>> allRows = new ArrayList<Map<String, String>>();

        for (Entry<CallTO, Collection<CallTO>> callGroup : allCalls.entrySet()) {
            log.trace("Start merging the group of calls: {}", callGroup);
            Map<String, String> row = new HashMap<String, String>();
            
            ExpressionCallTO basicExprTO = null, globalExprTO = null;
            NoExpressionCallTO basicNoExprTO = null , globalNoExprTO = null;
            for (CallTO call: callGroup.getValue()) {
                if (call instanceof  ExpressionCallTO) {
                    if (((ExpressionCallTO) call).isIncludeSubstructures()) {
                        globalExprTO = (ExpressionCallTO) call;
                    } else {
                        basicExprTO = (ExpressionCallTO) call;
                    }
                } else if (call instanceof NoExpressionCallTO){
                    if (((NoExpressionCallTO) call).isIncludeParentStructures()) {
                        globalNoExprTO = (NoExpressionCallTO) call;
                    } else {
                        basicNoExprTO = (NoExpressionCallTO) call;
                    }
                } else {
                    throw log.throwing(new IllegalArgumentException("The CallTO provided (" +
                            call.getClass() + ") is not manage for expression/no-expression data"));
                }
            }
            
            // Default values
            ExpressionData affyData = ExpressionData.NODATA, estData = ExpressionData.NODATA, 
                    inSituData = ExpressionData.NODATA, relaxedInSituData = ExpressionData.NODATA, 
                    rnaSeqData = ExpressionData.NODATA; 
            ExpressionData resume = ExpressionData.NODATA;
            Origin affyOrigin = Origin.NODATA, estOrigin = Origin.NODATA, 
                    inSituOrigin = Origin.NODATA, relaxedInSituOrigin = Origin.NODATA,
                    rnaSeqOrigin = Origin.NODATA; 
            
            if (basicExprTO != null && basicNoExprTO == null && globalExprTO == null && globalNoExprTO == null) {
                resume = this.resumeExpresionCallDataStates(basicExprTO);

                affyData = this.convertDateStateToExpressionData(basicExprTO.getAffymetrixData(), false);
                estData = this.convertDateStateToExpressionData(basicExprTO.getESTData(), false);
                inSituData = this.convertDateStateToExpressionData(basicExprTO.getInSituData(), false);
                rnaSeqData = this.convertDateStateToExpressionData(basicExprTO.getRNASeqData(), false);

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), null,null, null);
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,null, null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), null,null, null);
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), null, null, null);

            } else if (basicExprTO == null && basicNoExprTO != null && globalExprTO == null && globalNoExprTO == null) {
                resume = ExpressionData.NOEXPRESSION;

                affyData = this.convertDateStateToExpressionData(basicNoExprTO.getAffymetrixData(), true);
                inSituData = this.convertDateStateToExpressionData(basicNoExprTO.getInSituData(), true);
                relaxedInSituData = this.convertDateStateToExpressionData(basicNoExprTO.getRelaxedInSituData(), true);
                rnaSeqData = this.convertDateStateToExpressionData(basicNoExprTO.getRNASeqData(), true);

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getAffymetrixData(), null, null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getInSituData(), null, null);
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(), null, null);
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRNASeqData(), null, null);
                
            } else if (basicExprTO == null && basicNoExprTO == null && globalExprTO != null && globalNoExprTO == null) {
                resume = this.resumeExpresionCallDataStates(globalExprTO);
                
                affyData = this.convertDateStateToExpressionData(globalExprTO.getAffymetrixData(), false);
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.convertDateStateToExpressionData(globalExprTO.getInSituData(), false);
                rnaSeqData = this.convertDateStateToExpressionData(globalExprTO.getRNASeqData(), false);

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, globalExprTO.getAffymetrixData(), null);
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, globalExprTO.getInSituData(), null);
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, globalExprTO.getRNASeqData(), null);

            } else if (basicExprTO == null && basicNoExprTO == null && globalExprTO == null && globalNoExprTO != null) {
                resume = ExpressionData.NOEXPRESSION;

                affyData = this.convertDateStateToExpressionData(globalNoExprTO.getAffymetrixData(), true);
                inSituData = this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                relaxedInSituData = this.convertDateStateToExpressionData(globalNoExprTO.getRelaxedInSituData(), true);
                rnaSeqData = this.convertDateStateToExpressionData(globalNoExprTO.getRNASeqData(), true);

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, null, globalNoExprTO.getAffymetrixData());
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, null, globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null, null, globalNoExprTO.getRNASeqData());

            } else if (basicExprTO != null && basicNoExprTO != null && globalExprTO == null && globalNoExprTO == null) {
                resume = ExpressionData.HIGHAMBIGUITY;
                
                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getAffymetrixData(), basicNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(basicExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getInSituData(), basicNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(basicNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getRNASeqData(), basicNoExprTO.getRNASeqData());

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), basicNoExprTO.getAffymetrixData(),
                        null, null);
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        null, null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), basicNoExprTO.getInSituData(),
                        null, null);
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(),
                        null, null);
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), basicNoExprTO.getRNASeqData(),
                        null, null);

            } else if (basicExprTO != null && basicNoExprTO == null && globalExprTO != null && globalNoExprTO == null) {
                resume = this.resumeExpresionCallDataStates(globalExprTO);

                affyData = this.convertDateStateToExpressionData(globalExprTO.getAffymetrixData(), false);
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.convertDateStateToExpressionData(globalExprTO.getInSituData(), false);
                rnaSeqData = this.convertDateStateToExpressionData(globalExprTO.getRNASeqData(), false);
                
                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), null,
                        globalExprTO.getAffymetrixData(), null);
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), null,
                        globalExprTO.getInSituData(), null);
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), null,
                        globalExprTO.getRNASeqData(), null);

            } else if (basicExprTO != null && basicNoExprTO == null && globalExprTO == null && globalNoExprTO != null) {
                resume = ExpressionData.LOWAMBIGUITY;

                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(basicExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), null,
                        null, globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        null, null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), null,
                        null, globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), null,
                        null, globalNoExprTO.getRNASeqData());

            } else if (basicExprTO == null && basicNoExprTO != null && globalExprTO != null && globalNoExprTO == null) {
                throw new IllegalStateException("There is a no-expression call while there is no "
                        + "globla no-expression call.");
                
            } else if (basicExprTO == null && basicNoExprTO != null && globalExprTO == null && globalNoExprTO != null) {
                resume = ExpressionData.NOEXPRESSION;
                
                affyData = this.convertDateStateToExpressionData(globalNoExprTO.getAffymetrixData(), true);
                inSituData = this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getRelaxedInSituData(), true);
                rnaSeqData = this.convertDateStateToExpressionData(globalNoExprTO.getRNASeqData(), true);

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getAffymetrixData(),
                        null, globalNoExprTO.getAffymetrixData());
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getInSituData(),
                        null, globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(),
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRNASeqData(),
                        null, globalNoExprTO.getRNASeqData());

            } else if (basicExprTO == null && basicNoExprTO == null && globalExprTO != null && globalNoExprTO != null) {
                resume = ExpressionData.LOWAMBIGUITY;
                
                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());
                
                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

            } else if (basicExprTO != null && basicNoExprTO != null && globalExprTO != null && globalNoExprTO == null) {
                throw new IllegalStateException("There is a no-expression call while there is no "
                        + "globla no-expression call.");

            } else if (basicExprTO != null && basicNoExprTO != null && globalExprTO == null && globalNoExprTO != null) {
                resume = ExpressionData.HIGHAMBIGUITY;
                
                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(basicExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        basicExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());
                
                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), basicNoExprTO.getAffymetrixData(),
                        null, globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        null, null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), basicNoExprTO.getInSituData(),
                        null, globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(),
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), basicNoExprTO.getRNASeqData(),
                        null, globalNoExprTO.getRNASeqData());

            } else if (basicExprTO != null && basicNoExprTO == null && globalExprTO != null && globalNoExprTO != null) {
                resume = ExpressionData.HIGHAMBIGUITY;
                
                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), null,
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), null,
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), null,
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

            } else if (basicExprTO == null && basicNoExprTO != null && globalExprTO != null && globalNoExprTO != null) {
                resume = ExpressionData.HIGHAMBIGUITY;

                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getAffymetrixData(),
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, null,
                        globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getInSituData(),
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(),
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRNASeqData(),
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

            } else if (basicExprTO != null && basicNoExprTO != null && globalExprTO != null && globalNoExprTO != null) {
                resume = ExpressionData.HIGHAMBIGUITY;
                
                affyData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estData = this.convertDateStateToExpressionData(globalExprTO.getESTData(), false);
                inSituData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituData = 
                        this.convertDateStateToExpressionData(globalNoExprTO.getRelaxedInSituData(), true);
                rnaSeqData = this.mergeExprAndNoExprDataStatesInExprData(
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());

                affyOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getAffymetrixData(), basicNoExprTO.getAffymetrixData(),
                        globalExprTO.getAffymetrixData(), globalNoExprTO.getAffymetrixData());
                estOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getESTData(), null,
                        globalExprTO.getESTData(), null);
                inSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getInSituData(), basicNoExprTO.getInSituData(),
                        globalExprTO.getInSituData(), globalNoExprTO.getInSituData());
                relaxedInSituOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        null, basicNoExprTO.getRelaxedInSituData(),
                        null, globalNoExprTO.getRelaxedInSituData());
                rnaSeqOrigin = this.checkBasicAndGlobalDataStatesAndGenerateOrigin( 
                        basicExprTO.getRNASeqData(), basicNoExprTO.getRNASeqData(),
                        globalExprTO.getRNASeqData(), globalNoExprTO.getRNASeqData());
            } else {
                throw log.throwing(new IllegalStateException("No basic and global calls for the "
                        + "triplet gene(" + callGroup.getKey().getGeneId() + ") - organ (" + 
                        callGroup.getKey().getAnatEntityId() + ") - stage (" + 
                        callGroup.getKey().getStageId() + ")"));
            }                

            row.put(GENE_ID_COLUMN_NAME, callGroup.getKey().getGeneId());
            row.put(ANATENTITY_ID_COLUMN_NAME, callGroup.getKey().getAnatEntityId());
            row.put(STAGE_ID_COLUMN_NAME, callGroup.getKey().getStageId());
            
            row.put(AFFYMETRIXDATA_COLUMN_NAME, affyData.getStringRepresentation());
            row.put(ESTDATA_COLUMN_NAME, estData.getStringRepresentation());
            row.put(INSITUDATA_COLUMN_NAME, inSituData.getStringRepresentation());
            row.put(RELAXEDINSITUDATA_COLUMN_NAME, relaxedInSituData.getStringRepresentation());
            row.put(RNASEQDATA_COLUMN_NAME, rnaSeqData.getStringRepresentation());
            
            row.put(AFFYMETRIX_ORIGIN_COLUMN_NAME, affyOrigin.getStringRepresentation());
            row.put(EST_ORIGIN_COLUMN_NAME, estOrigin.getStringRepresentation());
            row.put(INSITU_ORIGIN_COLUMN_NAME, inSituOrigin.getStringRepresentation());
            row.put(RELAXEDINSITU_ORIGIN_COLUMN_NAME, relaxedInSituOrigin.getStringRepresentation());
            row.put(RNASEQ_ORIGIN_COLUMN_NAME, rnaSeqOrigin.getStringRepresentation());

            row.put(EXPRESSION_COLUMN_NAME, resume.getStringRepresentation());
            
            allRows.add(row);
            log.debug("Added row: {}", row);
        }
        
        return log.exit(allRows);
    }

    private ExpressionData resumeExpresionCallDataStates(ExpressionCallTO call) {
        log.entry(call);
        
        Set<DataState> allData = new HashSet<DataState>(
                Arrays.asList(call.getAffymetrixData(), call.getESTData(), call.getInSituData(), 
                        call.getRelaxedInSituData(), call.getRNASeqData()));
        if (allData.contains(DataState.HIGHQUALITY)) {
            return log.exit(ExpressionData.HIGHQUALITY);
        }
        if (allData.contains(DataState.LOWQUALITY)) {
            return log.exit(ExpressionData.LOWQUALITY);
        }
        return log.exit(ExpressionData.NODATA);
    }

    /**
     * Generate the {@code Origin} of a data type from basic and global call data states of a
     * triplet gene-anat.entity-stage of interest.
     * <p>
     * If a {@code DataState} is {@code null}, it means that the corresponding {@code CallTO} is 
     * also {@code null}. If the corresponding {@code CallTO} is not {@code null}, the 
     * {@code DataState} is set to {@code NODATA}.
     * 
     * @param basicExprCallDataState    A {@code DataState} that is the basic expression call 
     *                                  {@code DataState} for the triplet of interest.
     * @param basicNoExprCallDataState  A {@code DataState} that is the basic no-expression call 
     *                                  {@code DataState} for the triplet of interest.
     * @param globalExprCallDataState   A {@code DataState} that is the global expression call 
     *                                  {@code DataState} for the triplet of interest.
     * @param globalNoExprCallDataState A {@code DataState} that is the global no-expression call 
     *                                  {@code DataState} for the triplet of interest.
     * @return                          An {@code Origin} of the triplet of interest.  
     * @throws IllegalStateException    If there is an inconsistency between {@code DataState}s
     *                                  (for instance, expression and no-expression for the same 
     *                                  data type).
     */
    private Origin checkBasicAndGlobalDataStatesAndGenerateOrigin(
            DataState basicExprCallDataState, DataState basicNoExprCallDataState, 
            DataState globalExprCallDataState, DataState globalNoExprCallDataState) 
                    throws IllegalStateException { 
        // TODO : state or argument: argument for the function and state for the class
        log.entry(basicExprCallDataState, basicNoExprCallDataState, 
                globalExprCallDataState, globalNoExprCallDataState);
        
        boolean hasExpression = false;
        boolean hasNoExpression = false;
        
        if ((basicExprCallDataState != null && !basicExprCallDataState.equals(DataState.NODATA)) ||
                (globalExprCallDataState != null && !globalExprCallDataState.equals(DataState.NODATA))) {
            hasExpression = true;
        }
        
        if ((basicNoExprCallDataState != null && !basicNoExprCallDataState.equals(DataState.NODATA)) ||
                (globalNoExprCallDataState != null && !globalNoExprCallDataState.equals(DataState.NODATA))) {
            hasNoExpression = true;
        }

        if (hasExpression && hasNoExpression) {
            // Expression and no-expression
            throw log.throwing(new IllegalStateException("A basic expression call and " +
                    "a basic no-expression call have data for the same data type: " +
                    basicExprCallDataState.getStringRepresentation() + " for the expression call and " + 
                    basicNoExprCallDataState.getStringRepresentation() + " for the no-expression call."));
        }

        if (!hasExpression && !hasNoExpression) {
            return log.exit(Origin.NODATA);
        }

        if (hasExpression) {
            if (basicExprCallDataState == null || basicExprCallDataState.equals(DataState.NODATA)) {
                return log.exit(Origin.INFERRED);
            }
            if (globalExprCallDataState == null) {
                return log.exit(Origin.NOTINFERRED);
            }
            if (globalExprCallDataState.equals(DataState.NODATA)) {
                throw log.throwing(new IllegalStateException("No data in the global expression call " +
                        "while basic expression call has expression"));
            }
            if (basicExprCallDataState.ordinal() < globalExprCallDataState.ordinal()) {
                return log.exit(Origin.BOTH);
            } else if (basicExprCallDataState.ordinal() == globalExprCallDataState.ordinal()) {
                return log.exit(Origin.NOTINFERRED);
            } else {
                throw log.throwing(new IllegalStateException("The data of the global expression " +
                      "call (" + basicExprCallDataState.getStringRepresentation() +
                      ") is less than the corresponding basic expression call (" + 
                      globalExprCallDataState.getStringRepresentation()));
            }
        } else {
            if (basicNoExprCallDataState == null || basicNoExprCallDataState.equals(DataState.NODATA)) {
                return log.exit(Origin.INFERRED);
            }
            if (globalNoExprCallDataState == null) {
                return log.exit(Origin.NOTINFERRED);
            }
            if (globalNoExprCallDataState.equals(DataState.NODATA)) {
                throw log.throwing(new IllegalStateException("No data in the global no-expression " +
                        "call while basic no-expression call has no-expression"));
            }
            if (basicNoExprCallDataState.ordinal() < globalNoExprCallDataState.ordinal()) {
                return log.exit(Origin.BOTH);
            } else if (basicNoExprCallDataState.ordinal() == globalNoExprCallDataState.ordinal()) {
                return log.exit(Origin.NOTINFERRED);
            } else {
                throw log.throwing(new IllegalStateException("The data of the global no-expression " +
                      "call (" + basicNoExprCallDataState.getStringRepresentation() +
                      ") is less than the corresponding basic no-expression call (" + 
                      globalNoExprCallDataState.getStringRepresentation()));
            }
        }
    }

    /**
     * Converts a {@code DataState} into a {@code ExpressionData}.
     * 
     * @param dateState A {@code DataState} to be converted.
     * @return          The {@code ExpressionData} corresponding to the given {@code DataState}. 
     */
    private ExpressionData convertDateStateToExpressionData(DataState dateState, boolean isNoExpression) {
        log.entry(dateState, isNoExpression);
        
        ExpressionData exprData = null;
    
        if (dateState.equals(DataState.NODATA)) {
            exprData = ExpressionData.NODATA;            
        } else if (isNoExpression) {
            exprData = ExpressionData.NOEXPRESSION;
        } else if (dateState.equals(DataState.HIGHQUALITY)) {
                exprData = ExpressionData.HIGHQUALITY;                            
        } else if (dateState.equals(DataState.LOWQUALITY)) {
                exprData = ExpressionData.LOWQUALITY;                            
        } else {
            throw log.throwing(new IllegalArgumentException(
                    "The DataState provided (" + dateState.getStringRepresentation() + 
                    ") is unknown for " + DataState.class.getName()));            
        }
        return log.exit(exprData);
    }

    /**
     * Merge {@code DataState}s of one expression call and one no-expression call into 
     * an {@code ExpressionData}. 
     * 
     * @param dataStateExpr     A {@code DataState} from an expression call. 
     * @param dataStateNoExpr   A {@code DataState} from a no-expression call.
     * @return                  An {@code ExpressionData} combining {@code DataState}s of one 
     *                          expression call and one no-expression call.
     * @throws IllegalStateException    If an expression call and a no-expression call are found 
     *                                  for the same data type.
     */
    private ExpressionData mergeExprAndNoExprDataStatesInExprData(DataState dataStateExpr, 
            DataState dataStateNoExpr) throws IllegalStateException {
        log.entry(dataStateExpr, dataStateNoExpr);
    
        if (dataStateExpr == DataState.NODATA && dataStateNoExpr == DataState.NODATA) {
            return log.exit(ExpressionData.NODATA);
        }
        if (dataStateNoExpr == DataState.NODATA) {
            return log.exit(this.convertDateStateToExpressionData(dataStateExpr, false));
        }
        if (dataStateExpr == DataState.NODATA) {
            return log.exit(ExpressionData.NOEXPRESSION);
        }
        throw log.throwing(new IllegalStateException("An expression call and a no-expression call " +
                "could be found for the same data type."));
    }

    /**
     * Adds gene names in given {@code List}s of {@code Map}s where keys are column names and 
     * values are data associated to the column name.
     * <p>
     * The provided {@code List}s will be modified.
     * 
     * @param list1         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param list2         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param speciesId     A {@code String} that is the ID of species allowing to filter 
     *                      the genes to use.
     * @throws DAOException If an error occurred while getting the data from the Bgee database.
     */
    private void addGeneNames(List<Map<String, String>> list1, List<Map<String, String>> list2, 
            String speciesId) throws DAOException {
        log.entry(list1, list2, speciesId);
        
        log.info("Start retrieving gene names...");

        GeneDAO dao = this.getGeneDAO();
        dao.setAttributes(GeneDAO.Attribute.ID, GeneDAO.Attribute.NAME);
        
        Set<String> speciesFilter = new HashSet<String>();
        speciesFilter.add(speciesId);

        GeneTOResultSet rsGenes = dao.getGenes(speciesFilter);
        
        while (rsGenes.next()) {
            GeneTO geneTO = rsGenes.getTO();
            if (list1 != null) {
                for (Map<String, String> map1 : list1) {
                    if (map1.get(GENE_ID_COLUMN_NAME).equals(geneTO.getId())) {
                        map1.put(GENE_NAME_COLUMN_NAME, geneTO.getName());
                    }
                }
            }
            if (list2 != null) {
                for (Map<String, String> map2 : list2) {
                    if (map2.get(GENE_ID_COLUMN_NAME).equals(geneTO.getId())) {
                        map2.put(GENE_NAME_COLUMN_NAME, geneTO.getName());
                    }
                }
            }
        }
        rsGenes.close();
        log.info("Done retrieving gene names");
        log.debug("Modified list1: {}", list1);
        log.debug("Modified list2: {}", list2);
        log.exit();        
    }

    /**
     * Adds stage names in given {@code List}s of {@code Map}s where keys are column names and 
     * values are data associated to the column name.
     * <p>
     * The provided {@code List}s will be modified.
     * 
     * @param list1         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param list2         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param speciesId     A {@code String} that is the ID of species allowing to filter 
     *                      the stages to use.
     * @throws DAOException If an error occurred while getting the data from the Bgee database.
     */
    private void addStageNames(List<Map<String, String>> list1, List<Map<String, String>> list2, 
            String speciesId) throws DAOException {
        log.entry(list1, list2, speciesId);
        
        log.info("Start retrieving stage names...");

        StageDAO dao = this.getStageDAO();
        dao.setAttributes(StageDAO.Attribute.ID, StageDAO.Attribute.NAME);
        
        Set<String> speciesFilter = new HashSet<String>();
        speciesFilter.add(speciesId);

        StageTOResultSet rsStages = dao.getStages(speciesFilter);
        
        while (rsStages.next()) {
            StageTO stageTO = rsStages.getTO();
            if (list1 != null) {
                for (Map<String, String> map1 : list1) {
                    if (map1.get(STAGE_ID_COLUMN_NAME).equals(stageTO.getId())) {
                        map1.put(STAGE_NAME_COLUMN_NAME, stageTO.getName());
                    }
                }
            }
            if (list2 != null) {
                for (Map<String, String> map2 : list2) {
                    if (map2.get(STAGE_ID_COLUMN_NAME).equals(stageTO.getId())) {
                        map2.put(STAGE_NAME_COLUMN_NAME, stageTO.getName());
                    }
                }
            }
        }
        rsStages.close();
        log.info("Done retrieving stage names");
        log.debug("Modified list1: {}", list1);
        log.debug("Modified list2: {}", list2);
        log.exit();        
    }
    
    /**
     * Adds anatomical entity names in given {@code List} of {@code Map}s where keys are column 
     * names and values are data associated to the column name.
     * <p>
     * The provided {@code List}s will be modified.
     * 
     * @param list1         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param list2         A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @param speciesId     A {@code String} that is the ID of species allowing to filter 
     *                      the anatomical entities to use.
     * @throws DAOException If an error occurred while getting the data from the Bgee database.
     */
    private void addAnatEntityNames(List<Map<String, String>> list1, List<Map<String, String>> list2,
            String speciesId) throws DAOException {
        log.entry(list1, list2, speciesId);
        
        log.info("Start retrieving anatomical entity names...");

        AnatEntityDAO dao = this.getAnatEntityDAO();
        dao.setAttributes(AnatEntityDAO.Attribute.ID, AnatEntityDAO.Attribute.NAME);
        
        Set<String> speciesFilter = new HashSet<String>();
        speciesFilter.add(speciesId);

        AnatEntityTOResultSet rsAnatEntities = dao.getAnatEntities(speciesFilter);
        
        while (rsAnatEntities.next()) {
            AnatEntityTO anatEntityTO = rsAnatEntities.getTO();
            if (list1 != null) {
                for (Map<String, String> map1 : list1) {
                    if (map1.get(ANATENTITY_ID_COLUMN_NAME).equals(anatEntityTO.getId())) {
                        map1.put(ANATENTITY_NAME_COLUMN_NAME, anatEntityTO.getName());
                    }
                }
            }
            if (list2 != null) {
                for (Map<String, String> map2 : list2) {
                    if (map2.get(ANATENTITY_ID_COLUMN_NAME).equals(anatEntityTO.getId())) {
                        map2.put(ANATENTITY_NAME_COLUMN_NAME, anatEntityTO.getName());
                    }
                }

            }
        }
        rsAnatEntities.close();
        log.info("Done retrieving anatomical entity names");
        log.debug("Modified list1: {}", list1);
        log.debug("Modified list2: {}", list2);
        log.exit();        
    }

    /**
     * Write the download TSV files (simple and complete files). The data are provided 
     * by a {@code List} of {@code Map}s where keys are column names and values are data associated 
     * to the column name. 
     * <p>
     * The generated TSV file will have one header line. Both files do not have the same headers:
     * in the simple file, expression/no-expression from different data types are merged in a single
     * column called Expression/No-expression.
     * 
     * @param inputList         A {@code List} of {@code Map}s where keys are column names and 
     *                          values are data associated to the column name.
     * @param outputFile        A {@code String} that is the path to the simple output file
     *                          were data will be written as TSV.
     * @param isSimplifiedFile  A {@code boolean} defining whether the output file is a simple file.
     *                          If {code true}, data from different data types are merged
     *                          in a single column.
     * @param isDiffExpr        A {@code boolean} defining whether the output file contains 
     *                          expression/no-expression data or differential expression data.
     *                          If {code true}, output file will contain differential expression 
     *                          data.
     * @throws IOException      If an error occurred while trying to write the {@code outputFile}.
     */
    private void createDownloadFiles(List<Map<String, String>> inputList, String outputFile, 
            boolean isSimplifiedFile, boolean isDiffExpr) throws IOException {
        log.entry(inputList, outputFile, isSimplifiedFile, isDiffExpr);
                
        CellProcessor[] processorFile = generateCellProcessor(isSimplifiedFile, isDiffExpr);
        
        final String[] headerFile;
        if (isSimplifiedFile) {
            if (isDiffExpr) {
                headerFile= new String[] { 
                        GENE_ID_COLUMN_NAME, GENE_NAME_COLUMN_NAME, 
                        STAGE_ID_COLUMN_NAME, STAGE_NAME_COLUMN_NAME,
                        ANATENTITY_ID_COLUMN_NAME, ANATENTITY_NAME_COLUMN_NAME,
                        DIFFEXPRESSION_COLUMN_NAME};
            } else {
                headerFile= new String[] { 
                        GENE_ID_COLUMN_NAME, GENE_NAME_COLUMN_NAME, 
                        STAGE_ID_COLUMN_NAME, STAGE_NAME_COLUMN_NAME,
                        ANATENTITY_ID_COLUMN_NAME, ANATENTITY_NAME_COLUMN_NAME,
                        EXPRESSION_COLUMN_NAME};
            }
        } else {
            headerFile = new String[] {
                    GENE_ID_COLUMN_NAME, GENE_NAME_COLUMN_NAME, 
                    STAGE_ID_COLUMN_NAME, STAGE_NAME_COLUMN_NAME,
                    ANATENTITY_ID_COLUMN_NAME, ANATENTITY_NAME_COLUMN_NAME,
                    AFFYMETRIXDATA_COLUMN_NAME, AFFYMETRIX_ORIGIN_COLUMN_NAME,
                    ESTDATA_COLUMN_NAME, EST_ORIGIN_COLUMN_NAME,
                    INSITUDATA_COLUMN_NAME, INSITU_ORIGIN_COLUMN_NAME, 
                    RELAXEDINSITUDATA_COLUMN_NAME, RELAXEDINSITU_ORIGIN_COLUMN_NAME, 
                    RNASEQDATA_COLUMN_NAME, RNASEQ_ORIGIN_COLUMN_NAME,
                    EXPRESSION_COLUMN_NAME};
        }
            
        writeFileContent(inputList, outputFile, headerFile, processorFile,
                isSimplifiedFile, isDiffExpr);

        log.exit();
    }
    
    /**
     * Generate a {@code CellProcessor} needed to write a download file. 
     * 
     * @param isSimplifiedFile  A {@code boolean} defining whether the output file is a simple file.
     *                          If {code true}, data from different data types are merged
     *                          in a single column.
     * @param isDiffExpr        A {@code boolean} defining whether the output file contains 
     *                          expression/no-expression data or differential expression data.
     *                          If {code true}, output file will contain differential expression 
     *                          data.
     * @return                  A {@code CellProcessor} needed to write a simple or complete 
     *                          download file.
     */
    public static CellProcessor[] generateCellProcessor(boolean isSimplifiedFile, boolean isDiffExpr) {
        log.entry(isSimplifiedFile, isDiffExpr);
        
        List<Object> dataElements = new ArrayList<Object>();
        if (isDiffExpr) {
            for (DiffExpressionData data : DiffExpressionData.values()) {
                dataElements.add(data.getStringRepresentation());
            } 
        } else {
            for (ExpressionData data : ExpressionData.values()) {
                dataElements.add(data.getStringRepresentation());
            } 
        }
        
        List<Object> originElement = new ArrayList<Object>();
        for (Origin data : Origin.values()) {
            originElement.add(data.getStringRepresentation());
        } 
    
        final CellProcessor[] processors;
        if (isSimplifiedFile) {
                processors = new CellProcessor[] { 
                        new NotNull(), // gene ID
                        new NotNull(), // gene Name
                        new NotNull(), // developmental stage ID
                        new NotNull(), // developmental stage name
                        new NotNull(), // anatomical entity ID
                        new NotNull(), // anatomical entity name
                        new IsElementOf(dataElements)}; // Differential expression or Expression/No-expression
        } else {
                processors = new CellProcessor[] { 
                        new NotNull(), // gene ID
                        new NotNull(), // gene Name
                        new NotNull(), // developmental stage ID
                        new NotNull(), // developmental stage name
                        new NotNull(), // anatomical entity ID
                        new NotNull(), // anatomical entity name
                        new IsElementOf(dataElements),  // Affymetrix data
                        new IsElementOf(originElement),  // Affymetrix data inferred
                        new IsElementOf(dataElements),  // EST data
                        new IsElementOf(originElement),  // EST data inferred
                        new IsElementOf(dataElements),  // In Situ data
                        new IsElementOf(originElement),  // In Situ data inferred
                        new IsElementOf(dataElements),  // Relaxed in Situ data
                        new IsElementOf(originElement),  // Relaxed in Situ data inferred
                        new IsElementOf(dataElements), // RNA-seq data
                        new IsElementOf(originElement), // RNA-seq data inferred
                        new IsElementOf(dataElements)}; // Differential expression or Expression/No-expression
        }
        return log.exit(processors);
    }

    /**
     * Write a download TSV file according to the data are provided by a {@code List} of 
     * {@code Map}s where keys are column names and values are data associated to the column name, 
     * the given output file path, headers, cell processors and {@code boolean} defining whether 
     * it is a simple download file.
     * <p>
     * If the {@code isSimplifiedFile} is {code true}, expression data from different data types 
     * are merged in a single column called Expression.
     * 
     * @param inputList         A {@code List} of {@code Map}s where keys are column names and 
     *                          values are data associated to the column name. 
     * @param outputFile        A {@code String} that is the path to the output file
     *                          were data will be written as TSV.
     * @param headers           An {@code Array} of {@code String}s containing headers of the file.
     * @param processors        An {@code Array} of {@code CellProcessor}s containing cell 
     *                          processors which automates the data type conversions, and enforce 
     *                          column constraints.
     * @param isSimplifiedFile  A {@code boolean} defining whether the output file is a simple file.
     *                          If {code true}, data from different data types are merged
     *                          in a single column.
     * @param isDiffExpr        A {@code boolean} defining whether the output file contains 
     *                          expression/no-expression data or differential expression data.
     *                          If {code true}, output file will contain differential expression 
     *                          data.
     * @throws IOException      If an error occurred while trying to write {@code outputFile}.
     */
    private void writeFileContent(List<Map<String, String>> inputList, String outputFile, 
            String[] headers, CellProcessor[] processors, 
            boolean isSimplifiedFile, boolean isDiffExpr) throws IOException {
        log.entry(inputList, outputFile, headers, processors, isSimplifiedFile, isDiffExpr);
        
        try (ICsvMapWriter mapWriter = new CsvMapWriter(new FileWriter(outputFile),
                Utils.TSVCOMMENTED)) {
    
            mapWriter.writeHeader(headers);
                        
            for (Map<String, String> map: inputList) {                
                Map<String, Object> row = new HashMap<String, Object>();
                for (Entry<String, String> entry : map.entrySet()) {
                    if (!isSimplifiedFile ||
                            (!entry.getKey().equals(AFFYMETRIXDATA_COLUMN_NAME) &&
                             !entry.getKey().equals(AFFYMETRIX_ORIGIN_COLUMN_NAME) &&
                             !entry.getKey().equals(ESTDATA_COLUMN_NAME) &&
                             !entry.getKey().equals(EST_ORIGIN_COLUMN_NAME) &&
                             !entry.getKey().equals(INSITUDATA_COLUMN_NAME) && 
                             !entry.getKey().equals(INSITU_ORIGIN_COLUMN_NAME) && 
                             !entry.getKey().equals(RELAXEDINSITUDATA_COLUMN_NAME) && 
                             !entry.getKey().equals(RELAXEDINSITU_ORIGIN_COLUMN_NAME) && 
                             !entry.getKey().equals(RNASEQDATA_COLUMN_NAME) &&
                             !entry.getKey().equals(RNASEQ_ORIGIN_COLUMN_NAME))) {
                        // We do not write some columns in simple file.
                        row.put(entry.getKey(), entry.getValue());
                    }
                }
                log.trace("Write the row: {}", row);
                mapWriter.write(row, headers, processors);
            }
        }
        
        log.exit();
    }

    /**
     * Generate the {@code List} of {@code Map}s containing data to be written in download file
     * for differential expression.
     * 
     * @param speciesId     A {@code String} that is the ID of species for which data are retrieved.
     * @return              A {@code List} of {@code Map}s where keys are column names and 
     *                      values are data associated to the column name.
     * @throws UnsupportedOperationException Not yet implemented
     */
    private List<Map<String, String>> generateDiffExprRows(String speciesId) 
            throws UnsupportedOperationException {
        log.entry(speciesId);
        // TODO Auto-generated method stub
        throw log.throwing(new UnsupportedOperationException("Differential expression is not yet "
                + "supported, it's need to be implemented"));
    }
}
