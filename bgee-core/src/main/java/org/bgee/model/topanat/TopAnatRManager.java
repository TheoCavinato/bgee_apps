package org.bgee.model.topanat;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.BgeeProperties;

import rcaller.RCaller;
import rcaller.RCode;

public class TopAnatRManager {
    
    private final RCaller caller;
    
    private final RCode code;
    
    private BgeeProperties props;

    /**
     * 
     */
    private final static Logger log = LogManager
            .getLogger(TopAnatRManager.class.getName());
    
    public TopAnatRManager(RCaller caller, RCode code, BgeeProperties props){
     
        this.caller = caller;
        this.code = code;
        this.props = props;
        
    }

    private void performRCallerFunctions(String resultFileName, String resultPDFFileName) 
            throws IOException {
        log.entry();
        RCaller caller = new RCaller();
        RCode code = new RCode();
//        caller.setRscriptExecutable(this.props.getTopAnatRScriptExecutable());
//        if (log.isDebugEnabled()) {
//            caller.redirectROutputToFile(resultFileName + ".R_console", true);
//        }
//        code.clear();
//        code.addRCode("packageExistRgraphviz<-require(Rgraphviz)");
//        code.addRCode("if(!packageExistRgraphviz){");
//        code.addRCode("source('http://bioconductor.org/biocLite.R')");
//        code.addRCode("biocLite('Rgraphviz')}");
//
//        code.addRCode("packageExistRUniversal<-require(Runiversal)");
//        code.addRCode("if(!packageExistRUniversal){");
//        code.addRCode("source('http://bioconductor.org/biocLite.R')");
//        code.addRCode("biocLite('Runiversal')}");
//
//        code.addRCode("packageExistTopGO<-require(topGO)");
//        code.addRCode("if(!packageExistTopGO){");
//        code.addRCode("source('http://bioconductor.org/biocLite.R')");
//        code.addRCode("biocLite('topGO')}");
//
//        code.addRCode("packageExistRJava<-require(rJava)");
//        code.addRCode("if(!packageExistRJava){");
//        code.addRCode("source('http://bioconductor.org/biocLite.R')");
//        code.addRCode("biocLite('rJava')}");
//
//        code.addRCode("library(topGO)");
//        code.addRCode("setwd('" + prop.getTopAnatRWorkingDirectory()
//        + "')");
//        code.R_source(TopAnatAnalysis.class.getResource(this.prop.getTopAnatFunctionFile()).getPath());
//
//        code.addRCode("resultExist <- FALSE");
//
//        String[] topOBOResultFile = { resultFileName };
//        code.addStringArray("topOBOResultFile", topOBOResultFile);
//
//        String[] topOBOResultPDFFile = { resultPDFFileName };
//        code.addStringArray("resultPDF", topOBOResultPDFFile);
//
//        // Organ Relationships File
//        String[] organRelationships = { this.organRelationshipsFileName };
//        code.addStringArray("organRelationshipsFileName", organRelationships);
//        code.addRCode("tab <- read.table(organRelationshipsFileName,header=FALSE, sep='\t')");
//        code.addRCode("relations <- tapply(as.character(tab[,2]), as.character(tab[,1]), unique)");
//        code.addRCode("print('Relations:')");
//        code.addRCode("head(relations)");
//
//        // Gene to Organ Relationship File
//        String[] geneToOrgan = { this.geneToOrganFileName };
//        code.addStringArray("geneToOrganFileName", geneToOrgan);
//        //maybe the background is empty because of too stringent parameters.
//        //Check whether file is empty, otherwise an error would be generated
//        code.addRCode("if (file.info(geneToOrganFileName)$size != 0) {");
//        code.addRCode("  tab <- read.table(geneToOrganFileName,header=FALSE, sep='\t')");
//        code.addRCode("  gene2anatomy <- tapply(as.character(tab[,2]), as.character(tab[,1]), unique)");
//        code.addRCode("  print('GeneToAnaTomy:')");
//        code.addRCode("  head(gene2anatomy)");
//
//        // Organ Names File
//        String[] organNames = { this.organNamesFileName };
//        code.addStringArray("organNamesFileName", organNames);
//        code.addRCode("  organNames <- read.table(organNamesFileName, header = FALSE, sep='\t',row.names=1)");
//        code.addRCode("  names(organNames) <- organNames");
//        code.addRCode("  print('OrganNames:')");
//        code.addRCode("  head(organNames)");
//
//        code.addStringArray("StringIDs",
//                params.getSubmittedBackgroundIds().toArray(new String[0]));
//
//        code.addRCode("  geneList <- factor(as.integer(names(gene2anatomy) %in% StringIDs))");
//        code.addRCode("  names(geneList) <- names(gene2anatomy)");
//        code.addRCode("  print('GeneList:')");
//        code.addRCode("  head(geneList)");
//        //maybe all submitted genes are part of the background, or none of them, 
//        //in that case we cannot proceed to the tests
//        code.addRCode("  if (length(geneList) > 0 & length(levels(geneList)) == 2) {");
//
//        code.addRCode("    myData <- maketopGOdataObject(parentMapping = relations,allGenes = geneList,nodeSize = "+ params.getNodeSize() + ",gene2Nodes = gene2anatomy)");
//        //maybe make maketopGOdataObject to return an error code rather than using 'stop'?
//        //then: code.addRCode("if (is.character(myData)) {...}");
//
//        code.addRCode("    resFis <- runTest(myData, algorithm = '"
//                + this.params.getDecorelationType().getCode() +"', statistic = '"
//                + this.params.getStatisticTest().getCode() +"')");
//        //under-representation disabled
//        //code.addRCode("test.stat <- new('elimCount', testStatistic = GOFisherTestUnder, name ='Elim / Fisher test / underrepresentation')");
//        //code.addRCode("resFis.under <- getSigGroups(myData, test.stat)");
//
//        code.addRCode("    tableOver <- makeTable(myData,score(resFis), 1 , organNames)");
//        code.addRCode("    tableOver <- data.frame(lapply(tableOver,as.character), stringsAsFactors=FALSE)");
//
//        code.addRCode("    print(nrow(tableOver))");
//        code.addRCode("    print(ncol(tableOver))");
//
//        //if we get results, save the results and generate a graph visualization
//        code.addRCode("    if(nrow(tableOver)!=0  & ncol(tableOver)==8){");
//        code.addRCode("      print('RESULTS!')");
//        code.addRCode("      write.table(tableOver, file=topOBOResultFile, sep='\t', row.names=F, col.names=T, quote=F)");
//        code.addRCode("      resultExist <- TRUE");
//
//        code.addRCode("      pValFis <- score(resFis)");
//        code.addRCode("      pVal<-pValFis");
//        code.addRCode("      pVal[pValFis==0]<- 100");
//
//        code.addRCode("      organNames <- read.table(organNamesFileName, header = FALSE, sep='\t')");
//        code.addRCode("      rownames(organNames)<-organNames[,1]");
//
//        //get the number of terms with p-value below 0.01
//        code.addRCode("      resultCount <- sum(as.numeric(tableOver[, 7]) <= "+ params.getPvalueThreashold() +")");
//        //set the number of terms to be displayed (terms below p-value, but max 10)
//        code.addRCode("      resultCount <- min(c(resultCount , "+ params.getNumberOfSignificantNodes() +"))");
//        code.addRCode("      cat(paste('Number of nodes to display: ', resultCount, '\n'))");
//        //generate the graph only if we have significant nodes
//        code.addRCode("      if (resultCount != 0) {");
//        code.addRCode("        printTopOBOGraph(myData, pVal, firstSigNodes = resultCount, fileName = resultPDF , useInfo = 'all',pdfSW = TRUE, organNames=organNames)");
//        code.addRCode("      }");
//
//        code.addRCode("    }");
//        code.addRCode("  }");
//        code.addRCode("}");
//
//        //if there is no result, but no error occurred, we create an empty result file
//        code.addRCode("if (!resultExist) {");
//        code.addRCode("  cat('No result, creating an empty result file: ', topOBOResultFile, '\n')");
//        code.addRCode("  file.create(topOBOResultFile)");
//        //we need a tableOver object for RCaller to perform the commands
//        code.addRCode("    tableOver <- data.frame(1, 8)");
//        code.addRCode("}");
//
//        //create File to use its path as lock name (because the same name 
//        //is used in other methods)
//        File geneToOrganAssociationFile = new File(
//                prop.getTopAnatResultsWritingDirectory(),
//                this.geneToOrganFileName);
//        String geneToOrganAssociationFilePath = geneToOrganAssociationFile
//                .getPath();
//        File namesFile = new File(
//                prop.getTopAnatResultsWritingDirectory(),
//                this.organNamesFileName);
//        String namesFileName = namesFile.getPath();
//        File relsFile = new File(
//                prop.getTopAnatResultsWritingDirectory(),
//                this.organRelationshipsFileName);
//        String relsFileName = relsFile.getPath();
//
//        try {
//            this.acquireReadLock(namesFileName);
//            this.acquireReadLock(relsFileName);
//            this.acquireReadLock(geneToOrganAssociationFilePath);
//
//            log.info("Running statistical tests in R...");
//            caller.setRCode(code);
//            caller.runAndReturnResult("tableOver");
//
//        } finally {
//            this.releaseReadLock(namesFileName);
//            this.releaseReadLock(relsFileName);
//            this.releaseReadLock(geneToOrganAssociationFilePath);
//        }

        log.exit();

    }
    
}
