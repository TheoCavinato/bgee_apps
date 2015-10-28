package org.bgee.model.topanat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.BgeeProperties;
import org.bgee.model.QueryTool;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.expressiondata.CallFilter;
import org.bgee.model.expressiondata.CallService;

/**
 * @author Mathieu Seppey
 *
 */
public class TopAnatAnalysis extends QueryTool {

    /**
     * 
     */
    private final static Logger log = LogManager
            .getLogger(TopAnatAnalysis.class.getName());

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
     * {@code ConcurrentMap} used to manage concurrent access to the
     * read/write locks that are used to manage concurrent reading and writing
     * of the files that can be simultaneously accessed by different threads. In
     * this {@code Map}, {@code keys} are file names, and
     * {@code values} are {@link ReentrantReadWriteLock}}.
     */
    private static ConcurrentMap<String, ReentrantReadWriteLock> readWriteLocks =
            new ConcurrentHashMap<String, ReentrantReadWriteLock>();

    /**
     * A {@code String} representing the tsv file which contains the
     * results in TSV format. This file consists of 7 columns: Organ ID, Organ
     * Name, Number of annotated genes, number of significant genes, expected
     * value , P value and FDR.
     */
    private String resultTSVFileName;

    /**
     * A {@code String} representing the pdf file which contains the graph
     * result generated by topAnat. The graph contains information about the most
     * significantly expressed nodes.
     */
    private String resultGraphPDFFileName;

    /**
     * A {@code String} representing the path to the "organ relationships"
     * file. This file stores the is_a and part_of relationships between
     * anatomical structures, from the anatomical ontology being used. These
     * relations are actually always the same for a given ontology, so this file
     * is generated only once for a given ontology, if it does not already
     * exist.
     * 
     * @see #generateOrganRelationshipsFile()
     * @see #beginTopAnatAnalysis(String)
     */
    private String organRelationshipsFileName;

    /**
     * A {@code String} representing the path to "organ names" file. This
     * file stores the relations between IDs and names of anatomical structures
     * of the anatomical ontology being used. These names are actually always
     * the same for a given ontology, so this file is generated only once for a
     * given ontology, if it does not already exist.
     * 
     * @see #generateOrganNamesFile()
     * @see #beginTopAnatAnalysis(String)
     */
    private String organNamesFileName;

    /**
     * A {@code String} representing the path to the "gene to organ"
     * association file. This file stores the associations from background
     * genes, to anatomical structures, based on expression data.
     * 
     * @see #generateGenesToOrgansAssociationFile()
     * @see #beginTopAnatAnalysis(String)
     */
    private String geneToOrganFileName;

    /**
     * 
     */
    private final CallService callService;

    /**
     * 
     */
    private final AnatEntityService anatEntityService;

    @Override
    protected Logger getLogger() {
        return log;
    }
    
    public TopAnatAnalysis(TopAnatParams params, BgeeProperties props, 
            ServiceFactory serviceFactory) {
        this(params, props, serviceFactory, new TopAnatRManager(props));
    }
    /**
     * @param params
     */
    public TopAnatAnalysis(TopAnatParams params, BgeeProperties props, 
            ServiceFactory serviceFactory, TopAnatRManager rManager) {
        log.entry(params, props, serviceFactory, rManager);        
        this.params = params;
        this.anatEntityService = 
                serviceFactory.getAnatEntityService(); 
        this.callService = serviceFactory.getCallService();
        this.rManager = rManager;
        this.props = props;
        log.exit();
    }

    /**
     * @throws IOException
     */
    public TopAnatResults proceedToAnalysis() throws IOException{
        log.entry();

        this.resultTSVFileName = "topAnatResult_" + this.getTaskName()
        + ".tsv";
        log.info("Result File: {}", this.resultTSVFileName);
        File topAnatResult = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.resultTSVFileName);
        this.resultGraphPDFFileName = "topAnatResultPDF_"
                + this.getTaskName() + ".pdf";
        log.info(this.resultGraphPDFFileName);
        File topAnatResultPDF = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.resultGraphPDFFileName);

        //we will write results into tmp files, moved at the end if everything 
        //went fine.
        String tmpResultFileName = topAnatResult.getPath() + ".tmp";

        String tmpPDFFileName = topAnatResultPDF.getPath() + ".tmp";

        // Generate anatomic entities data
        this.generateOrganFiles();

        // Generate call data
        this.geneToOrganFileName = "geneToOrgan"+this.getTaskName()+".tsv";
        this.generateGenesToOrgansAssociationFile();

        // perform R function and write all outputs
        try{
            // perform the R anaysis
//            this.performRCallerFunctions(tmpResultFileName, tmpPDFFileName);
        }
        finally{
            // delete tmp files
            // unlock lock
        }
        log.exit();
        return null;
    }


    /**
     * Generates the Organ ID to Organ Name association file, and organ relationship file, 
     * only if they do not already exist.
     * <p>
     * The method will write into a file named 
     * {@link #organNamesFileName}, the association between the organ IDs of the current
     * species and their name (see {@link #writeOrganNamesToFile(String, String)}), and into 
     * a file named {@link #organRelationshipsFileName}, the relations between the organs 
     * of the species (see {@link #writeOrganRelationsToFile(String, String)}).
     * 
     * @throws IOException
     *             if the files cannot be opened or written to.
     * 
     * @see #organNamesFileName
     * @see #organRelationshipsFileName
     */
    private void generateOrganFiles() throws IOException {
        log.entry();

        log.info("Generating Organ files...");

        this.organNamesFileName = "OrganNames_" + this.params.getSpecies().getId() + ".tsv";
        this.organRelationshipsFileName = "OrganRelationships_" + this.params.getSpecies().getId() 
        + ".tsv";

        File namesFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.organNamesFileName);
        String namesFileName = namesFile.getPath();

        File relsFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.organRelationshipsFileName);
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
                log.info("Organ files already generated.");
                log.exit(); return;
            }

            this.writeOrganNamesToFile(namesTmpFileName);
            this.writeOrganRelationsToFile(relsTmpFileName);

            //move tmp files if successful
            //We check that there were no database error that could have corrupted the results
            //            if (Database.getDatabase().isError()) {
            //                throw log.throwing(new IllegalStateException("A database error occurred, " +
            //                        "analysis canceled"));
            //            }

            Files.move(namesTmpFile, finalNamesFile, StandardCopyOption.REPLACE_EXISTING);
            Files.move(relsTmpFile, finalRelsFile, StandardCopyOption.REPLACE_EXISTING);

        } finally {
            Files.deleteIfExists(namesTmpFile);
            Files.deleteIfExists(relsTmpFile);
            this.releaseWriteLock(namesTmpFileName);
            this.releaseWriteLock(namesFileName);
            this.releaseWriteLock(relsTmpFileName);
            this.releaseWriteLock(relsFileName);
        }

        log.info("organNamesFileName: {} - relationshipsFileName: {}", 
                this.organNamesFileName, this.organRelationshipsFileName);
        log.exit();
    }

    /**
     * Write into the file {@code organNameFile}, the association between names 
     * and IDs of organs, for the current species with the ID {@code speciesId}. It will be 
     * a TSV file with no header, with each organ corresponding to a line, with the ID 
     * in the first column, and the name in the second column.
     * <p>
     * Note that it is not the responsibility of this method to acquire a write lock 
     * on the file, it is the responsibility of the caller.
     * @param organNameFile    A {@code String} that is the path to file where organ names 
     *                         will be written.
     * @throws IOException     If an error occurred while writing in the file.
     */
    private void writeOrganNamesToFile(String organNameFile) throws IOException {
        log.entry();

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(organNameFile)))) {
            this.anatEntityService.getAnatEntities(this.params.getSpecies().getId())
            .forEach(entity 
                    -> out.println(entity.getId() + "\t" + entity.getName().replaceAll("'", "")));
        }

        log.exit();
    }

    /**
     * Write into the file {@code organRelFile}, the direct relations between organs, 
     * for the current species with the ID {@code speciesId}. It will be a TSV file with 
     * no header, with each line corresponding to a relation, with the ID of the descent
     * organ in the first column, and the ID of the parent organ in the second column. 
     * Only part_of and is_a relations should be considered.
     * <p>
     * Note that it is not the responsibility of this method to acquire a write lock 
     * on the file, it is the responsibility of the caller.
     * 
     * @param organRelFile     A {@code String} that is the path to file where organ relations 
     *                         will be written.
     *                         
     * @throws IOException     If an error occurred while writing in the file.
     */
    private void writeOrganRelationsToFile(String organRelFile)
            throws IOException {
        log.entry();

        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(organRelFile)))) {
            this.anatEntityService.getAnatEntitiesRelationships(this.params.getSpecies().getId())
            .forEach(
                    (id,descentIds) -> descentIds.forEach(
                            (descentId) -> out.println(descentId + '\t' + id)));
        }

        log.exit();
    }

    /**
     *
     */
    private void writeToGeneToOrganFile(String geneToOrganFile)
            throws IOException {

        log.entry(geneToOrganFile); 

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(
                geneToOrganFile)))) {
            this.callService.loadCalls(
                    this.params.getSpecies().getId(),new HashSet<CallFilter<?>>(
                    Arrays.asList(this.params.rawParametersToCallFilter()))
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
     * Writes association between genes and the anatomical entities where they are 
     * expressed in a TSV file, named according to the value returned by 
     * {@link #getGeneToOrganFileName()}.
     * 
     * @throws IOException
     *             if the {@code geneToOrganFileName} cannot be opened or
     *             written to.
     * 
     * @see #geneToOrganFileName
     * @see #writeToGeneToOrganFile(String)
     */
    private void generateGenesToOrgansAssociationFile() throws IOException {
        log.entry();
        log.info("Generating Gene to Organ Association file...");

        File geneToOrganAssociationFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.geneToOrganFileName);
        String geneToOrganAssociationFilePath = geneToOrganAssociationFile
                .getPath();

        //we will write results into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = geneToOrganAssociationFilePath + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalGeneToOrganFile = Paths.get(geneToOrganAssociationFilePath);

        try {
            this.acquireWriteLock(geneToOrganAssociationFilePath);
            this.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalGeneToOrganFile)) {
                log.info("Gene to organ association file already generated.");
                log.exit(); return;
            }

            this.writeToGeneToOrganFile(tmpFileName);
            //move tmp file if successful
            //We check that there were no database error that could have corrupted the results
            //            if (Database.getDatabase().isError()) {
            //                throw log.throwing(new IllegalStateException("A database error occurred, " +
            //                        "analysis canceled"));
            //            }
            Files.move(tmpFile, finalGeneToOrganFile, StandardCopyOption.REPLACE_EXISTING);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.releaseWriteLock(geneToOrganAssociationFilePath);
            this.releaseWriteLock(tmpFileName);
        }

        log.info("GeneToOrganAssociationFile: {}", this.geneToOrganFileName);
        log.exit();
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
