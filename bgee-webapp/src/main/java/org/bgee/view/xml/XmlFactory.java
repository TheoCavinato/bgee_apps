package org.bgee.view.xml;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.*;

/**
 * {@code ViewFactory} returning objects generating XML views.
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 14, May 2019
 * @since   Bgee 13, July 2014
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
    public ErrorDisplay getErrorDisplay() throws IOException {
        return log.exit(new XmlErrorDisplay(this.response, this.requestParameters, this.prop, this));
    }

    @Override
    public DocumentationDisplay getDocumentationDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public AboutDisplay getAboutDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public PrivacyPolicyDisplay getPrivacyPolicyDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public CollaborationDisplay getCollaborationDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public TopAnatDisplay getTopAnatDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

	@Override
	public GeneDisplay getGeneDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
	}

    @Override
    public ExpressionComparisonDisplay getExpressionComparisonDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public RawDataDisplay getRawCallDisplay() throws IOException {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public SpeciesDisplay getSpeciesDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

	@Override
	public SearchDisplay getSearchDisplay() throws IOException {
	    log.entry();
		return log.exit(new XmlSearchDisplay(this.response, this.requestParameters, this.prop, this));
	}

	@Override
    public SparqlDisplay getSparqlDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for JSON display"));
    }
	
    @Override
    public SourceDisplay getSourceDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public DAODisplay getDAODisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public JobDisplay getJobDisplay() {
        log.entry();
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }
    
    @Override
	public RPackageDisplay getRPackageDisplay() {
	    log.entry();
	    throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
	}

    @Override
    public FaqDisplay getFaqDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public ResourcesDisplay getResourceDisplay() throws IOException {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }

    @Override
    public AnatomicalSimilarityDisplay getAnatomicalSimilarityDisplay() {
        throw log.throwing(new UnsupportedOperationException("Not available for XML display"));
    }
}
