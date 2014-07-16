package org.bgee.view.html;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bgee.controller.RequestParameters;
import org.bgee.view.DownloadDisplay;
import org.bgee.view.GeneralDisplay;
import org.bgee.view.ViewFactory;


public class HtmlFactory extends ViewFactory
{
	
	public HtmlFactory(HttpServletResponse response, RequestParameters requestParameters)
    {
    	super(response, requestParameters);
    }
    
	@Override
	public DownloadDisplay getDownloadDisplay()  throws IOException
	{
		return new HtmlDownloadDisplay(this.response, this.requestParameters);
	}

	@Override
	public GeneralDisplay getGeneralDisplay() throws IOException {
		return new HtmlGeneralDisplay(this.response, this.requestParameters);
	}
    
}