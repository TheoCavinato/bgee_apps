package org.bgee.controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.URLParameters.Parameter;
import org.bgee.controller.exception.MultipleValuesNotAllowedException;
import org.bgee.controller.exception.RequestParametersNotFoundException;
import org.bgee.controller.exception.RequestParametersNotStorableException;
import org.bgee.controller.servletutils.BgeeHttpServletRequest;
import org.bgee.controller.utils.BgeeStringUtils;

/**
 * This class is intended to hold parameters of a query to the server, 
 * and also to generate URLs based on these parameters. 
 * <p>
 * This class can analyze a <code>HttpServletRequest</code> to extract 
 * and secure relevant parameters of a query to the server. 
 * It is also able to generate URLs based on these parameters, 
 * so that all links generated by the View are constructed by this class.
 * <p>
 * The parameters are represented by the class {@link URLParameters.Parameter}
 * and accessible through the class
 * {@link URLParameters} that provides individual access to each
 * {@code URLParameters.Parameter} or a
 * {@code List} that contains all existing {@code URLParameters.Parameter}.
 * These parameters are used by the present class as key to store their values in a 
 * {@code HashMap} 
 * <p>
 * When parameters are too long to be passed through URLs (because exceeding 
 * {@link BgeeProperties#getUrlMaxLength}, 
 * the query string is saved on disk for a later use. This mechanism is used,
 * rather than just putting parameters in session, so that parameters are 
 * indefinitely stored, and can be retrieved through an ID at any time.
 * <p>
 * The idea is: if, through a form, a user submit parameters that can be put in URL, 
 * then so be it. But if the user submit, for instance, a list of thousands of 
 * Ensembl gene IDs, that cannot be passed through URLs because of URLs length
 * limitation, these parameters will be stored on disk: 
 * the query string will be stored in a file, 
 * an ID will be generated to be used as an index to retrieve the file, 
 * and this ID will be passed in URL, 
 * so that the parameters can be retrieved indefinitely. 
 * <p>
 * There are two "big" categories of parameters: "storable" parameters, 
 * that are potentially linked to large data submission, 
 * and "non-storable" parameters, that should never be responsible of large data 
 * submission, and that are meaningful in the URL.
 * Only storable parameters are used to generate IDs, to be stored, and to be 
 * retrieved using the ID provided in the URL. 
 * The properties {@link URLParameters.Parameter#isStorable} tells whether 
 * the parameter is storable or not.
 * 
 * 
 * @author Mathieu Seppey
 * @author Frederic Bastian
 * @version Bgee 13, Jul 2014
 * @since Bgee 1
 */

public class RequestParameters {

	private final static Logger log = LogManager.getLogger(RequestParameters.class.getName());

	/**
	 * A {@code HashMap<URLParameters.Parameter<?>, Object} that store the
	 * values of parameters as an {@code Object} using a 
	 * {URLParameters.Parameter<T>} instance as key
	 */	
	private final HashMap<URLParameters.Parameter<?>, Object> values = 
			new HashMap<URLParameters.Parameter<?>, Object>();

	/**
	 * A <code>boolean</code> defining whether parameters should be url encoded 
	 * by the <code>encodeUrl</code> method.
	 * If <code>false</code>, then the <code>encodeUrl</code> method returns 
	 * Strings with no modifications, otherwise, they are url encoded if needed 
	 * (it does not necessarily mean they will. For index, if there are no 
	 * special chars to encode in the submitted String).
	 * <parameter>
	 * Default value is <code>true</code>.
	 * This parameter is loaded from {@link BgeeProperties}
	 * 
	 * @see #urlEncode(String)
	 */
	private boolean encodeUrl = BgeeProperties.isEncodeUrl();

	/**
	 * A {@code String} that contains the URL corresponding to the present state
	 * of the request. It has to be re-generated every time a parameter is modified
	 */
	private String parametersQuery;

	/**
	 * An instance of {@code URLParameters} that provides all the
	 * {@code URLParameters.Parameter} that can be present in the request. This follows the pattern 
	 * of dependency injection.
	 * 
	 * @see URLParameters
	 */
	private final URLParameters URLParametersInstance;

