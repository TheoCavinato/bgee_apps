/**
 * requestParameters handles the parameters that are received and sent through a request.
 * It checks the validity of the parameters (see urlparameters.js), adds/resets/returns
 * parameters and generates URLs.
 * Use new requestParameters(queryString) to create a new instance for every request you have
 * to manage. To create an empty requestParameters,  pass the value ""
 * 
 * @author Mathieu Seppey
 * @author Frederic Bastian
 * @version Bgee 13, Jul 2014
 * @since Bgee 13
 */

function requestParameters(queryString){

    /**
     * Associative array that contains the values for all parameters present in the query
     */
    this.values = null ;
    /**
     * Boolean to tell whether to encode the url or not
     */
    this.encodeUrl = null ;
    /**
     * Initialization
     */
    this.init = function(queryString){

        this.values = new Array();

        this.encodeUrl = bgeeProperties.isEncodeUrl();

        this.loadParametersFromRequest(queryString);
    };
    /**
     * Load the parameters that are present in the provided query string or
     * in the current URL if no string is provided.
     * Note : to load an empty requestParameters, pass the value ""
     * 
     * @param queryString   A {@code String}, the query to load parameters from
     */
    this.loadParametersFromRequest = function(queryString){
        // If no queryString was provided, use the one in the browser URL
        if(! queryString && queryString != ""){
            queryString = window.location.search.substr(1)
        }
        // Browse all available parameters
        var parametersAssocArr = this.getQueryStringVars(queryString,true);
        for (i in urlParameters.getList()){    
            var parameter = urlParameters.getList()[i];
            // Fetch the string values from the URL
            valuesFromUrl = parametersAssocArr[parameter.name]
            // If the param is set, check that the values are correct and store them
            if(valuesFromUrl != null){
                if(!parameter.allowsMultipleValues && valuesFromUrl instanceof Array){
                    throw(parameter.name + " does not accept mutliple values");
                }
                if(valuesFromUrl instanceof Array){
                    this.values[parameter.name] = new Array();
                    for(j in valuesFromUrl){
                        this.values[parameter.name][j] =  this.secureString(valuesFromUrl[j], 
                                parameter);
                    }
                }
                else{
                    this.values[parameter.name] = this.secureString(valuesFromUrl, parameter);
                }

            }
        }
    };
    /**
     * Add a value to the given {@code urlparameters.Parameter}
     * It resets (i.e. remove it) the key ( data ) when a value is added to a storable 
     * parameter. In this case, all the parameters that were formerly included in the key have
     * to be provided again.
     *  
     * @param parameter The {@code urlparameters.Parameter} to add the value to
     * 
     * @param value     The value to set
     */    
    this.addValue = function(parameter, value){
        // Secure the value
        if(value != null){
            value = this.secureString(value, parameter);
        }
        // fetch the existing values for the given parameter and try to add the value
        parameterValues = this.values[parameter.name];
        // Throw an exception if the param does not allow 
        // multiple values and has already one
        if (!parameter.allowsMultipleValues && parameterValues != null){
            throw(parameter.name + " does not accept mutliple values");
        }
        if(parameterValues == undefined){
            parameterValues = new Array();
        }
        if(!(parameterValues instanceof Array)){
            parameterValues = new Array(parameterValues);
        }
        parameterValues.push(value);
        this.values[parameter.name] = parameterValues;
        // Reset the key if the parameter is storable because the key is now obsolete
        if(parameter.isStorable){
            this.values[urlParameters.getParamData().name] = null;
        }
    };
    /**
     * Generate the URL from the current state of the parameters
     * 
     * @param parametersSeparator   A {@code String} that is used as parameters separator in the URL
     *                              
     * @return  A {@code String} that is the generated query
     */
    this.getRequestURL = function(parametersSeparator){
        urlFragment = "";
        // Browse all available parameters
        for (i in urlParameters.getList()){           
            // Fetch the values of this param and generate a query with all
            // its values
            parameter = urlParameters.getList()[i];
            parameterValues = this.values[parameter.name];
            if(!(parameterValues instanceof Array) && parameterValues != undefined){
                parameterValues = new Array(parameterValues);
            }
            if(parameterValues != undefined){
                for(j in parameterValues){
                    urlFragment += parameter.name+ "=";
                    urlFragment += this.urlEncode(parameterValues[j] + parametersSeparator);
                }
            }
        }
        // Remove the extra separator at the end
        paramSeparatorLength = this.urlEncode(parametersSeparator).length;
        if(urlFragment){
            urlFragment = urlFragment.substring(0, urlFragment.length - paramSeparatorLength);
        }
        return urlFragment;
    };
    /**
     * Return the values of the of the given {@code urlparameters.Parameter}
     * It can be an Array or a single value or null if empty
     * 
     * @param parameter     the {@code urlparameters.Parameter} 
     *                      that corresponds to the value to be returned
     *                  
     * @return  the values
     */
    this.getValues = function(parameter){
        if(this.values[parameter.name] != undefined){
            return this.values[parameter.name];
        }
        else{
            return null;
        }
    };
    /**
     * Return the first value of the given {@code urlparameters.Parameter} 
     * or null if it is empty. It is a safe guarantees to get a unique value no matter whether
     * the content is an Array or not.
     * 
     * @param parameter     the {@code urlparameters.Parameter} 
     *                      that corresponds to the value to be returned
     *                  
     * @return  A unique value
     */
    this.getFirstValue = function(parameter){
        if(this.values[parameter.name] != undefined && this.values[parameter.name] instanceof Array){
            return this.values[parameter.name][0];
        }
        else if(this.values[parameter.name] != undefined){
            return this.values[parameter.name];
        }
        else{
            return null;
        }
    };
    /**
     * Reset the value for the given {@code urlparameters.Parameter}
     *  
     * @param parameter The {@code urlparameters.Parameter} to reset
     */
    this.resetValues = function(parameter){
        this.values[parameter.name] = null;
    };
    /**
     * Get parameters from a query string and return them as an associative array.
     * 
     * @param   queryString         a <code>String</code> representing a query string 
     *                              (without the initial '?') 
     *                              to be parsed to extract parameters
     *                              and to return them as an associative array
     * @param   parametersEncoded   a <code>boolean</code> <code>true</code> if the 
     *                              parameters are URL encoded, 
     *                              and need to be decoded
     * @return  an associative array where the keys are parameter names, and values are 
     *          parameter values, 
     *          extracted from the argument <code>queryString</code>
     * source: http://stackoverflow.com/a/3855394
     */
    this.getQueryStringVars = function(queryString, parametersEncoded)
    {
        a = queryString.split('&');
        if (a === "") return {};
        var b = {};
        for (var i = 0; i < a.length; ++i)
        {
            var p=a[i].split('=');
            if (p.length !== 2) continue;
            var valueTemp = p[1];
            if (parametersEncoded === true) {
                valueTemp = decodeURIComponent(p[1].replace(/\+/g, " "));
            }
            var value = valueTemp;
            if (value === "") continue;
            if (b[p[0]] !== undefined) {
                if (typeof b[p[0]] === "string") {
                    var previousValue = b[p[0]];
                    b[p[0]] = new Array();
                    b[p[0]].push(previousValue);
                }
                b[p[0]].push(value);
            } else {
                b[p[0]] = value;
            }
        }
        return b;
    };
    /**
     * Perform security controls and prepare the submitted {@code String} for use. It includes
     * a check of the {@code String} length and the format of the {@code String}.
     * 
     * @param stringToCheck    A {@code String} to be checked 
     * @param parameter        The {@code urlparameters.Parameter} the {@code String} has to be
     *                         secured for. If its maxSize is greater than 0 and if the length
     *                         of {@code stringToCheck} is greater than its maxSize, this method
     *                         returns an empty string. If {@code stringToCheck} is equal to 0,
     *                         no control are performed on string length (but other modifications
     *                         are still performed, such as triming the {@code String}). 
     *                         Furthermore, the format properties of the {@code urlparameters.Parameter}
     *                         contains a regular expression that {@code stringToCheck} should match.
     *                         
     * @return a secured and prepared {@code String}. Return an empty String the stringToCheck
     *         was null
     */
    this.secureString = function(stringToCheck, parameter)
    {                        
        if (stringToCheck == null) {
            return "";
        }
        else if(parameter.maxSize != 0 && stringToCheck.length > parameter.maxSize){
            throw(parameter.name + " is too long");
        }
        else if(parameter.format != null && ! new RegExp(parameter.format).test(stringToCheck)){
            throw(parameter.name + " does not match the required format");
        }
        return String(stringToCheck).trim();
    }
    /**
     * Encode String to be used in URLs. 
     * This method is different from the {@code encodeURL} method 
     * of {@code HttpServletResponse}, as it does not include a logic 
     * for session tracking. It just converts special chars to be used in URL.
     * The encoding can be desactivated by setting the {@code encodeUrl} attribute to
     * {@code false}.
     * 
     * @param url   the {@code String} to be encoded.
     * @return  a {@code String} encoded, if needed (meaning, if including special chars), 
     *          and if the {@code encodeUrl} attribute is {@code true}
     * 
     * @see #encodeUrl
     */
    this.urlEncode = function(url){
        encodeString = url;
        if (!this.encodeUrl) {
            return encodeString;
        }
        encodeString = encodeURIComponent(url);
        return encodeString;
    };
    /**
     * @return  A {@code boolean} to tell whether the display is Xml or not
     */
    this.isXmlDisplayType = function() {
        if(this.getFirstValue(urlParameters.getParamDisplayType()) != null &&
                this.getFirstValue(urlParameters.getParamDisplayType()) == "xml"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the display is Csv or not
     */
    this.isCsvDisplayType = function() {
        if(this.getFirstValue(urlParameters.getParamDisplayType()) != null &&
                this.getFirstValue(urlParameters.getParamDisplayType()) == "csv"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the display is Tsv or not
     */
    this.isTsvDisplayType = function() {
        if(this.getFirstValue(urlParameters.getParamDisplayType()) != null &&
                this.getFirstValue(urlParameters.getParamDisplayType()) == "tsv"){
            return true;
        }
        return false;
    };
    /**
     * Allow to know if this request has been performed through AJAX. 
     * It is currently simply based on the fact that, in Bgee, all AJAX actions 
     * starts by "ajax_". It should be kept that way ;)
     * 
     * @return  {@code true} if this request was performed through AJAX
     */
    this.isAnAjaxRequest = function()
    {
        if (this.getFirstValue(urlParameters.getParamAction()) != null &&
                this.getFirstValue(urlParameters.getParamAction()).toLowerCase()
                .startsWith("ajax_")) {
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the page corresponds to the homepage
     */
    this.isTheHomePage = function(){
        if(this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "about"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "about"
     */
    this.isAnAboutPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "about") {
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "admin"
     */
    this.isAnAdminPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "admin") {
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "anatomy"
     */
    this.isAnAnatomyPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "anatomy") {
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "documentation"
     */
    this.isADocumentationPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "documentation") {
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "download"
     */
    this.isADownloadPageCategory = function(){
        if(this.getFirstValue(urlParameters.getParamPage()) != null &&
                this.getFirstValue(urlParameters.getParamPage()) == "download"){
            return true;
        }
        return false;
    };    
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "expression"
     */
    this.isAnExpressionPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "expression"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "gene"
     */
    this.isAGenePageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "gene"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "gene_family"
     */
    this.isAGeneFamilyPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "gene_family"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "log"
     */
    this.isALogPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) != null &&
                this.getFirstValue(urlParameters.getParamPage()) == "log"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "news"
     */
    this.isANewsPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "news"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "registration"
     */
    this.isARegistrationPageCategory = function()
    {

        if (this.getFirstValue(urlParameters.getParamPage()) != null &&
                this.getFirstValue(urlParameters.getParamPage()) == "registration"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "search"
     */
    this.isASearchPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "search"){
            return true;
        }
        return false;
    };
    /**
     * @return  A {@code boolean} to tell whether the request corresponds to a page of the
     * category "top_anat"
     */
    this.isATopOBOPageCategory = function()
    {
        if (this.getFirstValue(urlParameters.getParamPage()) == null || 
                this.getFirstValue(urlParameters.getParamPage()) == "top_anat"){
            return true;
        }
        return false;
    };
    /**
     * Determine whether the requested page contains sensitive information, 
     * such as passwords.
     * Such pages should then not be cached, or the URL be stored in the database, etc.
     * 
     * @return  {@code true} if the page contains sensitive information, {@code false} otherwise.
     */
    this.isASecuredPage = function() 
    {
        if (this.isALogPageCategory() || this.isARegistrationPageCategory()) {
            return true;
        }
        return false;
    };

    //  Init the instance of this class ( kind of call to the constructor )
    this.init(queryString);

};