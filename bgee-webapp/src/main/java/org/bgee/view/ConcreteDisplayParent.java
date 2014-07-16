package org.bgee.view;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

public abstract class ConcreteDisplayParent 
{
    protected HttpServletResponse response;
	protected PrintWriter out;
	
    protected String serverRoot;
    protected String homePage;
    protected String bgeeRoot;
    protected String downloadRootDirectory;
    protected String emailContact;
    
    protected boolean headersAlreadySent;
    protected boolean displayAlreadyStarted;
    
    
	public ConcreteDisplayParent(HttpServletResponse response) throws IOException
    {
		this.response = response;
			
		if (this.response != null) {
		    this.response.setCharacterEncoding("UTF-8");
		    this.out = this.response.getWriter();
		} 
		
        this.headersAlreadySent = false;
        this.displayAlreadyStarted = false;
    }
	
	public void writeln(String stringToWrite)
	{
		this.out.println(stringToWrite);
	}
	public void write(String stringToWrite)
	{
		this.out.print(stringToWrite);
	}
	
	public abstract void sendHeaders(boolean ajax);
}