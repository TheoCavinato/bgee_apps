package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;

/**
 * This class encapsulates the documentation for reference expression download files. 
 * It does not implement any interface from the {@code view} package, it is meant to be used 
 * by the class {@link HtmlDocumentationDisplay}. The only reason is that the class 
 * {@code HtmlDocumentationDisplay} was getting too large and too complicated. 
 * 
 * @author  Frederic Bastian
 * @see HtmlDocumentationDisplay
 * @version Bgee 13 June 2015
 * @since   Bgee 13
 */
public class HtmlDocumentationRefExprFile extends HtmlDocumentationDownloadFile {
    private static final Logger log = LogManager.getLogger(HtmlDocumentationRefExprFile.class.getName());

    /**
     * @return  A {@code String} that is the documentation menu for Affymetrix data, 
     *          formatted in HTML, with {@code ul} element including {@code li} elements.
     */
    private static String getAffyDocMenu() {
        log.entry();
        return log.exit("<ul>"
                + "<li><a href='#affy_exp' title='Quick jump to this section'>" + 
                "Experiments</a></li>"
                + "<li><a href='#affy_chip' title='Quick jump to this section'>" + 
                "Chips</a></li>"
                + "<li><a href='#affy_probeset' title='Quick jump to this section'>" + 
                "Probesets</a></li>"
                + "</ul>");
    }
    /**
     * @return  A {@code String} that is the documentation menu for RNA-Seq data, 
     *          formatted in HTML, with {@code ul} element including {@code li} elements.
     */
    private static String getRNASeqDocMenu() {
        log.entry();
        return log.exit("<ul>"
                + "<li><a href='#rna-seq_exp' title='Quick jump to this section'>" + 
                "Experiments</a></li>"
                + "<li><a href='#rna-seq_lib' title='Quick jump to this section'>" + 
                "Libraries</a></li>"
                + "<li><a href='#rna-seq_gene' title='Quick jump to this section'>" + 
                "RNA-Seq read counts and RPKM values</a></li>"
                + "</ul>");
    }


    /**
     * Default constructor. 
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that instantiated the
     *                          {@link HtmlDocumentationDisplay} object using this object.
     * @throws IllegalArgumentException If {@code factory} is {@code null}.
     * @throws IOException              If there is an issue when trying to get or to use the
     *                                  {@code PrintWriter} 
     */
    protected HtmlDocumentationRefExprFile(HttpServletResponse response,
            RequestParameters requestParameters, BgeeProperties prop, HtmlFactory factory)
            throws IllegalArgumentException, IOException {
        super(response, requestParameters, prop, factory);
    }
    
    
    /**
     * Write the documentation for the call download files. This method is not responsible 
     * for calling the methods {@code startDisplay} and {@code endDisplay}. Otherwise, 
     * all other elements of the page are written by this method.
     * 
     * @see HtmlDocumentationDisplay#displayCallDownloadFileDocumentation()
     */
    // TODO continue to write that documentation and then add schema.org properties. 
    // We don't want to index not finished pages
    protected void writeDocumentation() {
        log.entry();
        
        this.writeln("<h1 id='sectionname'>Reference expression download file documentation</h1>");
        RequestParameters urlDownloadGenerator = this.getNewRequestParameters();
        urlDownloadGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadGenerator.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);
        this.writeln("<div id='bgee_introduction'><p>Blabla. "
                + "This documentation describes the format of these "
                + "<a href='" + urlDownloadGenerator.getRequestURL()
                + "' title='Bgee reference expression data page'>download files</a>.</p></div>");
        
        //Documentation menu
        this.writeDocMenuForRefExprDownloadFiles();
        
        //Affymetrix download file documentation
        this.writeln("<div>");
        this.writeAffyRefExprFileDoc();
        this.writeln("</div>"); // end of Affymetrix
        
        
        //RNA-Seq download file documentation
        this.writeln("<div>");
        this.writeRNASeqRefExprFileDoc();
        this.writeln("</div>"); // end of RNA-Seq

