package org.bgee.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.TestAncestor;
import org.bgee.controller.exception.PageNotFoundException;
import org.bgee.model.ServiceFactory;
import org.bgee.model.file.DownloadFile;
import org.bgee.model.file.DownloadFile.CategoryEnum;
import org.bgee.model.file.SpeciesDataGroup;
import org.bgee.model.file.SpeciesDataGroupService;
import org.bgee.model.keyword.KeywordService;
import org.bgee.model.species.Species;
import org.bgee.view.DownloadDisplay;
import org.bgee.view.ViewFactory;
import org.junit.Test;

/**
 * Unit tests for {@link CommandDownload}.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13 Nov. 2015
 * @since   Bgee 13 Oct. 2015
 */
public class CommandDownloadTest extends TestAncestor {
    
    private final static Logger log = 
            LogManager.getLogger(CommandDownloadTest.class.getName());

    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Test {@link CommandDownload#processRequest()}.
     */
    @Test
    public void shouldProcessRequest() throws IOException, IllegalStateException, PageNotFoundException {
        //mock Services
        ServiceFactory serviceFac = mock(ServiceFactory.class);
        SpeciesDataGroupService groupService = mock(SpeciesDataGroupService.class);
        when(serviceFac.getSpeciesDataGroupService()).thenReturn(groupService);
        KeywordService keywordService = mock(KeywordService.class);
        when(serviceFac.getKeywordService()).thenReturn(keywordService);
        
        //mock data returned by Services
        List<SpeciesDataGroup> groups = getTestGroups();
        when(groupService.loadAllSpeciesDataGroup()).thenReturn(groups);
        when(keywordService.getKeywordForAllSpecies()).thenReturn(getTestSpeToKeywords());

        //mock view
        ViewFactory viewFac = mock(ViewFactory.class);
        DownloadDisplay display = mock(DownloadDisplay.class);
        when(viewFac.getDownloadDisplay()).thenReturn(display);
        
        //launch tests
        Map<String, Set<String>> speToTerms = getTestSpeciesToTerms();
        
        RequestParameters params = new RequestParameters();
        params.setPage(RequestParameters.PAGE_DOWNLOAD);
        params.setAction(RequestParameters.ACTION_DOWLOAD_CALL_FILES);
        CommandDownload controller = new CommandDownload(mock(HttpServletResponse.class), params, 
                mock(BgeeProperties.class), viewFac, serviceFac);
        controller.processRequest();
        verify(display).displayGeneExpressionCallDownloadPage(groups, speToTerms);
        
        params = new RequestParameters();
        params.setPage(RequestParameters.PAGE_DOWNLOAD);
        params.setAction(RequestParameters.ACTION_DOWLOAD_PROC_VALUE_FILES);
        controller = new CommandDownload(mock(HttpServletResponse.class), params, 
                mock(BgeeProperties.class), viewFac, serviceFac);
        controller.processRequest();
        verify(display).displayProcessedExpressionValuesDownloadPage(groups, speToTerms);
    }
    
    /**
     * @return  A {@code List} of {@code SpeciesDataGroup}s to be used in unit tests related to 
     *          download files.
     */
    public static List<SpeciesDataGroup> getTestGroups() {
        log.entry();
        //Species:
        Species spe1 = new Species("9606", "human", null, "Homo", "sapiens", "hsap1");
        Species spe2 = new Species("10090", "mouse", null, "Mus", "musculus", "mmus1");
        Species spe3 = new Species("7955", "zebrafish", null, "Danio", "rerio", "dre1");
        Species spe4 = new Species("7227", "fly", null, "Drosophila", "melanogaster", "dmel1");
        
        //make all file types available for at least one species
        Set<DownloadFile> dlFileGroup1 = new HashSet<DownloadFile>();
        int i = 0;
        for (CategoryEnum cat: CategoryEnum.values()) {
            dlFileGroup1.add(new DownloadFile("my/path/file" + i + ".tsv.zip", "file" + i + ".tsv.zip", 
                    cat, i * 200L, "singleSpeG1"));
            i++;
        }
        //arbitrary files for other groups
        Set<DownloadFile> dlFileGroup2 = new HashSet<DownloadFile>(Arrays.asList(
                new DownloadFile("my/path/fileg2_1.tsv.zip", "fileg2_1.tsv.zip", 
                        CategoryEnum.EXPR_CALLS_SIMPLE, 5000L, "singleSpeG2"), 
                new DownloadFile("my/path/fileg2_2.tsv.zip", "fileg2_2.tsv.zip", 
                        CategoryEnum.EXPR_CALLS_COMPLETE, 50000L, "singleSpeG2"), 
                new DownloadFile("my/path/fileg2_3.tsv.zip", "fileg2_3.tsv.zip", 
                        CategoryEnum.RNASEQ_ANNOT, 5000L, "singleSpeG2")
                ));
        Set<DownloadFile> dlFileGroup3 = new HashSet<DownloadFile>(Arrays.asList(
                new DownloadFile("my/path/fileg3_1.tsv.zip", "fileg3_1.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_SIMPLE, 500L, "singleSpeG3"), 
                new DownloadFile("my/path/fileg3_2.tsv.zip", "fileg3_2.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_COMPLETE, 5000L, "singleSpeG3"), 
                new DownloadFile("my/path/fileg3_3.tsv.zip", "fileg3_3.tsv.zip", 
                        CategoryEnum.AFFY_DATA, 5000L, "singleSpeG3"), 
                new DownloadFile("my/path/fileg3_4.tsv.zip", "fileg3_4.tsv.zip", 
                        CategoryEnum.AFFY_ANNOT, 5000L, "singleSpeG3")
                ));
        Set<DownloadFile> dlFileGroup4 = new HashSet<DownloadFile>(Arrays.asList(
                new DownloadFile("my/path/fileg4_1.tsv.zip", "fileg4_1.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_SIMPLE, 5500L, "singleSpeG4"), 
                new DownloadFile("my/path/fileg4_2.tsv.zip", "fileg4_2.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_COMPLETE, 55000L, "singleSpeG4"), 
                new DownloadFile("my/path/fileg4_3.tsv.zip", "fileg4_3.tsv.zip", 
                        CategoryEnum.AFFY_DATA, 55000L, "singleSpeG4"), 
                new DownloadFile("my/path/fileg4_4.tsv.zip", "fileg4_4.tsv.zip", 
                        CategoryEnum.AFFY_ANNOT, 55000L, "singleSpeG4"), 
                new DownloadFile("my/path/fileg4_5.tsv.zip", "fileg4_5.tsv.zip", 
                        CategoryEnum.RNASEQ_ANNOT, 55000L, "singleSpeG4"), 
                new DownloadFile("my/path/fileg4_6.tsv.zip", "fileg4_6.tsv.zip", 
                        CategoryEnum.RNASEQ_DATA, 55000L, "singleSpeG4")
                ));
        Set<DownloadFile> dlFileGroup5 = new HashSet<DownloadFile>(Arrays.asList(
                new DownloadFile("my/path/fileg5_1.tsv.zip", "fileg5_1.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_SIMPLE, 55000L, "multiSpeG5"), 
                new DownloadFile("my/path/fileg5_2.tsv.zip", "fileg5_2.tsv.zip", 
                        CategoryEnum.DIFF_EXPR_ANAT_COMPLETE, 55000L, "multiSpeG5"), 
                new DownloadFile("my/path/fileg5_3.tsv.zip", "fileg5_3.tsv.zip", 
                        CategoryEnum.ORTHOLOG, 55000L, "multiSpeG5")
                ));
        
        //groups: 
        return log.exit(Arrays.asList(
                new SpeciesDataGroup("singleSpeG1", "single spe g1", null, 
                        Arrays.asList(spe1), dlFileGroup1), 
                new SpeciesDataGroup("singleSpeG2", "single spe g2", null, 
                        Arrays.asList(spe2), dlFileGroup2), 
                new SpeciesDataGroup("singleSpeG3", "single spe g3", null, 
                        Arrays.asList(spe3), dlFileGroup3), 
                new SpeciesDataGroup("singleSpeG4", "single spe g4", null, 
                        Arrays.asList(spe4), dlFileGroup4), 
                new SpeciesDataGroup("multiSpeG5", "multi spe g5", null, 
                        Arrays.asList(spe2, spe3, spe4), dlFileGroup5)
                ));
    }
    
    /**
     * @return  A {@code Map} allowing to mock the value returned by 
     *          {@link KeywordService#getKeywordForAllSpecies()}.
     */
    public static Map<String, Set<String>> getTestSpeToKeywords() {
        log.entry();
        return log.exit(Stream.of(
            //regression test, no keywords associated to species 9606 on purpose, 
            //a previous version of the code threw a NPE
            new AbstractMap.SimpleEntry<>("10090", new HashSet<>(Arrays.asList("house mouse", "mice"))), 
            new AbstractMap.SimpleEntry<>("7955", new HashSet<>(Arrays.asList("leopard danio", "zebra danio"))), 
            new AbstractMap.SimpleEntry<>("7227", new HashSet<>(Arrays.asList("vinegar fly", "fruit fly"))))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    }
    
    /**
     * @return  A {@code Map} to provide mappings from species to related terms, 
     *          to be provided to the methods {@code DownloadDisplay.displayGeneExpressionCallDownloadPage} 
     *          and {@code DownloadDisplay.displayProcessedExpressionValuesDownloadPage}. 
     *          This {@code Map} is notably produced by using the groups returned by 
     *          {@link #getTestGroups()}, and the mapping to keywords returned by 
     *          {@link #getTestSpeToKeywords()}.
     */
    public static Map<String, Set<String>> getTestSpeciesToTerms() {
        log.entry();
        List<SpeciesDataGroup> groups = getTestGroups();
        return log.exit(Stream.of(
                new AbstractMap.SimpleEntry<>(groups.get(0).getMembers().get(0).getId(), new HashSet<>(
                        Arrays.asList(groups.get(0).getMembers().get(0).getName(), 
                                groups.get(0).getMembers().get(0).getScientificName(), 
                                groups.get(0).getMembers().get(0).getShortName(), 
                                groups.get(0).getMembers().get(0).getId()))), 
                new AbstractMap.SimpleEntry<>(groups.get(1).getMembers().get(0).getId(), new HashSet<>(
                        Arrays.asList("house mouse", "mice", 
                                groups.get(1).getMembers().get(0).getName(), 
                                groups.get(1).getMembers().get(0).getScientificName(), 
                                groups.get(1).getMembers().get(0).getShortName(), 
                                groups.get(1).getMembers().get(0).getId()))), 
                new AbstractMap.SimpleEntry<>(groups.get(2).getMembers().get(0).getId(), new HashSet<>(
                        Arrays.asList("leopard danio", "zebra danio", 
                                groups.get(2).getMembers().get(0).getName(), 
                                groups.get(2).getMembers().get(0).getScientificName(), 
                                groups.get(2).getMembers().get(0).getShortName(), 
                                groups.get(2).getMembers().get(0).getId()))), 
                new AbstractMap.SimpleEntry<>(groups.get(3).getMembers().get(0).getId(), new HashSet<>(
                        Arrays.asList("vinegar fly", "fruit fly", 
                                groups.get(3).getMembers().get(0).getName(), 
                                groups.get(3).getMembers().get(0).getScientificName(), 
                                groups.get(3).getMembers().get(0).getShortName(), 
                                groups.get(3).getMembers().get(0).getId()))))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
    }
}
