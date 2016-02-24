package org.bgee.view.html;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.model.file.SpeciesDataGroup;
import org.bgee.model.species.Species;
import org.bgee.view.GeneralDisplay;

/**
 * HTML View for the general category display
 * 
 * @author Mathieu Seppey
 * @author Frederic Bastian
 * @author Valentine Rech de Laval
 * @author Philippe Moret
 * @version Bgee 13, August 2015
 * @since Bgee 13
 */
public class HtmlGeneralDisplay extends HtmlParentDisplay implements GeneralDisplay {

    private final static Logger log = LogManager.getLogger(HtmlGeneralDisplay.class.getName());

    /**
     * Constructor
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that instantiated this object.
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter} 
     */
    public HtmlGeneralDisplay(HttpServletResponse response, RequestParameters requestParameters,
            BgeeProperties prop, HtmlFactory factory) throws IOException {
        super(response, requestParameters, prop, factory);
    }

    @Override
    public void displayHomePage(List<SpeciesDataGroup> groups) {
        log.entry(groups);

        this.startDisplay("Welcome to Bgee: a dataBase for Gene Expression Evolution");

        if (groups.stream().anyMatch(SpeciesDataGroup::isMultipleSpecies)) {
            throw log.throwing(new IllegalArgumentException(
                    "Only single-species groups should be displayed on the home page."));
        }

        this.displaySpeciesBanner(groups);

        this.displayStartBanner();

        this.displayExplanation();
        
        this.displayNews();

        this.displayMoreInfo();

        this.writeln(getImageSources());

        this.endDisplay();
        
        log.exit();
    }
    
    
    /**TODO
	 * @param groups
	 */
	private void displaySpeciesBanner(List<SpeciesDataGroup> groups) {
	    log.entry(groups);
	
	    StringBuilder homePageSpeciesSection = new StringBuilder();
	    
	    groups.stream().filter(sdg -> sdg.isSingleSpecies()).forEach(sdg -> {
	        Species species = sdg.getMembers().get(0);
	        Map<String,String> attrs = new HashMap<>();
	        attrs.put("src", this.prop.getSpeciesImagesRootDirectory() + htmlEntities(species.getId())+"_light.jpg");
	        attrs.put("alt", htmlEntities(species.getShortName()));
	        attrs.put("class", "species_img");
	        homePageSpeciesSection.append(getHTMLTag("img", attrs));
	        // FIXME: to be removed, it's to test larger number of species
	        homePageSpeciesSection.append(getHTMLTag("img", attrs));
	    });
	
	    this.writeln("<div id='bgee_species' class='row'>");
        this.writeln("<div class='hidden-xs col-sm-12'>");
	    this.writeln(homePageSpeciesSection.toString());
	    this.writeln("</div>");
	    this.writeln("</div>");
	
	    log.exit();
	}

	/**
	 * TODO
	 */
	private void displayStartBanner() {
		log.entry();
	
	    RequestParameters urlTopAnat = this.getNewRequestParameters();
	    urlTopAnat.setPage(RequestParameters.PAGE_TOP_ANAT);
	    RequestParameters urlGeneSearch = this.getNewRequestParameters();
	    urlGeneSearch.setPage(RequestParameters.PAGE_GENE);
	    RequestParameters urlDownload = this.getNewRequestParameters();
	    urlDownload.setPage(RequestParameters.PAGE_DOWNLOAD);
		
	    this.writeln("<div id='bgee_start' class='row'>");
	    
	    this.writeln("<div id='bgee_hp_logo'><img src='" + this.prop.getLogoImagesRootDirectory() 
	            + "bgee13_hp_logo.png' alt='Bgee logo'></div>");
	
	    this.writeln("<div class='mini_text'>Gene expression database in animals</div>");
	
	    this.writeln("<div id='start_buttons'>");
	    this.writeln("<a href='"+ urlTopAnat.getRequestURL() + "'>Start gene expression enrichment</a>");
	    this.writeln("<a href='"+ urlGeneSearch.getRequestURL() + "'>Gene search</a>");
	    this.writeln("<a href='"+ urlDownload.getRequestURL() + "'>Download</a>");
	    this.writeln("</div>"); // close start_buttons
	
	    this.writeln("</div>"); // close bgee_start row
	
	    log.exit();
	}