        log.exit();
    }
    
    /**
     * Write the documentation menu related to reference expression  
     * download files. Anchors used in this method for quick jump links 
     * have to stayed in sync with id attributes of h2, h3 and h4 tags defined in 
     * {@link #writeAffyRefExprFileDoc()},  and
     * {@link #writeRNASeqRefExprFileDoc()}.
     * 
     * @see #writeAffyRefExprFileDoc()
     * @see #writeRNASeqRefExprFileDoc()
     */
    private void writeDocMenuForRefExprDownloadFiles() {
        log.entry();
        
        this.writeln("<div class='documentationmenu'><ul>");
        //Affymetrix
        this.writeln("<li><a href='#affy' title='Quick jump to this section'>" + 
                "Affymetrix data download files</a>");
        this.writeln(getAffyDocMenu());
        this.writeln("</li>"); // end of Affymetrix

        //RNA-Seq
        this.writeln("<li><a href='#rna-seq' title='Quick jump to this section'>" + 
                "RNA-Seq data download files</a>");
        this.writeln(getRNASeqDocMenu());
        this.writeln("</li>"); // end of RNA-Seq

        this.writeln("<li><a href='#troubleshooting' title='Quick jump to this section'>" + 
                "Troubleshooting</a>");
        this.writeln("</ul></div>");// end of documentationmenu
        
        log.exit();
    }
    
    /**
     * Write the documentation related to Affymetrix reference expression download files. 
     * Anchors used in this method for quick jump links 
     * have to stayed in sync with id attributes of h4 tags defined in 
     * {@link #writeAffyExpFileDoc()}, {@link #writeAffyChipFileDoc()}, and 
     * {@link #writeAffyProbesetFileDoc()}.
     * 
     * @see #writeAffyExpFileDoc()
     * @see #writeAffyChipFileDoc()
     * @see #writeAffyProbesetFileDoc()
     */
    private void writeAffyRefExprFileDoc() {
        log.entry();
        
        this.writeln("<h2 id='single'>Affymetrix data download files</h2>");
        this.writeln("<div class='doc_content'>"
                + "<p>Affymetrix data used in Bgee are retrieved from <a target='_blank' rel='noopener' "
                + "title='External link to ArrayExpress' "
                + "href='https://www.ebi.ac.uk/arrayexpress/'>ArrayExpress</a> and "
                + "<a target='_blank' rel='noopener' title='External link to GEO' "
                + "href='https://www.ncbi.nlm.nih.gov/geo/'>GEO</a>. They are annotated "
                + "to anatomical and developmental stage ontologies, filtered by quality controls "
                + "and analyzed to produce expression data. Only \"normal\" "
                + "expression data are integrated in Bgee (i.e., no treatment, no disease, "
                + "no gene knock-out, etc.). Here are described the format "
                + "of the files providing processed, annotated Affymetrix data.</p>");
        this.writeln("<p>Jump to: </p>"
                + getAffyDocMenu());
        //experiments
//        this.writeAffyExpFileDoc();
        //chips
//        this.writeAffyChipFileDoc();
        //probesets
//      this.writeAffyProbesetFileDoc();
        
        
        this.writeln("</div>"); //end of doc_content
        
        log.exit();
    }
    
    /**
     * Write the documentation related to RNA-Seq reference expression download files. 
     * Anchors used in this method for quick jump links 
     * have to stayed in sync with id attributes of h4 tags defined in 
     * {@link #writeRNASeqExpFileDoc()}, {@link #writeRNASeqLibraryFileDoc()}, and 
     * {@link #writeRNASeqGeneFileDoc()}.
     * 
     * @see #writeRNASeqExpFileDoc()
     * @see #writeRNASeqLibraryFileDoc()
     * @see #writeRNASeqGeneFileDoc()
     */
    private void writeRNASeqRefExprFileDoc() {
        log.entry();
        
        this.writeln("<h2 id='single'>RNA-Seq data download files</h2>");
        this.writeln("<div class='doc_content'>"
                + "<p>blabla...</p>");
        this.writeln("<p>Jump to: </p>"
                + getRNASeqDocMenu());
        //experiments
//        this.writeRNASeqExpFileDoc();
        //libraries
//        this.writeRNASeqLibraryFileDoc();
        //genes
//      this.writeRNASeqGeneFileDoc();
        
        
        this.writeln("</div>"); //end of doc_content
        
        log.exit();
    }
}
