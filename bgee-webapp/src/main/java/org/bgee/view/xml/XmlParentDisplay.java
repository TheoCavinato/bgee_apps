package org.bgee.view.xml;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.ConcreteDisplayParent;
import org.bgee.view.ViewFactory;

/**
 * Parent of all display for the XML view.
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13, Feb. 2016
 * @since   Bgee 13, Feb. 2016
 */
//TODO javadoc
public class XmlParentDisplay extends ConcreteDisplayParent {

    private final static Logger log = LogManager.getLogger(XmlParentDisplay.class.getName());

	protected XmlParentDisplay(HttpServletResponse response, RequestParameters requestParameters, BgeeProperties prop,
			ViewFactory factory) throws IllegalArgumentException, IOException {
		super(response, requestParameters, prop, factory);
	}

	public void sendHeaders(boolean ajax) {
		log.entry(ajax);
		if (!this.headersAlreadySent) {
			this.response.setContentType("text/xml");
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

	protected void startDisplay(boolean ajax) {
		log.entry(ajax);
		this.sendHeaders(ajax);
		this.writeln("<?xml version='1.0' encoding='UTF-8' ?>");
		log.exit();
	}
	
	protected void endDisplay() {
		log.entry();
		log.exit();
	}

	@Override
	protected String getContentType() {
        log.entry();
        return log.exit("text/xml");
	}

	protected static String xmlEntities(String stringToWrite) {
		log.entry();
		try {
    		return log.exit(StringEscapeUtils.escapeHtml4(stringToWrite).replaceAll("'", "&apos;"));
    	} catch (Exception e) {
    		return log.exit("");
    	}
    }
}
