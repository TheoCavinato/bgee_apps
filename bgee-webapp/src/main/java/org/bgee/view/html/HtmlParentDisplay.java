package org.bgee.view.html;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.model.file.SpeciesDataGroup;
import org.bgee.model.species.Species;
import org.bgee.utils.JSHelper;
import org.bgee.view.ConcreteDisplayParent;
import org.bgee.view.ViewFactory;
import org.bgee.view.html.HtmlDownloadDisplay.DownloadPageType;

/**
 * Parent of all display for the HTML view.
 * 
 * @author Mathieu Seppey
 * @author Frederic Bastian
 * @author Valentine Rech de Laval
 * @author Philippe Moret
 * @version Bgee 13, June 2015
 * @since   Bgee 13
 */
public class HtmlParentDisplay extends ConcreteDisplayParent {

    private final static Logger log = LogManager.getLogger(HtmlParentDisplay.class.getName());

    /**
     * A {@code String} that is the page name of the 'processed expression values' download page.
     */
    protected final static String PROCESSED_EXPR_VALUES_PAGE_NAME = "Processed expression values";
    /**
     * A {@code String} that is the page name of the 'gene expression calls' download page.
     */
    protected final static String GENE_EXPR_CALLS_PAGE_NAME = "Gene expression calls";

    /**
     * Escape HTML entities in the provided {@code String}
     * @param stringToWrite A {@code String} that contains the HTML to escape
     * @return  The escaped HTML
     */
    public static String htmlEntities(String stringToWrite)
    {
        log.entry(stringToWrite);
    	try {                            
    	    return log.exit(StringEscapeUtils.escapeHtml4(stringToWrite).replaceAll("'", "&apos;"));
    	} catch (Exception e) {
    		return log.exit("");
    	}
    }

    /**
     * Constructor providing the necessary dependencies.
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that was used to instantiate this object.
     * 
     * @throws IllegalArgumentException If {@code factory} is {@code null}.
     * @throws IOException              If there is an issue when trying to get or to use the
     *                                  {@code PrintWriter} 
     */
    public HtmlParentDisplay(HttpServletResponse response, RequestParameters requestParameters, 
            BgeeProperties prop, HtmlFactory factory) throws IllegalArgumentException, IOException {
        super(response, requestParameters, prop, factory);
    }
    
    @Override
    protected String getContentType() {
        log.entry();
        return log.exit("text/html");
    }

    /**
     * Get the single feature logo with a description as a HTML 'div' element.
     *
     * @param url           A {@code String} that is the URL of the link.
     * @param externalLink  A {@code boolean} defining whether the link points to a Bgee 
     *                      internal URL, or an external resource (in which case a 'target' 
     *                      attribute with the '_blank' value will be append to the link). 
     *                      If {@code true}, the link points to an external resource.
     * @param title         A {@code String} that is the title and the alternate text of the image.
     * @param figcaption    A {@code String} that is the caption of the 'figure' element.
     * @param imgPath       A {@code String} that is the path of the image.
     * @param desc          A {@code String} that is the description of this feature.
     * @return              A {@code String} that is the single feature logo as a HTML 'div' element,
     *                      formated in HTML and HTML escaped if necessary.
     */
    protected static String getSingleFeatureLogo(
            String url, boolean externalLink, String title, String figcaption, 
            String imgPath, String desc) {
        log.entry(url, externalLink, title, figcaption, imgPath, desc);
        
        StringBuffer feature = new StringBuffer();
        feature.append("<div class='single_feature'>");
        feature.append("<a href='" + url + "' title='" + title + "'"
                + (externalLink ? " target='_blank'" : "") + ">" +
                "<figure><img src='" + imgPath + "' alt='" + title + " logo' />" +
                "<figcaption>" + figcaption + "</figcaption>" +
                "</figure></a>");
        if (desc != null && !desc.isEmpty()) {
            feature.append("<p>" + desc + "</p>");
        }
        feature.append("</div>");
        
        return log.exit(feature.toString());
    }

    public void emptyDisplay() {
        log.entry();
        this.sendHeaders();
        this.writeln("");
        log.exit();
    }

    /**
     * Generates the script tag for the speciesData object that is accessible
     * from the Javascript code of the page.
     * @param dataGroups The {@code List} of {@code SpeciesDataGroup} for which download files are availables
     * @return A {@String} containing the generated Javascript tag.
     */
    protected String getDataGroupScriptTag(List<SpeciesDataGroup> dataGroups) {
    	log.entry(dataGroups);
        StringBuffer sb = new StringBuffer("<script>");
        sb.append("var speciesData = ");
        sb.append(JSHelper.toJson(dataGroups.stream()
                .collect(Collectors.toMap(SpeciesDataGroup::getId, Function.identity()))));
        sb.append(";</script>");
        return log.exit(sb.toString());
    }
    
