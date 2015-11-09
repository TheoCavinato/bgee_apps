package org.bgee.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.controller.exception.MultipleValuesNotAllowedException;
import org.bgee.controller.exception.RequestParametersNotFoundException;
import org.bgee.controller.exception.RequestParametersNotStorableException;
import org.bgee.controller.exception.WrongFormatException;
import org.bgee.controller.servletutils.BgeeHttpServletRequest;
import org.bgee.TestAncestor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link RequestParameters}.
 * It obtains the test parameters from {@link TestURLParameters} that
 * extends {@link URLParameters} 
 * 
 * @author Mathieu Seppey
 * @version Bgee 13
 * @since Bgee 13
 */
public class RequestParametersTest extends TestAncestor {
    
    private final static Logger log = 
            LogManager.getLogger(RequestParametersTest.class.getName());
    
    /**
     * A {@code String} that will be used to define the value of the property with name 
     * {@link BgeeProperties#BGEE_ROOT_DIRECTORY_KEY}.
     */
    private static final String TEST_ROOT_DIR = "testRootDir/";
    
    /**
     * The instance of {@code URLParameters} that provides the parameters
     */
    private static TestURLParameters testURLParameters;

    /**
     * A mock {@code BgeeHttpServletRequest}
     */
    private BgeeHttpServletRequest mockHttpServletRequest;

    /**
     * A {@code String} corresponding to the key generated by {@code RequestParameters} objects 
     * instantiated based on {@link #mockHttpServletRequest} and modified by a call to 
     * {@link #addParamsToExceedThreshold(RequestParameters)}. This key is set by the method 
     * {@link #loadMockRequest()}.
     */
    private String generatedKey;

    /**
     * An instance of {@link RequestParameters} to do the tests on.
     */
    private RequestParameters requestParametersWithNoKey;

    /**
     * An instance of {@link RequestParameters} to do the tests on.
     */
    private RequestParameters requestParametersHavingAKey;

    /**
     * Default Constructor. 
     */
    public RequestParametersTest() {}

    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Load the object that will provide the parameters
     */
    @BeforeClass
    public static void loadParameters() {
        System.getProperties().setProperty(
                BgeeProperties.REQUEST_PARAMETERS_STORAGE_DIRECTORY_KEY,
                System.getProperty("java.io.tmpdir"));
        System.getProperties().setProperty(
                BgeeProperties.URL_MAX_LENGTH_KEY, "120");

        testURLParameters = new TestURLParameters();    

    }

    /**
     * Reset the properties to avoid to disturb other tests
     */
    @AfterClass
    public static void resetProperties(){
        System.clearProperty(BgeeProperties.PROPERTIES_FILE_NAME_KEY);
        System.clearProperty(BgeeProperties.BGEE_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.URL_MAX_LENGTH_KEY);
    }

