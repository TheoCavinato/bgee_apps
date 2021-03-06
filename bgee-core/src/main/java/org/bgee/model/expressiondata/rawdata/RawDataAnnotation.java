package org.bgee.model.expressiondata.rawdata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.source.Source;

/**
 * Annotation to a raw {@code Condition} (as opposed to a global {@code Condition},
 * for instance aggregating all raw conditions related to a given anatomical entity).
 * {@code RawDataAnnotation}s also provide information about the annotation itself
 * (about for instance, the curator creating the annotation, or the date of creation).
 *
 * @author Frederic Bastian
 * @version Bgee 14 Jul. 2018
 * @since Bgee 14 Jul. 2018
 */
public class RawDataAnnotation {
    private final static Logger log = LogManager.getLogger(RawDataAnnotation.class.getName());
    private final RawDataCondition rawDataCondition;
    private final String curator;
    private final Source annotationSource;
    private final String annotationDate;

    public RawDataAnnotation(RawDataCondition rawDataCondition, String curator, Source annotationSource, String annotationDate) {
        if (rawDataCondition == null) {
            throw log.throwing(new IllegalArgumentException("A condition must be provided"));
        }
        this.rawDataCondition = rawDataCondition;
        this.curator = curator;
        this.annotationSource = annotationSource;
        this.annotationDate = annotationDate;
    }

    public RawDataCondition getRawDataCondition() {
        return rawDataCondition;
    }

    public String getCurator() {
        return curator;
    }

    public Source getAnnotationSource() {
        return annotationSource;
    }

    public String getAnnotationDate() {
        return annotationDate;
    }
}