	/**
	 * <code>ConcurrentMap</code> used to manage concurrent access to 
	 * the read/write locks that are used to manage concurrent reading and writing 
	 * of the files storing query strings holding storable parameters. 
	 * The generated key of the <code>RequestParameters</code> object to be 
	 * loaded or stored is associated to the lock in this <code>Map</code>.
	 * 
	 * @see 	#store()
	 * @see 	#loadStorableParametersFromKey(String)
	 */
	private static final ConcurrentMap<String, ReentrantReadWriteLock> readWriteLocks= 
			new ConcurrentHashMap<String, ReentrantReadWriteLock>();

	/**
	 * Default constructor. 
	 * 
	 * @param 	URLParametersInstance		A instance of {@code URLParameters} that 
	 * 								is injected to provide the available parameters
	 * 								list. 
	 * 
	 * @throws 	RequestParametersNotFoundException		
	 * 								if the parameter used as key is set in the URL, 
	 * 								meaning that a stored query string should be 
	 * 								retrieved using this key, to populate the storable 
	 * 								parameters of this <code>RequestParameters</code> object, 
	 * 								but these parameters could not be found using this key. 
	 * 								See <code>loadStorableParametersFromKey()</code>
	 * @throws RequestParametersNotStorableException 	if an error occur while trying to use the key 
	 * 													or to write the query string in a file
	 * @throws MultipleValuesNotAllowedException 
	 */
	public RequestParameters(URLParameters URLParametersInstance) 
			throws RequestParametersNotFoundException, RequestParametersNotStorableException, 
			MultipleValuesNotAllowedException {
		// call the constructor with an empty request
		this(new BgeeHttpServletRequest(), URLParametersInstance);
	}

	/**
	 * Constructor building a <code>RequestParameters</code> object from a 
	 * <code>HttpServletRequest</code> object.
	 * <parameter>
	 * It means that the parameters are recovered from the query string or posted data.
	 * 
	 * @param 	request 			The HttpServletRequest object corresponding to the current 
	 * 								request to the server.
	 * 
	 * @param 	URLParametersInstance		A instance of {@code URLParameters} that 
	 * 								is injected to provide the available parameters
	 * 								list. 
	 * 
	 * @throws 	RequestParametersNotFoundException		if a <code>generatedKey</code> is set in the URL, 
	 * 								meaning that a stored query string should be retrieved using this key, 
	 * 								to populate the storable parameters of this <code>RequestParameters</code> object, 
	 * 								but these parameters could not be found using this key. 
	 * 								See <code>loadStorableParametersFromKey(HttpServletRequest)</code>
	 *@throws RequestParametersNotStorableException 	if an error occur while trying to use the key 
	 * 													or to write the query string in a file
	 * @throws MultipleValuesNotAllowedException 
	 */
	public RequestParameters(HttpServletRequest request, URLParameters URLParametersInstance) 
			throws RequestParametersNotFoundException, RequestParametersNotStorableException, 
			MultipleValuesNotAllowedException {
		log.entry(request, URLParametersInstance);

		this.URLParametersInstance = URLParametersInstance;

		// Load the parameters
		this.loadParameters(request);

		log.exit();

	}

	/**
	 * Load all the parameters related to the request, based on 
	 * the <code>HttpServletRequest</code> object. 
	 * It uses the parameters present in the request or load them from a file.
	 * If the current request includes a key to retrieve a stored query string, 
	 * the corresponding query string is retrieved from a file named as the key, 
	 * If no key is provided, the storable parameters are simply retrieved from
	 * the current request.
	 * 
	 * @param 	request 	the <code>HttpServletRequest</code> object 
	 * 						representing the current request to the server.
	 * @throws RequestParametersNotStorableException 
	 * @throws MultipleValuesNotAllowedException 
	 * 
	 * @see 	#loadParametersFromRequest
	 * @see		#loadStorableParametersFromKey
	 */
	private void loadParameters(HttpServletRequest request) 
			throws RequestParametersNotFoundException, RequestParametersNotStorableException, 
			MultipleValuesNotAllowedException {

		log.entry(request);

		if (BgeeStringUtils.isBlank(request.getParameter(
				this.getKeyParam().getName()))) {
			log.debug("The key is blank, load params from request");
			//no key set, get the parameters from the URL
			this.loadParametersFromRequest(request, true);
		} else {
			//a key is set, get the storable parameters from a file
			log.debug("The key is set, load params from the file");

			//we need to store the key, 
			//because setting storable parameters reset the generatedKey
			String key = this.getFirstValue(this.getKeyParam());
			try {
				this.loadStorableParametersFromKey(request.getParameter(
						this.getKeyParam().getName()));
			} catch (IOException e) {
				throw new RequestParametersNotFoundException(e);
			}
			//we need to set again the key, 
			//because setting storable parameters reset the key

			this.addValue(this.getKeyParam(), key);

			// load the non storable params
			this.loadParametersFromRequest(request, false);
		}

		log.exit();

	}

