package org.bgee.view.html;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.model.TaskManager;
import org.bgee.view.TopAnatDisplay;

/**
 * This class generates the HTML views relative to topAnat.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, Nov 2015
 * @since   Bgee 13
 */
public class HtmlTopAnatDisplay extends HtmlParentDisplay implements TopAnatDisplay {
    
    private final static Logger log = LogManager.getLogger(HtmlTopAnatDisplay.class.getName());

    /**
     * Constructor providing the necessary dependencies. 
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} handling the parameters of the 
     *                          current request.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @param factory           The {@code HtmlFactory} that instantiated this object.
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter} 
     */
    public HtmlTopAnatDisplay(HttpServletResponse response,
            RequestParameters requestParameters, BgeeProperties prop,
            HtmlFactory factory) throws IllegalArgumentException, IOException {
        super(response, requestParameters, prop, factory);
    }

    @Override
    public void displayTopAnatHomePage() {
        log.entry();
        
        this.startDisplay("Bgee TopAnat page");
        
        
        //AngularJS module container
        this.writeln("<div ng-app='app'>");

        
        this.writeln("<div id='appLoading' class='loader'>" + 
        		"<!-- BEGIN: Actual animated container. -->" + 
        		"<div class='anim-cover'>" + 
        			"<div class='messaging'>" +
        				"<h1>" + 
        					"<li class='fa fa-circle-o-notch fa-spin'></li> TopAnat is Loading" +
        				"</h1>" + 
        			"</div>" + 
        		"</div>" +
        	"</div>");
    
        this.writeln("<!--[if lt IE 7]>" +
        "<p class='browsehappy'>You are using an <strong>outdated</strong> browser. Please <a href='http://browsehappy.com/'>upgrade your" +
            "browser</a> to improve your experience.</p>" +
        "<![endif]-->");

        //FB: I really hate this ribbon :p
        //this.writeln("<div class='corner-ribbon top-left sticky red shadow'>Beta</div>");
        
        this.writeln("<div style='margin-left: 20px; margin-right: 20px' ng-view=''>" +

        "</div>");

        //End AngularJS module container
        this.writeln("</div>");


        
        this.endDisplay();

        log.exit();
    }
    
    @Override
    public void sendResultResponse(LinkedHashMap<String, Object> data, String msg) {
        throw log.throwing(new UnsupportedOperationException("Not available for HTML display"));
    }

    @Override
    public void sendTrackingJobResponse(LinkedHashMap<String, Object> data, String msg) {
        throw log.throwing(new UnsupportedOperationException("Not available for HTML display"));
    }

    @Override
    public void sendJobErrorResponse(TaskManager taskManager) {
        throw log.throwing(new UnsupportedOperationException("Not available for HTML display"));
    }

    @Override
    public void sendGeneListReponse(LinkedHashMap<String, Object> data, String msg) {
        throw log.throwing(new UnsupportedOperationException("Not available for HTML display"));
    }

    @Override
    public void sendTopAnatParameters(String hash) {
        throw log.throwing(new UnsupportedOperationException("Not available for HTML display"));
    }

    @Override
    protected void includeJs() {
        log.entry();
        super.includeJs();
        
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        this.includeJs(
            Arrays.asList("vendor_topanat.js", "script_topanat.js"), 
            Arrays.asList(
                //external libs used only by TopAnat
                "lib/angular.min.js", 
                "lib/angular_modules/angular-animate.min.js", 
                "lib/angular_modules/angular-messages.min.js", 
                "lib/angular_modules/angular-resource.min.js", 
                "lib/angular_modules/angular-route.min.js", 
                "lib/angular_modules/angular-sanitize.min.js", 
                "lib/angular_modules/angular-touch.min.js", 
                "lib/angular_modules/ui_modules/ui-grid.min.js", 
                "lib/angular_modules/ui_modules/ui-bootstrap-tpls.min.js", 
                "lib/jquery_plugins/bootstrap.min.js", 
                "lib/angular_modules/angular-file-upload.min.js", 
                "lib/jquery_plugins/toastr.min.js", 
                "lib/angular_modules/angular-location-update.min.js", 
                "lib/Blob.js", 
                "lib/FileSaver.min.js", 
                "lib/angular_modules/angular-file-saver.bundle.min.js", 
            
                //TopAnat JS files
                "topanat/topanat.js", 
                "topanat/services/logger.module.js", 
                "topanat/services/logger.js", 
                "topanat/controllers/main.js", 
                "topanat/controllers/result.js", 
                "topanat/services/bgeedataservice.js", 
                "topanat/services/bgeejobservice.js", 
                "topanat/services/helpservice.js", 
                "topanat/services/datatypefactory.js", 
                "topanat/services/config.js", 
                "topanat/services/lang.js", 
                "topanat/services/constants.js", 
                "topanat/directives/loading.js"), 
            null);
        
        log.exit();
    }
    @Override
    protected void includeCss() {
        log.entry();
        
        //the CSS files need to keep their relative location to other paths the same, 
        //this is why we keep their location and don't merge them all. 
        //And all merged css files are already included by super.includeCss().
        //If you ever add new files, you need to edit bgee-webapp/pom.xml 
        //to correctly merge/minify them.
        this.includeCss(
                Arrays.asList("lib/jquery_plugins/vendor_common.css", 
                        //CSS files of AngularJS modules only used by TopAnat
                        "lib/angular_modules/ui_grid/ui-grid.css", 
                        //font-awesome
                        "lib/font_awesome/css/font-awesome.css", 
                        //CSS files specific to TopAnat
                        "topanat.css"), 
                Arrays.asList(
                        "lib/jquery_plugins/bootstrap.min.css", 
                        "lib/jquery_plugins/jquery-ui.min.css", 
                        "lib/jquery_plugins/jquery-ui.structure.min.css", 
                        "lib/jquery_plugins/jquery-ui.theme.min.css", 
                        "lib/jquery_plugins/toastr.min.css", 
                        //CSS files of AngularJS modules only used by TopAnat
                        "lib/angular_modules/ui_grid/ui-grid.min.css", 
                        //font-awesome
                        "lib/font_awesome/css/font-awesome.min.css", 
                        //CSS files specific to TopAnat
                        "topanat.css")); 
        
        //we need to add the Bgee CSS files at the end, to override CSS file from external libs
        super.includeCss();
        
        log.exit();
    }

}
