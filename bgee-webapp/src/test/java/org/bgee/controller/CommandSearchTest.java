package org.bgee.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.TestAncestor;
import org.bgee.controller.exception.InvalidRequestException;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.ServiceFactory;
import org.bgee.model.gene.Gene;
import org.bgee.model.gene.GeneBioType;
import org.bgee.model.gene.GeneMatch;
import org.bgee.model.gene.GeneMatch.MatchSource;
import org.bgee.model.gene.GeneMatchResult;
import org.bgee.model.gene.GeneMatchResultService;
import org.bgee.model.species.Species;
import org.bgee.view.SearchDisplay;
import org.bgee.view.ViewFactory;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CommandSearch}.
 *
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Mar. 2018
 * @since   Bgee 14, Mar. 2018
 */
public class CommandSearchTest extends TestAncestor {
    private final static Logger log = LogManager.getLogger(CommandAboutTest.class.getName());

    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Test {@link CommandSearch#processRequest()}.
     * @throws InvalidRequestException
     */
    @Test
    //FIXME: no idea how up-to-date this test is
    @Ignore
    public void shouldProcessRequest() throws IOException, PageNotFoundException, InvalidRequestException {

        //mock Services
        ServiceFactory serviceFac = mock(ServiceFactory.class);
        GeneMatchResultService geneMatchService = mock(GeneMatchResultService.class);
        when(serviceFac.getGeneMatchResultService(any(BgeeProperties.class))).thenReturn(geneMatchService);

        List<GeneMatch> geneMatches = Collections.singletonList(new GeneMatch(
                new Gene("geneId", "name", "description", null, null, new Species(1), new GeneBioType("b"), 1),
                "synonym", MatchSource.ID));
        GeneMatchResult result = new GeneMatchResult(10000, geneMatches);
        when(geneMatchService.searchByTerm("gene", null, 0, 1)).thenReturn(result);

        //mock view
        ViewFactory viewFac = mock(ViewFactory.class);
        SearchDisplay display = mock(SearchDisplay.class);
        when(viewFac.getSearchDisplay()).thenReturn(display);

        RequestParameters params = mock(RequestParameters.class);
        when(params.getQuery()).thenReturn("gene");
        when(params.getAction()).thenReturn(RequestParameters.ACTION_AUTO_COMPLETE_GENE_SEARCH);

        CommandSearch controller = new CommandSearch(mock(HttpServletResponse.class), params,
                mock(BgeeProperties.class), viewFac, serviceFac);
        controller.processRequest();
        verify(display).displayGeneCompletionByGeneList(geneMatches);

        params = mock(RequestParameters.class);
        when(params.getAction()).thenReturn("any action");
        URLParameters urlParams = mock(URLParameters.class);
        when(params.getUrlParametersInstance()).thenReturn(urlParams);
        when(params.getUrlParametersInstance().getParamAction()).thenReturn(null);

        controller = new CommandSearch(mock(HttpServletResponse.class), params,
                mock(BgeeProperties.class), viewFac, serviceFac);
        try {
            controller.processRequest();
            fail("A PageNotFoundException should be thrown");
        } catch (PageNotFoundException e) { 
            // test passed
        }
    }
}
