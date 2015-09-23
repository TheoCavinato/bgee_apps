package org.bgee.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.ServiceFactory;
import org.bgee.model.dao.api.DAOManager;
import org.bgee.model.file.SpeciesDataGroup;
import org.bgee.view.DownloadDisplay;
import org.bgee.view.ViewFactory;

/**
 * Controller that handles requests having the category "download", i.e. with the parameter
 * page=download
 * 
 * @author  Mathieu Seppey
 * @author  Valentine Rech de Laval
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
public class CommandDownload extends CommandParent {

    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = LogManager.getLogger(CommandDownload.class.getName());

    /**
     * Constructor
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} that handles the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param viewFactory       A {@code ViewFactory} that provides the display type to be used.
     * @param serviceFactory    A {@code ServiceFactory} that provides bgee services.
     */
    public CommandDownload (HttpServletResponse response, RequestParameters requestParameters, 
            BgeeProperties prop, ViewFactory viewFactory, ServiceFactory serviceFactory) {
        super(response, requestParameters, prop, viewFactory, serviceFactory);
    }

    @Override
    public void processRequest() throws IOException, PageNotFoundException {
        log.entry();

        DownloadDisplay display = this.viewFactory.getDownloadDisplay();
        if (this.requestParameters.getAction() == null) {
            display.displayDownloadHomePage();
        } else if (this.requestParameters.getAction().equals(
                RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES)) {

            display.displayProcessedExpressionValuesDownloadPage(getAllSpeciesDataGroup());
        } else if (this.requestParameters.getAction().equals(
                RequestParameters.ACTION_DOWLOAD_CALL_FILES)) {

            display.displayGeneExpressionCallDownloadPage(getAllSpeciesDataGroup());
        } else {
            throw log.throwing(new PageNotFoundException("Incorrect " + 
                this.requestParameters.getUrlParametersInstance().getParamAction() + 
                " parameter value."));
        }
        
        log.exit();
    }

    /**
     * Gets the {@code SpeciesDataGroup} list that is used to generate the download file views.
     * @return A {@List} of {@code SpeciesDataGroup} to be displayed in the view.
     */
    private List<SpeciesDataGroup> getAllSpeciesDataGroup() {
        log.entry();
        try {
            return log.exit(serviceFactory.getSpeciesDataGroupService().loadAllSpeciesDataGroup());
        } catch (RuntimeException e) {
            log.error(e);
            return log.exit(new LinkedList<>());
        }
    }
}
