package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.ConcreteDisplayParent;
import org.bgee.view.DisplayParentInterface;

public class HtmlParentDisplay extends ConcreteDisplayParent implements DisplayParentInterface
{
	private int uniqueId;
	protected RequestParameters requestParameters;
	protected final static String parametersSeparator = "&amp;";

	public HtmlParentDisplay(HttpServletResponse response, RequestParameters requestParameters) throws IOException
	{
		super(response);
		this.uniqueId = 0;
		this.requestParameters = requestParameters;
	}

	protected int getUniqueId()
	{
		//need to return 0 the first time this method is called;
		int idToReturn = this.uniqueId;
		this.uniqueId++;
		return idToReturn;
	}

	@Override
	public void emptyDisplay()
	{
		this.sendHeaders(true);
		this.writeln("");
	}
	@Override
	public void startDisplay(String page, String title)
	{
		this.writeln("<!DOCTYPE html>");
		this.writeln("<html lang='en'>");
		this.writeln("<head>");
		this.writeln("<meta http-equiv='content-type' content='text/html; utf-8'>");
		this.writeln("<title>"+title+"</title>");
		this.writeln("<meta name='description' content='Bgee allows to automatically"
				+ " compare gene expression patterns between species, by referencing"
				+ " expression data on anatomical ontologies, and designing homology"
				+ " relationships between them.'/>");
		this.writeln("<meta name='keywords' content='bgee, gene expression, "
				+ "evolution, ontology, anatomy, development, evo-devo database, "
				+ "anatomical ontology, developmental ontology, gene expression "
				+ "evolution'/>");
		this.writeln("<meta name='dcterms.rights' content='Bgee copyright 2007/2014 UNIL' />");
		this.writeln("<link rel='shortcut icon' type='image/x-icon' href='"
				+BgeeProperties.getImagesRootDirectory()+"favicon.ico'/>");
		this.writeln("<link rel='stylesheet' type='text/css' href='"
				+BgeeProperties.getCssFilesRootDirectory()+page+".css' />");
		this.writeln("<link rel='stylesheet' type='text/css' href='"
				+BgeeProperties.getCssFilesRootDirectory()+"jquery-ui.css' />");
		this.includeJs();
		this.writeln("</head>");
		this.writeln("<body>");
        this.writeln("<noscript>Sorry, your browser does not support JavaScript!</noscript>");
		this.displayBgeeMenu();
		this.writeln("<div id='bgee_top'><a id='TOP'></a></div>");
        this.writeln("<div id='sib_container'>");
	}

	public void endDisplay()
	{
	    this.writeln("<div id = 'sib_footer'>");
	    this.writeln("<div id = 'sib_footer_content'>");
	    this.writeln("<a href = 'http://www.isb-sib.ch'>Swiss Institute of Bioinformatics</a>");
	    this.writeln("<div id = 'sib_footer_right'>");
	    this.writeln("<a href='#TOP' id = 'sib_footer_gototop'>"
	            + "<span style = 'padding-left: 10px'>Back to the Top</span></a>");
	    this.writeln("</div>");
	    this.writeln("</div>");
	    this.writeln("</div>");
	    this.writeln("</div>");
	    this.writeln("</body>");
	    this.writeln("</html>");
	}

	// TODO move into BgeeStringUtils ??? 
	public static String htmlEntities(String stringToWrite)
	{
		try {							
			// TODO check if new version, HTML5
			return StringEscapeUtils.escapeHtml4(stringToWrite).replaceAll("'", "&apos;");
		} catch (Exception e) {
			return "";
		}
	}

	public String nl2br(String string)
	{
		String localString = string;
		localString = localString.replaceAll("\r\n", "\n");
		localString = localString.replaceAll("\r",   "\n");	
		localString = localString.replaceAll("\n",   "<br/>");
		return localString;
	}

	@Override
	public void sendHeaders(boolean ajax)
	{
		if (this.response == null) {
			return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			if (ajax) {
				this.response.setDateHeader("Expires", 1);
				this.response.setHeader("Cache-Control", 
						"no-store, no-cache, must-revalidate, proxy-revalidate");
				this.response.addHeader("Cache-Control", "post-check=0, pre-check=0");
				this.response.setHeader("Pragma", "No-cache");
			}
			this.headersAlreadySent = true;
		}
	}

	public void sendServiceUnavailableHeaders()
	{
		if (this.response == null) {
			return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			this.response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			this.headersAlreadySent = true;
		}
	}

	protected void sendBadRequestHeaders()
	{
		if (this.response == null) {
			return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			this.response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			this.headersAlreadySent = true;
		}
	}

	protected void sendPageNotFoundHeaders()
	{
		if (this.response == null) {
			return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			this.response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			this.headersAlreadySent = true;
		}
	}

	protected void sendInternalErrorHeaders()
	{
		if (this.response == null) {
			return;
		}
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/html");
			this.response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			this.headersAlreadySent = true;
		}
	}

	public String urlEncode(String string)
	{
		if (string == null) {
			return null;
		}
		String encodeString = string;
		try {
			// warning, you need to add an attribut to the connector in server.xml  
			// in order to get the utf-8 encoding working : URIEncoding="UTF-8"
			encodeString = java.net.URLEncoder.encode(string, "ISO-8859-1");
		} catch (Exception e) {

		}
		return encodeString;
	}

	protected String displayHelpLink(String cat, String display)
	{
		return "<span class='help'><a href='#' class='help|" + 
				cat + "'>" + display + "</a></span>";
	}
	protected String displayHelpLink(String cat)
	{
		return this.displayHelpLink(cat, "[?]");
	}

	@Override
	public void displayBgeeMenu() {
		this.writeln("<div id='sib_header'>");
        // Bgee logo
		this.writeln("<div>");
		this.writeln("<a href='#' id='sib_other_logo' "
		        + "title='Bgee: a dataBase for Gene Expression Evolution'></a>");
        this.writeln("</div>");
        // Title
        this.writeln("<div>");
		this.writeln("<a href='#'>"
		        + "<h1>Bgee: Gene Expression Evolution</h1>"
		        + "<h2>Download release 13</h2></a>");
        this.writeln("</div>");
        // SIB logo
        this.writeln("<div>");
		this.writeln("<a href='http://www.isb-sib.ch/' id='sib_logo' "
		        + "title='SIB Swiss Institute of Bioinformatics'></a>");
        this.writeln("</div>");
        this.writeln("</div>");
	}

	protected void includeJs(){

	}

	protected void includeJs(String filename){
		this.writeln("<script  type='text/javascript' src='"+
		        BgeeProperties.getJavascriptFilesRootDirectory()+filename+"'></script>");
	}

}
