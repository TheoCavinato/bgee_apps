package org.bgee.model.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.CommonService;
import org.bgee.model.ServiceFactory;
import org.bgee.model.dao.api.source.SourceDAO.SourceTO;
import org.bgee.model.dao.api.source.SourceToSpeciesDAO.SourceToSpeciesTO;
import org.bgee.model.dao.api.source.SourceToSpeciesDAO.SourceToSpeciesTO.InfoType;
import org.bgee.model.expressiondata.baseelements.DataType;

/**
 * A {@link org.bgee.model.Service} to obtain {@link Source} objects. Users should use the
 * {@link ServiceFactory} to obtain {@code SourceService}s.
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Apr. 2019
 * @since   Bgee 13, Mar. 2016
 */
public class SourceService extends CommonService {

    private static final Logger log = LogManager.getLogger(SourceService.class.getName());

    /**
     * @param serviceFactory            The {@code ServiceFactory} to be used to obtain {@code Service}s 
     *                                  and {@code DAOManager}.
     * @throws IllegalArgumentException If {@code serviceFactory} is {@code null}.
     */
    public SourceService(ServiceFactory serviceFactory) {
        super(serviceFactory);
    }
    
    /**
     * Retrieve all {@code Source}s.
     * 
     * @param withSpeciesInfo   A {@code boolean}s defining whether species information of 
     *                          the source are retrieved or not.
     * @return                  A {@code List} of {@code Source}s that are sources used in Bgee.
     */
    public List<Source> loadAllSources(boolean withSpeciesInfo) {
        log.entry(withSpeciesInfo);
        List<Source> sources = getDaoManager().getSourceDAO().getAllDataSources(null).stream()
                .map(SourceService::mapFromTO)
                .collect(Collectors.toList());
        if (withSpeciesInfo) {
            sources = this.loadSpeciesInfo(sources);
        }
        return log.exit(sources);
    }
    
    /**
     * Retrieve {@code Source}s for a given set of source IDs.
     *
     * @param sourceIds     A {@code Collection} of {@code Integer}s that are IDs of sources 
     *                      for which to return the {@code Source}s.
     * @return              A {@code Map} storing the mappings from source IDs to {@code Source}s.
     */
    public Map<Integer, Source> loadSourcesByIds(Collection<Integer> sourceIds) {
        log.entry(sourceIds);
        return log.exit(this.getDaoManager().getSourceDAO()
                .getDataSourceByIds(sourceIds, null).stream()
                .map(SourceService::mapFromTO)
                .collect(Collectors.toMap(Source::getId, s -> s)));
    }
    
    /**
     * Retrieve {@code Source}s to be displayed.
     * 
     * @param withSpeciesInfo   A {@code boolean}s defining whether species information of 
     *                          the source are retrieved or not.
     * @return                  A {@code List} of {@code Source}s that are sources used in Bgee 
     *                          to be displayed.
     */
    public List<Source> loadDisplayableSources(boolean withSpeciesInfo) {
        log.entry(withSpeciesInfo);
        
        List<Source> sources = getDaoManager().getSourceDAO().getDisplayableDataSources(null).stream()
                .map(SourceService::mapFromTO)
                .collect(Collectors.toList());
        if (withSpeciesInfo) {
            sources = this.loadSpeciesInfo(sources);
        }
        
        return log.exit(sources);
    }

    /**
     * Retrieve {@code Source}s with species information.
     * 
     * @param sources   A {@code List} of {@code Source}s that are sources to be completed.
     * @return          A {@code List} of {@code Source}s that are sources used in Bgee to be displayed.
     */
    private List<Source> loadSpeciesInfo(List<Source> sources) {
        log.entry(sources);
        
        final List<SourceToSpeciesTO> sourceToSpeciesTOs = getDaoManager().getSourceToSpeciesDAO()
                .getAllSourceToSpecies(null).stream()
                .collect(Collectors.toList());
        
        List<Source> completedSources = new ArrayList<>();

        for (Source source : sources) {
            Map<Integer, Set<DataType>> forData = getDataTypesBySpecies(
                    sourceToSpeciesTOs, source.getId(), InfoType.DATA);
            Map<Integer, Set<DataType>> forAnnotation = getDataTypesBySpecies(
                    sourceToSpeciesTOs, source.getId(), InfoType.ANNOTATION);

            completedSources.add(new Source(source.getId(), source.getName(), source.getDescription(),
                    source.getXRefUrl(), source.getExperimentUrl(), source.getEvidenceUrl(),
                    source.getBaseUrl(), source.getReleaseDate(), source.getReleaseVersion(),
                    source.getToDisplay(), source.getCategory(), source.getDisplayOrder(),
                    forData.isEmpty() ? null : forData, forAnnotation.isEmpty() ? null : forAnnotation));
        }
        
        return log.exit(completedSources);
    }
    