	/**
	 * Load the parameters from the <code>HttpServletRequest</code> object 
	 * 
	 * @param request          the <code>HttpServletRequest</code> object representing 
	 *                         the current request to the server.
	 * 
	 * @param loadStorable     a {@code boolean} that indicates whether the storable 
	 *                         parameters have to be loaded from the request. 
	 *                         For example, if the storable parameters were
	 *                         loaded from the key, this method will be called to load
	 *                         the non-storable parameter only.
	 * @throws MultipleValuesNotAllowedException 
	 * @see #loadStorableParametersFromKey
	 * @see #loadParameters
	 */
	private void loadParametersFromRequest(HttpServletRequest request, boolean loadStorable) 
			throws MultipleValuesNotAllowedException {
		log.entry(request, loadStorable);

		// Browse all available parameters
		for (URLParameters.Parameter<?> parameter : this.URLParametersInstance.getList()){	

			// If it is a param that has the desired isStorable status, proceed...
			if (loadStorable || !parameter.isStorable()){
				// Fetch the string values from the URL
				String[] valuesFromUrl = request.getParameterValues(parameter.getName());
				// If the param is set, initialize an List to receive the values 
				// and browse them
				if(valuesFromUrl != null){
					if(!parameter.allowsMultipleValues() && valuesFromUrl.length > 1){
						throw(new MultipleValuesNotAllowedException(parameter.getName()));
					}
					List<Object> parameterValues = new ArrayList<Object>();

					for (String valueFromUrl : valuesFromUrl){
						// Convert the string values into the appropriate type and add it to
						// the list
						// First secure the string
						valueFromUrl = BgeeStringUtils.secureString(valueFromUrl, 
								parameter.getMaxSize(), parameter.getFormat());
						if(parameter.getType().equals(String.class)){
							parameterValues.add(valueFromUrl);
						} else if(parameter.getType().equals(Integer.class)){
							parameterValues.add(Integer.valueOf(valueFromUrl));
						} else if(parameter.getType().equals(Boolean.class)){
							parameterValues.add(Boolean.valueOf(valueFromUrl));
						}
					}
					// store the list of values in the HashMap using
					// the parameter itself as a key
					log.debug("Set {} as values for the param {}", parameterValues, parameter);
					this.values.put(parameter, parameterValues);
				}
			}
		}

		log.exit();

	}

	/**
	 * Load the storable parameters from the file corresponding to the provided key.
	 * <parameter>
	 * If a key is provided, but no stored query string is found corresponding to this key, 
	 * a RequestParametersNotFoundException is thrown.
	 * @throws MultipleValuesNotAllowedException 
	 *
	 * @throws 	IOException
	 * 
	 * @see #loadParameters
	 * @see #loadParametersFromRequest
	 */
	private void loadStorableParametersFromKey(String key) throws IOException, 
	MultipleValuesNotAllowedException {
		log.entry(key);

		ReentrantReadWriteLock lock = this.getReadWriteLock(key);
		try {
			lock.readLock().lock();

			while (readWriteLocks.get(key) == null ||  
					!readWriteLocks.get(key).equals(lock)) {

				lock = this.getReadWriteLock(key);
				lock.readLock().lock();
			}

			try (BufferedReader br = new BufferedReader(new FileReader(
					BgeeProperties.getRequestParametersStorageDirectory() + key))) {
				String retrievedQueryString;
				//just one line in the file, a query string including storable parameters, 
				//that will be used to recover storable parameters
				if ((retrievedQueryString = br.readLine()) != null) {
					//here we create a fake HttpServletRequest using the query 
					// string we retrieved.
					//this way we do not duplicate code to load parameters into 
					// this RequestParameters object.

					BgeeHttpServletRequest request = new BgeeHttpServletRequest(
							retrievedQueryString);
					this.loadParametersFromRequest(request, true);
				}
			}

		} finally {
			lock.readLock().unlock();
			this.removeLockIfPossible(key);
		}

		log.exit();

	}

