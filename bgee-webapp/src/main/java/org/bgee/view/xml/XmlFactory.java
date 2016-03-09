package org.bgee.view.xml;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.AboutDisplay;
import org.bgee.view.DocumentationDisplay;
import org.bgee.view.DownloadDisplay;
import org.bgee.view.ErrorDisplay;
import org.bgee.view.GeneDisplay;
import org.bgee.view.GeneralDisplay;
import org.bgee.view.SearchDisplay;
import org.bgee.view.SpeciesDisplay;
import org.bgee.view.TopAnatDisplay;
import org.bgee.view.ViewFactory;

/**
 * {@code ViewFactory} returning objects generating XML views.
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 13, Feb. 2016
 * @since   Bgee 13
 */
public class XmlFactory extends ViewFactory {
    
    private final static Logger log = LogManager.getLogger(XmlFactory.class.getName());

	public XmlFactory(HttpServletResponse response, RequestParameters requestParameters,
	        BgeeProperties prop) {
		super(response, requestParameters, prop);
	}
	
	@Override
	public DownloadDisplay getDownloadDisplay() {
	    log.entry();
		return log.exit(null);
	}

    @Override
	public GeneralDisplay getGeneralDisplay() {
	    log.entry();
		throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
	}

    @Override
    public ErrorDisplay getErrorDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public DocumentationDisplay getDocumentationDisplay() throws IOException {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public AboutDisplay getAboutDisplay() throws IOException {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public TopAnatDisplay getTopAnatDisplay() throws IOException {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

	@Override
	public GeneDisplay getGeneDisplay() throws IOException {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
	}

    @Override
    public SpeciesDisplay getSpeciesDisplay() throws IOException {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

	@Override
	public SearchDisplay getSearchDisplay() throws IOException {
	    log.entry();
		return log.exit(new XmlSearchDisplay(this.response, this.requestParameters, this.prop, this));
	}
}