    /**
     * To do before each test, (re)set the mock 
     * {@code BgeeHttpServletRequest} and the {@link RequestParameters} instances
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotStorableException 
     * @throws RequestParametersNotFoundException 
     * @throws WrongFormatException 
     */
    @Before
    public void loadMockRequest() throws RequestParametersNotFoundException,
    RequestParametersNotStorableException, MultipleValuesNotAllowedException, WrongFormatException{

        mockHttpServletRequest = mock(BgeeHttpServletRequest.class);

        // note : test_string cannot contain uppercase letters to be valid
        when(mockHttpServletRequest.getParameterValues("test_string"))
        .thenReturn(new String[]{"string1"}) // for new RequestParameters
        .thenReturn(new String[]{"string1"}) // for requestParametersWithNoKey
        .thenReturn(new String[]{"string1"}) // for requestParametersHavingAKey
        .thenReturn(new String[]{"string1","explode"}) // for testLoadTooMuchValue
        .thenReturn(new String[]{"STRING1"}); // for testLoadWrongFormatValue

        // note : test_string_list cannot contain uppercase letters to be valid
        when(mockHttpServletRequest.getParameterValues("test_string_list"))
        .thenReturn(new String[]{"s1", "s2"}) // for new RequestParameters
        .thenReturn(new String[]{"s1", "s2"}) // for requestParametersWithNoKey
        .thenReturn(new String[]{"s1", "s2"}); // for requestParametersHavingAKey

        when(mockHttpServletRequest.getParameterValues("test_boolean"))
        .thenReturn(new String[]{"true","false"})
        .thenReturn(new String[]{"true","false"}) // for requestParametersWithNoKey
        .thenReturn(new String[]{"false","true"}); // for requestParametersHavingAKey :
                                                   // wrong values, should not be used if
                                                   // the key is used as expected

        when(mockHttpServletRequest.getParameterValues("test_integer"))
        .thenReturn(new String[]{"1234","2345"})
        .thenReturn(new String[]{"1234","2345"}) // for requestParametersWithNoKey
        .thenReturn(new String[]{"9999","9999"}); // for requestParametersHavingAKey :
                                                  // wrong values, should not be used if
                                                  // the key is used as expected
        
        Properties props = new Properties();
        props.setProperty(BgeeProperties.REQUEST_PARAMETERS_STORAGE_DIRECTORY_KEY,
                System.getProperty("java.io.tmpdir"));
        props.setProperty(BgeeProperties.URL_MAX_LENGTH_KEY, "120");
        //in order to test that generated URLs include the Bgee root directory
        props.setProperty(BgeeProperties.BGEE_ROOT_DIRECTORY_KEY, TEST_ROOT_DIR);
        
        BgeeProperties bgeeProps = BgeeProperties.getBgeeProperties(props);
        
        // To ensure that the key is generated and written on the disk before the tests start,
        // generate a request parameter with an URL exceeding max length, 
        // and call getRequestURL on it.
        // The separator used in the tests is not the default separator "&", but the key 
        // still corresponds to the hash of the value with "&" between parameters.
        // This checks that the key is always generated with "&", no matter which separator
        // is provided
        RequestParameters rp = new RequestParameters(this.mockHttpServletRequest,
                RequestParametersTest.testURLParameters, BgeeProperties.getBgeeProperties(), 
                true, "&");	
        this.addParamsToExceedThreshold(rp);
        rp.getRequestURL("+"); 
        this.generatedKey = rp.getDataKey();

        // the first time, do not return a key, the second time provide a key.
        when(mockHttpServletRequest.getParameter("data"))
        .thenReturn(null)               // for requestParametersWithNoKey
        .thenReturn(this.generatedKey); //for requestParametersHavingAKey
        
        this.requestParametersWithNoKey = new RequestParameters(
                this.mockHttpServletRequest,
                RequestParametersTest.testURLParameters, BgeeProperties.getBgeeProperties(),
                true, "&");

        this.requestParametersHavingAKey= new RequestParameters(
                this.mockHttpServletRequest,
                RequestParametersTest.testURLParameters, BgeeProperties.getBgeeProperties(),
                true, "&");	
    }
    
    /**
     * Add some parameters to a {@code RequestParameters} object instantiated from 
     * {@link #mockHttpServletRequest} in order to exceed the URL length threshold, 
     * and to produce a key corresponding to {@link #generatedKey}.
     * 
     * @param rp    A {@code RequestParameters} object generated based on 
     *              {@link #mockHttpServletRequest}, to be modified so that the generated URL 
     *              exceeds the length threshold, and so that the key parameter is equal to 
     *              {@link #generatedKey}.
     */
    private void addParamsToExceedThreshold(RequestParameters rp) {
        log.entry(rp);
        rp.addValue(testURLParameters.getParamTestInteger(),987654321);
        rp.addValue(testURLParameters.getParamTestInteger(),987654322);
        log.exit();
    }

