package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.DocumentationDisplay;

/**
 * This class displays the documentation for the HTML view.
 *
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, June 2015
 * @since   Bgee 13
 */
public class HtmlDocumentationDisplay extends HtmlParentDisplay implements DocumentationDisplay {

    private final static Logger log = LogManager.getLogger(HtmlDocumentationDisplay.class.getName());

    //*******************************************************
    // MISCELLANEOUS STATIC METHODS
    //*******************************************************
    /**
     * Provide explanations about how to retrieve correct genes in Ensembl, when we use 
     * the genome of another species for a given species. For instance, for bonobo we use 
     * the chimpanzee genome, and replace the 'ENSPTRG' prefix of chimp genes by 
     * the prefix 'PPAG'.
     * 
     * @return  A {@code String} formatted in HTML, providing the explanation.
     */
    //TODO: this needs to be generated automatically from the species table in database.
    public static String getGenomeMappingExplanation() {
        log.entry();
        return log.exit("Please note that "
        + "for <i>P. paniscus</i> (bonobo) we use <i>P. troglodytes</i> genome (chimpanzee), "
        + "and that for <i>P. pygmaeus</i> (Bornean orangutan) we use <i>P. abelii</i> genome "
        + "(Sumatran orangutan). Only for those species (bonobo and Bornean orangutan), "
        + "we modify the Ensembl gene IDs, to ensure that we provide unique gene identifiers "
        + "over all species. It is therefore necessary, to obtain correct Ensembl gene IDs "
        + "for those species, to replace gene ID prefix 'PPAG' with 'ENSPTRG', "
        + "and 'PPYG' prefix with 'ENSPPYG'.");
    }
    /**
     * @return  A {@code String} that is a general introduction to the concept 
     *          of presence/absence calls of expression, to be used in various places 
     *          of the documentation, in HTML, and HTML escaped if necessary.
     */
    public static String getExprCallExplanation() {
        log.entry();
        return log.exit("<p>Bgee provides calls of presence/absence of expression. Each call "
                + "corresponds to a unique combination of a gene, an anatomical entity, "
                + "and a life stage, with reported presence or absence of expression. "
                + "Life stages describe development and aging. Only \"normal\" "
                + "expression is considered in Bgee (i.e., no treatment, no disease, "
                + "no gene knock-out, etc.). Bgee collects data from different types, "
                + "from different studies, in different organisms, and provides a summary "
                + "from all these data as unique calls <code>gene - anatomical entity - "
                + "developmental stage</code>, with confidence information, notably taking "
                + "into account potential conflicts.</p>"
                + "<p>Calls of presence/absence of expression are very similar to the data "
                + "that can be reported using <i>in situ</i> hybridization methods; Bgee applies "
                + "dedicated statistical analyses to generate such calls from EST, Affymetrix, "
                + "and RNA-Seq data, with confidence information, and also collects "
                + "<i>in situ</i> hybridization calls from model organism databases. "
                + "This offers the possibility to aggregate and compare these calls of "
                + "presence/absence of expression between different experiments, "
                + "different data types, and different species, and to benefit from both "
                + "the high anatomy coverage provided by low-throughput methods, "
                + "and the high genomic coverage provided by high-throughput methods.</p>");
    }
    /**
     * @return  A {@code String} that is a general introduction to the concept 
     *          of over-/under-expression calls, to be used in various places 
     *          of the documentation, in HTML, and HTML escaped if necessary.
     */
    public static String getDiffExprCallExplanation() {
        log.entry();
        return log.exit("<p>Bgee provides calls of over-/under-expression. A call "
                + "corresponds to a gene, with significant variation of "
                + "its level of expression, in an anatomical entity "
                + "during a developmental stage, as compared to, either: i) other anatomical entities "
                + "at the same (broadly defined) developmental stage (over-/under-expression "
                + "across anatomy); "
                + "ii) the same anatomical entity at different (precise) developmental stages "
                + "(over-/under-expression across life stages). "
                + "These analyses of differential expression are performed using Affymetrix "
                + "and RNA-Seq experiments with at least 3 suitable conditions (anatomical entity/"
                + "developmental stage), and at least 2 replicates for each; as for all data in Bgee, "
                + "only \"normal\" expression is considered (i.e., no treatment, no disease, "
                + "no gene knock-out, etc.). </p>"
                + "<p>Bgee runs all possible differential expression analyses for each experiment "
                + "independently, then collects all results and provides a summary "
                + "as unique calls <code>gene - anatomical entity - developmental stage</code>, "
                + "with confidence information, and conflicts within each data type resolved "
                + "using a voting system weighted by p-values (conflicts between different "
                + "data types are treated differently). This offers the possibility "
                + "to aggregate and compare these calls between different experiments, "
                + "different data types, and different species. </p>");
    }

    
    