	/**
	 * Store the part of the query string holding storable parameters into a file: 
	 * get the part of the query string containing "storable" parameters 
	 * (by calling <code>getCompleteStorableParametersQueryString()</code>), 
	 * generate a key based on that string, and store the string in a file named as the key.
	 * This allows to store parameters too lengthy to be put in URL, to replace these parameters 
	 * by the key, which is stored in the {@code URLParameters.Parameter} DATA 
	 * and to store these parameters to retrieve them at later pages 
	 * using that key.
	 * 
	 * @throws RequestParametersNotStorableException 	if an error occur while trying to use the key 
	 * 													or to write the query string in a file
	 * @see #generateParametersQuery()
	 * @see URLParameters#getParamData
	 */
	private void store() throws RequestParametersNotStorableException {
		log.entry();

		if (BgeeStringUtils.isBlank(this.getFirstValue(
				this.getKeyParam()))) {
			throw new RequestParametersNotStorableException("No key generated before storing a "
					+ "RequestParameters object");
		}

		//first check whether these parameters have already been serialized
		File storageFile = new File(BgeeProperties.getRequestParametersStorageDirectory() 
				+ this.getFirstValue(this.getKeyParam()));
		if (storageFile.exists()) {
			//file already exists, no need to continue
			return;
		}

		ReentrantReadWriteLock lock = this.getReadWriteLock(this.getFirstValue(
				this.getKeyParam()));
		try {
			lock.writeLock().lock();

			while (readWriteLocks.get(this.getFirstValue(
					this.getKeyParam())) == null ||  
					!readWriteLocks.get(this.getFirstValue(
							this.getKeyParam())).equals(lock)) {

				lock = this.getReadWriteLock(this.getFirstValue(
						this.getKeyParam()));
				lock.writeLock().lock();
			}

			try (BufferedWriter bufferedWriter = new BufferedWriter(
					new FileWriter(BgeeProperties.getRequestParametersStorageDirectory() 
							+ this.getFirstValue(this.getKeyParam())))) {

				boolean encodeUrlValue = this.encodeUrl;
				this.encodeUrl = false;
				bufferedWriter.write(generateParametersQuery(true, false, "&"));
				this.encodeUrl = encodeUrlValue;
			}

		} catch (IOException e) {
			//delete the file if something went wrong
			storageFile = new File(BgeeProperties.getRequestParametersStorageDirectory() 
					+ this.getFirstValue(this.getKeyParam()));
			if (storageFile.exists()) {
				storageFile.delete();
			}
			throw new RequestParametersNotStorableException(e.getMessage(), e);
		} finally {
			lock.writeLock().unlock();
			this.removeLockIfPossible(this.getFirstValue(
					this.getKeyParam()));
		}

		log.exit();
	}

	/**
	 * Try to remove the <code>ReentrantReadWriteLock</code> corresponding to 
	 * the argument <code>key</code>, from the <code>ConcurrentHashMap</code> 
	 * {@link #readWriteLocks}.
	 * The lock will be removed from the map only if there are no read or write locks, 
	 * and no ongoing request for a read or write lock.
	 * <p>
	 * Note: there might be here a race, where another thread acquired the lock and 
	 * actually locked it, i) just after this method tests the presence of read or write locks 
	 * and ongoing requests for a read or write lock, 
	 * and ii) just before removing it from the map.
	 * To solve this issue, methods acquiring a lock must check after locking it 
	 * whether it is still in the readWriteLocks map, 
	 * or whether the element present in the map for the key is equal to the acquired lock. 
	 * If it is not, they must generate a new lock to be used.
	 * 
	 * @param key 	a <code>String</code> corresponding to the key to retrieve the lock from 
	 * 				<code>readWriteLocks</code>, 
	 * 				to remove it. This key is generated by the method <code>generateKey</code>
	 * @see 		#generateKey(String)
	 * @see 		#readWriteLocks
	 */
	private void removeLockIfPossible(String key) {
		log.entry(key);

		//check if there is already a lock stored for this key
		ReentrantReadWriteLock lock = readWriteLocks.get(key);

		//there is a lock to remove
		if (lock != null) {
			//there is no thread with write lock, or read lock, or waiting to acquire a lock
			if (!lock.isWriteLocked() && lock.getReadLockCount() == 0 && !lock.hasQueuedThreads()) {
				readWriteLocks.remove(key);
			}
		}

		log.exit();
	}

