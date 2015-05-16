package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.ConcreteDisplayParent;

/**
 * Parent of all display for the {@code displayTypes} HTML
 * 
 * @author  Mathieu Seppey
 * @author Frederic Bastian
 * @version Bgee 13 Mar. 2015
 * @since   Bgee 13
 */
public class HtmlParentDisplay extends ConcreteDisplayParent {

    private final static Logger log = LogManager.getLogger(HtmlParentDisplay.class.getName());

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
     * The {@code RequestParameters} holding the parameters of the current query 
     * being treated.
     */
    protected final RequestParameters requestParameters;
    /**
     * TODO comment, what is this ?
     */
    private int uniqueId;

    /**
     * Constructor 
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * 
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter} 
     */
    public HtmlParentDisplay(HttpServletResponse response, RequestParameters requestParameters, 
            BgeeProperties prop) throws IOException {
        super(response,prop);
        this.requestParameters = requestParameters;
        this.uniqueId = 0;
    }

    protected static String getLogoLink(String url, 
            String title, String figcaption, String imgPath) {
        log.entry(url, title, figcaption, imgPath);
        return log.exit("<a href='" + url + "' title='" + title + "'>" +
                "<figure><img class='pageimg' src='" + imgPath + "' alt='" + title + " logo' />" +
                "<figcaption>" + figcaption + "</figcaption>" +
                "</figure></a>");
    }

    /**
     * @return An {@code int} TODO be more specific
     */
    protected int getUniqueId()
    {
        log.entry();
        //need to return 0 the first time this method is called;
        int idToReturn = this.uniqueId;
        this.uniqueId++;
        return log.exit(idToReturn);
    }

    public void emptyDisplay()
    {
        log.entry();
        this.sendHeaders(true);
        this.writeln("");
        log.exit();
    }
    //TODO: use an enum rather than a String page? => yep
    // but String page is not used
    //TODO: javadoc
    protected void startDisplay(String page, String title)
    {
        log.entry(page, title);
        this.sendHeaders(false);
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
        //TODO: what's the point of this empty link? If it is to use an anchor #TOP, 
        //you can use a span or div
        this.writeln("<div id='bgee_top'><a id='TOP'></a></div>");
        this.writeln("<div id='sib_container'>");
        this.displayBgeeHeader();
        this.writeln("<div id='sib_body'>");

        log.exit();
    }

