package org.bgee.view.html;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.CollaborationDisplay;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * This class displays the page having the category "collaborations", i.e. with the parameter
 * page=collaborations for the HTML view.
 *
 * @author  Valentine Rech de Laval
 * @version Bgee 14, May 2019
 * @since   Bgee 13, Apr. 2019
 */
public class HtmlCollaborationDisplay extends HtmlParentDisplay implements CollaborationDisplay {

    private final static Logger log = LogManager.getLogger(HtmlCollaborationDisplay.class.getName());

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
    public HtmlCollaborationDisplay(HttpServletResponse response, RequestParameters requestParameters,
                               BgeeProperties prop, HtmlFactory factory) throws IOException {
        super(response, requestParameters, prop, factory);
    }

    public void displayCollaborationPage() {
        log.entry();

        this.startDisplay("Bgee collaborations");

        this.writeln("<h1>Bgee collaborations</h1>");

        this.writeln("<div class='row'>");

        this.writeln("<div class='" + CENTERED_ELEMENT_CLASS + "'>");
        this.writeln("<p>This page provides current collaborations of the Bgee project" +
                " (in alphabetical order).</p>");


        this.writeln("<h2>BioSODA</h2>");
        this.writeln("<p><a target='_blank' title='BioSODA project description' href='" +
                "https://www.zhaw.ch/no_cache/en/research/research-database/project-detailview/projektid/1493/'>" +
                "BioSODA project</a> aims at enabling sophisticated semantic queries across large, " +
                "decentralized and heterogeneous databases via an intuitive interface. " +
                "The system will enable scientists, without prior training, to perform powerful " +
                "joint queries across resources in ways that cannot be anticipated and " +
                "therefore goes far and above the query functionality of specialized knowledge bases. " +
                "It is supported by <a href='http://www.nfp75.ch/en' target='_blank' " +
                "title='NFP75 Big Data website'>NFP75 'Big Data'</a>.<p>");

        this.writeln("<p>For this project, we created a sub-database of Bgee called " + BGEE_LITE_NAME +
                ". The MySQL dump is available on <a target='_blank' title='Download the dump' href='" +
                this.prop.getFTPRootDirectory() +"sql_lite_dump.tar.gz'>Bgee FTP</a>. " +
                "You can see the description of " + BGEE_LITE_NAME + " in <a title='" + BGEE_LITE_NAME + 
                " description' href='" + BGEE_GITHUB_URL +
                // TODO replace develop by master when bgee_pipeline will be release
                "/bgee_pipeline/tree/develop/pipeline/dblite_creation" +
                "#information-about-the-bgee-lite-database' target='_blank'>" +
                "Bgee pipeline documentation</a><p>");
        
        this.writeln("<p>BioSODA uses the " + BGEE_LITE_NAME + " relational database to expose a " +
                "Bgee SPARQL endpoint reachable through the following URL: " +
                "<span class='copyable-url'>http://biosoda.expasy.org:8080/rdf4j-server/repositories/bgeelight</span>. " +
                "In the context of this project, the <a href='http://biosoda.expasy.org' target='_blank'>" +
                "BioQuery web application</a> provides a user-friendly interface to query " + 
                BGEE_LITE_NAME + " database based on the SPARQL query language.<p>");
        
        this.writeln("<p>The SPARQL endpoint and " + BGEE_LITE_NAME + " are available and free " +
                "to use for other projects or applications.<p>");
        
        this.writeln("<h2>OMA</h2>");
        
        this.writeln("<p>The <a href='https://omabrowser.org/oma/home/' target='_blank'>OMA " +
                "(Orthologous MAtrix) project</a> is a method and database " +
                "for the inference of orthologs among complete genomes.<p>");

        this.writeln("<p>As part of a scientific collaboration with OMA, we generated files " +
                "containing gene expression in homologous anatomical entities. " +
                "These files are available on <a href='" +
                this.prop.getFTPRootDirectory() +"collaboration/branch_length_expression_divergence/'>" +
                "Bgee FTP</a>.<p>");

        this.writeln("<p>The homologous expression files are also available and free " +
                "to use for other projects or applications.<p>");

        this.writeln("<h2>OncoMX</h2>");

        this.writeln("<p><a href='https://www.oncomx.org/' target='_blank'>OncoMX</a> is " +
                "a knowledgebase of unified cancer genomics data from integrated mutation, " +
                "expression, literature, and biomarker databases, accessible through web portal. " +
                "It is supported by <a href='https://www.cancer.gov/' target='_blank'>NIH NCI</a>.<p>");
        
        this.writeln("<p>For this project, we generated files containing a subset of calls of " +
                "presence/absence of expression with expression level categories specific to OncoMX. " +
                "Files are available on <a href='" + this.prop.getFTPRootDirectory() +
                "/collaboration/oncoMX/'>Bgee FTP</a>. You can see the description of these files in " +
                // TODO replace develop by master when bgee_pipeline will be release
                "<a href='" + BGEE_GITHUB_URL + "/bgee_pipeline/tree/develop/pipeline/collaboration/oncoMX" +
                "#information-about-the-files-generated-for-oncomx' target='_blank'>" +
                "Bgee pipeline documentation</a><p>");

        this.writeln("</div>"); // close CENTERED_ELEMENT_CLASS

        this.writeln("</div>"); // close row

        this.endDisplay();

        log.exit();
    }

    @Override
    protected void includeCss() {
        log.entry();
        super.includeCss();
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        log.exit();
    }
}