	/**
	 * Obtain a <code>ReentrantReadWriteLock</code>, for the param <code>key</code>.
	 * 
	 * This method tries to obtain <code>ReentrantReadWriteLock</code> corresponding to the key, 
	 * from the <code>ConcurrentHashMap</code> <code>readWriteLocks</code>. 
	 * If the lock is not already stored, 
	 * create a new one, and put it in <code>readWriteLocks</code>, to be used by other threads.
	 * 
	 * @param key 				a <code>String</code> corresponding to the key to retrieve the lock from 
	 * 							<code>readWriteLocks</code>.
	 * 							This key is generated by the method <code>generateKey</code>
	 * @return 					a <code>ReentrantReadWriteLock</code> corresponding to the key.
	 * @see 					#generateKey(String)
	 * @see 					#readWriteLocks
	 */
	private ReentrantReadWriteLock getReadWriteLock(String key) {
		log.entry(key);

		//check if there is already a lock stored for this key
		ReentrantReadWriteLock readWritelock = readWriteLocks.get(key);

		//no lock already stored
		if (readWritelock == null) {
			ReentrantReadWriteLock newReadWriteLock = new ReentrantReadWriteLock(true);
			//try to put the new lock in the ConcurrentHashMap
			readWritelock = readWriteLocks.putIfAbsent(key, newReadWriteLock);
			//if readWritelock is null, the newLock has been successfully put in the map, and we use it.
			//otherwise, it means that another thread has inserted a new lock for this key in the mean time.
			//readWritelock then corresponds to this value, that we should use.
			if (readWritelock == null) {
				readWritelock = newReadWriteLock;
			}
		}

		return log.exit(readWritelock);
	}

	/**
	 * Generate the query from the current state of the parameters 
	 * @param parametersSeparator	A {@code} String that is used as parameters separator in the URL
	 * @throws RequestParametersNotStorableException if an error occur while trying to use the key 
	 * 													or to write the query string in a file
	 * @throws RequestParametersNotStorableException
	 * @throws MultipleValuesNotAllowedException 
	 */
	private void generateParametersQuery(String parametersSeparator) throws 
	RequestParametersNotStorableException,
	MultipleValuesNotAllowedException {
		log.entry();

		// If there is a key already present, continue to work with a key
		if(BgeeStringUtils.isNotBlank(this.getFirstValue(
				this.getKeyParam()))){

			// Regenerate the key in case a storable param has changed
			this.generateKey(this.generateParametersQuery(true, false,parametersSeparator));

			// Regenerate the parameters query, with the non storable that include
			// the key parameter
			this.parametersQuery = generateParametersQuery(false, true,parametersSeparator);


		} else{
			// No key for the moment, generate the query and then evaluate if its
			// length is still under the threshold at which the key is used
			this.parametersQuery = generateParametersQuery(true, true,parametersSeparator);
			if(this.isUrlTooLong()){
				// Generate the key, store the values and regenerate the query
				this.generateKey(this.generateParametersQuery(true, false,parametersSeparator));
				if(BgeeStringUtils.isNotBlank(this.getFirstValue(
						this.getKeyParam()))){
					this.store();
					this.generateParametersQuery(parametersSeparator);
				}
			}	
		}

		log.exit();
	}

	/**
	 * Generate the query from the current state of the parameters and can 
	 * include or not some elements depending on the given params.
	 * @param parametersSeparator	A {@code} String that is used as parameters separator in the URL
	 * @param includeStorable	A {@code boolean} to indicate whether to include
	 * 											 the storable parameters
	 * @param includeNonStorable 	A {@code boolean} to indicate whether to include
	 * 												 the non-storable parameters
	 * @return					A {@code String} that is the generated query
	 */
	private String generateParametersQuery(boolean includeStorable, 
			boolean includeNonStorable, String parametersSeparator){

		log.entry(includeStorable, includeNonStorable);

		String urlFragment = "";

		// Browse all available parameters
		for (URLParameters.Parameter<?> parameter : this.URLParametersInstance.getList()){

			// If it is one of the param to include, proceed...
			if((includeStorable && parameter.isStorable()) || (includeNonStorable 
					&& !parameter.isStorable())){

				// Fetch the values of this param and generate a query with all
				// its values
				List<?> parameterValues = this.getValues(parameter);

				if(parameterValues != null && !parameterValues.isEmpty()){
					for(Object parameterValue : parameterValues){
						if(parameterValue != null && BgeeStringUtils.isNotBlank(
								parameterValue.toString())){
							urlFragment += parameter.getName()+ "=";
							urlFragment += this.urlEncode(parameterValue.toString() 
									+ parametersSeparator);
						}
					}
				}
			}

		}

		// Remove the extra separator at the end 
		if(BgeeStringUtils.isNotBlank(urlFragment)){
			urlFragment = urlFragment.substring(0, urlFragment.length()-1);
		}

		return log.exit(urlFragment);

	}

