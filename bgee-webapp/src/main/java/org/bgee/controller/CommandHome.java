package org.bgee.controller;

/**
 * Controller that handles requests related the home (or default) page
 *  
 * @author 	Mathieu Seppey
 * @version Bgee 13 Jul 2014
 * @since 	Bgee 13
 *
 */

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.ServiceFactory;
import org.bgee.model.file.SpeciesDataGroup;
import org.bgee.view.GeneralDisplay;
import org.bgee.view.ViewFactory;

/**
 * Controller that handles requests for the home page, i.e. without any value for the 
 * parameter page that would lead to other categories.
 * 
 * @author  Mathieu Seppey
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
public class CommandHome extends CommandParent
{

    private final static Logger log = LogManager.getLogger(CommandHome.class.getName());

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
     * @param serviceFactory    A {@code ServiceFactory} that provides the services to be used.
     */
    public CommandHome(HttpServletResponse response, RequestParameters requestParameters,
    		BgeeProperties prop, ViewFactory viewFactory, ServiceFactory serviceFactory) {
        super(response, requestParameters, prop, viewFactory, serviceFactory);
    }

    @Override
    public void processRequest() throws IOException, PageNotFoundException {
        log.entry();
        GeneralDisplay display = this.viewFactory.getGeneralDisplay();
        
        if (requestParameters.isTheHomePage()) {
            List<SpeciesDataGroup> singlesSpeciesGroup = serviceFactory.getSpeciesDataGroupService()
                    .loadAllSpeciesDataGroup()
                    .stream().filter(g -> g.isSingleSpecies())
                    .collect(Collectors.toList());
            display.displayHomePage(singlesSpeciesGroup);
        } else {
            throw new PageNotFoundException();
        }
        log.exit();
    }

}
