package com.witboost.provisioning.bigquery.service;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.Identity;
import com.witboost.provisioning.bigquery.config.PrincipalMappingServiceConfig;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GCPPrincipalMappingService implements PrincipalMappingService {

    private final Logger logger = LoggerFactory.getLogger(GCPPrincipalMappingService.class);

    private final PrincipalMappingServiceConfig principalMappingServiceConfig;

    public GCPPrincipalMappingService(PrincipalMappingServiceConfig principalMappingServiceConfig) {
        this.principalMappingServiceConfig = principalMappingServiceConfig;
    }

    @Override
    public Map<String, Either<FailedOperation, Identity>> map(Set<String> subjects) {
        return subjects.stream()
                .map(s -> {
                    if (isWitboostUser(s)) {
                        var eitherMail = getMailFromWitboostIdentity(s);
                        return eitherMail.fold(
                                l -> new AbstractMap.SimpleEntry<String, Either<FailedOperation, Identity>>(s, left(l)),
                                mail -> new AbstractMap.SimpleEntry<>(s, mapUser(mail)));
                    } else if (isWitboostGroup(s)) {
                        String group = getGroup(s);
                        return new AbstractMap.SimpleEntry<>(s, mapGroup(group));
                    } else {
                        return new AbstractMap.SimpleEntry<>(s, mapUnkownIdentity(s));
                    }
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private Either<FailedOperation, String> getMailFromWitboostIdentity(String witboostIdentity) {
        String user = getUser(witboostIdentity);
        int underscoreIndex = user.lastIndexOf("_");
        if (underscoreIndex == -1) {
            String userMessage = "Received an invalid user";
            String errorMessage =
                    String.format("The subject %s has not the expected format for a user", witboostIdentity);
            logger.error(errorMessage);
            return left(new FailedOperation(userMessage, Collections.singletonList(new Problem(errorMessage))));
        } else {
            return right(user.substring(0, underscoreIndex) + "@" + user.substring(underscoreIndex + 1));
        }
    }

    private Either<FailedOperation, Identity> mapGroup(String group) {
        // here we suppose GCP group has the same name present in Witboost followed by a configurable domain
        return right(Identity.group(group + principalMappingServiceConfig.groupMailDomain()));
    }

    private Either<FailedOperation, Identity> mapUser(String mail) {
        return right(Identity.user(mail));
    }

    private Either<FailedOperation, Identity> mapUnkownIdentity(String s) {
        String userMessage = "Received an unknown identity";
        String errorMessage = String.format("The subject %s is neither a Witboost user nor a group", s);
        logger.error(errorMessage);
        return left(new FailedOperation(userMessage, Collections.singletonList(new Problem(errorMessage))));
    }

    private String getUser(String witboostIdentity) {
        return witboostIdentity.substring(5);
    }

    private String getGroup(String witboostIdentity) {
        return witboostIdentity.substring(6);
    }

    private boolean isWitboostGroup(String witboostIdentity) {
        return witboostIdentity.startsWith("group:");
    }

    private boolean isWitboostUser(String witboostIdentity) {
        return witboostIdentity.startsWith("user:");
    }
}
