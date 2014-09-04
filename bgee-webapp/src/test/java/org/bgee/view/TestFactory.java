package org.bgee.view;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;
import org.bgee.controller.TestURLParameters;

/**
 * This class is a fake {@code ViewFactory} used for tests. It return a {@code TestDownloadDisplay}
 * when a {@code DownloadDisplay} is requested. It returns a mock {@code GeneralDisplay} in other
 * cases.
 * @author Mathieu Seppey
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
public class TestFactory extends ViewFactory
{
    
    public TestFactory(HttpServletResponse response, RequestParameters requestParameters)
    {
        super(response, requestParameters);
    }
    
    @Override
    /**
     * This method check that the injected {@code BgeeProperties} and {@code URLParameters}
     * are present with correct values.
     * @param prop  The injected {@code BgeeProperties}
     * @return  A new {@code TestURLParameters} if the parameters are correct, else {@code null}
     */
    public DownloadDisplay getDownloadDisplay(BgeeProperties prop)  throws IOException
    {
        if(prop.getUrlMaxLength() == 9999 && this.requestParameters.getFirstValue(
                ((TestURLParameters)this.requestParameters.getURLParametersInstance())
                .getParamTestString()).equals("test")){
            return new TestDownloadDisplay(this.response, this.requestParameters, prop);
        }
        else return null;
    }

    @Override
    /**
     * This method should not be called if the code is error free.
     * @param prop  A {@code BgeeProperties}
     * @return  {@code null}
     */
    public GeneralDisplay getGeneralDisplay(BgeeProperties prop) throws IOException {
        return null;
    }
    
}