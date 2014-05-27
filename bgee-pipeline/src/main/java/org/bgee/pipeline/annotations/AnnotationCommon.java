package org.bgee.pipeline.annotations;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.pipeline.CommandRunner;
import org.bgee.pipeline.Utils;
import org.bgee.pipeline.uberon.Uberon;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.io.ICsvMapWriter;

/**
 * Class to perform tasks common to all annotations used in Bgee (similarity annotations, 
 * expression data annotations, ...).
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class AnnotationCommon {
    /**
     * {@code Logger} of the class.
     */
    private final static Logger log = 
            LogManager.getLogger(AnnotationCommon.class.getName());
    
    /**
     * An unmodifiable {@code Set} of {@code String}s that are the potential names 
     * of columns containing anatomical entity IDs, in annotation files (for instance, 
     * expression data annotation files assign samples to one anatomical entity).
     * We allow multiple values because maybe this is inconsistent in the various 
     * annotation files.
     */
    public static final Set<String> ANAT_ENTITY_COL_NAMES = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(Uberon.ANAT_ENTITY_ID)));
    /**
     * An unmodifiable {@code Set} of {@code String}s that are the potential names 
     * of columns containing multiple anatomical entity IDs, in annotation files 
     * (for instance, in the similarity annotation file, to describe homology between 
     * "lung" and "swim bladder", we would use the syntax "UBERON:0002048|UBERON:0006860").
     * We allow multiple values because maybe this is inconsistent in the various 
     * annotation files. Allowed separators between entities are listed in 
     * {@link #ENTITY_SEPARATORS}.
     */
    public static final Set<String> MULTIPLE_ANAT_ENTITY_COL_NAMES = 
            Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList(SimilarityAnnotation.ENTITY_COL_NAME)));
    
    /**
     * A {@code String} that is the default separator between entities, in columns 
     * containing multiple entities, in annotation files. See {@link #ENTITY_SEPARATORS}.
     * @see #ENTITY_SEPARATORS
     */
    public final static String DEFAULT_ENTITY_SEPARATOR = "|";
    
    /**
     * An unmodifiable {@code Set} of {@code String}s that are the allowed separators 
     * between entities in columns containing multiple entities, in annotation files. 
     * See {@link #MULTIPLE_ANAT_ENTITY_COL_NAMES} for an example of such columns.
     * 
     * @see #DEFAULT_ENTITY_SEPARATOR
     */
    public final static Set<String> ENTITY_SEPARATORS = 
            Collections.unmodifiableSet(new HashSet<String>(
                    Arrays.asList(DEFAULT_ENTITY_SEPARATOR, ",")));
    
    /**
     * Actions that can be launched from this main method, depending on the first 
     * element in {@code args}: 
     * <ul>
     * <li>If the first element in {@code args} is "filterInfoFiles", the action 
     * will be to filter the information obtained following simplification of Uberon, 
     * to contain only anatomical entities used in our annotations. See {@link 
     * #filterUberonSimplificationInfo(Set, Set, Set, String)} for more details. 
     * Following elements in {@code args} must then be: 
     *   <ol>
     *   <li>A list of paths to information files generated from Uberon simplification, 
     *   separated by the {@code String} {@link CommandRunner#LIST_SEPARATOR}.
     *   <li>A list of paths to annotation files using single anatomical entities 
     *   for annotations (see {@link #ANAT_ENTITY_COL_NAMES}), 
     *   separated by the {@code String} {@link CommandRunner#LIST_SEPARATOR}.
     *   <li>A list of paths to annotation files using multiple anatomical entities 
     *   for annotations (see {@link #MULTIPLE_ENTITY_COL_NAMES}), 
     *   separated by the {@code String} {@link CommandRunner#LIST_SEPARATOR}.
     *   <li>The path to the directory where to store filtered info files. 
     *   </ol>
     * </ul>
     * 
     * @param args  An {@code Array} of {@code String}s containing the requested parameters.
     * @throws IOException             
     * @throws FileNotFoundException 
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        log.entry((Object[]) args);
        
        if (args[0].equalsIgnoreCase("filterInfoFiles")) {
            if (args.length != 5) {
                throw log.throwing(new IllegalArgumentException(
                        "Incorrect number of arguments provided, expected " + 
                        "5 arguments, " + args.length + " provided."));
            }
            
            AnnotationCommon.filterUberonSimplificationInfo(
                    new HashSet<String>(CommandRunner.parseListArgument(args[1])), 
                    new HashSet<String>(CommandRunner.parseListArgument(args[2])), 
                    new HashSet<String>(CommandRunner.parseListArgument(args[3])), 
                    args[4]);
        } else {
            throw log.throwing(new UnsupportedOperationException("The following action " +
            		"is not recognized: " + args[0]));
        }
        
        log.exit();
    }
    
    /**
     * Filter the anatomical entities reported from {@link 
     * org.bgee.pipeline.uberon.Uberon#saveSimplificationInfo(OWLOntology, String, Collection) 
     * Uberon#saveSimplificationInfo} method, to keep only those used in annotations. 
     * This method will generate new files, corresponding to those listed in {@code infoFiles}, 
     * in the directory {@code filteredFileDirectory} (so make sure you provide a different 
     * directory from where original info files are stored). 
     * <p>
     * This method must be provided with the list of information files generated by 
     * {@code saveSimplificationInfo}, with the list of annotation files using 
     * single anatomical entity for annotations (see {@link #ANAT_ENTITY_COL_NAMES} 
     * for details), and with the list of annotation files using multiple anatomical entities 
     * for annotations (see {@link #MULTIPLE_ANAT_ENTITY_COL_NAMES} for details).
     * <p>
     * Files generated by {@code saveSimplificationInfo} are expected to have a header, 
     * and a column whose name is listed in {@link #ANAT_ENTITY_COL_NAMES}. 
     * <p>
     * Annotation files using single anatomical entity for annotations are expected 
     * to have a header, and a column whose name is listed in {@link #ANAT_ENTITY_COL_NAMES}.
     * <p>
     * Annotation files using multiple anatomical entities for annotations are expected 
     * to have a header, a column whose name is listed in 
     * {@link #MULTIPLE_ENTITY_COL_NAMES}, with entities separated by one of the separators 
     * listed in {@link #ENTITY_SEPARATORS}. 
     * 
     * @param infoFiles                 A {@code Set} of {@code String}s that are the paths 
     *                                  to the files generated by the method 
     *                                  {@code Uberon#saveSimplificationInfo}.
     * @param singleAnatEntityFiles     A {@code Set} of {@code String}s that are the paths 
     *                                  to the annotation files using single anatomical entity.
     * @param multipleAnatEntitieFiles  A {@code Set} of {@code String}s that are the paths 
     *                                  to the annotation files using multiple anatomical 
     *                                  entities.
     * @param filteredFileDirectory     A {@code String} that is the directory where to store 
     *                                  generated filtered info files.
     * @throws IOException              If an error occurred while reading or writing a file.
     * @throws FileNotFoundException    If a provided file could not be found.
     */
    public static void filterUberonSimplificationInfo(Set<String> infoFiles, 
            Set<String> singleAnatEntityFiles, Set<String> multipleAnatEntitieFiles, 
            String filteredFileDirectory) throws FileNotFoundException, IOException {
        log.entry(infoFiles, singleAnatEntityFiles, multipleAnatEntitieFiles, 
                filteredFileDirectory);
        log.info("Start filtering Uberon simplification info...");
        
        //first, read all annotation files to get all anatomical entity IDs used
        Set<String> anatEntityIds = new HashSet<String>();
        for (String singleAnatEntityFile: singleAnatEntityFiles) {
            anatEntityIds.addAll(AnnotationCommon.extractAnatEntityIdsFromFile(
                    singleAnatEntityFile, true));
        }
        for (String multipleAnatEntitiesFile: multipleAnatEntitieFiles) {
            anatEntityIds.addAll(AnnotationCommon.extractAnatEntityIdsFromFile(
                    multipleAnatEntitiesFile, false));
        }
        
        //now, for each file infoFile, we generate a corresponding file with IDs filtered
        //(only those present in an annotation file will be used)
        for (String infoFile: infoFiles) {
            log.trace("Filtering info file {}...", infoFile);
            
            //read existing info file 
            try (ICsvMapReader mapReader = 
                    new CsvMapReader(new FileReader(infoFile), Utils.TSVCOMMENTED)) {
                
                String[] header = mapReader.getHeader(true);
                String entityColName = null;
                for (String colName: header) {
                    if (ANAT_ENTITY_COL_NAMES.contains(colName)) {
                        entityColName = colName;
                        break;
                    }
                }
                if (entityColName == null) {
                    throw log.throwing(new IllegalArgumentException("The info file " + 
                        infoFile + " does not contain any column to retrieve anatomical " +
                        		"entities from. Offending header: " + header));
                }

                //generate new filtered file
                Path infoFilePath = Paths.get(infoFile);
                Path filteredFilePath = Paths.get(filteredFileDirectory, 
                        infoFilePath.getFileName().toString());
                if (Files.exists(filteredFilePath)) {
                    if (Files.isSameFile(infoFilePath, filteredFilePath)) {
                        throw log.throwing(new IllegalArgumentException("The directory " +
                    		"provided to store filtered files (" + 
                            Paths.get(filteredFileDirectory).toAbsolutePath().toString() + 
                            ") contains also the original files."));
                    }
                    Files.delete(filteredFilePath);
                }
                //write to new filtered file
                try (ICsvMapWriter mapWriter = new CsvMapWriter(
                        new FileWriter(filteredFilePath.toFile()), Utils.TSVCOMMENTED)) {
                    
                    mapWriter.writeHeader(header);
                    Map<String, String> row;
                    while( (row = mapReader.read(header)) != null ) {
                        if (anatEntityIds.contains(row.get(entityColName))) {
                            log.trace("Writing row: {}", row);
                            mapWriter.write(row, header);
                        } else {
                            if (log.isTraceEnabled()) {
                                log.trace("Discarding row because anatomical entity " +
                                		"not used in annotations: " + infoFile + " - " + 
                                        mapReader.getLineNumber() + " - " + 
                                        mapReader.getUntokenizedRow());
                            }
                        }
                    }
                }
                
            }
            log.trace("Done filtering {}", infoFile);
        }
        
        log.info("Done filtering Uberon simplification info.");
        log.exit();
    }
    
    /**
     * Extract from the annotation TSV file {@code pathToTSVFile} the anatomical entity IDs used. 
     * This file must have a header, and can either have a column whose name is listed 
     * in {@link #ANAT_ENTITY_COL_NAMES}, containing single entity values, or a column 
     * whose name is listed in {@link #MULTIPLE_ANAT_ENTITY_COL_NAMES}, containing multiple 
     * entity values, separated by one of the separator listed in {@link #ENTITY_SEPARATORS}.
     * 
     * @param pathToTSVFile     A {@code String} that is the path to the TSV file to be read.
     * @param singleAnatEntity  A {@code boolean} defining whether the provided file contains 
     *                          a column with single entity values, or a column with 
     *                          multiple entities values. 
     * @return                  A {@code Set} of {@code String}s that are the IDs 
     *                          of the anatomical entities used in the provided TSV file.
     * @throws FileNotFoundException    If {@code pathToTSVFile} could not be found.
     * @throws IOException              If {@code pathToTSVFile} could not be read.
     * @throws IllegalArgumentException If {@code pathToTSVFile} did not allow to retrieve 
     *                                  anatomical entity IDs.
     */
    public static Set<String> extractAnatEntityIdsFromFile(String pathToTSVFile, 
            boolean singleAnatEntity) throws FileNotFoundException, IOException {
        log.entry(pathToTSVFile, singleAnatEntity);
        
        Set<String> anatEntityIds = new HashSet<String>();
        try (ICsvListReader listReader = new CsvListReader(
                new FileReader(pathToTSVFile), Utils.TSVCOMMENTED)) {

            //localize the proper column
            String[] header = listReader.getHeader(true);
            //column indexes start at 1 in super csv 
            int columnIndex = 0;
            for (int i = 0; i < header.length; i++) {
                if (singleAnatEntity && ANAT_ENTITY_COL_NAMES.contains(header[i])) {
                    //column indexes start at 1 in super csv 
                    columnIndex = i + 1;
                    break;
                } else if (!singleAnatEntity && 
                        MULTIPLE_ANAT_ENTITY_COL_NAMES.contains(header[i])) {
                    //column indexes start at 1 in super csv 
                    columnIndex = i + 1;
                    break;
                }
            }
            
            if (columnIndex == 0) {
                throw log.throwing(new IllegalArgumentException("The file " + pathToTSVFile + 
                        " does not contain any column to retrieve " +
                        "anatomical entity IDs from."));
            }
            
            //read file
            while( (listReader.read()) != null ) {
                String columnValue = listReader.get(columnIndex);
                if (StringUtils.isBlank(columnValue)) {
                    throw log.throwing(new IllegalArgumentException("The file " + pathToTSVFile + 
                            " contains a row with no anatomical entities. Offending row: " +
                            listReader.getLineNumber() + " - " + 
                            listReader.getUntokenizedRow()));
                }
                if (singleAnatEntity) {
                    anatEntityIds.add(columnValue.trim());
                } else {
                    anatEntityIds.addAll(
                            AnnotationCommon.parseMultipleEntitiesColumn(columnValue));
                }
            }
        }
        
        if (anatEntityIds.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("The file " + pathToTSVFile + 
                    " did not allow to retrieve any anatomical entity IDs."));
        }
        
        return log.exit(anatEntityIds);
    }
    
    
    /**
     * Parses a value extracted from a column accepting multiple entities 
     * for annotations (see {@link #MULTIPLE_ANAT_ENTITY_COL_NAMES} for an example). 
     * These multiple entities must be separated by one of the separators listed in 
     * {@link #ENTITY_SEPARATORS}.
     * <p>
     * The {@code String}s in the returned {@code List} are ordered using 
     * their natural ordering, to allow easier diff on generated file between releases.
     * 
     * @param columnContent     A {@code String} extracted from a column accepting multiple 
     *                          entities.
     * @return                  A {@code List} of {@code String}s that are the values 
     *                          corresponding to each individual entity,  
     *                          ordered by alphabetical order.
     */
    public static List<String> parseMultipleEntitiesColumn(String columnContent) {
        log.entry(columnContent);
        
        if (columnContent == null) {
            return log.exit(null);
        }
        String splitPattern = "";
        for (String separator: ENTITY_SEPARATORS) {
            if (!splitPattern.equals("")) {
                splitPattern += "|";
            }
            splitPattern += Pattern.quote(separator);
        }
        String[] values = columnContent.split(splitPattern);
        List<String> orderedValues = new ArrayList<String>();
        for (String uberonId: values) {
            orderedValues.add(uberonId.trim());
        }
        //perform the alphabetical ordering
        Collections.sort(orderedValues);
        
        return log.exit(orderedValues);
    }
    
    /**
     * Generates a {@code String} based on {@code terms}, that can be used as value 
     * in a column accepting multiple terms, in annotation files 
     * (see {@link #MULTIPLE_ANAT_ENTITY_COL_NAMES} for an example). 
     * The order of the {@code String}s in {@code terms} are preserved, the separator used 
     * is {@link #DEFAULT_ENTITY_SEPARATOR}. 
     * 
     * @param terms     A {@code List} of {@code String}s that are the terms used 
     *                  in a same column.
     * @return          A {@code String} that is the formatting of {@code terms} 
     *                  to be used in a multiple entities column 
     *                  of an annotation file.
     */
    public static String getTermsToColumnValue(List<String> terms) {
        log.entry(terms);
        String colValue = "";
        for (String term: terms) {
            if (!colValue.equals("")) {
                colValue += DEFAULT_ENTITY_SEPARATOR;
            }
            colValue += term.trim();
        }
        return log.exit(colValue);
    }
    
}
