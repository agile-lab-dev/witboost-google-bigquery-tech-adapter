package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQueryException;
import com.witboost.provisioning.bigquery.config.PrincipalMappingServiceConfig;
import com.witboost.provisioning.model.common.Problem;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GCPPrincipalMappingServiceTest {

    @Mock
    private PrincipalMappingServiceConfig principalMappingServiceConfig;

    @InjectMocks
    private GCPPrincipalMappingService principalMappingService;

    private final String witboostUserIdentity = "user:name.surname_example.com";
    private final String mail = "name.surname@example.com";
    private final String witboostGroupIdentity = "group:name";
    private final String groupName = "name";
    private final String groupMail = "name@example.com";
    private final Problem expectedProblem = new Problem("Error", new BigQueryException(401, "Unauthorized"));

    @Test
    public void testMapExistingUser() {
        var actualRes = principalMappingService.map(Collections.singleton(witboostUserIdentity));

        assertTrue(actualRes.containsKey(witboostUserIdentity));
        assertTrue(actualRes.get(witboostUserIdentity).isRight());
        assertEquals(mail, actualRes.get(witboostUserIdentity).get().getValue());
    }

    @Test
    public void testMapExistingGroup() {
        when(principalMappingServiceConfig.groupMailDomain()).thenReturn("@example.com");

        var actualRes = principalMappingService.map(Collections.singleton(witboostGroupIdentity));

        assertTrue(actualRes.containsKey(witboostGroupIdentity));
        assertTrue(actualRes.get(witboostGroupIdentity).isRight());
        assertEquals(groupMail, actualRes.get(witboostGroupIdentity).get().getValue());
    }

    @Test
    public void testMapUnknownIdentity() {
        String unknownIdentity = "an_unknown_identity";
        Problem expectedUnknownProblem =
                new Problem("The subject an_unknown_identity is neither a Witboost user nor a group");

        var actualRes = principalMappingService.map(Collections.singleton(unknownIdentity));

        assertTrue(actualRes.containsKey(unknownIdentity));
        assertTrue(actualRes.get(unknownIdentity).isLeft());
        assertEquals(1, actualRes.get(unknownIdentity).getLeft().problems().size());
        actualRes.get(unknownIdentity).getLeft().problems().forEach(p -> assertEquals(expectedUnknownProblem, p));
    }

    @Test
    public void testMapExistingUserAndGroup() {
        when(principalMappingServiceConfig.groupMailDomain()).thenReturn("@example.com");

        var actualRes = principalMappingService.map(Set.of(witboostUserIdentity, witboostGroupIdentity));

        assertTrue(actualRes.containsKey(witboostUserIdentity));
        assertTrue(actualRes.get(witboostUserIdentity).isRight());
        assertEquals(mail, actualRes.get(witboostUserIdentity).get().getValue());
        assertTrue(actualRes.containsKey(witboostGroupIdentity));
        assertTrue(actualRes.get(witboostGroupIdentity).isRight());
        assertEquals(groupMail, actualRes.get(witboostGroupIdentity).get().getValue());
    }

    @Test
    public void testMapUserWithWrongMailFormat() {
        String wrongUserIdentity = "user:no-underscore.example.com";
        Problem expectedWrongProblem =
                new Problem("The subject user:no-underscore.example.com has not the expected format for a user");

        var actualRes = principalMappingService.map(Collections.singleton(wrongUserIdentity));

        assertTrue(actualRes.containsKey(wrongUserIdentity));
        assertTrue(actualRes.get(wrongUserIdentity).isLeft());
        assertEquals(1, actualRes.get(wrongUserIdentity).getLeft().problems().size());
        actualRes.get(wrongUserIdentity).getLeft().problems().forEach(p -> assertEquals(expectedWrongProblem, p));
    }
}
