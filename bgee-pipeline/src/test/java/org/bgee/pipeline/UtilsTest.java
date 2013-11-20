package org.bgee.pipeline;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

/**
 * Unit tests for {@link Utils}.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class UtilsTest extends TestAncestor {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(UtilsTest.class.getName());

    /**
     * Default Constructor. 
     */
    public UtilsTest() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Tests {@link Utils.getTaxonIds(String)}.
     */
    @Test
    public void shouldGetTaxonIds() throws IllegalArgumentException, 
        FileNotFoundException, IOException {
        Set<Integer> speciesIds = new Utils().getTaxonIds(
                this.getClass().getResource("/species/species.tsv").getFile());
        assertTrue("Incorrect species IDs returned: " + speciesIds, 
                speciesIds.size() == 3 && 
                speciesIds.contains(8) && speciesIds.contains(13) && 
                speciesIds.contains(15));
    }
    
    /**
     * Tests {@link Utils.parseColumnAsString(String, String, CellProcessor)}.
     */
    @Test
    public void shouldParseColumnAsString() throws IllegalArgumentException, 
        FileNotFoundException, IOException {
        CellProcessor processor = mock(CellProcessor.class);
        when(processor.execute(anyObject(), any(CsvContext.class))).thenAnswer(
            new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    return args[0];
                }
        });
        
        List<String> values = new Utils().parseColumnAsString(
                this.getClass().getResource("/utils/tsvTestFile.tsv").getFile(), 
                "column2", processor);
        //the column to read in the second line is empty, it is in purpose 
        //for a regression test
        assertEquals("Incorrect values returned", 2, values.size());
        assertEquals("Incorrect values returned", "b1", values.get(0));
        assertEquals("Incorrect values returned", "b3", values.get(1));
        verify(processor, times(3)).execute(anyObject(), any(CsvContext.class));
        
        //an IllegalArgumentException should be thrown if no column with the provided 
        //name could be found.
        try {
            new Utils().parseColumnAsString(
                    this.getClass().getResource("/utils/tsvTestFile.tsv").getFile(), 
                    "fakeColumn", processor);
            //if we reach this point, test failed
            throw new AssertionError("No IllegalArgumentException was thrown when " +
            		"providing a non-existing column name");
        } catch(IllegalArgumentException e) {
            //test passed
        }
    }
    
    /**
     * Tests {@link Utils.parseColumnAsInteger(String, String, CellProcessor)}.
     */
    @Test
    public void shouldParseColumnAsInteger() throws IllegalArgumentException, 
        FileNotFoundException, IOException {
        CellProcessor processor = mock(CellProcessor.class);
        when(processor.execute(anyObject(), any(CsvContext.class))).thenAnswer(
            new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] args = invocation.getArguments();
                    return args[0];
                }
        });
        
        List<Integer> values = new Utils().parseColumnAsInteger(
                this.getClass().getResource("/utils/tsvTestFile.tsv").getFile(), 
                "column3", processor);
        assertEquals("Incorrect values returned", 3, values.size());
        assertEquals("Incorrect values returned", 3, (int) values.get(0));
        assertEquals("Incorrect values returned", 2, (int) values.get(1));
        assertEquals("Incorrect values returned", 1, (int) values.get(2));
        verify(processor, times(3)).execute(anyObject(), any(CsvContext.class));
        
        //an IllegalArgumentException should be thrown if no column with the provided 
        //name could be found.
        try {
            new Utils().parseColumnAsString(
                    this.getClass().getResource("/utils/tsvTestFile.tsv").getFile(), 
                    "fakeColumn", processor);
            //if we reach this point, test failed
            throw new AssertionError("No IllegalArgumentException was thrown when " +
                    "providing a non-existing column name");
        } catch(IllegalArgumentException e) {
            //test passed
        }
        
        //we check if everything works fine even if we provide a ParseInt CellProcessor 
        //(regression test)
        new Utils().parseColumnAsInteger(
                this.getClass().getResource("/utils/tsvTestFile.tsv").getFile(), 
                "column3", new ParseInt());
    }
}