    /**
     * Display the end of the page
     */
    protected void endDisplay()
    {
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

    //TODO: javadoc
    private void displayBgeeHeader() {
        log.entry();
        this.writeln("<header>");
        
        // Bgee logo
        this.writeln("<a href='" + this.prop.getBgeeRootDirectory() + "' title='Go to Bgee home page'>"
                + "<img id='sib_other_logo' src='"+this.prop.getImagesRootDirectory()
                + "logo/bgee13_logo.png' title='Bgee: a dataBase for Gene Expression Evolution' "
                + "alt='Bgee: a dataBase for Gene Expression Evolution' />"
                + "</a>");
    
        // Title
        //TODO: change this hardcoded 'Release 13 download page', provide it as argument
        this.writeln("<h1>Bgee: Gene Expression Evolution</h1>"
                //+ "<h2>Release 13 download page</h2>"
                );
    
        // SIB logo
        this.writeln("<a href='http://www.isb-sib.ch/' target='_blank' " +
                "title='Link to the SIB Swiss Institute of Bioinformatics'>" + 
                "<img id='sib_logo' src='"+this.prop.getImagesRootDirectory()+
                "logo/sib_logo.png' " + 
                "title='Bgee is part of the SIB Swiss Institute of Bioinformatics' " + 
                "alt='SIB Swiss Institute of Bioinformatics' /></a>");
    
        RequestParameters urlDownloadGenerator = this.getNewRequestParameters();
        urlDownloadGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);

        RequestParameters urlDownloadRawDataGenerator = this.getNewRequestParameters();
        urlDownloadRawDataGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadRawDataGenerator.setAction(RequestParameters.ACTION_DOWLOAD_RAW_FILES);
        
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
        urlDownloadFilesDocGenerator.setAction(RequestParameters.ACTION_DOC_DOWLOAD_FILES);

        RequestParameters urlAboutGenerator = this.getNewRequestParameters();
        urlAboutGenerator.setPage(RequestParameters.PAGE_ABOUT);


        // Navigation bar
        this.writeln("<div id='nav'>");
        this.writeln("<ul>");
        this.writeln("<li>");
        this.writeln("<a title='Expression data page' href='" + urlDownloadGenerator.getRequestURL() + 
                "'>Expression data</a>" + this.getCaret());
        this.writeln("<ul>");
        this.writeln("<li><a class='drop' title='Processed raw data' href='" + 
                urlDownloadRawDataGenerator.getRequestURL() + "'>Processed raw data</a></li>");
        this.writeln("<li><a class='drop' title='Gene expression calls' href='" + 
                urlDownloadCallsGenerator.getRequestURL() + "'>Gene expression calls</a></li>");
        this.writeln("</ul>");
        this.writeln("</li>");
        this.writeln("<li>");
        this.writeln("<a title='Documentation page' href='" + urlDocGenerator.getRequestURL() + 
                "'>Documentation</a>" + this.getCaret());
        this.writeln("<ul>");
        this.writeln("<li><a class='drop' title='How to access to Bgee data' href='" + 
                urlBgeeAccessGenerator.getRequestURL() + "'>Bgee data accesses</a></li>");
        this.writeln("<li><a class='drop' title='' href='" + 
                urlDownloadFilesDocGenerator.getRequestURL() + "'>Download files</a></li>");
        this.writeln("</ul>");
        this.writeln("</li>");
//        this.writeln("<li>");
//        this.writeln("<a id='about' rel='help' title='About page' href='" + 
//                urlAboutGenerator.getRequestURL() + "'>About</a>");
//        this.writeln("</li>");
        this.writeln("<li id='contact'>");
        this.writeln(this.getObfuscateEmail(0));
        this.writeln("</li>");

        this.writeln("</ul>");

        this.writeln("<div id='social-links'>");

        this.writeln("<a id='twitter' class='social-link' title='See @Bgeedb account' " +
                "href='https://twitter.com/Bgeedb'>" + 
                "<img alt='Twitter logo' src='" + this.prop.getImagesRootDirectory() + 
                "Twitter.png'></img></a>");
        this.writeln("<a id='wordpress' class='social-link'  alt='' title='See our blog' " + 
                "href='https://bgeedb.wordpress.com'>" + 
                "<img alt='Wordpress logo' src='" + this.prop.getImagesRootDirectory() + 
                "wordpress.png'></img></a>");
        this.writeln("</div>"); // end social-links

        this.writeln("</div>");
        
        this.writeln("</header>");
        log.exit();
    }
    
    /**
     * @param nbCalled  An {@code int} that is the different number every time 
     *                  this method is called per page!
     * @return          the {@code String} that is the HTML code of the Contact link.
     */
    //TODO move javascript in common.js
    //XXX: what's the point of this nbCalled argument?
    private String getObfuscateEmail(int nbCalled) {
        return "<script type=\"text/javascript\">eval(unescape('%66%75%6E%63%74%69%6F%6E%20%74%72%61%6E%73%70%6F%73%65%32%30%28%68%29%20%7B%76%61%72%20%73%3D%27%61%6D%6C%69%6F%74%42%3A%65%67%40%65%73%69%2D%62%69%73%2E%62%68%63%27%3B%76%61%72%20%72%3D%27%27%3B%66%6F%72%28%76%61%72%20%69%3D%30%3B%69%3C%73%2E%6C%65%6E%67%74%68%3B%69%2B%2B%2C%69%2B%2B%29%7B%72%3D%72%2B%73%2E%73%75%62%73%74%72%69%6E%67%28%69%2B%31%2C%69%2B%32%29%2B%73%2E%73%75%62%73%74%72%69%6E%67%28%69%2C%69%2B%31%29%7D%68%2E%68%72%65%66%3D%72%3B%7D%64%6F%63%75%6D%65%6E%74%2E%77%72%69%74%65%28%27%3C%61%20%68%72%65%66%3D%22%23%22%20%6F%6E%4D%6F%75%73%65%4F%76%65%72%3D%22%6A%61%76%61%73%63%72%69%70%74%3A%74%72%61%6E%73%70%6F%73%65%32%30%28%74%68%69%73%29%22%20%6F%6E%46%6F%63%75%73%3D%22%6A%61%76%61%73%63%72%69%70%74%3A%74%72%61%6E%73%70%6F%73%65%32%30%28%74%68%69%73%29%22%20%74%69%74%6C%65%3D%22%43%6F%6E%74%61%63%74%20%75%73%22%20%63%6C%61%73%73%3D%22%6D%65%6E%75%22%3E%43%6F%6E%74%61%63%74%3C%2F%61%3E%27%29%3B'));</script>";
    }

