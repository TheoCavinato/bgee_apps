package org.bgee.controller;

/**
 * Controller that handles requests related to download pages
 *  
 * @author 	Mathieu Seppey
 * @version Bgee 13 Jul 2014
 * @since 	Bgee 13
 *
 */

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.bgee.view.DownloadDisplay;

public class CommandDownload extends CommandParent
{

	public CommandDownload
	(HttpSession session, HttpServletResponse response, RequestParameters requestParameters) 
	{
		super(session, response, requestParameters);
	}

	@Override
	public void processRequest() throws IOException 
	{
       	DownloadDisplay display = this.viewFactory.getDownloadDisplay();

       	display.displayDownloadPage();
	}
}