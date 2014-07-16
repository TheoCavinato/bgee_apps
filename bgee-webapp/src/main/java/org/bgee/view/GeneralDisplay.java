package org.bgee.view;


public interface GeneralDisplay extends DisplayParentInterface
{
	public void displayAbout();
	/**
	 * Display an error message if no stored request parameters 
	 * could be found using the key provided by the user (most likely in the URL). 
	 * This display is launched when a <code>RequestParametersNotFoundException</code> is thrown. 
	 * 
	 * See {@link controller.exception.RequestParametersNotFoundException RequestParametersNotFoundException} 
	 * for more details. 
	 * 
	 * @param key 	a <code>String</code> representing the key used when trying to retrieve the stored parameters.
	 * @see controller.exception.RequestParametersNotFoundException
	 */
	public void displayRequestParametersNotFound(String key);
	/**
	 * Display an error message when a <code>PageNotFoundException</code> is thrown 
	 * (basically, a "404 not found"), most likely by a controller that could not understand a query.
	 * 
	 * @param message 	a <code>String</code> providing information about the missing or wrong parameters.
	 * @see controller.exception.PageNotFoundException
	 */
	public void displayPageNotFound(String message);
	/**
	 * Display an error message in an unexpected error occurred.
	 */
	public void displayUnexpectedError();
	/**
	 * Display an error message when the number of parameters is not correct
	 */
	public void displayMultipleParametersNotAllowed(String message);
	/**
	 * Display an error message when the number of parameters is not correct
	 */
	public void displayRequestParametersNotStorable(String message);
}