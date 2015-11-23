package org.bgee.model.topanat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.BgeeProperties;
import org.bgee.model.ServiceFactory;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.anatdev.AnatEntityService;
import org.bgee.model.expressiondata.CallService;
import org.bgee.model.expressiondata.baseelements.DataQuality;
import org.bgee.model.expressiondata.baseelements.DataType;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneService;
import org.bgee.model.topanat.exception.InvalidForegroundException;
import org.bgee.model.topanat.exception.InvalidSpeciesGenesException;

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
    
    protected final static AnatEntity FAKE_ANAT_ENTITY_ROOT = new AnatEntity("BGEE:0", "root", 
            "A root added on top of all orphan terms.");

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
    private final TopAnatController controller;

    /**
     * @param params
     * @param props
     * @param serviceFactory
     * @param rManager
     */
    public TopAnatAnalysis(TopAnatParams params, BgeeProperties props, 
            ServiceFactory serviceFactory, TopAnatRManager rManager, TopAnatController controller) {
        log.entry(params, props, serviceFactory, rManager); 
        this.params = params;
        this.anatEntityService = 
                serviceFactory.getAnatEntityService(); 
        this.callService = serviceFactory.getCallService();
        this.geneService = serviceFactory.getGeneService();
        this.rManager = rManager;
        this.props = props;
        this.controller = controller;
    }

    /**
     * @throws IOException
     * @throws InvalidForegroundException 
     * @throws InvalidSpeciesException 
     */
    protected TopAnatResults proceedToAnalysis() throws IOException, InvalidForegroundException, 
    InvalidSpeciesGenesException{
        log.entry();
        log.info("Result File: {}", this.getResultFileName());

        // Validate and load the gene in the foreground and background
        this.validateForegroundAndBackground();
        
        //Do the analysis/file creation only if results don't already exist
        if (!this.isAnalysisDone()) {
            //TODO: actually, it would be better if the files were created in a directory 
            //named as the hash, and with standard file names (not including the hash)
            
            // Generate anatomic entities data
            this.generateAnatEntitiesFiles();
            
            // Generate call data
            this.generateGenesToAnatEntitiesAssociationFile();
            
            // Write the params on the disk
            this.generateTopAnatParamsFile();
            
            // Generate R code and write it on the disk
            this.generateRCodeFile();
            
            // Copy the Rscript file to the working directory, if it doesn't already exists
            String sourceFunctionFileName = TopAnatAnalysis.class.getResource(
                    this.props.getTopAnatFunctionFile()).getPath();
            Path source = Paths.get(sourceFunctionFileName);
            File targetFunctionFile = new File(
                    this.props.getTopAnatResultsWritingDirectory() + 
                    source.getFileName());
            Path target = Paths.get(targetFunctionFile.getPath());
            if (!targetFunctionFile.exists()) {
                try{
                    this.controller.acquireReadLock(sourceFunctionFileName);
                    this.controller.acquireWriteLock(targetFunctionFile.getPath());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } finally{
                    this.controller.releaseReadLock(sourceFunctionFileName);
                    this.controller.releaseWriteLock(targetFunctionFile.getPath());
                }
            }
            
            // Run the R analysis
            this.runRcode();
            
            if(this.params.isWithZip()){
                // create the zip file
                this.generateZipFile();
            }
        }

        // return the result
        return log.exit(new TopAnatResults(
                this.params,
                this.getResultFileName(),
                this.getResultPDFFileName(),
                this.getRScriptAnalysisFileName(),
                this.getParamsOutputFileName(),
                this.getAnatEntitiesNamesFileName(),
                this.getAnatEntitiesRelationshipsFileName(),
                this.getGeneToAnatEntitiesFileName(),
                this.getRScriptConsoleFileName(),
                this.getZipFileName(),
                this.controller)
                );
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

            this.controller.acquireReadLock(namesFileName);
            this.controller.acquireReadLock(relsFileName);
            this.controller.acquireReadLock(geneToAnatEntitiesFile);

            this.controller.acquireWriteLock(tmpFileName);
            this.controller.acquireWriteLock(fileName);

            this.controller.acquireWriteLock(tmpPdfFileName);
            this.controller.acquireWriteLock(pdfFileName);

            //check, AFTER having acquired the locks, that the final files do not 
            //already exist (maybe another thread generated the files before this one 
            //acquires the lock)
            if (Files.exists(finalFile)) {
                log.info("Result files already generated.");
                log.exit();return;
            }

            try {
                this.rManager.performRFunction(this.getRScriptConsoleFileName());
            } catch (rcaller.exception.ParseException e) {
                log.catching(e);
                //RCaller throws an exception when there is no result. 
                //Just to be sure this exception was really thrown because there was no result, 
                //we check the exception message. This is highly version specific, but we should add 
                //an IT to make sure this test is always in sync with the actual message thrown by RCaller. 
                if (!e.getMessage().matches("^.*?Can not parse output: The generated file .+? is empty.*$")) {
                    //Not the excepted exception, rethrow
                    throw log.throwing(e);
                }
                //we create an empty result file, so that we don't re-run the analysis for nothing
                Files.createFile(tmpFile);
            }

            this.move(tmpFile, finalFile, false);
            //maybe it was not requested to generate the pdf, or there was no results 
            //and this file was not generated
            if (Files.exists(tmpPdfFile)) {
                this.move(tmpPdfFile, finalPdfFile, false);
            }

        } finally {
            Files.deleteIfExists(tmpFile);
            Files.deleteIfExists(tmpPdfFile);
            this.controller.releaseWriteLock(tmpFileName);
            this.controller.releaseWriteLock(fileName);
            this.controller.releaseWriteLock(tmpPdfFileName);
            this.controller.releaseWriteLock(pdfFileName);
            this.controller.releaseReadLock(namesFileName);
            this.controller.releaseReadLock(relsFileName);
            this.controller.releaseReadLock(geneToAnatEntitiesFile);   
        }

        log.info("Result file name: {}", 
                this.getResultFileName());

        log.exit();
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
            this.controller.acquireWriteLock(tmpFileName);
            this.controller.acquireWriteLock(fileName);

            //if the file already exists, we remove it, because we need anyway to call writeRcodeFile, 
            //to set the R code (bad design)
            if (Files.exists(finalFile)) {
                log.info("R code file already generated, removing it to reload the R code.");
                Files.delete(finalFile);
            }

            this.writeRcodeFile(tmpFileName);

            this.move(tmpFile, finalFile, true);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.controller.releaseWriteLock(tmpFileName);
            this.controller.releaseWriteLock(fileName);
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
            //TODO: not good to manually add the ".tmp" here, maybe the method could take 
            //these two file names as argument
            out.println(this.rManager.generateRCode(
                    this.getResultFileName()+".tmp",
                    this.getResultPDFFileName()+".tmp",
                    this.getAnatEntitiesNamesFileName(),
                    this.getAnatEntitiesRelationshipsFileName(),
                    this.getGeneToAnatEntitiesFileName(),
                    this.params.getSubmittedForegroundIds()));
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
        log.debug("Absolute path to name file: {}", namesTmpFile.toAbsolutePath());
        String relsTmpFileName = relsFileName + ".tmp";
        Path relsTmpFile = Paths.get(relsTmpFileName);
        Path finalRelsFile = Paths.get(relsFileName);

        try {
            this.controller.acquireWriteLock(namesTmpFileName);
            this.controller.acquireWriteLock(namesFileName);
            this.controller.acquireWriteLock(relsTmpFileName);
            this.controller.acquireWriteLock(relsFileName);

            //check, AFTER having acquired the locks, that the final files do not 
            //already exist (maybe another thread generated the files before this one 
            //acquires the lock)
            if (Files.exists(finalNamesFile) && Files.exists(finalRelsFile)) {
                log.info("AnatEntities files already generated.");
                log.exit(); return;
            }

            this.writeAnatEntitiesNamesToFile(namesTmpFileName);
            this.writeAnatEntitiesRelationsToFile(relsTmpFileName);

            this.move(namesTmpFile, finalNamesFile, true);
            this.move(relsTmpFile, finalRelsFile, true);

        } finally {
            Files.deleteIfExists(namesTmpFile);
            Files.deleteIfExists(relsTmpFile);
            this.controller.releaseWriteLock(namesTmpFileName);
            this.controller.releaseWriteLock(namesFileName);
            this.controller.releaseWriteLock(relsTmpFileName);
            this.controller.releaseWriteLock(relsFileName);
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
            this.anatEntityService.loadAnatEntitiesBySpeciesIds(Arrays.asList(this.params.getSpeciesId()))
            .forEach(entity 
                    -> out.println(entity.getId() + "\t" + entity.getName().replaceAll("'", "")));
            //We add a fake root, TopAnat doesn't manage multiple root
            out.println(FAKE_ANAT_ENTITY_ROOT.getId() + "\t" + FAKE_ANAT_ENTITY_ROOT.getName());
        }

        log.exit();
    }

    /**
     */
    private void writeAnatEntitiesRelationsToFile(String AnatEntitiesRelFile)
            throws IOException {
        log.entry(AnatEntitiesRelFile);

        Map<String, Set<String>> relations = this.anatEntityService.loadDirectIsAPartOfRelationships(
                Arrays.asList(this.params.getSpeciesId()));
        
        //We add a fake root, and we map all orphan terms to it: TopAnat don't manage multiple roots. 
        //Search for parent terms never seen as child of another term.
        Set<String> allChildIds = relations.values().stream()
                .flatMap(Set::stream).collect(Collectors.toSet());
        Set<String> roots = relations.keySet().stream()
                .filter(termId -> !allChildIds.contains(termId))
                .collect(Collectors.toSet());
        log.trace("Roots identified in the graph: " + roots);
        assert roots.size() > 0;
        if (roots.size() > 1) {
            relations.put(FAKE_ANAT_ENTITY_ROOT.getId(), roots);
        }
        
        try (PrintWriter out = new PrintWriter(new BufferedWriter(
                new FileWriter(AnatEntitiesRelFile)))) {
            relations.forEach(
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
                    this.params.getSpeciesId(), 
                    Arrays.asList(this.params.convertRawParametersToCallFilter()), 
                    EnumSet.of(CallService.Attribute.GENE_ID, CallService.Attribute.ANAT_ENTITY_ID), 
                    null
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
            this.controller.acquireWriteLock(geneToAnatEntitiesAssociationFilePath);
            this.controller.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalGeneToAnatEntitiesFile)) {
                log.info("Gene to AnatEntities association file already generated.");
                log.exit(); return;
            }

            this.writeToGeneToAnatEntitiesFile(tmpFileName);
            //move tmp file if successful
            this.move(tmpFile, finalGeneToAnatEntitiesFile, false);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.controller.releaseWriteLock(geneToAnatEntitiesAssociationFilePath);
            this.controller.releaseWriteLock(tmpFileName);
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
            this.controller.acquireWriteLock(topAnatParamsFilePath);
            this.controller.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalTopAnatParamsFile)) {
                log.info("TopAnatParams file already generated.");
                log.exit(); return;
            }

            this.writeToTopAnatParamsFile(tmpFileName);

            this.move(tmpFile, finalTopAnatParamsFile, true);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.controller.releaseWriteLock(topAnatParamsFilePath);
            this.controller.releaseWriteLock(tmpFileName);
        }

        log.info("TopAnatParamsFile: {}", this.getParamsOutputFileName());
        log.exit();
    }  

    private void generateZipFile() throws IOException{
        log.entry();
        log.info("Generating Zip file...");

        File zipFile = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getZipFileName());
        String zipFilePath = zipFile
                .getPath();

        //we will write into a tmp file, moved at the end if everything 
        //went fine.
        String tmpFileName = zipFilePath + ".tmp";
        Path tmpFile = Paths.get(tmpFileName);
        Path finalZipFile = Paths.get(zipFilePath);

        try {
            this.controller.acquireWriteLock(zipFilePath);
            this.controller.acquireWriteLock(tmpFileName);

            //check, AFTER having acquired the locks, that the final file does not 
            //already exist (maybe another thread generated the files before this one 
            //acquired the lock)
            if (Files.exists(finalZipFile)) {
                log.info("Zip file already generated.");
                log.exit(); return;
            }

            this.writeZipFile(tmpFileName);

            this.move(tmpFile, finalZipFile, true);

        } finally {
            Files.deleteIfExists(tmpFile);
            this.controller.releaseWriteLock(zipFilePath);
            this.controller.releaseWriteLock(tmpFileName);
        }

        log.info("TopAnatParamsFile: {}", this.getParamsOutputFileName());
        log.exit();
    }

    /**
     * 99% from here: 
     * http://examples.javacodegeeks.com/
     * core-java/util/zip/create-zip-file-from-multiple-files-with-zipoutputstream/
     * @throws IOException 
     */
    private void writeZipFile(String path) throws IOException {

        String zipFile = path;

        String[] srcFiles = { 
                this.props.getTopAnatResultsWritingDirectory() + this.getResultFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getResultPDFFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getRScriptConsoleFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getAnatEntitiesNamesFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getGeneToAnatEntitiesFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getParamsOutputFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getAnatEntitiesRelationshipsFileName(),
                this.props.getTopAnatResultsWritingDirectory() + this.getRScriptAnalysisFileName(),
                this.props.getTopAnatResultsWritingDirectory() + Paths.get(TopAnatAnalysis.class.getResource(
                        this.props.getTopAnatFunctionFile()).getPath()).getFileName().toString()
        };

        // create byte buffer
        byte[] buffer = new byte[1024];

        FileOutputStream fos = new FileOutputStream(zipFile);

        ZipOutputStream zos = new ZipOutputStream(fos);

        for (int i=0; i < srcFiles.length; i++) {

            File srcFile = new File(srcFiles[i]);
            srcFile.createNewFile();

            FileInputStream fis = new FileInputStream(srcFile);

            // begin writing a new ZIP entry, positions the stream to the start of the entry data
            zos.putNextEntry(new ZipEntry(srcFile.getName()));

            int length;

            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();

            // close the InputStream
            fis.close();

        }

        // close the ZipOutputStream
        zos.close();

    }


    /**
     * 
     */
    protected String getResultFileName(){
        return TopAnatAnalysis.FILE_PREFIX + this.params.getKey() + ".tsv";
    }

    /**
     * 
     */
    protected String getRScriptConsoleFileName(){
        return TopAnatAnalysis.FILE_PREFIX + this.params.getKey() + ".R_console";
    }

    /**
     * 
     */
    protected String getResultPDFFileName(){
        return TopAnatAnalysis.FILE_PREFIX + "PDF_" + this.params.getKey()  + ".pdf";
    }

    /**
     *
     */
    //TODO: unit test this logic, of different file names depending on parameters
    protected String getGeneToAnatEntitiesFileName(){
        
        //for the background file, if there is no custom background requested, 
        //we take into account only some info
        if (this.params.getSubmittedBackgroundIds() == null || 
                this.params.getSubmittedBackgroundIds().isEmpty()) {
            
            //TODO: use some kind of encoding of the Strings for file name (see replacement for stage ID)
            final StringBuilder sb = new StringBuilder();
            sb.append(this.params.getSpeciesId());
            sb.append("_").append(this.params.getCallType().toString());
            Optional.ofNullable(this.params.getDevStageId())
                //replace column in IDs
                .ifPresent(e -> sb.append("_").append(e.replace(":", "_")));
            //use EnumSet for consistent ordering
            Optional.ofNullable(this.params.getDataTypes()).map(e -> EnumSet.copyOf(e))
            .orElse(EnumSet.allOf(DataType.class))
            .stream()
            .forEach(e -> sb.append("_").append(e.toString()));
            sb.append("_").append(Optional.ofNullable(this.params.getDataQuality()).orElse(DataQuality.LOW)
                    .toString());
            
            return log.exit(TopAnatAnalysis.FILE_PREFIX 
                    + "GeneToAnatEntities_" + sb.toString()  + ".tsv");
        }
        //custom background provided, use the hash
        return log.exit(TopAnatAnalysis.FILE_PREFIX 
                + "GeneToAnatEntities_" + this.params.getKey()  + ".tsv");
    }

    /**
     * @return
     */
    protected String getAnatEntitiesNamesFileName(){
        return TopAnatAnalysis.FILE_PREFIX + "AnatEntitiesNames_" + this.params.getSpeciesId() 
        + ".tsv";
    }

    /**
     * 
     */
    protected String getAnatEntitiesRelationshipsFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "AnatEntitiesRelationships_" + this.params.getSpeciesId() + ".tsv";
    }

    /**
     * 
     */
    protected String getRScriptAnalysisFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "RScript_" + this.params.getKey()  + ".R";
    }

    /**
     * 
     */
    protected String getParamsOutputFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + "Params_" + this.params.getKey() + ".txt";
    }

    /**
     * 
     */
    protected String getZipFileName(){
        return TopAnatAnalysis.FILE_PREFIX 
                + this.params.getKey() + ".zip";
    }

    /**
     * 
     * @return
     */
    protected boolean isAnalysisDone(){
        log.entry();
        File file = new File(
                this.props.getTopAnatResultsWritingDirectory(),
                this.getResultFileName());
        //if it was requested to generate a zip, then we check for existence of the zip, 
        //that is generated last
        if (this.params.isWithZip()) {
            log.trace("Using zip file for checking for presence of results.");
            file = new File(
                    this.props.getTopAnatResultsWritingDirectory(),
                    this.getZipFileName());
        }
        //At this point, if the analysis is being run by another thread, we don't want 
        //to wait for the lock on the file: results are not generated, period.
        //TODO: here, we should have a way to ensure that the String used for acquiring the lock
        //is the same in the methods generating these files, like, having a method 
        //creating the File and returning file.getPath().
        if (this.controller.getReadWriteLock(file.getPath()).isWriteLocked()) {
            return log.exit(false);
        }
        //no need to acquire read lock to test for file existence, as it has been written already.
        if (file.exists()) {
            return log.exit(true);
        }
        return log.exit(false);
    }

    /**
     * 
     * @param src
     * @param dest
     * @throws IOException 
     */
    private void move(Path src, Path dest, boolean checkSize) throws IOException{
        if(!checkSize || Files.size(src) > 0) 
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
        else {
            throw log.throwing(new IllegalStateException("Empty tmp file"));
        }
    }

}