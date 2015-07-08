package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.TopAnatDisplay;

/**
 * This class generates the HTML views relative to topAnat.
 * 
 * @author  Frederic Bastian
 * @version Bgee 13, Jul 2015
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
    public void displayTopAnatPage() {
        log.entry();

        this.startDisplay("topAnat: enrichment of gene expression localization");
        
        //AngularJs container
        this.writeln("<div ng-app='angularBasicTest'><!-- AngularJs container -->");
        
        //First basic AngularJS test
        this.writeln("<div ng-controller='PhoneListCtrl'>"
                + "<!-- First basic AngularJS test container-->");
        
        this.writeln("<p>Query input is: {{query}}</p>");
        
        this.writeln("<div class='container-fluid'>");
        this.writeln("<div class='row'>");
        
        this.writeln("<div class='col-md-2'>"
                    + "<!--Sidebar content-->"
                    + "Search: <input ng-model='query'>"
                + "</div>");
        
        this.writeln("<div class='col-md-10'>"
                    + "<!--Body content-->"
                    + "<ul class='phones'>"
                        + "<li ng-repeat='phone in phones | filter:query'>"
                        + "<span>{{phone.name}}</span>"
                        + "<p>{{phone.snippet}}</p>"
                        + "</li>"
                    + "</ul>"
                + "</div>");
        
        this.writeln("</div>");       //end div class='row'
        this.writeln("</div>");   //end div class='container-fluid'
        
        this.writeln("</div>"
            + "<!-- End First basic AngularJS test container-->"); //end First basic AngularJS test
        
        //AngularJs AJAX test container.
        this.writeln("<!-- Example from http://tutorials.jenkov.com/angularjs/ajax.html -->");
        this.writeln("<div ng-controller='ajaxTestCtrl'>");
        this.writeln("<button ng-click='myData.doClick(item, $event)'>Send AJAX Request</button>"
                + "<br/>"
                + "Data from server: {{myData.fromServer}}");
        this.writeln("</div>"); //end AngularJs AJAX test container

        this.writeln("</div><!-- End AngularJs container -->"); //AngularJs container

        this.endDisplay();
        
        log.exit();
    }


    @Override
    protected void includeJs() {
        log.entry();
        super.includeJs();
        this.includeJs("topanat.js");
        log.exit();
    }
    @Override
    protected void includeCss() {
        log.entry();
        super.includeCss();
        this.includeCss("topanat.css");
        log.exit();
    }
}