	/** 
	 * Determine whether the submitted <code>String</code>, representing an URL, 
	 * exceeds the URL length restriction. 
	 * See <code>BgeeProperties#getUrlMaxLength</code> for more details.
	 * 
	 * @return 		<code>true</code> if the <code>String</code>, representing an URL, 
	 * 				exceeds the max allowed URL length.
	 * 				<code>false</code> otherwise.
	 * @see   		BgeeProperties#getUrlMaxLength
	 */
	private boolean isUrlTooLong() {

		log.entry();

		if (this.parametersQuery.length() > BgeeProperties.getUrlMaxLength()) {
			return log.exit(true);
		}
		return log.exit(false);
	}

	/**
	 * Generate a key to set the parameter {@code URLParameters.getParamData}, 
	 * based on <code>urlFragment</code>, 
	 * in order to store this <code>RequestParameters</code> object on the disk
	 * 
	 * 
	 * This key is a hash of an URL fragment generated from the storable attributes
	 * of this object, without any length restriction (all the storable attributes are then represented). 
	 * It will be used as an index to store and retrieve this 
	 * <code>RequestParameters</code> object.
	 * <p>
	 * A new call to this method will then trigger the computation of a new key.
	 * 
	 * @param 	urlFragment 	The fragment of URL based on the storable parameters
	 * @throws RequestParametersNotStorableException 
	 * @throws MultipleValuesNotAllowedException 
	 * @see 	#store()
	 */
	private void generateKey(String urlFragment) throws RequestParametersNotStorableException,
	MultipleValuesNotAllowedException {
		log.entry(urlFragment);

		log.info("Trying to generate a key based on urlFragment: {}", urlFragment);

		if (BgeeStringUtils.isNotBlank(urlFragment)) {
			// Reset the present key and add the new one
			this.resetValues(this.getKeyParam());
			this.addValue(this.getKeyParam(), 
					DigestUtils.sha1Hex(urlFragment.toLowerCase(Locale.ENGLISH)));
		}

		log.info("Key generated: {}", this.getFirstValue(
				this.getKeyParam()));

		log.exit();
	}

	/**
	 * Encode String to be used in URLs. 
	 * <parameter>
	 * This method is different from the <code>encodeURL</code> method 
	 * of <code>HttpServletResponse</code>, as it does not incude a logic 
	 * for session tracking. It just converts special chars to be used in URL.
	 * <parameter>
	 * The encoding can be desactivated by setting the <code>encodeUrl</code> attribute to <code>false</code>.
	 * 
	 * @param url 		the <code>String</code> to be encoded.
	 * @return 			a <code>String</code> encoded, if needed (meaning, if including special chars), 
	 * 					and if the <code>encodeUrl</code> attribute is <code>true</code>
	 * 
	 * @see #encodeUrl
	 */
	private String urlEncode(String url){

		log.entry(url);

		if(this.encodeUrl){
			return BgeeStringUtils.urlEncode(url);
		}
		return log.exit(url);
	}
	
	/**
	 * @return The Parameter<String> that contains the key used to store the storable parameters
	 */
	private Parameter<String> getKeyParam(){
		return this.URLParametersInstance.getParamData();
	}

	/**
	 * @param parametersSeparator	A {@code} String that is used as parameters separator in the URL
	 * @return A String that contains the URL corresponding to the present state of the request. 
	 * It will change every time a parameter is modified
	 * @throws MultipleValuesNotAllowedException 
	 * @throws RequestParametersNotStorableException 
	 */
	public String getRequestURL(String parametersSeparator) throws RequestParametersNotStorableException,
	MultipleValuesNotAllowedException{
		this.generateParametersQuery(parametersSeparator);
		return this.parametersQuery;
	}

