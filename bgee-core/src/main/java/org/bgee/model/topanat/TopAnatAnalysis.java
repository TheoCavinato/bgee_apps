package org.bgee.model.topanat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.BgeeProperties;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.expressiondata.CallFilter;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneService;
import org.bgee.model.species.SpeciesService;
import org.bgee.model.topanat.exception.InvalidForegroundException;
import org.bgee.model.topanat.exception.InvalidSpeciesGenesException;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDouble;
import org.supercsv.cellprocessor.constraint.DMinMax;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapReader;
import org.supercsv.io.ICsvMapReader;
import org.supercsv.prefs.CsvPreference;

/**
 * @author Mathieu Seppey
 * @author Frederic Bastian
 */
public class TopAnatAnalysis {

    /**
     * 
     */
    private final static Logger log = LogManager
            .getLogger(TopAnatAnalysis.class.getName());

    /**
     * 
     */
    private final static String FILE_PREFIX = "topAnat_";

    /**
     * 
     */
    private final TopAnatParams params;

    /**
     * 
     */
    private final TopAnatRManager rManager;

    /**
     * 
     */
    private final BgeeProperties props;


    /**
     * 
     */
    private final static ConcurrentMap<String, ReentrantReadWriteLock> readWriteLocks =
            new ConcurrentHashMap<String, ReentrantReadWriteLock>();

    /**
     * 
     */
    private final CallService callService;

    /**
     * 
     */
    private final AnatEntityService anatEntityService;

    /**
     * 
     */
    private final GeneService geneService;

    /**
     * 
     */
    private final SpeciesService speciesService;

    /**
     * @param params
     * @param props
     * @param serviceFactory
     */
    public TopAnatAnalysis(TopAnatParams params, BgeeProperties props, 
            ServiceFactory serviceFactory) {
        this(params, props, serviceFactory, new TopAnatRManager(props,params));
    }

    /**
     * @param params
     * @param props
     * @param serviceFactory
     * @param rManager
     */
    public TopAnatAnalysis(TopAnatParams params, BgeeProperties props, 
            ServiceFactory serviceFactory, TopAnatRManager rManager) {
        log.entry(params, props, serviceFactory, rManager); 
        this.params = params;
        this.anatEntityService = 
                serviceFactory.getAnatEntityService(); 
        this.callService = serviceFactory.getCallService();
        this.geneService = serviceFactory.getGeneService();
        this.speciesService = serviceFactory.getSpeciesService();
        this.rManager = rManager;
        this.props = props;
    }

    /**
     * @throws IOException
     * @throws InvalidForegroundException 
     * @throws InvalidSpeciesException 
     */
    public TopAnatResults proceedToAnalysis() throws IOException, InvalidForegroundException, 
    InvalidSpeciesGenesException{
        log.entry();
        log.info("Result File: {}", this.getResultFileName());

        // Validate and load the gene in the foreground and background
        this.validateForegroundAndBackground();

        // Generate anatomic entities data
        this.generateAnatEntitiesFiles();

        // Generate call data
        this.generateGenesToAnatEntitiesAssociationFile();

        // Write the params on the disk
        this.generateTopAnatParamsFile();

        // Generate R code and write it on the disk
        this.generateRCodeFile();

        // Run the R analysis and return the result
        this.runRcode();

        List<TopAnatResults.TopAnatResultRow> resultRows = this.getResultRows();
        if (resultRows != null){
            return log.exit(new TopAnatResults(
                    resultRows,this.params,
                    this.getResultFileName(),
                    this.getResultPDFFileName(),
                    this.getRScriptAnalysisFileName(),
                    this.getParamsOutputFileName(),
                    this.getAnatEntitiesNamesFileName(),
                    this.getAnatEntitiesRelationshipsFileName(),
                    this.getGeneToAnatEntitiesFileName(),
                    this.getRScriptConsoleFileName()));
        }
        return null;
    }

