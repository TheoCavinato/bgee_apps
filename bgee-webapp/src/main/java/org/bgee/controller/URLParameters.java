package org.bgee.controller;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is designed to act like an enum of {@code URLParameter<T>} to provide a specific name to
 * access to every parameters. All {@code URLParameter<T>} to be used in Bgee have to be instantiated 
 * by this class and nowhere else. Note that this class does not store the values contained
 * by a specific occurrence of a parameter and this role is fulfilled by {@link RequestParameters}.
 * <p>
 * The use of a true {@code enum} was not possible along with generics,
 * therefore this class declares a constant for every {@code URLParameter<T>} to makes the individual
 * access possible. It also maintains an {@code ArrayList<URLParameter<T>} to allow operations on all
 * parameters without explicitly mentioning them.
 * <p>
 * This class is not meant to be instantiated and provides only static methods and variables.
 * <p>
 * The constants that correspond to the parameters appear in alphabetical order in the class. 
 * The parameters are stored in the {@code ArrayList<URLParameter<T>} in their order of importance that
 * will define their order in the URL.
 * 
 * @author Mathieu Seppey
 * @version Bgee 13, Jul 2014
 * @since Bgee 13
 * @see URLParameter
 * @see	RequestPararmeters
 */
public class URLParameters {
	
	private final static Logger log = LogManager.getLogger(URLParameters.class.getName());

	// ********************************************************
	//
	//	Constants to provide URLParameter's default values 
	//
	// ********************************************************

	/**
	 * A {@code boolean} that contains the default value to use for
	 * {@link URLParameter#allowsMultipleValues}
	 */
	private static final boolean DEFAULT_ALLOWS_MULTIPLE_VALUES = true ;
	
	/**
	 * A {@code boolean} that contains the default value for {@link URLParameter#isStorable}
	 */
	private static final boolean DEFAULT_IS_STORABLE = true ;
	
	/**
	 * A {@code boolean} that contains the default value for {@link URLParameter#isSecure}
	 */
	private static final boolean DEFAULT_IS_SECURE = false ;
	
	/**
	 * An {@code int} that contains the default value for {@link URLParameter#maxSize}
	 */
	private static final int DEFAULT_MAX_SIZE = 128 ;
	
	/**
	 * A {@code String} that contains the default value for {@link URLParameter#format}
	 */
	private static final String DEFAULT_FORMAT = null ;
	
	// *************************************
	//
	// Parameters declaration
	//
	// Reminder : parameters are declared in alphabetic order in the class but added to 
	// the list according to there desired order in the URL.
	//	
	//
	// !!!! DON'T FORGET TO ADD ANY NEW PARAMS TO ArrayList<URLParameter<?>> list !!!!
	// 
	//	TODO Complete the list and add description for every declared parameters.
	//
	// *************************************
	
	/**
	 * DESCRIPTION PARAM
	 */
	public static final URLParameter<String> ACTION = new URLParameter<String>("action",
			DEFAULT_ALLOWS_MULTIPLE_VALUES, DEFAULT_IS_STORABLE, DEFAULT_IS_SECURE, 
			DEFAULT_MAX_SIZE, DEFAULT_FORMAT,String.class);

	/**
	 * DESCRIPTION PARAM
	 */
	public static final URLParameter<Boolean> ALL_ORGANS = new URLParameter<Boolean>("all_organs",
			DEFAULT_ALLOWS_MULTIPLE_VALUES, DEFAULT_IS_STORABLE, DEFAULT_IS_SECURE, 
			DEFAULT_MAX_SIZE, DEFAULT_FORMAT,Boolean.class);
	
	/**
	 * DESCRIPTION PARAM
	 */	
	public static final URLParameter<Integer> CHOSEN_DATA_TYPE = new URLParameter<Integer>("chosen_data_type",
			DEFAULT_ALLOWS_MULTIPLE_VALUES, DEFAULT_IS_STORABLE, DEFAULT_IS_SECURE, 
			DEFAULT_MAX_SIZE, DEFAULT_FORMAT,Integer.class);
	
	/**
	 * DESCRIPTION PARAM
	 */
	public static final URLParameter<String> EMAIL = new URLParameter<String>("email",
			DEFAULT_ALLOWS_MULTIPLE_VALUES, DEFAULT_IS_STORABLE, DEFAULT_IS_SECURE, 
			DEFAULT_MAX_SIZE, "[\\w\\._-]+@[\\w\\._-]+\\.[a-zA-Z][a-zA-Z][a-zA-Z]?$",String.class);
	
	/**
	 * DESCRIPTION PARAM
	 */	
	public static final URLParameter<Boolean> STAGE_CHILDREN = new URLParameter<Boolean>("stage_children",
			DEFAULT_ALLOWS_MULTIPLE_VALUES, DEFAULT_IS_STORABLE, DEFAULT_IS_SECURE, 
			DEFAULT_MAX_SIZE, DEFAULT_FORMAT,Boolean.class);
	
	/**
	 * An {@code ArrayList<URLParameter<T>>} to list all declared {@code URLParameter<T>}
	 */
	private static final ArrayList<URLParameter<?>> list = new ArrayList<URLParameter<?>>(){

		private static final long serialVersionUID = -2814205136365213851L;

		{
			log.entry();
			// Reminder : parameters are declared in alphabetic order in the class but added to 
			// the list according to there desired order in the URL.
			this.add(ACTION);
			this.add(CHOSEN_DATA_TYPE);
			this.add(STAGE_CHILDREN);
			this.add(ALL_ORGANS);
			this.add(EMAIL);

		}
	};
	
	/**
	 * Private constructor to prevent any instantiation of this class
	 */
	private URLParameters(){}

	/**
	 * @return An {@code ArrayList<URLParameter<T>>} to list all declared {@code URLParameter<T>}
	 */
	public static ArrayList<URLParameter<?>> getList() {
		return list;
	}

}