	/**
	 * Return all the values for the given {@code URLParameters.Parameter<T>} 
	 * in a {@code List<T>} or null if it is empty
	 * @param parameter the {@code URLParameters.Parameter<T>} that corresponds 
	 * to the values to be returned
	 * @return an {@code List<T>} of values
	 */
	public <T> List<T> getValues(URLParameters.Parameter<T> parameter){

		// Because the data type of URLParameters.Parameter is always checked 
		// when the value is stored, it is safe to not check.
		@SuppressWarnings("unchecked")
		ArrayList<T> values = (ArrayList<T>) this.values.get(parameter);

		try{
			// Return a copy of the list and not the original list
			// As the values contained are only immutable object, such as String
			// Integer, Boolean, Long, there is no need to clone the content
			return new ArrayList<T>(values);

		} catch(NullPointerException e){
			return null;
		}
	}

	/**
	 * Return the first value of the given {@code URLParameters.Parameter<T>} 
	 * or null if it is empty
	 * @param parameter the {@code URLParameters.Parameter<T>} 
	 * that corresponds to the value to be returned
	 * @return a {@code T}, the value
	 */
	@SuppressWarnings("unchecked")	// Because the data type of URLParameters.Parameter
	// is always checked when the value is stored, it should be safe.
	public <T> T getFirstValue(URLParameters.Parameter<T> parameter){

		log.entry(parameter);

		try{
			return log.exit(((List<T>) this.values.get(parameter)).get(0));

		} catch(IndexOutOfBoundsException | NullPointerException e){
			return log.exit(null);			
		}
	}

	/**
	 * Add a value to the given {@code URLParameters.Parameter<T>} 
	 * @param parameter The {@code URLParameters.Parameter<T>} to add the value to
	 * @param value	A {@code T}, the value to set
	 * @throws MultipleValuesNotAllowedException 
	 * @throws RequestParametersNotStorableException 
	 */	
	@SuppressWarnings("unchecked")
	public <T> void addValue(URLParameters.Parameter<T> parameter, T value) 
			throws MultipleValuesNotAllowedException, RequestParametersNotStorableException {
		log.entry(parameter,value);

		// Secure the value
		if(value != null){
		value = (T) BgeeStringUtils.secureString(value.toString(), parameter.getMaxSize(),
				parameter.getFormat());
		}
		
		// fetch the existing values for the given parameter and try to add the value
		List<T> parameterValues = (List<T>) this.values.get(parameter);
		try{
			// Throw an exception if the param does not allow 
			// multiple values and has already one
			if (!parameter.allowsMultipleValues() && parameterValues.get(0) != null){
				throw(new MultipleValuesNotAllowedException(parameter.getName()));
			}
			parameterValues.add(value);
		}
		// If nullpointer, it means that there were no previous values at all, 
		// create the list
		catch(NullPointerException e){
			parameterValues = new ArrayList<T>();
			parameterValues.add(value);
		}

		this.values.put(parameter, parameterValues);
	}

	/**
	 * Reset the value for the given {@code URLParameters.Parameter<T>} 
	 * @param parameter The {@code URLParameters.Parameter<T>} to reset
	 * @throws RequestParametersNotStorableException 
	 * @throws MultipleValuesNotAllowedException 
	 */
	public <T>  void resetValues(URLParameters.Parameter<T> parameter) throws 
	RequestParametersNotStorableException, MultipleValuesNotAllowedException{
		log.entry(parameter);
		this.values.put(parameter, null);
		log.exit();
	}