    //************************************
    // Instance attributes and methods
    //************************************
    
    /**
     * A {@code HtmlDocumentationCallFile} used to write the documentation 
     * about call download files (see {@link #displayCallDownloadFileDocumentation()}).
     */
    private final HtmlDocumentationCallFile callFileDoc;
    /**
     * A {@code HtmlDocumentationRefExprFile} used to write the documentation 
     * about ref. expression download files (see {@link #displayRefExprDownloadFileDocumentation()}).
     */
    private final HtmlDocumentationRefExprFile refExprFileDoc;
    /**
     * A {@code HtmlDocumentationTopAnat} used to write the documentation 
     * about TopAnat (see {@link #displayTopAnatDocumentation()}).
     */
    private final HtmlDocumentationTopAnat topAnatDoc;

    /**
     * Default constructor.
     *
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client.
     * @param requestParameters A {@code RequestParameters} handling the parameters of the 
     *                          current request, to determine the requested displayType, 
     *                          and for display purposes.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that instantiated this object.
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter}.
     */
    public HtmlDocumentationDisplay(HttpServletResponse response,
            RequestParameters requestParameters, BgeeProperties prop, HtmlFactory factory) 
                    throws IOException {
        this(response, requestParameters, prop, factory, null, null, null);
    }
    /**
     * Constructor providing other dependencies.
     *
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client.
     * @param requestParameters A {@code RequestParameters} handling the parameters of the 
     *                          current request, to determine the requested displayType, 
     *                          and for display purposes.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that instantiated this object.
     * @param callFileDoc       A {@code HtmlDocumentationCallFile} used to write the documentation 
     *                          about call download files (see {@link 
     *                          #displayCallDownloadFileDocumentation()}). If {@code null}, 
     *                          the default implementation will be used 
     *                          ({@link HtmlDocumentationCallFile}).
     * @param refExprFileDoc    A {@code HtmlDocumentationRefExprFile} used to write the documentation 
     *                          about ref. expression download files (see {@link 
     *                          #displayRefExprDownloadFileDocumentation()}). If {@code null}, 
     *                          the default implementation will be used 
     *                          ({@link HtmlDocumentationRefExprFile}).
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter}.
     */
    public HtmlDocumentationDisplay(HttpServletResponse response,
            RequestParameters requestParameters, BgeeProperties prop, HtmlFactory factory,
            HtmlDocumentationCallFile callFileDoc, HtmlDocumentationRefExprFile refExprFileDoc,
            HtmlDocumentationTopAnat topAnatDoc) 
                    throws IOException {
        super(response, requestParameters, prop, factory);
        if (callFileDoc == null) {
            this.callFileDoc = 
                    new HtmlDocumentationCallFile(response, requestParameters, prop, factory);
        } else {
            this.callFileDoc = callFileDoc;
        }
        if (refExprFileDoc == null) {
            this.refExprFileDoc = 
                    new HtmlDocumentationRefExprFile(response, requestParameters, prop, factory);
        } else {
            this.refExprFileDoc = refExprFileDoc;
        }
        if (topAnatDoc == null) {
        	this.topAnatDoc = new HtmlDocumentationTopAnat(response, requestParameters, prop, factory);
        } else {
        	this.topAnatDoc = topAnatDoc;
        }
    }
    
    @Override
    public void displayDocumentationHomePage() {
        log.entry();
        
        this.startDisplay("Bgee release 13 documentation home page");

        this.writeln("<h1>Bgee release 13 documentation pages</h1>");

        this.writeln("<div class='feature_list'>");

        this.writeln(this.getFeatureDocumentationLogos());

        this.writeln("</div>");
        
        this.endDisplay();

        log.exit();
    }