    /**
     * Generates the script tag for the species keywords (one entry is created per species group).
     * from the Javascript code of the page.
     * @param keywords The {@code Map} of species id to keywords
     * @param groups   The {@code List} of species data groups
     * @return A {@String} containing the generated Javascript tag.
     */
    protected String getKeywordScriptTag(Map<String, Set<String>> keywords, List<SpeciesDataGroup> groups) {
    	log.entry(keywords, groups);
        StringBuffer sb = new StringBuffer("<script>");
        sb.append("var keywords = ");
        Map<String, Set<String>> idToMembers = groups.stream()
        		.collect(Collectors.toMap(g -> g.getId(), 
        				g -> g.getMembers().stream().map(m -> m.getId()).collect(Collectors.toSet()))
        				);
        log.trace(idToMembers.size());
        log.trace(keywords.size());
        sb.append(JSHelper.toJson(idToMembers.entrySet()
        		.stream()
        		.collect(Collectors.toMap(e -> e.getKey(),
        				e -> 	e.getValue().stream()
        		// we get the keyword set for each group and then use reduce to concatenate the strings
        		.flatMap(id -> keywords.get(id).stream()).reduce( (s0,s1) -> s0+" "+s1).get() 
        				))));
        sb.append(";\n");
        sb.append("var autocomplete = ");
        sb.append(JSHelper.toJson(keywords.values().stream()
        		.flatMap(s -> s.stream())
        		.collect(Collectors.toSet())));
        sb.append(";</script>");
        return log.exit(sb.toString());
    }
    
    /**
     * Display the start of the HTML page (common to all pages).
     *
     * @param title A {@code String} that is the title to be used for the page. 
     */
    protected void startDisplay(String title) {
        log.entry(title);
        this.sendHeaders();
        this.writeln("<!DOCTYPE html>");
        this.writeln("<html lang='en'>");
        this.writeln("<head>");
        this.writeln("<meta charset='UTF-8'>");
        this.writeln("<title>"+title+"</title>");
        this.writeln("<meta name='description' content='Bgee allows to automatically"
                + " compare gene expression patterns between species, by referencing"
                + " expression data on anatomical ontologies, and designing homology"
                + " relationships between them.'/>");
        this.writeln("<meta name='keywords' content='bgee, gene expression, "
                + "evolution, ontology, anatomy, development, evo-devo database, "
                + "anatomical ontology, developmental ontology, gene expression "
                + "evolution'/>");
        this.writeln("<meta name='dcterms.rights' content='Bgee copyright 2007/2015 UNIL' />");
        this.writeln("<link rel='shortcut icon' type='image/x-icon' href='"
                +this.prop.getImagesRootDirectory()+"favicon.ico'/>");
        this.includeCss(); // load default css files, and css files specific of a view 
                           // (views must override this method if needed)
        this.includeJs();  // load default js files, and css files specific of a view 
                           // (views must override this method if needed)
        //google analytics
        //TODO: add the UA ID to properties. If no UA ID defined, do not display the google analytics code.
        //This will notably allow to stop messing up the google analytics results with our development tests 
        //(there would be no UA ID defined in test resource properties)
        this.writeln("<script>");
        this.writeln("(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){");
        this.writeln("(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),");
        this.writeln("m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)");
        this.writeln("})(window,document,'script','//www.google-analytics.com/analytics.js','ga');");
        this.writeln("ga('create', 'UA-18281910-2', 'auto');");
        this.writeln("ga('send', 'pageview');");
        this.writeln("</script>");
        
        this.writeln("</head>");
        
        this.writeln("<body>");
        this.writeln("<noscript>Sorry, your browser does not support JavaScript!</noscript>");
        this.writeln("<div id='bgee_top'><span id='TOP'></span></div>");
        this.writeln("<div id='sib_container'>");
        this.displayBgeeHeader();
        this.writeln("<div id='sib_body'>");

        log.exit();
    }

    /**
     * Display the end of the HTML page (common to all pages).
     */
    protected void endDisplay() {
        log.entry();

        this.writeln("</div>");
        //FIXME: I noticed that this footer messes up print version on firefox
        this.writeln("<footer>");
        this.writeln("<div id='sib_footer_content'>");
        this.writeln("<a href='http://www.isb-sib.ch'>SIB Swiss Institute of Bioinformatics</a>");
        this.writeln("<div id='sib_footer_right'>");
        this.writeln("<a href='#TOP' id='sib_footer_gototop'>"
                + "<span style='padding-left: 10px'>Back to the Top</span></a>");
        this.writeln("</div>");
        this.writeln("</div>");
        this.writeln("</footer>");
        this.writeln("</div>");
        
        this.writeln("</body>");
        this.writeln("</html>");
        log.exit();
    }