	/**
     * TODO
     */
    private void displayExplanation() {
    	log.entry();
    	
        this.writeln("<div id='bgee_explanations' class='row'>");
		
        this.writeln("<div class='col-md-offset-2 col-md-2'>");
        this.writeln("<span>Lorem ipsum</span>");
        this.writeln("<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. "
        		+ "Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, "
        		+ "dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper "
        		+ "congue, euismod non, mi.</p>");
        this.writeln("</div>");
        
        this.writeln("<div class='col-md-offset-1 col-md-2'>");
        this.writeln("<span>Lorem ipsum</span>");
        this.writeln("<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. "
        		+ "Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, "
        		+ "dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper "
        		+ "congue, euismod non, mi.</p>");
        this.writeln("</div>");

        this.writeln("<div class='col-md-offset-1 col-md-2'>");
        this.writeln("<span>Lorem ipsum</span>");
        this.writeln("<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. "
        		+ "Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, "
        		+ "dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper "
        		+ "congue, euismod non, mi.</p>");
        this.writeln("</div>");

        this.writeln("</div>"); // close bgee_explanations

        log.exit();
	}

	/**
	 * TODO
	 */
	private void displayNews() {
	    log.entry();
	    
	    RequestParameters urlTopAnat = this.getNewRequestParameters();
	    urlTopAnat.setPage(RequestParameters.PAGE_TOP_ANAT);
	    
	    RequestParameters urlDownload = this.getNewRequestParameters();
	    urlDownload.setPage(RequestParameters.PAGE_DOWNLOAD);
	    
	    RequestParameters urlDownloadProcValues = this.getNewRequestParameters();
	    urlDownloadProcValues.setPage(RequestParameters.PAGE_DOWNLOAD);
	    urlDownloadProcValues.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);
	    