    /**
     * Test the methods {@link RequestParameters#getRequestURL()} and 
     * {@link RequestParameters#getRequestURL(String)}.
     * 
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotStorableException 
     * @throws RequestParametersNotFoundException 
     * @throws WrongFormatException 
     */
    @Test
    public void testGetRequestURL() throws RequestParametersNotStorableException,
    MultipleValuesNotAllowedException, WrongFormatException{

        // Check that the query returned corresponds to the parameters declared in
        // the mockHttpServletRequest. Do it with the default parameters separator and with
        // a custom one
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1&"
                + "test_string_list=s1,s2&test_integer=1234&"
                + "test_integer=2345&test_boolean=true&test_boolean="
                + "false",this.requestParametersWithNoKey.getRequestURL());

        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1+"
                + "test_string_list=s1,s2+test_integer=1234+"
                + "test_integer=2345+test_boolean=true+test_boolean="
                + "false",this.requestParametersWithNoKey.getRequestURL("+"));

        // Add a parameter value to exceed the threshold over which a key is used
        // (120 for this test, see method loadMockRequest()),
        // and check that indeed, a key is present after a new call of 
        // getRequestURL(), with still test_string written because
        // it is non storable
        this.addParamsToExceedThreshold(this.requestParametersWithNoKey);

        assertEquals("Incorrect query returned ", TEST_ROOT_DIR 
                + "?test_string=string1+test_string_list=s1,s2"
                        + "+data=" + this.generatedKey, 
                        this.requestParametersWithNoKey.getRequestURL("+"));

        // Check that the storable parameters are loaded correctly from the 
        // provided key
        // and that getRequestURL() returns the key correctly with the 
        // non storable parameters as well.
        // the parameter values are defined in the methods loadMockRequest 
        //and addParamsToExceedThreshold
        assertEquals("Parameters are not correctly loaded from the key ", 
                Arrays.asList(1234, 2345, 987654321, 987654322), 
                this.requestParametersHavingAKey.getValues(
                        testURLParameters.getParamTestInteger()));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1"
                        + "+test_string_list=s1,s2+data=" + this.generatedKey, 
                        this.requestParametersHavingAKey.getRequestURL("+"));

    }
    
    /**
     * Test the methods {@link RequestParameters#getRequestURL(Collection, boolean)} and 
     * {@link RequestParameters#getRequestURL(String, Collection, boolean)}.
     * 
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotStorableException 
     * @throws RequestParametersNotFoundException 
     * @throws WrongFormatException 
     */
    @Test
    public void testGetRequestURLWithHash() throws RequestParametersNotStorableException,
    MultipleValuesNotAllowedException, WrongFormatException{
        // Check that the query returned corresponds to the parameters declared in
        // the mockHttpServletRequest, but by requesting some to be in the hash, 
        // in different ways. Do it with the default parameters separator and with
        // a custom one.
        // The URL root directory to use to generate URLs is defined in loadMockRequest().
        Collection<URLParameters.Parameter> params = new HashSet<URLParameters.Parameter>();
        params.add(RequestParametersTest.testURLParameters.getParamTestString());
        params.add(RequestParametersTest.testURLParameters.getParamTestBoolean());
        
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1"
                + "&test_boolean=true&test_boolean=false" 
                + RequestParameters.JS_HASH_SEPARATOR + "test_string_list=s1,s2&test_integer=1234"
                + "&test_integer=2345",this.requestParametersWithNoKey.getRequestURL(
                        params, true));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string_list=s1,s2"
                + "&test_integer=1234&test_integer=2345" + RequestParameters.JS_HASH_SEPARATOR 
                + "test_string=string1&test_boolean=true&test_boolean=false",
                this.requestParametersWithNoKey.getRequestURL(
                        params, false));
        
        //if we provide an empty Collection, we should be able to get either 
        //all params in the search part, or all params in the hash part
        params.clear();
        
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1"
                + "&test_string_list=s1,s2&test_integer="
                + "1234&test_integer=2345&test_boolean=true&test_boolean="
                + "false", this.requestParametersWithNoKey.getRequestURL(
                        params, false));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + RequestParameters.JS_HASH_SEPARATOR 
                + "test_string=string1&test_string_list=s1,s2&test_integer=1234&test_integer=2345"
                + "&test_boolean=true&test_boolean=false", 
                this.requestParametersWithNoKey.getRequestURL(
                        params, true));
        
        //same tests, but with custom separator
        params = new HashSet<URLParameters.Parameter>();
        params.add(RequestParametersTest.testURLParameters.getParamTestString());
        params.add(RequestParametersTest.testURLParameters.getParamTestBoolean());
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1"
                + "+test_boolean=true+test_boolean=false" 
                + RequestParameters.JS_HASH_SEPARATOR + "test_string_list=s1,s2+test_integer=1234"
                + "+test_integer=2345",this.requestParametersWithNoKey.getRequestURL("+", 
                        params, true));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string_list=s1,s2"
                + "+test_integer=1234+test_integer=2345" + RequestParameters.JS_HASH_SEPARATOR 
                + "test_string=string1+test_boolean=true+test_boolean=false",
                this.requestParametersWithNoKey.getRequestURL("+", 
                        params, false));
        
        params.clear();
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + "?test_string=string1"
                + "+test_string_list=s1,s2+test_integer="
                + "1234+test_integer=2345+test_boolean=true+test_boolean="
                + "false", this.requestParametersWithNoKey.getRequestURL("+", 
                        params, false));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + RequestParameters.JS_HASH_SEPARATOR 
                + "test_string=string1+test_string_list=s1,s2+test_integer=1234+test_integer=2345"
                + "+test_boolean=true+test_boolean=false", 
                this.requestParametersWithNoKey.getRequestURL("+", 
                        params, true));
        

        // Add a parameter value to exceed the threshold over which a key is used
        // (120 for this test, see method loadMockRequest()),
        // and check that indeed, a key is present after a new call of 
        // getRequestURL(), with still test_string written because
        // it is non storable, and with parameters attributed to search or hash part
        this.addParamsToExceedThreshold(this.requestParametersWithNoKey);

        params = new HashSet<URLParameters.Parameter>();
        params.add(RequestParametersTest.testURLParameters.getParamData());
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + 
                "?test_string=string1+test_string_list=s1,s2" + RequestParameters.JS_HASH_SEPARATOR 
                + "data=" + this.generatedKey, 
                this.requestParametersWithNoKey.getRequestURL("+", params, false));
        assertEquals("Incorrect query returned ", TEST_ROOT_DIR + 
                "?data=" + this.generatedKey + RequestParameters.JS_HASH_SEPARATOR 
                + "test_string=string1+test_string_list=s1,s2", 
                this.requestParametersWithNoKey.getRequestURL("+", params, true));

        //check that an exception is thrown if setURLHash is used along with setting 
        //data parameters in hash
        this.requestParametersWithNoKey.setURLHash("myHash");
        try {
            this.requestParametersWithNoKey.getRequestURL("+", params, true);
            //test failed
            throw log.throwing(new AssertionError("An exception should have been thrown."));
        } catch (IllegalStateException e) {
            //test passed.
        }
    }
    
    /**
     * Check appropriate behavior of {@code getRequestURL} methods 
     * following a call to {@link RequestParameters#setURLHash(String)}.
     */
    @Test
    public void testSetURLHash() {
        this.requestParametersWithNoKey.setURLHash("myHash1");
        
        assertEquals("Incorrect query returned ", 
                TEST_ROOT_DIR + "?test_string=string1&test_string_list=s1,s2&test_integer="
                + "1234&test_integer=2345&test_boolean=true&test_boolean="
                + "false#myHash1", 
                this.requestParametersWithNoKey.getRequestURL());
        assertEquals("Incorrect query returned ", 
                TEST_ROOT_DIR + "?test_string=string1+test_string_list=s1,s2+test_integer="
                + "1234+test_integer=2345+test_boolean=true+test_boolean="
                + "false#myHash1", 
                this.requestParametersWithNoKey.getRequestURL("+"));
        
       // Add a parameter value to exceed the threshold over which a key is used
        // (120 for this test, see method loadMockRequest()),
        // and check that indeed, a key is present after a new call of 
        // getRequestURL(), with still test_string written because
        // it is non storable
        this.addParamsToExceedThreshold(this.requestParametersWithNoKey);

        assertEquals("Incorrect query returned ", TEST_ROOT_DIR 
                + "?test_string=string1+test_string_list=s1,s2+data=" + this.generatedKey + "#myHash1", 
                        this.requestParametersWithNoKey.getRequestURL("+"));
    }

    /**
     * Test getValues() 
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotStorableException 
     */
    @Test
    public void testGetValues()  {

        // Check for each type that the object returned is a list of the correct
        // data type and that contains the values provided by the request

        List<String> testString = this.requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestString());
        assertEquals("Incorrect list returned ", "string1",
                testString.get(0));

        List<Integer> testInteger = requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestInteger());	
        assertEquals("Incorrect list returned ", (Integer) 1234,
                testInteger.get(0));		
        assertEquals("Incorrect list returned ", (Integer) 2345,
                testInteger.get(1));    

        List<Boolean> testBoolean = requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestBoolean());		
        assertEquals("Incorrect list returned ", true,
                testBoolean.get(0));	       
        assertEquals("Incorrect list returned ", false,
                testBoolean.get(1));

        // Check that getValues return a copy of the list and not 
        // the list itself. As all objects contained within the list should be
        // immutable stuff, there is no need to care whether the content of the 
        // list is a copy or a reference

        List<Boolean> testBoolean2 = requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestBoolean());

        assertFalse("The method did not return a copy of the original object",
                testBoolean == testBoolean2);

        // Test that an empty parameter returns null
        requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestBoolean());

        assertNull("The value returned is not null as expected",
                requestParametersWithNoKey.getValues(
                        testURLParameters.getParamTestBoolean()));

    }

    /**
     * Test getFirstValue()
     */
    @Test
    public void testGetFirstValue() {

        // Test that the value is indeed the first one
        int integerValue = this.requestParametersWithNoKey
                .getFirstValue(testURLParameters.getParamTestInteger());

        assertEquals("Incorrect value returned ",1234,integerValue);

        // Test that an empty parameter return null
        requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestBoolean());

        assertNull("The value returned is not null as expected",
                requestParametersWithNoKey.getFirstValue(
                        testURLParameters.getParamTestBoolean()));

    }

    /**
     * Test addValue()
     * @throws MultipleValuesNotAllowedException 
     * @throws WrongFormatException 
     */
    @Test
    public void testAddValue() throws MultipleValuesNotAllowedException, WrongFormatException {

        // Add two values and check that they are there
        this.requestParametersWithNoKey.addValue(
                testURLParameters.getParamTestInteger(), 20);

        this.requestParametersWithNoKey.addValue(
                testURLParameters.getParamTestInteger(), 21);

        List<Integer> integerList = this.requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestInteger());
        assertEquals("Incorrect list returned after setting the value ", 
                "[1234, 2345, 20, 21]",
                integerList.toString());

        // Check that adding a value on an empty parameter works
        requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestInteger());

        this.requestParametersWithNoKey.addValue(
                testURLParameters.getParamTestInteger(), 21);

        integerList = this.requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestInteger());
        assertEquals("Incorrect list returned after setting the value ", 
                "[21]",
                integerList.toString());

    }

    /**
     * Test addValue() with too much values
     * @throws MultipleValuesNotAllowedException 
     * @throws WrongFormatException 
     */
    @Test (expected=MultipleValuesNotAllowedException.class)
    public void testAddTooMuchValue() throws MultipleValuesNotAllowedException, WrongFormatException {

        // Try to add to much param when it is not allowed,
        // i.e test_string does not allow multiple values => exception
        this.requestParametersWithNoKey.addValue(
                testURLParameters.getParamTestString(), "explode");

    }
    
    /**
     * Test addValues()
     * @throws MultipleValuesNotAllowedException
     * @throws WrongFormatException
     */
    @Test
    public void testAddValues() throws MultipleValuesNotAllowedException, WrongFormatException {

        // Add two values and an empty one, and check that they are there
        this.requestParametersWithNoKey.addValues(
                testURLParameters.getParamTestInteger(), Arrays.asList(21,22));
        
        List<Integer> integerList = this.requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestInteger());
        assertEquals("Incorrect list returned after setting the value ",
                "[1234, 2345, 21, 22]",
                integerList.toString());

        // Check that adding values on an empty parameter works
        requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestInteger());

        this.requestParametersWithNoKey.addValues(
                testURLParameters.getParamTestInteger(), Arrays.asList(21,22));

        integerList = this.requestParametersWithNoKey.getValues(
                testURLParameters.getParamTestInteger());
        assertEquals("Incorrect list returned after setting the value ", 
                "[21, 22]",
                integerList.toString());
    }

    /**
     * Test resetValues() 
     */
    @Test
    public void testResetValues() {
        this.requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestInteger());
        assertNull(this.requestParametersWithNoKey.getFirstValue(
                testURLParameters.getParamTestInteger()));
    }

    /**
     * Test testcloneWithAllParameters()
     * @throws RequestParametersNotStorableException 
     */
    @Test
    public void testCloneWithAllParameters() throws RequestParametersNotStorableException {

        // Test that the cloned parameters has exactly the same behavior as the source
        // when there is no key

        RequestParameters clone = this.requestParametersWithNoKey
                .cloneWithAllParameters();

        assertEquals("Wrong state of the parameters of the cloned object",
                this.requestParametersWithNoKey.getRequestURL("+"),
                clone.getRequestURL("+"));


        // Test that the cloned parameters has exactly the same behavior as the source
        // when there is a key

        RequestParameters clone2 = this.requestParametersHavingAKey
                .cloneWithAllParameters();

        assertEquals("Wrong state of the parameters of the cloned object",
                clone2.getRequestURL("+"),
                this.requestParametersHavingAKey.getRequestURL("+"));

    }

    /**
     * Test cloneWithStorableParameters()
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotFoundException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    @Test
    public void testCloneWithStorableParameters() throws RequestParametersNotStorableException {

        // Test that the cloned parameters return the same storable parameters
        // only as the source when there is no key

        RequestParameters clone = this.requestParametersWithNoKey
                .cloneWithStorableParameters();

        // After cloning, remove the non storable parameter of the source to do the 
        // comparison (as there were already a key, it will continue to use it
        // if even if it is now shorter than the limit)
        this.requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestString());
        this.requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestStringList());
        
        assertEquals("Wrong state of the parameters of the cloned object",
                this.requestParametersWithNoKey.getRequestURL("+"),
                clone.getRequestURL("+"));


        // Test that the cloned parameters return only the same storable parameters
        // as the source when there is a key

        RequestParameters clone2 = this.requestParametersHavingAKey
                .cloneWithStorableParameters();


        // After cloning, remove the non storable parameter of the source to do the 
        // comparison (as there were already a key, it will continue to use it
        // if even if it is now shorter than the limit)
        this.requestParametersHavingAKey.resetValues(
                testURLParameters.getParamTestString());
        this.requestParametersHavingAKey.resetValues(
                testURLParameters.getParamTestStringList());

        assertEquals("Wrong state of the parameters of the cloned object",
                this.requestParametersHavingAKey.getRequestURL("+"),
                clone2.getRequestURL("+")
                );
    }

    /**
     * Test that trying to load too much values for a param that does not
     * allow multiple values throws an exception
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotFoundException 
     * @throws WrongFormatException 
     */
    @Test (expected=MultipleValuesNotAllowedException.class)
    public void testLoadTooMuchValue() throws RequestParametersNotFoundException,
    MultipleValuesNotAllowedException, WrongFormatException{

        // This is actually the forth time a request parameter is instantiated
        // for this test => see the @before loadMockRequest, thus it tries to load 
        // too much params for test_string (string1,explode) => exception

        new RequestParameters(
                this.mockHttpServletRequest,
                RequestParametersTest.testURLParameters,BgeeProperties.getBgeeProperties(),true,"&");
    }

    /**
     * Check that loading a value that does not match the format is not 
     * accepted 
     * @throws MultipleValuesNotAllowedException 
     * @throws RequestParametersNotFoundException 
     * @throws WrongFormatException 
     */
    @Test (expected=WrongFormatException.class)
    public void testLoadWrongFormatValue() throws MultipleValuesNotAllowedException, 
    RequestParametersNotFoundException, WrongFormatException{

        // Load the forth instance of RequestParameters that is used in
        // testLoadTooMuchValue and just ignore the exception that is not
        // interesting here. That's ugly and should be handled in a different
        // way
        try{
            new RequestParameters(
                    this.mockHttpServletRequest,
                    RequestParametersTest.testURLParameters,BgeeProperties.getBgeeProperties(),true,"&");
        }
        catch(Exception e){
            // Do nothing
        }

        // This is the forth time a request parameter is instantiated
        // for this test => see the @before loadMockRequest, thus it tries to load 
        // a wrong params for test_string (STRING1) => exception

        new RequestParameters(
                this.mockHttpServletRequest,
                RequestParametersTest.testURLParameters,BgeeProperties.getBgeeProperties(),
                true,"&");

    }

    /**
     * Check that adding a value that does not match the format is not 
     * accepted 
     * @throws MultipleValuesNotAllowedException 
     * @throws WrongFormatException 
     */
    @Test (expected=WrongFormatException.class)
    public void testAddWrongFormatValue() throws MultipleValuesNotAllowedException, 
    WrongFormatException{

        // test_string does not accept upper case => exception

        this.requestParametersWithNoKey.resetValues(
                testURLParameters.getParamTestString());

        this.requestParametersWithNoKey.addValue(
                testURLParameters.getParamTestString(),"STRING1");

    }

}
