package org.bgee.model.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.exception.QueryInterruptedException;
import org.bgee.model.dao.api.file.DownloadFileDAO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link Service} to obtain {@link DownloadFile}s. 
 * Users should use the {@link org.bgee.model.ServiceFactory} to obtain {@code DownloadFileService}s.
 *
 * @author Philippe Moret
 */
public class DownloadFileService extends Service {

    private static final Logger log = LogManager.getLogger(DownloadFileService.class.getName());

    /**
     * @param serviceFactory            The {@code ServiceFactory} to be used to obtain {@code Service}s 
     *                                  and {@code DAOManager}.
     * @throws IllegalArgumentException If {@code serviceFactory} is {@code null}.
     */
    public DownloadFileService(ServiceFactory serviceFactory) {
        super(serviceFactory);
    }

    /**
     * Gets all available {@code DownloadFile}.
     *
     * @return a {@code List} of {@code DownloadFile}
     * @throws DAOException                 If an error occurred while accessing a {@code DAO}.
     * @throws QueryInterruptedException    If a query to a {@code DAO} was intentionally interrupted.
     */
    public List<DownloadFile> getAllDownloadFiles() throws DAOException, QueryInterruptedException {
        log.entry();
        return log.exit(getDaoManager().getDownloadFileDAO().getAllDownloadFiles().stream()
                .map(DownloadFileService::mapFromTO)
                .collect(Collectors.toList()));
    }


    /**
     * Maps {@link DownloadFileDAO.DownloadFileTO} to a {@link DownloadFile}.
     *
     * @param downloadFileTO The {@link DownloadFileDAO.DownloadFileTO} to map.
     * @return The mapped  {@link DownloadFile}.
     */
    private static DownloadFile mapFromTO(DownloadFileDAO.DownloadFileTO downloadFileTO) {
        log.entry(downloadFileTO);
        if (downloadFileTO == null) {
            return log.exit(null);
        }
        return log.exit(new DownloadFile(downloadFileTO.getPath(),
                downloadFileTO.getName(),
                //currently, the IDs of DownloadFile.CategoryEnum correspond exactly to
                //the DownloadFileTO.CategoryEnum#getStringRepresentation(), 
                //this might change in the future.
                DownloadFile.CategoryEnum.getById(downloadFileTO.getCategory().getStringRepresentation()),
                downloadFileTO.getSize(),
                downloadFileTO.getSpeciesDataGroupId()));
    }


}