	/**
	 * Clone this <code>RequestParameters</code> object and return a new one, 
	 * with all parameters copied. 
	 * 
	 * @return 	a new <code>RequestParameters</code> object, 
	 * 			with all parameter copied.
	 * @throws MultipleValuesNotAllowedException 
	 * @throws RequestParametersNotStorableException 
	 * @throws RequestParametersNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public RequestParameters cloneWithAllParameters() throws InstantiationException,
	IllegalAccessException, RequestParametersNotFoundException,
	RequestParametersNotStorableException, MultipleValuesNotAllowedException {
		log.entry();

		//to avoid duplicating methods, 
		//we we simulate a HttpServletRequest with parameters corresponding by a query string we provide 
		//holding storable parameters of this object

		// disable temporarily the url encoding to generate a new BgeeHttpServletRequest using the url
		boolean encodeUrlValue = this.encodeUrl;
		this.encodeUrl = false;
		String queryString = this.generateParametersQuery(true, true, "&");
		BgeeHttpServletRequest request = new BgeeHttpServletRequest(queryString);
		this.encodeUrl = encodeUrlValue;

		RequestParameters clonedRequestParameters = 
				new RequestParameters(request, this.URLParametersInstance.getClass().newInstance());

		return log.exit(clonedRequestParameters);
	}

	/**
	 * Clone this <code>RequestParameters</code> object and return a new one, 
	 * but only with the "storable" parameters and server parameters copied. 
	 * 
	 * @return 	a new <code>RequestParameters</code> object, 
	 * 			with the same this.values for "storable" and server parameters as this one, 
	 * 			but with "non-storable" parameters simply initialized.
	 * @throws RequestParametersNotStorableException 
	 * @throws RequestParametersNotFoundException 
	 * @throws MultipleValuesNotAllowedException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public RequestParameters cloneWithStorableParameters() throws RequestParametersNotFoundException, 
	RequestParametersNotStorableException, MultipleValuesNotAllowedException, 
	InstantiationException, IllegalAccessException {
		log.entry();

		//to avoid duplicating methods, 
		//we we simulate a HttpServletRequest with parameters corresponding by 
		// a query string we provide 
		//holding storable parameters of this object
		// disable temporarily the url encoding to generate a new BgeeHttpServletRequest
		// using the url
		boolean encodeUrlValue = this.encodeUrl;
		this.encodeUrl = false;
		String queryString = this.generateParametersQuery(true, false, "&");
		BgeeHttpServletRequest request = new BgeeHttpServletRequest(queryString);
		this.encodeUrl = encodeUrlValue;

		RequestParameters clonedRequestParameters = new RequestParameters(request, 
				this.URLParametersInstance.getClass().newInstance());

		// Add the key which is not a storable parameters and was not included
		clonedRequestParameters.addValue(this.getKeyParam(), 
				this.getFirstValue(this.getKeyParam()));

		return log.exit(clonedRequestParameters);
	}

	/**
	 * @return An instance of {@code URLParameters} that provides all the
	 * {@code URLParameters.Parameter} that can be present in the request
	 */
	public URLParameters getURLParametersInstance() {
		return URLParametersInstance;
	}

	/**
	 * @return A {@code boolean} to tell whether the display is Xml or not
	 */
	public boolean isXmlDisplayType() {
		if(this.getFirstValue(this.URLParametersInstance.getParamDisplayType()) != null &&
				this.getFirstValue(this.URLParametersInstance.getParamDisplayType()).equals("xml")){
			return true;
		}
		return false;
	}

	/**
	 * @return A {@code boolean} to tell whether the display is Csv or not
	 */
	public boolean isCsvDisplayType() {
		if(this.getFirstValue(this.URLParametersInstance.getParamDisplayType()) != null &&
				this.getFirstValue(this.URLParametersInstance.getParamDisplayType()).equals("csv")){
			return true;
		}
		return false;
	}

	/**
	 * @return A {@code boolean} to tell whether the display is Tsv or not
	 */
	public boolean isTsvDisplayType() {
		if(this.getFirstValue(this.URLParametersInstance.getParamDisplayType()) != null &&
				this.getFirstValue(this.URLParametersInstance.getParamDisplayType()).equals("tsv")){
			return true;
		}
		return false;
	}

	/**
	 * @return A {@code boolean} to tell whether the page corresponds to the homepage
	 */
	public boolean isTheHomePage(){
		if(this.getFirstValue(this.URLParametersInstance.getParamPage()) == null || 
				this.getFirstValue(this.URLParametersInstance.getParamPage()).equals("about")){
			return true;
		}
		return false;
	}

	/**
	 * @return A {@code boolean} to tell whether the page corresponds a download page
	 */
	public boolean isADownloadPageCategory(){

		if(this.getFirstValue(this.URLParametersInstance.getParamPage()) != null &&
				this.getFirstValue(this.URLParametersInstance.getParamPage()).equals("download")){
			return true;
		}
		return false;
	}	

}


