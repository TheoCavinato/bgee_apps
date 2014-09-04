package org.bgee.view;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bgee.controller.BgeeProperties;
import org.bgee.controller.RequestParameters;

import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * This test class extends {@code ViewFactoryProvider} and returns a {@code TestFactory} only 
 * if the XML display type is requested (Arbitrary chosen to be used in tests). Return null in
 * all other cases. This is useful to assess that the display type is correctly handled by
 * controllers.
 * 
 * @author Mathieu Seppey
 * @version Bgee 13 Aug 2014
 * @since   Bgee 13
 */
public class TestFactoryProvider extends ViewFactoryProvider
{    
    /**
     * Constructor
     */
    public TestFactoryProvider()
    {
        super();
    }

    /**
     * Return the Test {@code ViewFactory} if the {@code displayType} is XML, else return null
     * 
     * @param response          the {@code HttpServletResponse} where the outputs of the view 
     *                          classes will be written
     * @param displayType       an {@code int} specifying the requested display type, 
     *                          corresponding to either 
     *                          {@code HTML}, {@code XML}, {@code TSV}, or {@code CSV}.
     * @param requestParameters the {@code RequestParameters} handling the parameters of the 
     *                          current request, for display purposes.
     * @return     A {@code TestFactory} if the {@code displayType} is XML, else null
     */
    @Override
    protected synchronized ViewFactory getFactory(HttpServletResponse response, 
            displayTypes displayType, 
            RequestParameters requestParameters)
    {
                       
        if (displayType == displayTypes.XML) {
            return new TestFactory(response, requestParameters);
        }
        ViewFactory mockFactory = mock(ViewFactory.class);
        try {
            when(mockFactory.getGeneralDisplay(any(BgeeProperties.class)))
            .thenReturn(mock(GeneralDisplay.class));
        } catch (IOException e) {
            // Do nothing, should not occur with a mock
        }
        return mockFactory;
    }

}