    /** 
     * Retrieve data types by species from {@code SourceToSpeciesTO}.
     * 
     * @param sourceToSpeciesTOs    A {@code List} of {@code SourceToSpeciesTO}s that are sources 
     *                              to species to be grouped.
     * @param sourceId              An {@code Integer} that is the source ID for which to return
     *                              data types by species.
     * @param infoType              An {@code InfoType} that is the information type for which
     *                              to return data types by species.
     * @return                      A {@code Map} where keys are {@code Integer}s corresponding to 
     *                              species IDs, the associated values being a {@code Set} of 
     *                              {@code DataType}s corresponding to data types of {@code infoType}
     *                              data of the provided {@code sourceId}.
     */
    private Map<Integer, Set<DataType>> getDataTypesBySpecies(
            final List<SourceToSpeciesTO> sourceToSpeciesTOs, Integer sourceId, InfoType infoType) {
        log.entry(sourceToSpeciesTOs, sourceId, infoType);
        Map<Integer, Set<DataType>> map = sourceToSpeciesTOs.stream()
            .filter(to -> to.getDataSourceId().equals(sourceId))
            .filter(to -> to.getInfoType().equals(infoType))
            .collect(Collectors.toMap(to -> to.getSpeciesId(), 
                to -> new HashSet<DataType>(Arrays.asList(convertDaoDataTypeToDataType(to.getDataType()))), 
                (v1, v2) -> {
                    Set<DataType> newSet = new HashSet<>(v1);
                    newSet.addAll(v2);
                    return newSet;
                }));
        return log.exit(map);
    }
    
    /**
     * Maps {@link SourceTO} to a {@link Source}.
     * 
     * @param sourceTO  The {@link SourceTO} to map.
     * @return          The mapped {@link Source}.
     */
    private static Source mapFromTO(SourceTO sourceTO) {
        log.entry(sourceTO);
        if (sourceTO == null) {
            return log.exit(null);
        }
        return log.exit(new Source(sourceTO.getId(), sourceTO.getName(), sourceTO.getDescription(),
                sourceTO.getXRefUrl(), sourceTO.getExperimentUrl(), sourceTO.getEvidenceUrl(),
                sourceTO.getBaseUrl(), sourceTO.getReleaseDate(), sourceTO.getReleaseVersion(),
                sourceTO.isToDisplay(), convertSourceCategoryTOToSourceCategory(sourceTO.getSourceCategory()),
                sourceTO.getDisplayOrder()));
    }
    
    private static SourceCategory convertSourceCategoryTOToSourceCategory(SourceTO.SourceCategory cat) 
            throws IllegalStateException{
        log.entry(cat);
        switch(cat) {
            case NONE: 
                return log.exit(SourceCategory.NONE);
            case GENOMICS:
                return log.exit(SourceCategory.GENOMICS);
            case PROTEOMICS: 
                return log.exit(SourceCategory.PROTEOMICS);
            case IN_SITU: 
                return log.exit(SourceCategory.IN_SITU);
            case AFFYMETRIX: 
                return log.exit(SourceCategory.AFFYMETRIX);
            case EST: 
                return log.exit(SourceCategory.EST);
            case RNA_SEQ: 
                return log.exit(SourceCategory.RNA_SEQ);
            case ONTOLOGY: 
                return log.exit(SourceCategory.ONTOLOGY);
        default: 
            throw log.throwing(new IllegalStateException("Unsupported SourceTO.SourceCategory: " + cat));
        }
    }
}