    /**
     * Display the Bgee header of the HTML page.
     */
    private void displayBgeeHeader() {
        log.entry();
        this.writeln("<header>");
        
        // Bgee logo
        this.writeln("<a href='" + this.getNewRequestParameters().getRequestURL() 
                + "' title='Go to Bgee home page'>");
        this.writeln("<img id='sib_other_logo' src='" + 
                this.prop.getLogoImagesRootDirectory() + "bgee13_logo.png' alt='Bgee logo' />");
        this.writeln("</a>");
    
        // Title
        this.writeln("<h1>Bgee: Gene Expression Evolution</h1>");
    
        // SIB logo
        this.writeln("<a href='http://www.isb-sib.ch/' target='_blank' " +
                "title='Link to the SIB Swiss Institute of Bioinformatics'>");
        this.writeln("<img id='sib_logo' src='"+this.prop.getLogoImagesRootDirectory() +
                "sib_logo.png' alt='SIB Swiss Institute of Bioinformatics' />");
        this.writeln("</a>");
    
        this.writeln(this.getNavBar());
        
        this.writeln("</header>");
        log.exit();
    }
    
    /**
     * @return  the {@code String} that is the HTML code of the navigation bar.
     */
    private String getNavBar() {
        log.entry();
        
        RequestParameters urlDownloadGenerator = this.getNewRequestParameters();
        urlDownloadGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);

        RequestParameters urlDownloadRefExprGenerator = this.getNewRequestParameters();
        urlDownloadRefExprGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadRefExprGenerator.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);
        
        RequestParameters urlDownloadCallsGenerator = this.getNewRequestParameters();
        urlDownloadCallsGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadCallsGenerator.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);

        RequestParameters urlDocGenerator = this.getNewRequestParameters();
        urlDocGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);

        RequestParameters urlBgeeAccessGenerator = this.getNewRequestParameters();
        urlBgeeAccessGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlBgeeAccessGenerator.setAction(RequestParameters.ACTION_DOC_HOW_TO_ACCESS);

        RequestParameters urlDownloadFilesDocGenerator = this.getNewRequestParameters();
        urlDownloadFilesDocGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlDownloadFilesDocGenerator.setAction(RequestParameters.ACTION_DOC_CALL_DOWLOAD_FILES);

        RequestParameters urlAboutGenerator = this.getNewRequestParameters();
        urlAboutGenerator.setPage(RequestParameters.PAGE_ABOUT);


        // Navigation bar
        StringBuffer navbar = new StringBuffer();

        navbar.append("<div id='nav'>");
        
        navbar.append("<ul id='bgee_links'>");
        navbar.append("<li>");
        navbar.append("<a title='Expression data page' href='" + 
                urlDownloadGenerator.getRequestURL() + "'>Expression data" + this.getCaret() 
                + "</a>");
        navbar.append("<ul>");
        navbar.append("<li><a class='drop' title='" + GENE_EXPR_CALLS_PAGE_NAME + "' href='" + 
                urlDownloadCallsGenerator.getRequestURL() + "'>" + GENE_EXPR_CALLS_PAGE_NAME + 
                "</a></li>");
        navbar.append("<li><a class='drop' title='" + PROCESSED_EXPR_VALUES_PAGE_NAME + "' href='" + 
                urlDownloadRefExprGenerator.getRequestURL() + "'>" + 
                PROCESSED_EXPR_VALUES_PAGE_NAME + "</a></li>");
        navbar.append("</ul>");
        navbar.append("</li>");
        navbar.append("<li>");
        navbar.append("<a title='Documentation page' href='" + urlDocGenerator.getRequestURL() + 
                "'>Documentation" + this.getCaret() 
                + "</a>");
        navbar.append("<ul>");
        navbar.append("<li><a class='drop' title='See how to access to Bgee data' href='" + 
                urlBgeeAccessGenerator.getRequestURL() + "'>How to access Bgee data</a></li>");
        navbar.append("<li><a class='drop' title='' href='" + 
                urlDownloadFilesDocGenerator.getRequestURL() 
                + "'>Expression call file documentation</a></li>");
        navbar.append("</ul>");
        navbar.append("</li>");
