package org.bgee.view.html;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.DownloadDisplay;

/**
 * This class displays the page having the category "download", i.e. with the parameter
 * page=download for the {@code displayTypes} HTML
 * 
 * @author  Mathieu Seppey
 * @author  Valentine Rech de Laval
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
//TODO: all images should have an alt attribute
public class HtmlDownloadDisplay extends HtmlParentDisplay implements DownloadDisplay {
 
    private final static Logger log = LogManager.getLogger(HtmlDownloadDisplay.class.getName());

    /**
     * A {@code String} that is the group name of the pairwise Human/Mouse.
     */
    private final static String GROUP_NAME_HUMAN_MOUSE= "Human/Mouse";
    /**
     * A {@code String} that is the group name of the pairwise Human/Zebrafish.
     */
    private final static String GROUP_NAME_HUMAN_ZEBRAFISH = "Human/Zebrafish";
    /**
     * A {@code String} that is the group name of the pairwise Human/Fruit fly.
     */
    private final static String GROUP_NAME_HUMAN_FRUITFLY = "Human/Fruit fly";
    /**
     * A {@code String} that is the group name of the pairwise Human/Nematode.
     */
    private final static String GROUP_NAME_HUMAN_NEMATODE = "Human/Nematode";
    /**
     * A {@code String} that is the group name of the pairwise Mouse/Zebrafish.
     */
    private final static String GROUP_NAME_MOUSE_ZEBRAFISH= "Mouse/Zebrafish";
    /**
     * A {@code String} that is the group name of the pairwise Mouse/Fruit fly.
     */
    private final static String GROUP_NAME_MOUSE_FRUITFLY = "Mouse/Fruit fly";
    /**
     * A {@code String} that is the group name of the pairwise Mouse/Nematode.
     */
    private final static String GROUP_NAME_MOUSE_NEMATODE = "Mouse/Nematode";
    /**
     * A {@code String} that is the group name of the pairwise Zebrafish/Fruit fly.
     */
    private final static String GROUP_NAME_ZEBRAFISH_FRUITFLY = "Zebrafish/Fruit fly";
    /**
     * A {@code String} that is the group name of the pairwise Zebrafish/Nematode.
     */
    private final static String GROUP_NAME_ZEBRAFISH_NEMATODE = "Zebrafish/Nematode";
    /**
     * A {@code String} that is the group name of the pairwise Fruit fly/Nematode.
     */
    private final static String GROUP_NAME_FRUITFLY_NEMATODE = "Fruit fly/Nematode";
    /**
     * A {@code String} that is the group name of Primates.
     */
    private final static String GROUP_NAME_CATARRHINI = "Catarrhini";
    /**
     * A {@code String} that is the group name of Rodentia.
     */
    private final static String GROUP_NAME_MURINAE = "Murinae";
    /**
     * A {@code String} that is the group name of Theria.
     */
   private final static String GROUP_NAME_THERIA = "Theria";
   /**
    * A {@code String} that is the group name of Mammalia.
    */
    private final static String GROUP_NAME_MAMMALIA = "Mammalia";
    /**
     * A {@code String} that is the group name of Amniota.
     */
    private final static String GROUP_NAME_AMNIOTA = "Amniota";
    /**
     * A {@code String} that is the group name of Bilateria.
     */
    private final static String GROUP_NAME_BILATERIA = "Bilateria";
        
    public enum DownloadPageType {
        RAW_DATA, EXPR_CALLS;
    }
    /**
     * Constructor
     * 
     * @param response          A {@code HttpServletResponse} that will be used to display the 
     *                          page to the client
     * @param requestParameters The {@code RequestParameters} handling the parameters of the 
     *                          current request, to determine the requested displayType, 
     *                          and for display purposes.
     * @param prop              A {@code BgeeProperties} instance that contains the properties
     *                          to use.
     * @throws IOException      If there is an issue when trying to get or to use the
     *                          {@code PrintWriter} 
     */
    public HtmlDownloadDisplay(HttpServletResponse response, RequestParameters requestParameters, 
            BgeeProperties prop) throws IOException {
        super(response,requestParameters, prop);
    }

    @Override
    public void displayDownloadHomePage() {
        log.entry();
        this.startDisplay("download", "Bgee release 13 download pages");

        this.writeln("<h1>Bgee release 13 download page</h1>");

        RequestParameters urlDownloadRawGenerator = this.getNewRequestParameters();
        urlDownloadRawGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadRawGenerator.setAction(RequestParameters.ACTION_DOWLOAD_RAW_FILES);

        RequestParameters urlDownloadCallsGenerator = this.getNewRequestParameters();
        urlDownloadCallsGenerator.setPage(RequestParameters.PAGE_DOWNLOAD);
        urlDownloadCallsGenerator.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);
        
        this.writeln("<div id='feature_list'>");
        
        this.writeln(HtmlParentDisplay.getLogoLink(urlDownloadRawGenerator.getRequestURL(), 
                "Bgee processed raw data page", "Processed raw data", 
                this.prop.getImagesRootDirectory() + "logo/raw_data_logo.png"));

        this.writeln(HtmlParentDisplay.getLogoLink(urlDownloadCallsGenerator.getRequestURL(), 
                "Bgee gene expression call page", "Gene expression calls", 
                this.prop.getImagesRootDirectory() + "logo/expr_calls_logo.png"));

        this.writeln("</div>");
        
        this.endDisplay();

        log.exit();
    }
    
    @Override
    public void displayGeneExpressionCallDownloadPage() {
        log.entry();
        
        this.startDisplay("download", "Bgee release 13 gene expression call download page");

        this.getMoreResultDivs();

        this.writeln("<h1>Gene expression calls</h1>");

        this.writeln("<div class='expr_calls'>");
        
        // Introduction
        this.writeln("<div id='bgee_introduction' class='downloadsection'>");
        this.writeln("<p>Bgee is a database to retrieve and compare gene expression patterns between animal species. ");
        this.writeln("This is a beta download page, more features will be deployed soon. </p>");
        this.writeln("<p>Click on a species to browse files to download. You can also download " +
        //TODO: change this ugly '../' once we'll have added a property to distinguish 
        //FTP root and download_files directory. See todo in BgeeProperties
        		"<a href='" + this.prop.getDownloadRootDirectory() + "../statistics.tsv' " +
        				"title='Database statistics TSV file'>database statistics</a>.</p>");
        this.writeln("<p>See also previous <a href='http://bgee.unil.ch/bgee/bgee'>Bgee release 12</a>.</p>");
        this.writeln("</div>");

        // Search Box
        this.writeln(this.getSearchBox());
        
        // Single species part
        this.writeln(this.getSingleSpeciesSection(DownloadPageType.EXPR_CALLS));

        // Black banner when a species or a group is selected.
        this.writeln(this.getBlackBanner(DownloadPageType.EXPR_CALLS));
        
        // Multi-species part
        this.writeln(this.getMultiSpeciesSection(DownloadPageType.EXPR_CALLS));

        this.writeln("</div>");

        // Image sources
        this.writeln(this.getImageSources());
        
        this.endDisplay();
        
        log.exit();
    }

    @Override
    public void displayProcessedRawDataDownloadPage() {
        log.entry();
        this.startDisplay("download", "Bgee release 13 processed raw data download page");
        
        this.getMoreResultDivs();
    
        this.writeln("<h1>Processed raw data</h1>");
    
        this.writeln("<div class='raw_data'>");
    
        // Introduction
        this.writeln("<div id='bgee_introduction' class='downloadsection'>");
        this.writeln("<p>Bgee is a database to retrieve and compare processed raw data in species. ");
        this.writeln("This is a beta download page, more features will be deployed soon. </p>");
        this.writeln("<p>Click on a species to browse files to download. You can also download " +
        //TODO: change this ugly '../' once we'll have added a property to distinguish 
        //FTP root and download_files directory. See todo in BgeeProperties
                "<a href='" + this.prop.getDownloadRootDirectory() + "../statistics.tsv' " +
                        "title='Database statistics TSV file'>database statistics</a>.</p>");
        this.writeln("<p>See also previous <a href='http://bgee.unil.ch/bgee/bgee'>Bgee release 12</a>.</p>");
        this.writeln("</div>");
    
        // Search Box
        this.writeln(this.getSearchBox());
        
        // Single species part
        this.writeln(this.getSingleSpeciesSection(DownloadPageType.RAW_DATA));

        // Black banner when a species or a group is selected.
        this.writeln(this.getBlackBanner(DownloadPageType.RAW_DATA));
        
        this.writeln("</div>"); // close raw_data div

        // Image sources
        this.writeln(this.getImageSources());
        
        this.endDisplay();
        
        log.exit();
    }

    /**
     * Get the 'More results' of a download page as a HTML 'div' element. 
     *
     * @return  the {@code String} that is the 'More results' HTML 'div' element.
     */
    private String getMoreResultDivs() {
        log.entry();
        
        //TODO: do not use &nbsp;, use css with a left-margin or something
        return log.exit(
                "<div id='bgee_more_results_up'> &uarr;&nbsp;&nbsp;&nbsp;More result(s)</div>" +
                "<div id='bgee_more_results_down'> &darr;&nbsp;&nbsp;&nbsp;More result(s)</div>");
    }

    /**
     * Get the search box of a download page as a HTML 'div' element. 
     *
     * @return  the {@code String} that is the search box as HTML 'div' element.
     */
    private String getSearchBox() {
        log.entry();
    
        return log.exit(
                "<div id='bgee_search_box'>" +
                        "<form action='/' method='get'>" +
                            "<label for='search_label'>Search species</label>&nbsp;&nbsp;" +
                            "<input id='search_label' class='sib_text' type='text' name='search' " +
                                "value='Scientific name, common name...'/>&nbsp;&nbsp;" +
                            "<input type='image' alt='Submit' " +
                                "src='"+this.prop.getImagesRootDirectory()+"submit_button.png'/>" +
                            "<div id='results_nb'></div>" +
                        "</form>" +
                "</div>");
    }

    /**
     * Get the single species section of a download page as a HTML 'div' element,
     * according the provided page type.
     *
     * @param pageType  A {@code DownloadPageType} that is the type of the page.
     * @return          the {@code String} that is the single species section as HTML 'div' element,
     *                  according {@code pageType}.
     */
    private String getSingleSpeciesSection(DownloadPageType pageType) {
        log.entry(pageType);

        StringBuffer s = new StringBuffer(); 
        s.append("<div id='bgee_uniq_species'> ");
        s.append("<h2 class='downloadsection'>Single-species</h2>");
        s.append("<div class='downloadsection'>");
        s.append(generateSpeciesFigure(9606, pageType));
        s.append(generateSpeciesFigure(10090, pageType));
        s.append(generateSpeciesFigure(7955, pageType));
        s.append(generateSpeciesFigure(7227, pageType));
        s.append(generateSpeciesFigure(6239, pageType));
        s.append(generateSpeciesFigure(9597, pageType));
        s.append(generateSpeciesFigure(9598, pageType));
        s.append(generateSpeciesFigure(9593, pageType));
//        s.append(generateSpeciesFigure(9600, pageType)); // no more data for Pongo pygmaeus
        s.append(generateSpeciesFigure(9544, pageType));
        s.append(generateSpeciesFigure(10116, pageType));
        s.append(generateSpeciesFigure(9913, pageType));
        s.append(generateSpeciesFigure(9823, pageType));
        s.append(generateSpeciesFigure(13616, pageType));
        s.append(generateSpeciesFigure(9258, pageType));
        s.append(generateSpeciesFigure(9031, pageType));
        s.append(generateSpeciesFigure(28377, pageType));
        s.append(generateSpeciesFigure(8364, pageType));
//        s.append(generateSpeciesFigure(99883, pageType)); // no more data for Tetraodon nigroviridis
        s.append("</div>");
        s.append("</div>");
        
        return log.exit(s.toString());
    }

    /**
     * Get the multi-species section of a download page as a HTML 'div' element, 
     * according the provided page type.
     *
     * @param pageType  A {@code DownloadPageType} that is the type of the page.
     * @return          the {@code String} that is the multi-species section as HTML 'div' element,
     *                  according {@code pageType}.
     */
    private String getMultiSpeciesSection(DownloadPageType pageType) {
        log.entry();

        StringBuffer s = new StringBuffer(); 
        s.append("<div id='bgee_multi_species'>");
        s.append("<h2 class='downloadsection'>Multi-species</h2> <span>(homologous genes in homologous anatomical structures)</span>");
        s.append("<div class='downloadsection'>");
        //TODO set all groups and with all species when all files will be generated 
        // Pairwises
        s.append(generateSpeciesFigure(Arrays.asList(9606, 10090), GROUP_NAME_HUMAN_MOUSE, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9606, 7955), GROUP_NAME_HUMAN_ZEBRAFISH, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9606, 7227), GROUP_NAME_HUMAN_FRUITFLY, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9606, 6239), GROUP_NAME_HUMAN_NEMATODE, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(10090, 7955), GROUP_NAME_MOUSE_ZEBRAFISH, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(10090, 7227), GROUP_NAME_MOUSE_FRUITFLY, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(10090, 6239), GROUP_NAME_MOUSE_NEMATODE, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(7955, 7227), GROUP_NAME_ZEBRAFISH_FRUITFLY, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(7955, 6239), GROUP_NAME_ZEBRAFISH_NEMATODE, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(7227, 6239), GROUP_NAME_FRUITFLY_NEMATODE, true, pageType));
        // Groups
        //        s.append(generateSpeciesFigure(Arrays.asList(9598, 9597, 9606, 9593, 9544), GROUP_NAME_PRIMATES, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(10090, 10116), GROUP_NAME_RODENTIA, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9598, 9597, 9606, 9593, 9544, 10116, 10090, 9913, 9823, 13616), GROUP_NAME_THERIA, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9598, 9597, 9606, 9593, 9544, 10116, 10090, 9913, 9823, 13616, 9258), GROUP_NAME_MAMMALIA, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9598, 9597, 9606, 9593, 9544, 10116, 10090, 9913, 9823, 13616, 9258, 28377, 9031), GROUP_NAME_AMNIOTA, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9598, 9597, 9606, 9593, 9544, 10116, 10090, 9913, 9823, 13616, 9258, 28377, 9031, 8364, 7955, 7227, 6239), GROUP_NAME_BILATERIA, true, pageType));
        //        s.append(generateSpeciesFigure(Arrays.asList(9606, 9544), GROUP_NAME_CATARRHINI, true, pageType));
        s.append(generateSpeciesFigure(Arrays.asList(10090, 10116), GROUP_NAME_MURINAE, true, pageType));
        s.append(generateSpeciesFigure(Arrays.asList(9606, 9544, 10116, 10090, 9913), GROUP_NAME_THERIA, true, pageType));
        s.append(generateSpeciesFigure(Arrays.asList(9606, 9544, 10116, 10090, 9913, 9258), GROUP_NAME_MAMMALIA, true, pageType));
        s.append(generateSpeciesFigure(Arrays.asList(9606, 9544, 10116, 10090, 9913, 9258, 9031), GROUP_NAME_AMNIOTA, true, pageType));
        s.append(generateSpeciesFigure(Arrays.asList(9606, 9544, 10116, 10090, 9913, 9258, 9031, 8364, 7227), GROUP_NAME_BILATERIA, true, pageType));
        s.append("</div>");
        s.append("</div>");

        return log.exit(s.toString());
    }

    /**
     * Get the images sources of a download page as a HTML 'div' element. 
     *
     * @return  the {@code String} that is the images sources as HTML 'div' element.
     */
    private String getImageSources() {
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

    /**
     * Get the black banner of a download page as a HTML 'div' element, 
     * according the provided page type.
     *
     * @param pageType  A {@code DownloadPageType} that is the type of the page.
     * @return          the {@code String} that is the black banner of a download page 
     *                  as a HTML 'div' element according {@code pageType}.
     */
    // FIXME: change method name, what if tomorrow the banner becomes red?
    private String getBlackBanner(DownloadPageType pageType) {
        log.entry(pageType);
    
        StringBuffer banner = new StringBuffer();
        // This section is empty, it will be filled by JavaScript.
        banner.append("<div id='bgee_data_selection'>");
        
        // Cross to close the banner
        banner.append("<div id='bgee_data_selection_cross'>");
        banner.append("<img src='"+this.prop.getImagesRootDirectory()+"cross.png' "
                + "title='Close banner' alt='Close banner' /> ");
        banner.append("</div>");
        
        // Section on the left of the black banner: image for single species or patchwork for group
        banner.append("<div id='bgee_data_selection_img'></div>");
    
        // Section on the right of the black banner
        banner.append("<div id='bgee_data_selection_text'>");
        banner.append("<h1 class='scientificname'></h1>&nbsp;&nbsp;<h1 class='commonname'></h1>");
        banner.append("<p class='groupdescription'></p>");
        
        if (pageType.equals(DownloadPageType.EXPR_CALLS)) {
            // Presence/absence expression files
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>Presence/absence of expression</h2>");    
            banner.append("<p id='expr_no_data' class='no_data'>Not enough data</p>");
            banner.append("<p id='expr_coming_soon' class='no_data'>Coming soon</p>");
            banner.append("<a id='expr_simple_csv' class='download_link' href='' download></a>");
            banner.append("&nbsp;&nbsp;");
            banner.append("<a id='expr_complete_csv' class='download_link' href='' download></a>");
            banner.append("</div>");
            // Differential expression files across anatomy
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>Over-/Under-expression across anatomy</h2>");
            banner.append("<p id='diffexpr_anatomy_no_data' class='no_data'>Not enough data</p>");
            banner.append("<p id='diffexpr_anatomy_coming_soon' class='no_data'>Coming soon</p>");
            banner.append("<a id='diffexpr_anatomy_simple_csv' class='download_link' href='' download></a>");
            banner.append("&nbsp;&nbsp;");
            banner.append("<a id='diffexpr_anatomy_complete_csv' class='download_link' href='' download></a>");
            banner.append("</div>");
            // Differential expression files across life stages
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>Over-/Under-expression across life stages</h2>");
            banner.append("<p id='diffexpr_development_no_data' class='no_data'>Not enough data</p>");
            banner.append("<p id='diffexpr_development_coming_soon' class='no_data'>Coming soon</p>");
            banner.append("<a id='diffexpr_development_simple_csv' class='download_link' href='' download></a>");
            banner.append("&nbsp;&nbsp;");
            banner.append("<a id='diffexpr_development_complete_csv' class='download_link' href='' download></a>");
            banner.append("</div>");            
        } else {
            // XX
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>AA</h2>");    
            banner.append("<p id='aa' class='no_data'>Coming soon</p>");
            banner.append("</div>");
            // BB
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>BB</h2>");
            banner.append("<p id='bb' class='no_data'>Coming soon</p>");
            banner.append("</div>");
            // CC
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>CC</h2>");
            banner.append("<p id='cc' class='no_data'>Coming soon</p>");
            banner.append("</div>");            
            // DD
            banner.append("<div class='bgee_download_file_buttons'>");
            banner.append("<h2>DD</h2>");
            banner.append("<p id='dd' class='no_data'>Coming soon</p>");
            banner.append("</div>");            
        }
        
        banner.append("</div>");
        banner.append("</div>");
    
        return log.exit(banner.toString());
    }

    /**
     * Generate the HTML figure tag with a figcaption tag from a {@code int} that is a 
     * species ID.
     * 
     * @param speciesId     An {@code int} that is the species ID of the species to be 
     *                      displayed.
     * @param pageType      A {@code DownloadPageType} that is the type of the page.
     * @return              A {@code String} that is the  HTML figure tag generated from the 
     *                      provided {@code int} of a species ID.
     */
    private String generateSpeciesFigure(int speciesId, DownloadPageType pageType) {
        log.entry(speciesId);
        return log.exit(generateSpeciesFigure(Arrays.asList(speciesId), null, false, pageType));
    }

    /**
     * Generate the HTML figure tag from a {@code List} of species IDs.
     * 
     * @param speciesIds   A {@code List} of {@code Integer} containing the species IDs to
     *                     be diplayed.
     * @param figcaption   A {@code String} that is the fieldcaption of the figure. If empty 
     *                     or {@code null}, it's generated with the last species of the 
     *                     {@code List}.
     * @param isGroup      A {@code boolean} that is {@code true} if the figure represents 
     *                     a group of species.
     * @param              A {@code DownloadPageType} that is the type of the page.
     * @return             A {@code String} that is the  HTML figure tag generated from the 
     *                     provided {@code List} of species IDs.
     */
    private String generateSpeciesFigure(List<Integer> speciesIds, String figcaption, 
            boolean isGroup, DownloadPageType pageType) {
        log.entry(speciesIds, figcaption, isGroup, pageType);
        
        StringBuilder images = new StringBuilder();
        if (speciesIds == null || speciesIds.size() == 0) {
            return ("");
        }
        
        String name = null, commonName = null, shortName = null, alternateNames = null;

        // Hidden info, to improve the jQuery search, allow to look for any of the name, short,
        // or common name, even if not displayed... for example droso.
        String hiddenInfo = "";
        for (Integer speciesId: speciesIds) {
            switch(speciesId) {
                case 9606: 
                    name = "Homo sapiens";
                    shortName = "H. sapiens";
                    commonName = "human";
                    alternateNames = "";
                    break;
                case 10090: 
                    name = "Mus musculus";
                    shortName="M. musculus";
                    commonName = "mouse";
                    alternateNames = "house mouse, mice";
                    break;
                case 7955: 
                    name = "Danio rerio";
                    shortName = "D. rerio";
                    commonName = "zebrafish";
                    alternateNames = "leopard danio, zebra danio";
                    break;
                case 7227: 
                    name = "Drosophila melanogaster";
                    shortName = "D. melanogaster";
                    commonName = "fruit fly";
                    alternateNames = "vinegar fly";
                    break;
                case 6239: 
                    name = "Caenorhabditis elegans";
                    shortName = "C. elegans";
                    commonName = "nematode";
                    alternateNames = "worm, roundworm";
                    break;
                case 9597: 
                    name = "Pan paniscus";
                    shortName = "P. paniscus";
                    commonName = "bonobo";
                    alternateNames = "pygmy chimpanzee";
                    break;
                case 9598: 
                    name = "Pan troglodytes";
                    shortName = "P. troglodytes";
                    commonName = "chimpanzee";
                    alternateNames = "";
                    break;
                case 9593: 
                    name = "Gorilla gorilla";
                    shortName = "G. gorilla";
                    commonName = "gorilla";
                    alternateNames = "western gorilla";
                    break;
                case 9600: 
                    name = "Pongo pygmaeus";
                    shortName = "P. pygmaeus";
                    commonName = "orangutan";
                    alternateNames = "orang utan, orang-utan";
                    break;
                case 9544: 
                    name = "Macaca mulatta";
                    shortName = "M. mulatta";
                    commonName = "macaque";
                    alternateNames = "rhesus monkey";
                    break;
                case 10116: 
                    name = "Rattus norvegicus";
                    shortName = "R. norvegicus";
                    commonName = "rat";
                    alternateNames = "brown rat";
                    break;
                case 9913: 
                    name = "Bos taurus";
                    shortName = "B. taurus";
                    commonName = "cattle";
                    alternateNames = "cow, domestic cow, domestic cattle, bovine cow";
                    break;
                case 9823: 
                    name = "Sus scrofa";
                    shortName = "S. scrofa";
                    commonName = "pig";
                    alternateNames = "domestic pig, swine";
                    break;
                case 13616: 
                    name = "Monodelphis domestica";
                    shortName = "M. domestica";
                    commonName = "opossum";
                    alternateNames = "gray short-tailed opossum, gray short tailed opossum";
                    break;
                case 9258: 
                    name = "Ornithorhynchus anatinus";
                    shortName = "O. anatinus";
                    commonName = "platypus";
                    alternateNames = "duckbill platypus, duck-billed platypus";
                    break;
                case 9031: 
                    name = "Gallus gallus";
                    shortName = "G. gallus";
                    commonName = "chicken";
                    alternateNames = "bantam, red junglefowl, red jungle fowl";
                    break;
                case 28377: 
                    name = "Anolis carolinensis";
                    shortName = "A. carolinensis";
                    commonName = "green anole";
                    alternateNames = "anolis, carolina anole";
                    break;
                case 8364: 
                    name = "Xenopus tropicalis";
                    shortName = "X. tropicalis";
                    commonName = "western clawed frog";
                    alternateNames = "xenopus";
                    break;
                case 99883: 
                    name = "Tetraodon nigroviridis";
                    shortName = "T. nigroviridis";
                    commonName = "tetraodon";
                    alternateNames = "spotted green pufferfish";
                    break;
                default:
                    return ("");
            }
            
            if (isGroup) {
                hiddenInfo = hiddenInfo.concat(name + ", " + commonName + ", ");
            } else {
                hiddenInfo = name;
            }
            
            images.append(
                    generateSpeciesImg(speciesId, name, shortName, commonName, alternateNames, true));
        }
        if (StringUtils.isBlank(figcaption)) {
            // If empty or null, it's generated with the last species ID of the given List. 
            figcaption = "<p><i>" + shortName + "</i></p><p>" + commonName + "</p>";   
        }

        String figure = null;
        if (pageType.equals(DownloadPageType.EXPR_CALLS)) {
            if (isGroup) {
                figure = "<figure data-bgeegroupname='" + figcaption + "' " + 
                        this.getGroupFileData(figcaption) + ">";
            } else {
                figure = "<figure " + this.getSingleSpeciesFileData(speciesIds.get(0)) + ">";
            }
        } else if (pageType.equals(DownloadPageType.RAW_DATA)) {
            figure = "<figure>";
        } else {
            //TODO
        }
        

        figure += "<div>" + images + "</div>" +
                  "<figcaption>" + figcaption + 
                  " <span class='invisible'>" + hiddenInfo + "</span>" + 
                  "</figcaption>" + 
                  "</figure>";
        return log.exit(figure);
    }

    /**
     * Get custom data for a group.
     * 
     * @param groupName A {@code String} that is the name of the group.
     * @return          A {@code String} that is data according to the given group name.
     */
    private String getGroupFileData(String groupName) {
        log.entry(groupName);
        
        String diffExprAnatSimpleFileSize = null, diffExprAnatCompleteFileSize = null,
//                exprSimpleFileSize = null, exprCompleteFileSize = null,  
//                diffExprDevSimpleFileSize = null, diffExprDevCompleteFileSize = null, 
                filePrefix = null;

        switch (groupName) {
            //TODO: set file sizes            
            case GROUP_NAME_HUMAN_MOUSE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "1.1 MB";
                diffExprAnatCompleteFileSize  = "5.9 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "human_mouse";
                break;
            case GROUP_NAME_HUMAN_ZEBRAFISH:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "human_zebrafish";
                break;
            case GROUP_NAME_HUMAN_FRUITFLY:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "human_fruitfly";
                break;
            case GROUP_NAME_HUMAN_NEMATODE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "human_nematode";
                break;
            case GROUP_NAME_MOUSE_ZEBRAFISH:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "mouse_zebrafish";
                break;
            case GROUP_NAME_MOUSE_FRUITFLY:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "mouse_fruitfly";
                break;
            case GROUP_NAME_MOUSE_NEMATODE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "mouse_nematode";
                break;
            case GROUP_NAME_ZEBRAFISH_FRUITFLY:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "zebrafish_fruitfly";
                break;
            case GROUP_NAME_ZEBRAFISH_NEMATODE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "zebrafish_nematode";
                break;
            case GROUP_NAME_FRUITFLY_NEMATODE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "fruitfly_nematode";
                break;
            case GROUP_NAME_CATARRHINI:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
//                diffExprAnatSimpleFileSize = "xx MB";
//                diffExprAnatCompleteFileSize  = "xx MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "catarrhini";
                break;
            case GROUP_NAME_MURINAE:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "745 KB";
                diffExprAnatCompleteFileSize  = "3.9 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "murinae";
                break;
            case GROUP_NAME_THERIA:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "2.2 MB";
                diffExprAnatCompleteFileSize  = "12 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "theria";
                break;
            case GROUP_NAME_MAMMALIA:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "1.7 MB";
                diffExprAnatCompleteFileSize  = "9 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "mammalia";
                break;
            case GROUP_NAME_AMNIOTA:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "2.6 MB";
                diffExprAnatCompleteFileSize  = "14 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "amniota";
                break;
            case GROUP_NAME_BILATERIA:
//                exprSimpleFileSize = "xx MB";
//                exprCompleteFileSize = "xx MB"; 
                diffExprAnatSimpleFileSize = "632 KB";
                diffExprAnatCompleteFileSize  = "3.4 MB";
//                diffExprDevSimpleFileSize = "xx MB";
//                diffExprDevCompleteFileSize = "xx MB"; 
                filePrefix= "bilateria";
                break;
            default:
                return ("");
        }
        
        // XXX: use MultiSpeciesDiffExprFileType instead of string?
//        String beginExprFilePath = this.prop.getDownloadMultiExprFilesRootDirectory() + filePrefix + "_";
        String beginDiffExprFilePath = this.prop.getDownloadMultiDiffExprFilesRootDirectory() + filePrefix + "_";
        String extension = ".tsv.zip";
        
        String data = "";
        
//        if (exprSimpleFileSize != null) {
//            data += " data-bgeeexprsimplefileurl='" + beginExprFilePath + 
//                    "multi-expr-simple" + extension +
//                    "' data-bgeeexprsimplefilesize='" + diffExprAnatSimpleFileSize + "'"; 
//        }
//        if (exprCompleteFileSize != null) {
//            data += " data-bgeeexprcompletefileurl='" + beginExprFilePath + 
//                    "multi-expr-complete" + extension +
//                    "' data-bgeeexprcompletefilesize='" + diffExprAnatSimpleFileSize + "'"; 
//        }
        
        if (diffExprAnatSimpleFileSize != null) {
            data += " data-bgeediffexpranatomysimplefileurl='" + beginDiffExprFilePath + 
                    "multi-diffexpr-anatomy-simple" + extension +
                    "' data-bgeediffexpranatomysimplefilesize='" + diffExprAnatSimpleFileSize + "'"; 
        }
        if (diffExprAnatCompleteFileSize != null) {
            data += " data-bgeediffexpranatomycompletefileurl='" + beginDiffExprFilePath + 
                    "multi-diffexpr-anatomy-complete" + extension +
                    "' data-bgeediffexpranatomycompletefilesize='" + diffExprAnatCompleteFileSize + "'"; 
        }
//        if (diffExprDevSimpleFileSize != null) {
//            data += " data-bgeediffexprdevelopmentsimplefileurl='" + beginDiffExprFilePath + 
//                    "multi-diffexpr-development-simple" + extension +
//                    "' data-bgeediffexprdevelopmentsimplefilesize='" + diffExprDevSimpleFileSize + "'"; 
//        }
//        if (diffExprDevCompleteFileSize != null) {
//            data += " data-bgeediffexprdevelopmentcompletefileurl='" + beginDiffExprFilePath + 
//                    "multi-diffexpr-development-complete" + extension +
//                    "' data-bgeediffexprdevelopmentcompletefilesize='" + diffExprDevCompleteFileSize + "'"; 
//        }
        return log.exit(data);
    }
    
    /**
     * Get custom data for a single species.
     * 
     * @param speciesId A {@code String} that is the ID of the species.
     * @return          A {@code String} that is data according to the given species ID.
     */
    private String getSingleSpeciesFileData(int speciesId) {
        log.entry(speciesId);
        
        String exprSimpleFileSize = null, exprCompleteFileSize = null, 
                diffExprAnatSimpleFileSize = null, diffExprAnatCompleteFileSize = null, 
                diffExprDevSimpleFileSize = null, diffExprDevCompleteFileSize = null, 
                latinName = null;

        switch (speciesId) {
            case 9606: 
                exprSimpleFileSize = "85 MB";
                exprCompleteFileSize = "963 MB"; 
                diffExprAnatSimpleFileSize = "4.3 MB";
                diffExprAnatCompleteFileSize  = "24.5 MB";
                diffExprDevSimpleFileSize = "0.7 MB";
                diffExprDevCompleteFileSize = "15.8 MB"; 
                latinName = "Homo_sapiens";
                break;
            case 10090: 
                exprSimpleFileSize = "115 MB";
                exprCompleteFileSize = "1.5 GB"; 
                diffExprAnatSimpleFileSize = "7.8 MB";
                diffExprAnatCompleteFileSize  = "30.9 MB";
                diffExprDevSimpleFileSize = "4.3 MB";
                diffExprDevCompleteFileSize = "33.2 MB"; 
                latinName = "Mus_musculus";
                break;
            case 7955: 
                exprSimpleFileSize = "4.3 MB";
                exprCompleteFileSize = "588 MB"; 
                diffExprDevSimpleFileSize = "0,4 MB";
                diffExprDevCompleteFileSize = "1.3 MB"; 
                latinName = "Danio_rerio";
                break;
            case 7227: 
                exprSimpleFileSize = "4.9 MB";
                exprCompleteFileSize = "730 MB"; 
                diffExprAnatSimpleFileSize = "0.4 MB";
                diffExprAnatCompleteFileSize  = "1.3 MB";
                diffExprDevSimpleFileSize = "0.2 MB";
                diffExprDevCompleteFileSize = "0.8 MB"; 
                latinName = "Drosophila_melanogaster";
                break;
            case 6239: 
                exprSimpleFileSize = "1.2 MB";
                exprCompleteFileSize = "340 MB"; 
                diffExprDevSimpleFileSize = "0.1 MB";
                diffExprDevCompleteFileSize = "1.2 MB"; 
                latinName = "Caenorhabditis_elegans";
                break;
            case 9597: 
                exprSimpleFileSize = "0.7 MB";
                exprCompleteFileSize = "38 MB"; 
                latinName = "Pan_paniscus";
                break;
            case 9598: 
                exprSimpleFileSize = "0.5 MB";
                exprCompleteFileSize = "31 MB"; 
                latinName = "Pan_troglodytes";
                break;
            case 9593: 
                exprSimpleFileSize = "0.5 MB";
                exprCompleteFileSize = "30 MB"; 
                latinName = "Gorilla_gorilla";
                break;
            case 9600: 
                exprSimpleFileSize = "33 MB";
                exprCompleteFileSize = "34 GB"; 
                latinName = "Pongo_pygmaeus";
                break;
            case 9544: 
                exprSimpleFileSize = "1.2 MB";
                exprCompleteFileSize = "112 MB"; 
                diffExprAnatSimpleFileSize = "0.4 MB";
                diffExprAnatCompleteFileSize  = "2.5 MB";
                latinName = "Macaca_mulatta";
                break;
            case 10116: 
                exprSimpleFileSize = "0.8 MB";
                exprCompleteFileSize = "59 MB"; 
                diffExprAnatSimpleFileSize = "0.5 MB";
                diffExprAnatCompleteFileSize  = "1.9 MB";
                latinName = "Rattus_norvegicus";
                break;
            case 9913: 
                exprSimpleFileSize = "0.7 MB";
                exprCompleteFileSize = "58 MB"; 
                diffExprAnatSimpleFileSize = "0.3 MB";
                diffExprAnatCompleteFileSize  = "1.8 MB";
                latinName = "Bos_taurus";
                break;
            case 9823: 
                exprSimpleFileSize = "0.3 MB";
                exprCompleteFileSize = "6.4 MB"; 
                latinName = "Sus_scrofa";
                break;
            case 13616: 
                exprSimpleFileSize = "0.9 MB";
                exprCompleteFileSize = "49 MB"; 
                latinName = "Monodelphis_domestica";
                break;
            case 9258: 
                exprSimpleFileSize = "0.6 MB";
                exprCompleteFileSize = "34 MB"; 
                diffExprAnatSimpleFileSize = "0.2 MB";
                diffExprAnatCompleteFileSize  = "1.2 MB";
                latinName = "Ornithorhynchus_anatinus";
                break;
            case 9031: 
                exprSimpleFileSize = "1 MB";
                exprCompleteFileSize = "55 MB"; 
                diffExprAnatSimpleFileSize = "0.4 MB";
                diffExprAnatCompleteFileSize  = "1.7 MB";
                latinName = "Gallus_gallus";
                break;
            case 28377: 
                exprSimpleFileSize = "0.3 MB";
                exprCompleteFileSize = "19 MB"; 
                latinName = "Anolis_carolinensis";
                break;
            case 8364: 
                exprSimpleFileSize = "2.6 MB";
                exprCompleteFileSize = "287 MB"; 
                diffExprAnatSimpleFileSize = "0.2 MB";
                diffExprAnatCompleteFileSize  = "1 MB";
                diffExprDevSimpleFileSize = "0.1 MB";
                diffExprDevCompleteFileSize = "0.6 MB"; 
                latinName = "Xenopus_tropicalis";
                break;
            case 99883: 
                exprSimpleFileSize = "73 MB";
                exprCompleteFileSize = "74 GB"; 
                latinName = "Tetraodon_nigroviridis";
                break;
            default:
                return ("");
        }
        
        String beginExprFilePath = this.prop.getDownloadExprFilesRootDirectory() + latinName + "_";
        String beginDiffExprFilePath = this.prop.getDownloadDiffExprFilesRootDirectory() + latinName + "_";
        String extension = ".tsv.zip";
        
        String data = " data-bgeeexprsimplefileurl='" + beginExprFilePath + "expr-simple" + extension + 
                "' data-bgeeexprsimplefilesize='" + exprSimpleFileSize + 
                "' data-bgeeexprcompletefileurl='" + beginExprFilePath + "expr-complete" + extension + 
                "' data-bgeeexprcompletefilesize='" + exprCompleteFileSize+ "'";
        
        if (diffExprAnatSimpleFileSize != null) {
            data += " data-bgeediffexpranatomysimplefileurl='" + beginDiffExprFilePath + 
                                    "diffexpr-anatomy-simple" + extension +
                    "' data-bgeediffexpranatomysimplefilesize='" + diffExprAnatSimpleFileSize + "'"; 
        }
        if (diffExprAnatCompleteFileSize != null) {
            data += " data-bgeediffexpranatomycompletefileurl='" + beginDiffExprFilePath + 
                                    "diffexpr-anatomy-complete" + extension +
                    "' data-bgeediffexpranatomycompletefilesize='" + diffExprAnatCompleteFileSize + "'"; 
        }
        if (diffExprDevSimpleFileSize != null) {
            data += " data-bgeediffexprdevelopmentsimplefileurl='" + beginDiffExprFilePath + 
                                    "diffexpr-development-simple" + extension +
                    "' data-bgeediffexprdevelopmentsimplefilesize='" + diffExprDevSimpleFileSize + "'"; 
        }
        if (diffExprDevCompleteFileSize != null) {
            data += " data-bgeediffexprdevelopmentcompletefileurl='" + beginDiffExprFilePath + 
                                    "diffexpr-development-complete" + extension +
                    "' data-bgeediffexprdevelopmentcompletefilesize='" + diffExprDevCompleteFileSize + "'"; 
        }
        return log.exit(data);
    }

    /**
     * Generate the HTML img tag of one species.
     * 
     * @return             A {@code String} that is the  HTML figure tag generated from the 
     *                     provided {@code List} of species IDs
     * @param id           An {@code int} of the species IDs to be diplayed.
     * @param name         A {@code String} that is the species name.
     * @param commonName   A {@code String} that is the species common name.
     * @param lightImg     A {@code boolean} that is {@code true} if the image to use is 
     *                     the light one.
     * @return             A {@code String} that is the  HTML img tag of the provided species 
     *                     data.
     */
    private String generateSpeciesImg(int id, String name, String shortName, 
            String commonName, String alternateNames, boolean lightImg) {
        log.entry(id, name, shortName, commonName, alternateNames, lightImg);
        StringBuilder image = new StringBuilder();
        image.append("<img src='");
        image.append(this.prop.getImagesRootDirectory());
        image.append("species/");
        image.append(id);
        if (lightImg) {
            image.append("_light");
        }
        image.append(".jpg' alt='");
        image.append(name);
        image.append("' data-bgeespeciesid='");
        image.append(id);
        image.append("' data-bgeespeciesname='");
        image.append(name);
        image.append("' data-bgeespeciesshortname='");
        image.append(shortName);
        image.append("' data-bgeespeciescommonname='");
        image.append(commonName);
        image.append("' data-bgeespeciesalternatenames='");
        image.append(alternateNames);
        image.append("' />");
        return log.exit(image.toString());
    }

    @Override
    protected void includeJs() {
        log.entry();
        super.includeJs();
        this.includeJs("download.js");
        log.exit();
    }

    @Override
    protected void includeCss() {
        log.entry();
        super.includeCss();
        this.includeCss("download.css");
        log.exit();
    }
}

