package net.sparkworks.mapper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sparkworks.cargo.client.ResourceClient;
import net.sparkworks.cargo.common.dto.ResourceDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final Map<String, ResourceDTO> resourceCache = new HashMap<>();

    private final ResourceClient resourceClient;

    public void placeInCorrectGroup(final UUID groupUuid, final Collection<String> systemNames) {
        for (final String systemName : systemNames) {
            if (resourceCache.containsKey(systemName) && groupUuid.equals(resourceCache.get(systemName).getGroupUuid())) {
                log.info("[{}] already in group [{}]", systemName, groupUuid);
            } else {
                log.info("[{}] searching group", systemName);
                final Collection<ResourceDTO> resources = resourceClient.listAll();
                for (final ResourceDTO resource : resources) {
                    if (StringUtils.equals(resource.getSystemName(), systemName)) {
                        log.info("[{}] group {} should be {}", systemName, resource.getGroupUuid(), groupUuid);
                        if (groupUuid.equals(resource.getGroupUuid())) {
                            log.info("[{}] already in group [{}] caching...", systemName, groupUuid);
                            resourceCache.put(systemName, resource);
                        } else {
                            log.info("[{}] already in group [{}] moving...", systemName, groupUuid);
                            ResourceDTO movedResource = resourceClient.move(resource.getUuid(), groupUuid);
                            if (groupUuid.equals(movedResource.getGroupUuid())) {
                                resourceCache.put(systemName, movedResource);
                            }
                        }
                    }
                }
            }
        }
    }
}
