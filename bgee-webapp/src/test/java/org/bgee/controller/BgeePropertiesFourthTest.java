package org.bgee.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for {@link BgeeProperties}.
 * It checks that the properties are loaded from the correct source
 * These tests are split in several test classes to avoid conflicts between tests due to
 * the per-thread singleton behavior.
 * 
 * @author Mathieu Seppey
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 * @see BgeePropertiesParentTest
 * @see BgeePropertiesFirstTest
 * @see BgeePropertiesSecondTest
 * @see BgeePropertiesThirdTest
 * @see BgeePropertiesFourthTest
 */
public class BgeePropertiesFourthTest extends BgeePropertiesParentTest {
    
    /**
     * Test that the {@code java.util.Properties} are loaded using the default values
     */
    @Test
    public void testLoadDefaultProperties(){
        // First clear the system properties that would be used if present.
        System.clearProperty(BgeeProperties.BGEE_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.URL_MAX_LENGTH_KEY);
        System.clearProperty(BgeeProperties.REQUEST_PARAMETERS_STORAGE_DIRECTORY_KEY);  
        System.clearProperty(BgeeProperties.DOWNLOAD_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.JAVASCRIPT_FILES_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.CSS_FILES_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.IMAGES_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.TOP_OBO_RESULTS_URL_ROOT_DIRECTORY_KEY);
        System.clearProperty(BgeeProperties.WEBPAGES_CACHE_CONFIG_FILE_NAME_KEY);
        // Also, set the properties file to an non-existing file, 
        // so that no property file is used.
        System.setProperty(BgeeProperties.PROPERTIES_FILE_NAME_KEY, "/none");

        // get the instance of bgeeproperties and check the values
        this.bgeeProp = BgeeProperties.getBgeeProperties();
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.BGEE_ROOT_DIRECTORY_DEFAULT, bgeeProp.getBgeeRootDirectory());
        assertEquals("Wrong property value retrieved", BgeeProperties.URL_MAX_LENGTH_DEFAULT, 
                bgeeProp.getUrlMaxLength());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.REQUEST_PARAMETERS_STORAGE_DIRECTORY_DEFAULT, 
                bgeeProp.getRequestParametersStorageDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.DOWNLOAD_ROOT_DIRECTORY_DEFAULT, 
                bgeeProp.getDownloadRootDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.JAVASCRIPT_FILES_ROOT_DIRECTORY_DEFAULT, 
                bgeeProp.getJavascriptFilesRootDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.CSS_FILES_ROOT_DIRECTORY_DEFAULT, 
                bgeeProp.getCssFilesRootDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.IMAGES_ROOT_DIRECTORY_DEFAULT, 
                bgeeProp.getImagesRootDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.TOP_OBO_RESULTS_URL_ROOT_DIRECTORY_DEFAULT, 
                bgeeProp.getTopOBOResultsUrlRootDirectory());
        assertEquals("Wrong property value retrieved", 
                BgeeProperties.WEBPAGES_CACHE_CONFIG_FILE_NAME_DEFAULT, 
                bgeeProp.getWebpagesCacheConfigFileName());
    }
}