    /***
     * @throws InvalidForegroundException
     * @throws InvalidSpeciesException 
     */
    private void validateForegroundAndBackground() throws InvalidForegroundException, 
    InvalidSpeciesGenesException{
        // First check whether the foreground match the background
        if(this.params.getSubmittedBackgroundIds() != null && 
                !this.params.getSubmittedBackgroundIds().isEmpty() && 
                !this.params.getSubmittedBackgroundIds().containsAll(
                        this.params.getSubmittedForegroundIds())){

            Set<String> notIncludedIds = new HashSet<String>(this.params.getSubmittedForegroundIds());
            notIncludedIds.removeAll(this.params.getSubmittedBackgroundIds());
            throw new InvalidForegroundException("All foreground Ids are not included "
                    + "in the background",notIncludedIds);
        }

        Set<String> allGeneIds = new HashSet<String>(this.params.getSubmittedForegroundIds());
        if (this.params.getSubmittedBackgroundIds() != null) {
            allGeneIds.addAll(this.params.getSubmittedBackgroundIds());
        }
        allGeneIds.removeAll(this.geneService.loadGenesByIdsAndSpeciesIds(allGeneIds, 
                Arrays.asList(this.params.getSpeciesId())).stream()
                .map(Gene::getId)
                .collect(Collectors.toSet()));
        if (!allGeneIds.isEmpty()) {
            throw log.throwing(new InvalidSpeciesGenesException("Some gene IDs are unrecognized, "
                    + "or does not belong to the selected species",allGeneIds));
        }
    }

    /**
     * 
     * @throws IOException
     */
    private void runRcode() throws IOException {
        log.entry();

        log.info("Run R code...");

        File file = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getResultFileName());
        String fileName = file.getPath();