	    RequestParameters urlDownloadCalls = this.getNewRequestParameters();
	    urlDownloadCalls.setPage(RequestParameters.PAGE_DOWNLOAD);
	    urlDownloadCalls.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);
	    
	    RequestParameters urlCallDoc = this.getNewRequestParameters();
	    urlCallDoc.setPage(RequestParameters.PAGE_DOCUMENTATION);
	    urlCallDoc.setAction(RequestParameters.ACTION_DOC_CALL_DOWLOAD_FILES);
	    
	    this.writeln("<div id='bgee_news' class='panel panel-default'>");
	    this.writeln("<div class='panel-heading'>");
	    this.writeln("<span class='panel-title'>News</span>" +
	                 "<span class='header_details'>(features are being added incrementally)</span>");
	    this.writeln("</div>"); // close panel-heading
	    
	    this.writeln("<div class='panel-body'>");
	    
	    this.writeOneNews("2015-12-24", "major update of <a href='" + urlTopAnat.getRequestURL()
	                      + "' title='Perform gene expression enrichment tests with TopAnat'>TopAnat</a>. "
	                      + "Happy Christmas!");
	    
	    this.writeOneNews("2015-11-24", "we are happy to release of our new exclusive tool "
	                      + "for gene expression enrichment analyses: <a href='" + urlTopAnat.getRequestURL()
	                      + "' title='Perform gene expression enrichment tests with TopAnat'>TopAnat</a>. "
	                      + "This is a tool with absolutely no equivalent, developped in collaboration with "
	                      + "the Web-Team  of the Swiss Institute of Bioinformatics. Check it out!");
	    this.writeOneNews("2015-08-26", "update of the home page.");
	    
	    this.writeOneNews("2015-06-08", "release of Bgee release 13.1: "
	                      + "<ul>"
	                      + "<li>Update of the website interfaces.</li>"
	                      + "<li><a href='" + urlDownloadProcValues.getRequestURL()
	                      + "'>New download page</a> providing processed expression values.</li>"
	                      + "<li>Addition of mouse <i>in situ</i> data from MGI, see "
	                      + "<a href='" + urlDownloadCalls.getRequestURL() + "#id10090"
	                      + "'>mouse data</a>.</li>"
	                      + "<li>Differential expression data have been added for "
	                      + "<a href='" + urlDownloadCalls.getRequestURL() + "#id7955"
	                      + "'>zebrafish</a>, <a href='" + urlDownloadCalls.getRequestURL() + "#id9598"
	                      + "'>chimpanzee</a>, <a href='" + urlDownloadCalls.getRequestURL() + "#id9593"
	                      + "'>gorilla</a>, and <a href='" + urlDownloadCalls.getRequestURL() + "#id13616"
	                      + "'>opossum</a>.</li>"
	                      + "<li>Addition of new multi-species differential expression data, see "
	                      + "for instance <a href='" + urlDownloadCalls.getRequestURL() + "#id9598_9544"
	                      + "'>chimpanzee/macaque comparison</a>.</li>"
	                      + "<li>New format to provide gene orthology information in multi-species files, "
	                      + "see for instance <a href='" + urlCallDoc.getRequestURL() + "#oma_hog"
	                      + "'>OMA Hierarchical orthologous groups documentation</a>.</li>"
	                      + "<li>Removal of data incorrectly considered as normal in <i>C. elegans</i>, "
	                      + "see <a href='" + urlDownloadCalls.getRequestURL() + "#id6239"
	                      + "'>worm data</a>.</li>"
	                      + "<li>Improved filtering of propagated no-expression calls. As a result, "
	                      + "complete expression calls files do not contain invalid conditions anymore.</li>"
	                      + "<li>Filtering of invalid developmental stages for differential expression analyses.</li>"
	                      + "</ul>");
	    this.writeOneNews("2015-04-16", "release of the multi-species " +
	                      "differential expression data (across anatomy) for 6 groups, see <a href='" +
	                      urlDownload.getRequestURL() + "' " + "title='Bgee download page'>" +
	                      "download page</a>.");
	    this.writeOneNews("2015-03-03", "release of the single-species " +
	                      "differential expression data for 11 species, see <a href='" +
	                      urlDownload.getRequestURL() + "' " + "title='Bgee download page'>" +
	                      "download page</a>.");
	    this.writeOneNews("2014-12-19", "release of the single-species " +
	                      "expression data for 17 species, see <a href='" +
	                      urlDownload.getRequestURL() + "' " + "title='Bgee download page'>" +
	                      "download page</a>.");
	    
	    this.writeln("</div>"); // close panel-body
	    this.writeln("</div>"); // close panel
	
	    log.exit();
	}

	/**
     * TODO
     */
    private void displayMoreInfo() {
    	log.entry();
    	
        this.writeln("<div id='bgee_more_info' class='row'>");
	    
	    this.writeln("<p>The complete website remains available for the previous release of Bgee:</p>");
	    
	    this.writeln("<div class='feature_list'>");
	    this.writeln(HtmlParentDisplay.getSingleFeatureLogo("http://bgee.org/bgee/bgee",
	    		true, "Bgee 12 home page", "Bgee 12",
	    		this.prop.getLogoImagesRootDirectory() + "bgee12_logo.png", null));
	    this.writeln("</div>"); // close feature_list
	    
        this.writeln("</div>"); // close bgee_more_info row
        
        log.exit();
	}

	/**TODO
     * @param date
     * @param description
     */
    private void writeOneNews(String date, String description) {
        log.entry(date, description);
        
        this.writeln("<div class='row'>");
        this.writeln("<div class='col-md-offset-1 col-md-2 news-date'>");
        this.writeln(date);
        this.writeln("</div>");
        this.writeln("<div class='col-md-8 news-desc'>");
        this.writeln(description);
        this.writeln("</div>");
        this.writeln("</div>");
        
        log.exit();
    }

    @Override
    public void displayAbout() {
        log.entry();
        this.startDisplay("Information about Bgee: a dataBase for Gene Expression Evolution");

        this.writeln("<h1>What is Bgee?</h1>");

        this.endDisplay();
        log.exit();
    }
    
    @Override
    protected void includeJs() {
        log.entry();
        super.includeJs();
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        this.includeJs("general.js");
        log.exit();
    }

    @Override
    protected void includeCss() {
        log.entry();
        super.includeCss();
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        this.includeCss("general.css");
        log.exit();
    }
}
