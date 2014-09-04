package org.bgee.view;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;

/**
 * This class exists only to be the parent of {@code TestDownloadDisplay}
 * 
 * @author Mathieu Seppey
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
public class TestParentDisplay extends ConcreteDisplayParent implements DisplayParentInterface
{

	public TestParentDisplay(HttpServletResponse response, RequestParameters requestParameters, 
	        BgeeProperties prop) throws IOException
	{
		super(response,prop);
	}

    @Override
    public void emptyDisplay() {
        // Do nothing  
    }

    @Override
    public void startDisplay(String page, String title) {
        // Do nothing   
    }

    @Override
    public void displayBgeeMenu() {
        // Do nothing   
    }

    @Override
    public void sendHeaders(boolean ajax) {
        // Do nothing
    }

}