//        navbar.append("<li>");
//        navbar.append("<a id='about' rel='help' title='About page' href='" + 
//                urlAboutGenerator.getRequestURL() + "'>About</a>");
//        navbar.append("</li>");
        navbar.append("<li id='help'>");
        navbar.append(this.getObfuscateEmail());
        navbar.append("</li>");
        
        navbar.append("</ul>"); //end of Bgee links

        navbar.append("<ul id='social_links'>");
        navbar.append("<li><a id='twitter' class='social-link' title='See @Bgeedb account' " +
                "target='_blank' href='https://twitter.com/Bgeedb'>" + 
                "<img alt='Twitter logo' src='" + this.prop.getImagesRootDirectory() + 
                "Twitter.png'></a></li>");
        navbar.append("<li><a id='wordpress' class='social-link' title='See our blog' " + 
                "target='_blank' href='https://bgeedb.wordpress.com'>" + 
                "<img alt='Wordpress logo' src='" + this.prop.getImagesRootDirectory() + 
                "wordpress.png'></a></li>");
        navbar.append("</ul>");  // end #social-links

        navbar.append("</div>"); // end #nav

        return log.exit(navbar.toString());
    }

    /**
     * @param nbCalled  An {@code int} that is the different number every time 
     *                  this method is called per page!
     * @return          the {@code String} that is the HTML code of the Contact link.
     */
    //TODO move javascript in common.js
    private String getObfuscateEmail() {
        return "<script type='text/javascript'>eval(unescape('%66%75%6E%63%74%69%6F%6E%20%73%65%62%5F%74%72%61%6E%73%70%6F%73%65%32%28%68%29%20%7B%76%61%72%20%73%3D%27%61%6D%6C%69%6F%74%42%3A%65%67%40%65%73%69%2D%62%69%73%2E%62%68%63%27%3B%76%61%72%20%72%3D%27%27%3B%66%6F%72%28%76%61%72%20%69%3D%30%3B%69%3C%73%2E%6C%65%6E%67%74%68%3B%69%2B%2B%2C%69%2B%2B%29%7B%72%3D%72%2B%73%2E%73%75%62%73%74%72%69%6E%67%28%69%2B%31%2C%69%2B%32%29%2B%73%2E%73%75%62%73%74%72%69%6E%67%28%69%2C%69%2B%31%29%7D%68%2E%68%72%65%66%3D%72%3B%7D%64%6F%63%75%6D%65%6E%74%2E%77%72%69%74%65%28%27%3C%61%20%68%72%65%66%3D%22%23%22%20%6F%6E%4D%6F%75%73%65%4F%76%65%72%3D%22%6A%61%76%61%73%63%72%69%70%74%3A%73%65%62%5F%74%72%61%6E%73%70%6F%73%65%32%28%74%68%69%73%29%22%20%6F%6E%46%6F%63%75%73%3D%22%6A%61%76%61%73%63%72%69%70%74%3A%73%65%62%5F%74%72%61%6E%73%70%6F%73%65%32%28%74%68%69%73%29%22%3E%48%65%6C%70%3C%2F%61%3E%27%29%3B'));</script>";
    }

    /**
     * @return  the {@code String} that is the HTML code of the caret in navbar.
     */
    private String getCaret() {
        return "<img class='deploy' src='" + 
                this.prop.getImagesRootDirectory() + "arrow_down_dark.png' alt='Deploy'/>";
    }
    
    /**
     * Get the main logo of the documentation page, as HTML 'div' element.
     *
     * @return  A {@code String} that is the main documentation logo as HTML 'div' element,
     *          formated in HTML and HTML escaped if necessary.
     */
    protected String getMainDocumentationLogo() {
        log.entry();
        
        RequestParameters urlDocumentationGenerator = this.getNewRequestParameters();
        urlDocumentationGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
    
        return log.exit(HtmlParentDisplay.getSingleFeatureLogo(
                urlDocumentationGenerator.getRequestURL(), false, 
                "Bgee documentation page", "Documentation", 
                this.prop.getLogoImagesRootDirectory() + "doc_logo.png", null));
    }

    /**
     * Get the feature logos of the documentation page, as HTML 'div' elements.
     *
     * @return  A {@code String} that is the feature documentation logos as HTML 'div' elements,
     *          formated in HTML and HTML escaped if necessary.
     */
    //XXX: why isn't this in the HtmlDocumentationDisplay?
    protected String getFeatureDocumentationLogos() {
        log.entry();

        RequestParameters urlHowToAccessGenerator = this.getNewRequestParameters();
        urlHowToAccessGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlHowToAccessGenerator.setAction(RequestParameters.ACTION_DOC_HOW_TO_ACCESS);
        
        RequestParameters urlCallFilesGenerator = this.getNewRequestParameters();
        urlCallFilesGenerator.setPage(RequestParameters.PAGE_DOCUMENTATION);
        urlCallFilesGenerator.setAction(RequestParameters.ACTION_DOC_CALL_DOWLOAD_FILES);

        StringBuffer logos = new StringBuffer(); 

        logos.append(HtmlParentDisplay.getSingleFeatureLogo(urlHowToAccessGenerator.getRequestURL(), 
                false, "How to access to Bgee data", "Access to Bgee data", 
                this.prop.getLogoImagesRootDirectory() + "bgee_access_logo.png", null));

        logos.append(HtmlParentDisplay.getSingleFeatureLogo(urlCallFilesGenerator.getRequestURL(), 
                false, "Download file documentation page", "Download file documentation", 
                this.prop.getLogoImagesRootDirectory() + "download_logo.png", null));

        return log.exit(logos.toString());
    }