        File pdfFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getResultPDFFileName());
        String pdfFileName = pdfFile.getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = fileName + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalFile = Paths.get(fileName);

        String tmpPdfFileName = pdfFileName + ".tmp";
        Path tmpPdfFile = Paths.get(tmpPdfFileName);
        Path finalPdfFile = Paths.get(pdfFileName);

        String namesFileName = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getAnatEntitiesNamesFileName()).getPath();
        String relsFileName = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getAnatEntitiesRelationshipsFileName()).getPath();
        String geneToAnatEntitiesFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getGeneToAnatEntitiesFileName()).getPath();

        try {

            this.acquireReadLock(namesFileName);
            this.acquireReadLock(relsFileName);
            this.acquireReadLock(geneToAnatEntitiesFile);

            this.acquireWriteLock(tmpFileName);
            this.acquireWriteLock(fileName);

            this.acquireWriteLock(tmpPdfFileName);
            this.acquireWriteLock(pdfFileName);

            //check, AFTER having acquired the locks, that the final files do not 
            //already exist (maybe another thread generated the files before this one 
            //acquires the lock)
            if (Files.exists(finalFile)) {
                log.info("R result file already generated.");
                log.exit();return;
            }

            this.rManager.performRFunction();

            this.move(tmpFile, finalFile);
            this.move(tmpPdfFile, finalPdfFile);

        } finally {
            Files.deleteIfExists(tmpFile);
            Files.deleteIfExists(tmpPdfFile);
            this.releaseWriteLock(tmpFileName);
            this.releaseWriteLock(fileName);
            this.releaseWriteLock(tmpPdfFileName);
            this.releaseWriteLock(pdfFileName);
            this.releaseReadLock(namesFileName);
            this.releaseReadLock(relsFileName);
            this.releaseReadLock(geneToAnatEntitiesFile);   
        }

        log.info("Result file name: {}", 
                this.getResultFileName());

        log.exit();
    }

    /**
     * 
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private List<TopAnatResults.TopAnatResultRow> getResultRows() throws FileNotFoundException,
    IOException{

        File resultFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getResultFileName());

        this.acquireReadLock(resultFile.getPath());

        List<TopAnatResults.TopAnatResultRow> listToReturn 
        = new ArrayList<TopAnatResults.TopAnatResultRow>();

        try (ICsvMapReader mapReader = 
                new CsvMapReader(new FileReader(resultFile), 
                        CsvPreference.TAB_PREFERENCE)) {
            String[] header = mapReader.getHeader(true);
            CellProcessor[] processors = new CellProcessor[] { 
                    new NotNull(), // AnatEntity Id
                    new Optional(), // AnatEntity Name
                    new NotNull(new ParseDouble()), // Annotated
                    new NotNull(new ParseDouble()), // Significant
                    new NotNull(new ParseDouble()), // Expected
                    new NotNull(new ParseDouble()), // fold enrich
                    new NotNull(new DMinMax(0d,1d)), // p
                    new NotNull(new DMinMax(0d,1d)) // fdr
            };
            Map<String, Object> row;
            if(header != null){
                while( (row = mapReader.read(header, processors)) != null ) {
                    listToReturn.add(new TopAnatResults.TopAnatResultRow(row));
                }
            }
        }

        this.releaseReadLock(resultFile.getPath());

        return listToReturn;
    }

    /**
     * 
     * @throws IOException
     */
    private void generateRCodeFile() throws IOException {
        log.entry();

        log.info("Generating R code file...");

        File file = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getRScriptAnalysisFileName());
        String fileName = file.getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = fileName + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalFile = Paths.get(fileName);

        try {
            this.acquireWriteLock(tmpFileName);
            this.acquireWriteLock(fileName);

            //check, AFTER having acquired the locks, that the final files do not 
            //already exist (maybe another thread generated the files before this one 
            //acquires the lock)
            if (Files.exists(finalFile)) {
                log.info("R code file already generated.");
                log.exit(); return;
            }

            this.writeRcodeFile(tmpFileName);

            this.move(tmpFile, finalFile);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.releaseWriteLock(tmpFileName);
            this.releaseWriteLock(fileName);
        }

        log.info("Rcode file name: {}", 
                this.getRScriptAnalysisFileName());
        log.exit();
    }


    /**
     */
    private void writeRcodeFile(String RcodeFile) throws IOException {
        log.entry(RcodeFile);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(RcodeFile)))) {
            out.println(this.rManager.generateRCode(
                    this.getResultFileName()+".tmp",
                    this.getResultPDFFileName()+".tmp",
                    this.getAnatEntitiesNamesFileName(),
                    this.getAnatEntitiesRelationshipsFileName(),
                    this.getGeneToAnatEntitiesFileName(),
                    this.params.getSubmittedBackgroundIds()));
        }

        log.exit();
    }

    /**
     * 
     * @throws IOException
     */
    private void generateAnatEntitiesFiles() throws IOException {
        log.entry();

        log.info("Generating AnatEntities files...");

        File namesFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getAnatEntitiesNamesFileName());
        String namesFileName = namesFile.getPath();

        File relsFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getAnatEntitiesRelationshipsFileName());
        String relsFileName = relsFile.getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String namesTmpFileName = namesFileName + ".tmp";
        Path namesTmpFile = Paths.get(namesTmpFileName);
        Path finalNamesFile = Paths.get(namesFileName);
        String relsTmpFileName = relsFileName + ".tmp";
        Path relsTmpFile = Paths.get(relsTmpFileName);
        Path finalRelsFile = Paths.get(relsFileName);

        try {
            this.acquireWriteLock(namesTmpFileName);
            this.acquireWriteLock(namesFileName);
            this.acquireWriteLock(relsTmpFileName);
            this.acquireWriteLock(relsFileName);

            //check, AFTER having acquired the locks, that the final files do not 
            //already exist (maybe another thread generated the files before this one 
            //acquires the lock)
            if (Files.exists(finalNamesFile) && Files.exists(finalRelsFile)) {
                log.info("AnatEntities files already generated.");
                log.exit(); return;
            }

            this.writeAnatEntitiesNamesToFile(namesTmpFileName);
            this.writeAnatEntitiesRelationsToFile(relsTmpFileName);

            this.move(namesTmpFile, finalNamesFile);
            this.move(relsTmpFile, finalRelsFile);

        } finally {
            Files.deleteIfExists(namesTmpFile);
            Files.deleteIfExists(relsTmpFile);
            this.releaseWriteLock(namesTmpFileName);
            this.releaseWriteLock(namesFileName);
            this.releaseWriteLock(relsTmpFileName);
            this.releaseWriteLock(relsFileName);
        }

        log.info("AnatEntitiesNamesFileName: {} - relationshipsFileName: {}", 
                this.getAnatEntitiesNamesFileName(), this.getAnatEntitiesRelationshipsFileName());
        log.exit();
    }


    /**
     * 
     * @param AnatEntitiesNameFile
     * @throws IOException
     */
    private void writeAnatEntitiesNamesToFile(String AnatEntitiesNameFile) throws IOException {
        log.entry(AnatEntitiesNameFile);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(AnatEntitiesNameFile)))) {
            this.anatEntityService.getAnatEntities(this.params.getSpeciesId())
            .forEach(entity 
                    -> out.println(entity.getId() + "\t" + entity.getName().replaceAll("'", "")));
        }

        log.exit();
    }

    /**
     */
    private void writeAnatEntitiesRelationsToFile(String AnatEntitiesRelFile)
            throws IOException {
        log.entry(AnatEntitiesRelFile);

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(AnatEntitiesRelFile)))) {
            this.anatEntityService.getAnatEntitiesRelationships(this.params.getSpeciesId())
            .forEach(
                    (id,descentIds) -> descentIds.forEach(
                            (descentId) -> out.println(descentId + '\t' + id)));
        }

        log.exit();
    }

    /**
     *
     */
    private void writeToGeneToAnatEntitiesFile(String geneToAnatEntitiesFile)
            throws IOException {

        log.entry(geneToAnatEntitiesFile); 

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
                geneToAnatEntitiesFile)))) {
            this.callService.loadCalls(
                    this.params.getSpeciesId(),new HashSet<CallFilter<?>>(
                            Arrays.asList(this.params.convertRawParametersToCallFilter()))
                    ).forEach(
                            call -> out.println(
                                    call.getGeneId() + '\t' + 
                                    call.getCondition().getAnatEntityId()
                                    )
                            );
        }
        log.exit();
    }    

    /**
     */
    private void generateGenesToAnatEntitiesAssociationFile() throws IOException {
        log.entry();
        log.info("Generating Gene to AnatEntities Association file...");

        File geneToAnatEntitiesAssociationFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getGeneToAnatEntitiesFileName());
        String geneToAnatEntitiesAssociationFilePath = geneToAnatEntitiesAssociationFile
                .getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = geneToAnatEntitiesAssociationFilePath + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalGeneToAnatEntitiesFile = Paths.get(geneToAnatEntitiesAssociationFilePath);

        try {
            this.acquireWriteLock(geneToAnatEntitiesAssociationFilePath);
            this.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalGeneToAnatEntitiesFile)) {
                log.info("Gene to AnatEntities association file already generated.");
                log.exit(); return;
            }

            this.writeToGeneToAnatEntitiesFile(tmpFileName);
            //move tmp file if successful
            this.move(tmpFile, finalGeneToAnatEntitiesFile);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.releaseWriteLock(geneToAnatEntitiesAssociationFilePath);
            this.releaseWriteLock(tmpFileName);
        }

        log.info("GeneToAnatEntitiesAssociationFile: {}", this.getGeneToAnatEntitiesFileName());
        log.exit();
    }    

    /**
     * 
     * @param tmpFileName
     * @throws IOException
     */
    private void writeToTopAnatParamsFile(String topAnatParamsFileName) throws IOException {
        log.entry();

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(topAnatParamsFileName)))) {
            out.println("# Warning, this file contains the initial values of the parameters for your information only.");
            out.println("# Changing a value in this file won't affect the R script.");
            out.println(this.params.toString(true));
        }

        log.exit();
    }

    /**
     * 
     * @throws IOException
     */
    private void generateTopAnatParamsFile() throws IOException {
        log.entry();
        log.info("Generating TopAnatParams file...");

        File topAnatParamsFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getParamsOutputFileName());
        String topAnatParamsFilePath = topAnatParamsFile
                .getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = topAnatParamsFilePath + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalTopAnatParamsFile = Paths.get(topAnatParamsFilePath);

        try {
            this.acquireWriteLock(topAnatParamsFilePath);
            this.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalTopAnatParamsFile)) {
                log.info("TopAnatParams file already generated.");
                log.exit(); return;
            }

            this.writeToTopAnatParamsFile(tmpFileName);

            this.move(tmpFile, finalTopAnatParamsFile);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.releaseWriteLock(topAnatParamsFilePath);
            this.releaseWriteLock(tmpFileName);
        }

        log.info("TopAnatParamsFile: {}", this.getParamsOutputFileName());
        log.exit();
    }  

    /**
     * 
     */
    public String getResultFileName(){
        return TopAnatAnalysis.FILE_PREFIX + this.params.getKey() + ".tsv";
    }

    /**
     * 
     */
    public String getRScriptConsoleFileName(){
        return TopAnatAnalysis.FILE_PREFIX + this.params.getKey() + ".tsv.tmp.R_console";
    }

    /**
     * 
     */
    public String getResultPDFFileName(){
        return TopAnatAnalysis.FILE_PREFIX + "PDF_" + this.params.getKey()  + ".pdf";
    }

    /**
     *
     */
    public String getGeneToAnatEntitiesFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "GeneToAnatEntities_" + this.params.getKey()  + ".tsv";
    }

    /**
     * @return
     */
    public String getAnatEntitiesNamesFileName(){
        return TopAnatAnalysis.FILE_PREFIX + "AnatEntitiesNames_" + this.params.getSpeciesId() 
        + ".tsv";
    }

    /**
     * 
     */
    public String getAnatEntitiesRelationshipsFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "AnatEntitiesRelationships_" + this.params.getSpeciesId() + ".tsv";
    }

    /**
     * 
     */
    public String getRScriptAnalysisFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "RScript_" + this.params.getKey()  + ".R";
    }

    /**
     * 
     */
    public String getParamsOutputFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "Params_" + this.params.getKey() + ".txt";
    }

    /**
     * 
     * @param src
     * @param dest
     * @throws IOException 
     */
    private void move(Path src, Path dest) throws IOException{
        if(Files.size(src) > 0)
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        else{
            throw log.throwing(new IllegalStateException("Empty tmp file"));
        }
    }


    // *************************************************
    // FILE LOCKING
    // *************************************************
    /**
     * Acquires a write lock corresponding to the {@code fileName} by
     * calling the {@link #acquireLock(String, boolean)} method
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to acquire
     *            the read lock.
     * @see #acquireLock(String, boolean)
     */
    private void acquireReadLock(String fileName) {
        this.acquireLock(fileName, true);
    }

    /**
     * Acquires a write lock corresponding to the {@code fileName} by
     * calling the {@link #acquireLock(String, boolean)} method
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to acquire
     *            the write lock.
     * @see #acquireLock(String, boolean)
     */
    private void acquireWriteLock(String fileName) {
        this.acquireLock(fileName, false);
    }

    /**
     * Releases the write lock corresponding to the {@code fileName} by
     * calling the {@link #releaseLock(String, boolean)} method
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to release
     *            the write lock
     * @see #releaseLock(String, boolean)
     */
    private void releaseWriteLock(String fileName) {
        this.releaseLock(fileName, false);
    }

    /**
     * Method to acquire a lock on a file, corresponding to the param
     * {@code fileName}
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to
     *            retrieve the lock from {@code readWriteLocks}
     * @param readLock
     *            {@code true} if a read lock should be acquired.
     *            {@code false} if it should be a read lock
     * @see #readWriteLocks
     */
    private void acquireLock(String fileName, boolean readLock) {
        ReentrantReadWriteLock lock = this.getReadWriteLock(fileName);

        if (readLock) {
            lock.readLock().lock();
        } else {
            lock.writeLock().lock();
        }
        // {@code removeLockIfPossible(String)} determines whether the lock
        // could be removed
        // from the {@code ConcurrentHashMap} {@code readWriteLocks}.
        // The problem is that {@code removeLockIfPossible(String)} could
        // remove the lock from the map,
        // AFTER this method acquire a lock and put it in the map
        // (this.getReadWriteLock(this.getGeneratedKey())),
        // but BEFORE actually locking it (lock.readLock().lock()).
        // To solve this issue, this method will test after locking the lock
        // whether it is still in the map,
        // or whether the element present in the map is equal to the "locked"
        // lock.
        // If it is not, it will call again
        // {@code getReadWriteLock(String)}
        // to generate a new lock to be put in the map, or to obtain the lock
        // generated by another thread.
        while (readWriteLocks.get(fileName) == null
                || !readWriteLocks.get(fileName).equals(lock)) {

            lock = this.getReadWriteLock(fileName);
            if (readLock) {
                lock.readLock().lock();
            } else {
                lock.writeLock().lock();
            }
        }
    }

    /**
     * Releases the read lock corresponding to the {@code fileName} by
     * calling the {@link #releaseLock(String, boolean)} method
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to release
     *            the read lock
     * @see #releaseLock(String, boolean)
     */
    private void releaseReadLock(String fileName) {
        this.releaseLock(fileName, true);
    }

    /**
     * Method to release a lock on a file, corresponding to the param
     * {@code fileName}
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to release
     *            the lock from {@code readWriteLocks}
     * @param readLock
     *            {@code true} if a read lock should be acquired.
     *            {@code false} if it should be a read lock
     * @see #readWriteLocks
     */
    private void releaseLock(String fileName, boolean readLock) {
        ReentrantReadWriteLock lock = this.getReadWriteLock(fileName);
        if (readLock) {
            lock.readLock().unlock();
        } else {
            lock.writeLock().unlock();
        }
        this.removeLockIfPossible(fileName);
    }

    /**
     * Try to remove the {@code ReentrantReadWriteLock} corresponding to
     * the param {@code fileName}, from the {@code ConcurrentHashMap}
     * {@code readWriteLocks}. The lock will be removed from the map only
     * if there are no read or write locks, and no ongoing request for a read or
     * write lock.
     * <p>
     * Note: there might be here a race, where another thread acquired the lock
     * and actually locked it, i) just after this method tests the presence of
     * read or write locks and ongoing requests for a read or write lock, and
     * ii) just before removing it from the map. To solve this issue, methods
     * acquiring a lock must check after locking it whether it is still in the
     * readWriteLocks map, or whether the element present in the map for the key
     * is equal to the acquired lock. If it is not, they must generate a new
     * lock to be used.
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to
     *            retrieve the lock from {@code readWriteLocks}, to remove
     *            it.
     * @see #readWriteLocks
     */
    private void removeLockIfPossible(String fileName) {
        // check if there is already a lock stored for this key
        ReentrantReadWriteLock lock = readWriteLocks.get(fileName);

        // there is a lock to remove
        if (lock != null) {
            // there is no thread with write lock, or read lock, or waiting to
            // acquire a lock
            if (!lock.isWriteLocked() && lock.getReadLockCount() == 0
                    && !lock.hasQueuedThreads()) {
                // there might be here a race, where another thread acquired the
                // lock and
                // actually locked it, just after the precedent condition test,
                // and just before the following remove statement.
                // to solve this issue, methods acquiring a lock must check
                // after locking it
                // whether it is still in the readWriteLocks map.
                // if it is not, they must generate a new lock to be used.
                readWriteLocks.remove(fileName);
            }
        }
    }

    /**
     * Obtain a {@code ReentrantReadWriteLock}, for the param
     * {@code fileName}.
     * 
     * This method tries to obtain {@code ReentrantReadWriteLock}
     * corresponding to the fileName, from the {@code ConcurrentHashMap}
     * {@code readWriteLocks}. If the lock is not already stored, create a
     * new one, and put it in {@code readWriteLocks}, to be used by other
     * threads.
     * 
     * @param fileName
     *            a {@code String} corresponding to the fileName to
     *            retrieve the lock from {@code readWriteLocks}.
     * 
     * @return a {@code ReentrantReadWriteLock} corresponding to the
     *         fileName.
     * 
     * @see #readWriteLocks
     */
    private ReentrantReadWriteLock getReadWriteLock(String fileName) {
        // check if there is already a lock stored for this key
        ReentrantReadWriteLock readWritelock = readWriteLocks.get(fileName);

        // no lock already stored
        if (readWritelock == null) {
            ReentrantReadWriteLock newReadWriteLock = new ReentrantReadWriteLock(
                    true);
            // try to put the new lock in the ConcurrentHashMap
            readWritelock = readWriteLocks.putIfAbsent(fileName,
                    newReadWriteLock);
            // if readWritelock is null, the newLock has been successfully put
            // in the map, and we use it.
            // otherwise, it means that another thread has inserted a new lock
            // for this key in the mean time.
            // readWritelock then corresponds to this value, that we should use.
            if (readWritelock == null) {
                readWritelock = newReadWriteLock;
            }
        }
        return readWritelock;
    }

}
