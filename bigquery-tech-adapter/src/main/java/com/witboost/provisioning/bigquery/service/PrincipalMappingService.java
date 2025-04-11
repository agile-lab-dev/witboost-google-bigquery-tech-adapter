package com.witboost.provisioning.bigquery.service;

import com.google.cloud.Identity;
import com.witboost.provisioning.model.common.FailedOperation;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Set;

/***
 * Principal mapping services
 */
public interface PrincipalMappingService {
    /**
     * Map subjects to GCP Identities
     *
     * @param subjects the set of subjects to map
     * @return return a FailedOperation or the mapped Identity for every subject to be mapped
     */
    Map<String, Either<FailedOperation, Identity>> map(Set<String> subjects);
}