//    /**
//     * Get the main logo of the download page, as HTML 'div' element.
//     *
//     * @return  A {@code String} that is the main download logo as HTML 'div' element,
//     *          formated in HTML and HTML escaped if necessary.
//     */
//    protected String getMainDownloadLogo() {
//        log.entry();
//        
//        RequestParameters urlDownloadGenerator = this.getNewRequestParameters();
//        urlDownloadGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
//    
//        return log.exit(HtmlParentDisplay.getSingleFeatureLogo(urlDownloadGenerator.getRequestURL(), 
//                "Bgee expression data page", "Expression data", 
//                this.prop.getLogoImagesRootDirectory() + "download_logo.png", 
//                "Calls of baseline presence/absence of expression, "
//                + "and of differential over-/under-expression."));
//    }

    /**
     * Get the feature logos of the download page, as HTML 'div' elements.
     *
     * @return  A {@code String} that is the feature download logos as HTML 'div' elements,
     *          formated in HTML and HTML escaped if necessary.
     */
    protected String getFeatureDownloadLogos() {
        log.entry();

        RequestParameters urlDownloadRefExprGenerator = this.getNewRequestParameters();
        urlDownloadRefExprGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadRefExprGenerator.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);

        RequestParameters urlDownloadCallsGenerator = this.getNewRequestParameters();
        urlDownloadCallsGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadCallsGenerator.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);
        
        StringBuffer logos = new StringBuffer(); 
        
        logos.append(HtmlParentDisplay.getSingleFeatureLogo(
                urlDownloadCallsGenerator.getRequestURL(), false, 
                "Bgee " + GENE_EXPR_CALLS_PAGE_NAME.toLowerCase() + " page", GENE_EXPR_CALLS_PAGE_NAME, 
                this.prop.getLogoImagesRootDirectory() + "expr_calls_logo.png", 
                "Calls of baseline presence/absence of expression, "
                + "and of differential over-/under-expression, in single or multiple species."));

        logos.append(HtmlParentDisplay.getSingleFeatureLogo(
                urlDownloadRefExprGenerator.getRequestURL(), false, 
                "Bgee " + PROCESSED_EXPR_VALUES_PAGE_NAME.toLowerCase() + " page", 
                PROCESSED_EXPR_VALUES_PAGE_NAME, 
                this.prop.getLogoImagesRootDirectory() + "proc_values_logo.png", 
                "Annotations and processed expression data (e.g., read counts, RPKM values, "
                + "Affymetrix probeset signal intensities)."));
        
        return log.exit(logos.toString());
    }
    
    /**
     * TODO comment
     */
    protected String displayHelpLink(String cat, String display) {
        //TODO: to provide the cat, use a html5 data- attribute rather than 
        //a formatted String for the class attribute
        log.entry(cat, display);
        return log.exit("<span class='help'><a href='#' class='help|" + 
                cat + "'>" + display + "</a></span>");
    }
    
    /**
     * TODO comment
     */
    protected String displayHelpLink(String cat) {
        log.entry(cat);
        return log.exit(this.displayHelpLink(cat, "[?]"));
    }

    /**
     * Write HTML code allowing to include common javascript files. Subclasses needing to include 
     * additional javascript files must override this method. 
     * <p>
     * <strong>Important</strong>:
     * <ul>
     * <li>Javascript files should always be included by calling {@link #includeJs(String)}. 
     * {@link #includeJs(String)} will set the proper directory, and will automatically 
     * define versioned file names.
     * <li>{@code super.includeJs()} should always be called by these overriding methods, 
     * unless the aim is to generate a special page not using the common Bgee javascript libraries.
     * </ul>
     * @see #includeJs(String)
     */
    protected void includeJs() {
        log.entry();
        this.includeJs("lib/jquery.min.js");
        this.includeJs("lib/jquery.visible.js");
        this.includeJs("lib/jquery-ui.min.js");
        this.includeJs("lib/angular.min.js");
        this.includeJs("bgeeproperties.js");
        this.includeJs("urlparameters.js");
        this.includeJs("requestparameters.js");
        this.includeJs("common.js");
        log.exit();
    }
    /**
     * Write the HTML code allowing to include the javascript file named {@code fileName}. 
     * This method will notably retrieve the directory hosting the files, and will 
     * define the versioned file name corresponding to {@code fileName}, as hosted 
     * on the server. HTML is written using {@link #writeln()}.
     * <strong>It should be called only within a {@link #includeJs()} method, whether overridden 
     * or not.</strong>.
     * 
     * @param filename  The original name of the javascript file to include.
     * @see #getVersionedJsFileName(String)
     */
    protected void includeJs(String fileName) {
        log.entry(fileName);
        this.writeln("<script type='text/javascript' src='" +
                this.prop.getJavascriptFilesRootDirectory() + 
                this.getVersionedJsFileName(fileName) + "'></script>");
        log.exit();
    }
    /**
     * Transform the name of a javascript file into a name including version information, 
     * following the pattern used for javascript files hosted on the server. This is to avoid 
     * caching issues. The extension to use for version information is provided by 
     * {@link BgeeProperties#getJavascriptVersionExtension()}. 
     * <p>
     * For instance, if {@code getJavascriptVersionExtension} returns "-13", 
     * and if {@code originalFileName} is equal to "common.js", the value returned 
     * by this method will be: "common-13.js".
     * <p>
     * For simplicity, only file names ending with '.js' are accepted, otherwise, 
     * an {@code IllegalArgumentException} is thrown.
     * 
     * @param originalFileName  A {@code String} that is the name of a javascript file, 
     *                          ending with ".js", to transform into a versioned file name.
     * @return                  A {@code String} that is the versioned javascript file name, 
     *                          as used on the server, including the version extension 
     *                          returned by {@link BgeeProperties#getJavascriptVersionExtension()}.
     */
    protected String getVersionedJsFileName(String originalFileName) {
        log.entry(originalFileName);
        if (!originalFileName.endsWith(".js")) {
            throw log.throwing(new IllegalArgumentException("The provided file name "
                    + "must end with an extension '.js'."));
        }
        if (StringUtils.isBlank(this.prop.getJavascriptVersionExtension())) {
            return log.exit(originalFileName);
        }
        return log.exit(originalFileName.replaceAll("(.+?)\\.js", 
                "$1" + this.prop.getJavascriptVersionExtension() + ".js"));
    }
    
    /**
     * Write HTML code allowing to include common CSS files. Subclasses needing to include 
     * additional CSS files must override this method. 
     * <p>
     * <strong>Important</strong>:
     * <ul>
     * <li>CSS files should always be included by calling {@link #includeCss(String)}. 
     * {@link #includeCss(String)} will set the proper directory, and will automatically 
     * define versioned file names.
     * <li>{@code super.includeCss()} should always be called by these overriding methods, 
     * unless the aim is to generate a special page not using the common CSS definitions.
     * </ul>
     * @see #includeCss(String)
     */
    protected void includeCss() {
        this.includeCss("bgee.css"); 
    }
    /**
     * Write the HTML code allowing to include the CSS file named {@code fileName}. 
     * This method will notably retrieve the directory hosting the files, and will 
     * define the versioned file name corresponding to {@code fileName}, as hosted 
     * on the server. HTML is written using {@link #writeln()}.
     * <strong>It should be called only within a {@link #includeCss()} method, whether overridden 
     * or not.</strong>.
     * 
     * @param fileName  The original name of the CSS file to include.
     * @see #getVersionedCssFileName(String)
     */
    protected void includeCss(String fileName) {
        log.entry(fileName);
        this.writeln("<link rel='stylesheet' type='text/css' href='"
                + this.prop.getCssFilesRootDirectory() 
                + this.getVersionedCssFileName(fileName) + "'/>");
        log.exit();
    }
    /**
     * Transform the name of a CSS file into a name including version information, 
     * following the pattern used for CSS files hosted on the server. This is to avoid 
     * caching issues. The extension to use for version information is provided by 
     * {@link BgeeProperties#getCssVersionExtension()}. 
     * <p>
     * For instance, if {@code getCssVersionExtension} returns "-13", 
     * and if {@code originalFileName} is equal to "bgee.css", the value returned 
     * by this method will be: "bgee-13.css".
     * <p>
     * For simplicity, only file names ending with '.css' are accepted, otherwise, 
     * an {@code IllegalArgumentException} is thrown.
     * 
     * @param originalFileName  A {@code String} that is the name of a CSS file, 
     *                          ending with ".css", to transform into a versioned file name.
     * @return                  A {@code String} that is the versioned CSS file name, 
     *                          as used on the server, including the version extension 
     *                          returned by {@link BgeeProperties#getCssVersionExtension()}.
     */
    protected String getVersionedCssFileName(String originalFileName) {
        log.entry(originalFileName);
        if (!originalFileName.endsWith(".css")) {
            throw log.throwing(new IllegalArgumentException("The provided file name "
                    + "must end with an extension '.css'."));
        }
        if (StringUtils.isBlank(this.prop.getCssVersionExtension())) {
            return log.exit(originalFileName);
        }
        return log.exit(originalFileName.replaceAll("(.+?)\\.css", 
                "$1" + this.prop.getCssVersionExtension() + ".css"));
    }

    /**
     * Return a new {@code RequestParameters} object to be used to generate URLs. 
     * This new {@code RequestParameters} will use the same {@code URLParameters} 
     * as those returned by {@link #getRequestParameters()} when calling 
     * {@link RequestParameters#getUrlParametersInstance()}, 
     * and the {@code BgeeProperties} {@link #prop}. 
     * Also, parameters will be URL encoded, and parameter separator will be {@code &amp;}.
     * 
     * @return  A newly created RequestParameters object.
     */
    protected RequestParameters getNewRequestParameters() {
        log.entry();
        return log.exit(new RequestParameters(
                this.getRequestParameters().getUrlParametersInstance(), 
                this.prop, true, "&amp;"));
    }

    /**
     * @return  The {@code HtmlFactory} that instantiated this object. This method is provided 
     *          only for convenience to avoid having to cast the {@code Viewfactory} returned by 
     *          {@link #getFactory()}.
     */
    protected HtmlFactory getHtmlFactory() {
        return (HtmlFactory) super.getFactory();
    }
    /**
     * @return  The {@code ViewFactory} that instantiated this object, of type {@code HtmlFactory}.
     *          See {@link getHtmlFactory()} for a method returning directly the factory 
     *          as a {@code HtmlFactory}.
     */
    @Override
    //method overridden only to provide more accurate javadoc
    protected ViewFactory getFactory() {
        return super.getFactory();
    }
    
    /**
     * Get the single species section of a download page as a HTML 'div' element,
     * according the provided page type.
     *
     * @param pageType  A {@code DownloadPageType} that is the type of the page.
     * @return          the {@code String} that is the single species section as HTML 'div' element,
     *                  according {@code pageType}.
     */
    protected String getSingleSpeciesSection(DownloadPageType pageType, List<SpeciesDataGroup> groups) {
        log.entry(pageType);

        StringBuilder s = new StringBuilder();
        s.append("<div id='bgee_uniq_species'> ");
        s.append("<h2>Single-species</h2>");
        s.append("<div class='bgee_section bgee_download_section'>");
        s.append(getSingleSpeciesFigures(pageType, groups));
        s.append("</div>");
        s.append("</div>");
        
        return log.exit(s.toString());
    }
    
    /**
     * Gets the single species section as a {@code String}
     * @param pageType the {@code DownloadPageType} of this page
     * @param groups   the {@code List} of {@code SpeciesDataGroup} to display
     * @return A {@String} containing the html section 
     */
    protected String getSingleSpeciesFigures(DownloadPageType pageType, List<SpeciesDataGroup> groups) {
        StringBuilder sb = new StringBuilder();

        groups.stream().filter(sdg -> sdg.isSingleSpecies()).forEach(sdg -> {
            Species species = sdg.getMembers().get(0);
            Map<String, String> attr = new HashMap<>();
            attr.put("id", sdg.getId());
            sb.append(getHTMLTag("figure", attr, getHTMLTag("div", getImage(species)) + getCaption(species)));
        });
    	return sb.toString();
    }
    
    /**
     * Gets the html code for a species image.
     * @param species The {@code Species} for which to get the image
     * @return A {@String} containing the html code for this image
     */
    protected String getImage(Species species) {
    	Map<String,String> attrs = new HashMap<>();
    	attrs.put("src", "img/species/"+species.getId()+"_light.jpg");
    	attrs.put("alt", species.getShortName());
    	attrs.put("class", "species_img");

    	return getHTMLTag("img", attrs);
    }
    
    /**
     * Gets the {@code figcaption} element for a given {@code Species}
     * @param species A {@Species}
     * @return A {@code String} containing the html code
     */
    protected String getCaption(Species species) {
    	return getHTMLTag("figcaption", getShortNameTag(species)
    			+getHTMLTag("p", species.getName()));
    }
    
    /**
     * Gets the {@code figcaption} element for a given {@code SpeciesDataGroup}
     * @param species A {SpeciesDataGroup}
     * @return A {@code String} containing the html code
     */
    protected String getCaption(SpeciesDataGroup speciesDataGroup) {
    	return getHTMLTag("figcaption", speciesDataGroup.getName());
    }
    
    /**
     * Gets the short name element for a given {@Species}
     * @param species A {@Species}
     * @return A {@code String} containing the html code
     */
    protected String getShortNameTag(Species species) {
    	return getHTMLTag("p", getHTMLTag("i", species.getShortName()));
    }
    
   /**
    * Helper method to get an html tag
    * @param name    A {@code String} representing the name of the element 
    * @param content A {@code String} reprensenting the content of the element
    * @return The HTML code as {@code String}
    */
    protected String getHTMLTag(String name, String content) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("<").append(name).append(">\n")
    	  .append(content)
    	  .append("</").append(name).append(">\n");
    	return sb.toString();
    }
    
    /**
     * Helper method to get an html tag
     * @param name    A {@code String} representing the name of the element 
     * @param content A {@code String} reprensenting the content of the element
     * @return The HTML code as {@code String}
     */
    protected String getHTMLTag(String name, Map<String, String> attributes) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("<").append(name); 
    	for (Map.Entry<String, String> attr: attributes.entrySet()) {
    		sb.append(" ").append(attr.getKey()).append("=\"").append(attr.getValue()).append("\"");
    	}
    	sb.append(" />\n");
    	return sb.toString();
    }
    
    /**
     * Helper method to get an html tag
     * @param name    A {@code String} representing the name of the element 
     * @param content A {@code String} reprensenting the content of the element
     * @return The HTML code as {@code String}
     */
    protected String getHTMLTag(String name, Map<String, String> attributes, String content) {
    	StringBuffer sb = new StringBuffer();
    	sb.append("<").append(name); 
    	for (Map.Entry<String, String> attr: attributes.entrySet()) {
    		sb.append(" ").append(attr.getKey()).append("='").append(attr.getValue()).append("'");
    	}
    	sb.append(">\n").append(content).append("</").append(name).append(">\n");
    	return sb.toString();
    }
    
    /**
     * Get the images sources of a download page as a HTML 'div' element. 
     *
     * @return  the {@code String} that is the images sources as HTML 'div' element.
     */
    protected String getImageSources() {
        log.entry();
        
        StringBuffer sources = new StringBuffer();
        sources.append("<p id='creativecommons_title'>Images from Wikimedia Commons. In most cases, pictures corresponds to the sequenced strains. <a>Show information about original images.</a></p>");
        sources.append("<div id='creativecommons'>");
        sources.append("<p><i>Homo sapiens</i> picture by Leonardo da Vinci (Life time: 1519) [Public domain]. <a target='_blank' href='http://commons.wikimedia.org/wiki/File:Da_Vinci%27s_Anatomical_Man.jpg#mediaviewer/File:Da_Vinci%27s_Anatomical_Man.jpg'>See <i>H. sapiens</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Mus musculus</i> picture by Rasbak [<a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0/'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AApodemus_sylvaticus_bosmuis.jpg'>See <i>M. musculus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Danio rerio</i> picture by Azul (Own work) [see page for license], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AZebrafisch.jpg'>See <i>D. rerio</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Drosophila melanogaster</i> picture by Andr&eacute; Karwath aka Aka (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/2.5'>CC-BY-SA-2.5</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ADrosophila_melanogaster_-_side_(aka).jpg'>See <i>D. melanogaster</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Caenorhabditis elegans</i> picture by Bob Goldstein, UNC Chapel Hill http://bio.unc.edu/people/faculty/goldstein/ (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ACelegansGoldsteinLabUNC.jpg'>See <i>C. elegans</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Pan paniscus</i> picture by Ltshears (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a> or <a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ABonobo1_CincinnatiZoo.jpg'>See <i>P. paniscus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Pan troglodytes</i> picture by Thomas Lersch (Own work) [<a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a>, <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0/'>CC-BY-SA-3.0</a> or <a target='_blank' href='http://creativecommons.org/licenses/by/2.5'>CC-BY-2.5</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ASchimpanse_Zoo_Leipzig.jpg'>See <i>P. troglodytes</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Gorilla gorilla</i> picture by Brocken Inaglory (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a> or <a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AMale_gorilla_in_SF_zoo.jpg'>See <i>G. gorilla</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Pongo pygmaeus</i> picture by Greg Hume (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ASUMATRAN_ORANGUTAN.jpg'>See <i>P. pygmaeus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Macaca mulatta</i> picture by Aiwok (Own work) [<a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0-2.5-2.0-1.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AMacaca_mulatta_3.JPG'>See <i>M. mulatta</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Rattus norvegicus</i> picture by Reg Mckenna (originally posted to Flickr as Wild Rat) [<a target='_blank' href='http://creativecommons.org/licenses/by/2.0'>CC-BY-2.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AWildRat.jpg'>See <i>R. norvegicus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Bos taurus</i> picture by User Robert Merkel on en.wikipedia (US Department of Agriculture) [Public domain], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AHereford_bull_large.jpg'>See <i>B. taurus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Sus scrofa</i> picture by Joshua Lutz (Own work) [Public domain], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ASus_scrofa_scrofa.jpg'>See <i>S. scrofa</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Monodelphis domestica</i> picture by <i>Marsupial Genome Sheds Light on the Evolution of Immunity.</i> Hill E, PLoS Biology Vol. 4/3/2006, e75 <a rel='nofollow' href='http://dx.doi.org/10.1371/journal.pbio.0040075'>http://dx.doi.org/10.1371/journal.pbio.0040075</a> [<a target='_blank' href='http://creativecommons.org/licenses/by/2.5'>CC-BY-2.5</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AOpossum_with_young.png'>See <i>M. domestica</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Ornithorhynchus anatinus</i> picture by Dr. Philip Bethge (private) [<a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0-2.5-2.0-1.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AOrnithorhynchus.jpg'>See <i>O. anatinus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Gallus gallus</i> picture by Subramanya C K (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ARed_jungle_fowl.png'>See <i>G. gallus</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Anolis carolinensis</i> picture by PiccoloNamek (Moved from Image:P1010027.jpg) [<a target='_blank' href='http://www.gnu.org/copyleft/fdl.html'>GFDL</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0/'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AAnolis_carolinensis.jpg'>See <i>A. carolinensis</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Xenopus tropicalis</i> picture by V&aacute;clav Gvo&zcaron;d&iacute;k (http://calphotos.berkeley.edu) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/2.5'>CC-BY-SA-2.5</a>, <a target='_blank' href='http://creativecommons.org/licenses/by-sa/2.5'>CC-BY-SA-2.5</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3AXenopus_tropicalis01.jpeg'>See <i>X. tropicalis</i> picture via Wikimedia Commons</a></p>");
        sources.append("<p><i>Tetraodon nigroviridis</i> picture by Starseed (Own work) [<a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0/de/deed.en'>CC-BY-SA-3.0-de</a> or <a target='_blank' href='http://creativecommons.org/licenses/by-sa/3.0'>CC-BY-SA-3.0</a>], <a target='_blank' href='http://commons.wikimedia.org/wiki/File%3ATetraodon_nigroviridis_1.jpg'>See <i>T. nigroviridis</i> picture via Wikimedia Commons</a></p>");
        sources.append("</div>");
    
        return log.exit(sources.toString());
    }
}
