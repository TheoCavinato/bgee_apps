package org.bgee.controller.exception;

public class IncorrectRequest extends RuntimeException {

    private static final long serialVersionUID = 92035576451396047L;

    /**
     * Default constructor
     */
    public IncorrectRequest() {
        super();
    }
    /**
     * Constructor with an additional {@code message} argument. 
     * This message must be really basic and understandable, 
     * as it will be displayed to the user. 
     * 
     * The full error message will be generated by the {@code FrontController}. 
     * Only the {@code FrontController} should set a real error message 
     * for this exception.
     * 
     * @param message   a {@code String} giving details about the exception. 
     */
    public IncorrectRequest(String message) {        
        super(message);
    }
    /**
     * Constructor with an additional {@code cause} argument. 
     * 
     * @param cause     a {@code Throwable} giving the cause of the exception.
     */
    public IncorrectRequest(Throwable cause) {
        super(cause);
    }
    /**
     * Constructor with additional {@code message} and {@code cause} arguments. 
     * This message must be really basic and understandable, 
     * as it will be displayed to the user. 
     * 
     * @param message   a {@code String} giving details about the exception.
     * @param cause     a {@code Throwable} giving the cause of the exception.
     */
    public IncorrectRequest(String message, Throwable cause) {
        super(message, cause);
    }
}