    /**
     * Get the feature logos of the documentation page, as HTML 'div' elements.
     *
     * @return  A {@code String} that is the feature documentation logos as HTML 'div' elements,
     *          formated in HTML and HTML escaped if necessary.
     */
    private String getFeatureDocumentationLogos() {
        log.entry();

        RequestParameters urlHowToAccessGenerator = this.getNewRequestParameters();
        urlHowToAccessGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlHowToAccessGenerator.setAction(RequestParameters.ACTION_DOC_HOW_TO_ACCESS);
        
        RequestParameters urlCallFilesGenerator = this.getNewRequestParameters();
        urlCallFilesGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlCallFilesGenerator.setAction(RequestParameters.ACTION_DOC_CALL_DOWLOAD_FILES);
        
        RequestParameters urlTopAnatGenerator = this.getNewRequestParameters();
        urlTopAnatGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlTopAnatGenerator.setAction(RequestParameters.ACTION_DOC_TOP_ANAT);

        StringBuilder logos = new StringBuilder(); 

        logos.append(HtmlParentDisplay.getSingleFeatureLogo(urlHowToAccessGenerator.getRequestURL(), 
                false, "How to access to Bgee data", "Access to Bgee data", 
                this.prop.getLogoImagesRootDirectory() + "bgee_access_logo.png", null));

        logos.append(HtmlParentDisplay.getSingleFeatureLogo(urlCallFilesGenerator.getRequestURL(), 
                false, "Download file documentation page", "Download file documentation", 
                this.prop.getLogoImagesRootDirectory() + "download_logo.png", null));

        //TODO uncomment when top ant logo is created
//        logos.append(HtmlParentDisplay.getSingleFeatureLogo(urlTopAnatGenerator.getRequestURL(), 
//                false, "TopAnat documentation page", "TopAnat documentation", 
//                this.prop.getLogoImagesRootDirectory() + "topAnat_logo.png", null));

        return log.exit(logos.toString());
    }

    //*******************************************************
    // DOCUMENTATION FOR CALL DOWNLOAD FILES 
    //*******************************************************
    @Override
    public void displayCallDownloadFileDocumentation() {
        log.entry();
        
        this.startDisplay("Expression call download file documentation");
        
        this.writeln("<div class='" + CENTERED_ELEMENT_CLASS + "'>");

        this.callFileDoc.writeDocumentation();
        
        this.writeln("</div>");

        this.endDisplay();

        log.exit();
    }
    @Override
    public void displayRefExprDownloadFileDocumentation() {
        log.entry();
        
        this.startDisplay(PROCESSED_EXPR_VALUES_PAGE_NAME + " download file documentation");
        
        this.writeln("<div class='" + CENTERED_ELEMENT_CLASS + "'>");

        this.refExprFileDoc.writeDocumentation();
        
        this.writeln("</div>");

        this.endDisplay();

        log.exit();
    }

    @Override
    public void displayTopAnatDocumentation() {
        log.entry();
        
        this.startDisplay(TOP_ANAT_PAGE_NAME + " documentation");

        this.writeln("<div class='" + CENTERED_ELEMENT_CLASS + "'>");
        
        this.topAnatDoc.writeDocumentation();
        
        this.writeln("</div>");

        this.endDisplay();

        log.exit();
    }


    @Override
    //TODO: use a different ID than 'feature_list', to provide a different look, 
    //notably with much larger elements, to provide more text below the figures.
    public void displayHowToAccessDataDocumentation() {
        log.entry();
        
        this.startDisplay("How to access to Bgee data");

        this.writeln("<h1>How to access to Bgee data</h1>");

        RequestParameters urlDownloadProcExprValuesGenerator = this.getNewRequestParameters();
        urlDownloadProcExprValuesGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadProcExprValuesGenerator.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);

        RequestParameters urlDownloadCallsGenerator = this.getNewRequestParameters();
        urlDownloadCallsGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadCallsGenerator.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);
        
        this.writeln("<div class='feature_list'>");
        
        this.writeln(this.getFeatureDownloadLogos());

        this.writeln(HtmlParentDisplay.getSingleFeatureLogo("https://github.com/BgeeDB", 
                true, "GitHub of the Bgee project", "GitHub", 
                this.prop.getLogoImagesRootDirectory() + "github_logo.png", 
                "Retrieve our annotations of homology between anatomical structures, "
                + "as well as the Confidence Information Ontology (CIO) "
                + "and the Homology Ontology (HOM), from our GitHub repository."));

        this.writeln(HtmlParentDisplay.getSingleFeatureLogo(this.prop.getFTPRootDirectory() + 
                "sql_dump.tar.gz", false, "Download dump the MySQL Bgee database", "MySQL dump", 
                this.prop.getLogoImagesRootDirectory() + "mysql_logo.png", 
                "Download the complete dump of the MySQL Bgee database, that contains "
                + "all the data used to generate the information displayed on this website."));

        this.writeln("</div>");
        
        this.endDisplay();

        log.exit();
    }

    //*******************************************************
    // COMMON METHODS
    //*******************************************************
//    /**
//     * @return  the {@code String} that is the link of the back to the top.
//     */
//    private String getBackToTheTopLink() {
//        log.entry();
//        return log.exit("<a class='backlink' href='#sectionname'>Back to the top</a>");
//    }

    @Override
    protected void includeCss() {
        log.entry();
        super.includeCss();
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        this.includeCss("documentation.css");
        log.exit();
    }
}
