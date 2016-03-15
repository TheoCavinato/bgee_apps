package org.bgee.view.csv;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.view.ConcreteDisplayParent;
import org.bgee.view.ViewFactory;
import org.supercsv.prefs.CsvPreference;

/**
 * Parent class of all CSV views. 
 * 
 * @author  Frederic Bastian
 * @version Bgee 13, Mar. 2016
 * @since   Bgee 13
 */
public class CsvParentDisplay extends ConcreteDisplayParent {
    private final static Logger log = LogManager.getLogger(CsvParentDisplay.class.getName());
    
    /**
     * A {@code CsvPreference} defining preferences to write CSV views.
     */
    protected final CsvPreference csvPref;
    /**
     * A {@code Delimiter} defining the delimiter between columns in CSV views. 
     */
    private final Delimiter delimiter;
    
    protected CsvParentDisplay(HttpServletResponse response, RequestParameters requestParameters, 
            BgeeProperties prop, ViewFactory factory, Delimiter delimiter) 
                    throws IllegalArgumentException, IOException {
        super(response, requestParameters, prop, factory);
        
        CsvPreference pref = null;
        if (Delimiter.TAB.equals(delimiter)) {
            pref = CsvPreference.TAB_PREFERENCE;
        } else if (Delimiter.COMMA.equals(delimiter)) {
            pref = CsvPreference.STANDARD_PREFERENCE;
        } else {
            throw log.throwing(new IllegalStateException("Unsupported delimiter type: " + delimiter));
        }
        this.csvPref = new CsvPreference.Builder(pref).build();
        this.delimiter = delimiter;
    }

    @Override
    protected String getContentType() {
        if (Delimiter.TAB.equals(this.delimiter)) {
            return log.exit("text/tab-separated-values");
        } else if (Delimiter.COMMA.equals(this.delimiter)) {
            return log.exit("text/csv");
        } else {
            throw log.throwing(new IllegalStateException("Unsupported delimiter type: " + this.delimiter));
        }
    }
    

    protected void startDisplay() {
        log.entry();
        this.sendHeaders();
        log.exit();
    }
    protected void endDisplay() {
        log.entry();
        log.exit();
    }
}