    /**
     * @return  the {@code String} that is the HTML code of the caret in navbar.
     */
    private String getCaret() {
        return "<img class='deploy' src='" + 
                this.prop.getImagesRootDirectory() + "arrow_down_dark.png' alt='deploy'/>";
    }
    
//    @Override
	protected void sendHeaders(boolean ajax) {
	    log.entry(ajax);
		if (this.response == null) {
			log.exit(); return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			log.trace("Set content type text/html");
			if (ajax) {
				this.response.setDateHeader("Expires", 1);
				this.response.setHeader("Cache-Control", 
						"no-store, no-cache, must-revalidate, proxy-revalidate");
				this.response.addHeader("Cache-Control", "post-check=0, pre-check=0");
				this.response.setHeader("Pragma", "No-cache");
			}
			this.headersAlreadySent = true;
		}
		log.exit();
	}

    /**
     * Send the header in case of HTTP 503 error
     */
    public void sendServiceUnavailableHeaders() {
        log.entry();
        if (this.response == null) {
            return;
        }
        if (!this.headersAlreadySent) {
            this.response.setContentType("text/html");
            this.response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            this.headersAlreadySent = true;
        }
        log.exit();
    }
    /**
     * Send the header in case of HTTP 400 error
     */
    protected void sendBadRequestHeaders() {
        log.entry();
        if (this.response == null) {
            return;
        }
        if (!this.headersAlreadySent) {
            this.response.setContentType("text/html");
            this.response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            this.headersAlreadySent = true;
        }
        log.exit();
    }
    /**
     * Send the header in case of HTTP 404 error
     */
    protected void sendPageNotFoundHeaders() {
        log.entry();
        if (this.response == null) {
            return;
        }
        if (!this.headersAlreadySent) {
            this.response.setContentType("text/html");
            this.response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            this.headersAlreadySent = true;
        }
        log.exit();
    }
    /**
     * Send the header in case of HTTP 500 error
     */
    protected void sendInternalErrorHeaders() {
        log.entry();
        if (this.response == null) {
            return;
        }
        if (!this.headersAlreadySent) {
            this.response.setContentType("text/html");
            this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            this.headersAlreadySent = true;
        }
        log.exit();
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
        this.includeJs("common.js");
        this.includeJs("requestparameters.js");
        this.includeJs("urlparameters.js");
//        this.includeJs("bgeeproperties.js");
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
        return log.exit(originalFileName.replaceAll("(.+?)\\.css", 
                "$1" + this.prop.getCssVersionExtension() + ".css"));
    }

    /**
     * Return a new {@code RequestParameters} object to be used to generate URLs. 
     * This new {@code RequestParameters} will use the same {@code URLParameters} 
     * as those returned by {@link #requestParameters} when calling 
     * {@link #getUrlParametersInstance()}, and the {@code BgeeProperties} {@link #prop}. 
     * Also, parameters will be URL encoded, and parameter separator will be {@code &amp;}.
     * 
     * @return  A newly created RequestParameters object.
     */
    protected RequestParameters getNewRequestParameters() {
        log.entry();
        return log.exit(new RequestParameters(this.requestParameters.getUrlParametersInstance(), 
                this.prop, true, "&amp;"));
    }
}
