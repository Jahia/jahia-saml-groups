package org.jahiacommunity.saml.groups;

import org.apache.commons.lang.StringUtils;
import org.jahia.api.usermanager.JahiaUserManagerService;
import org.jahia.modules.jahiaauth.service.ConnectorConfig;
import org.jahia.modules.jahiaauth.service.ConnectorResultProcessor;
import org.jahia.modules.jahiaauth.service.JahiaAuthConstants;
import org.jahia.services.content.decorator.JCRGroupNode;
import org.jahia.services.content.decorator.JCRUserNode;
import org.jahia.services.usermanager.JahiaGroupManagerService;
import org.jahia.taglibs.user.User;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;

@Component(service = ConnectorResultProcessor.class, immediate = true)
public class GroupProcessor implements ConnectorResultProcessor {
    private static final Logger logger = LoggerFactory.getLogger(GroupProcessor.class);

    private JahiaUserManagerService jahiaUserManagerService;
    private JahiaGroupManagerService jahiaGroupManagerService;
    private String groupsAttributeAssignment;

    public GroupProcessor() {
        groupsAttributeAssignment = "groups";
    }

    @Reference
    private void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        this.jahiaUserManagerService = jahiaUserManagerService;
    }

    @Reference
    private void setJahiaGroupManagerService(JahiaGroupManagerService jahiaGroupManagerService) {
        this.jahiaGroupManagerService = jahiaGroupManagerService;
    }

    @Activate
    private void onActivate(Map<String, ?> configuration) {
        if (configuration.containsKey("groups-attribute-assignement")) {
            groupsAttributeAssignment = StringUtils.defaultIfBlank((String) configuration.get("groups-attribute-assignement"), "groups");
        }
    }

    @Override
    public void execute(ConnectorConfig connectorConfig, Map<String, Object> results) {
        if (logger.isDebugEnabled()) {
            logger.debug("SAML properties: {}", results);
        }
        try {
            if (results.containsKey(JahiaAuthConstants.SSO_LOGIN)) {
                String userId = (String) results.get(JahiaAuthConstants.SSO_LOGIN);
                JCRUserNode userNode = jahiaUserManagerService.lookupUser(userId, connectorConfig.getSiteKey());
                if (userNode != null) {
                    removeUserMembership(userNode);
                    if (results.containsKey(groupsAttributeAssignment)) {
                        manageUserGroups(userNode, connectorConfig.getSiteKey(), (List<String>) results.get(groupsAttributeAssignment));
                    }
                    userNode.saveSession();
                }
            }
        } catch (RepositoryException e) {
            logger.error("", e);
        }
    }

    private void removeUserMembership(JCRUserNode userNode) {
        User.getUserMembership(userNode).forEach((key, group) -> {
            try {
                group.removeMember(userNode);
                group.saveSession();
            } catch (RepositoryException e) {
                logger.error("", e);
            }
        });
    }

    private void manageUserGroups(JCRUserNode userNode, String siteKey, List<String> groups) {
        if (groups != null) {
            groups.forEach(groupName -> {
                try {
                    JCRGroupNode group = jahiaGroupManagerService.lookupGroup(siteKey, groupName);
                    if (group != null) {
                        group.addMember(userNode);
                        group.saveSession();
                    }
                } catch (RepositoryException e) {
                    logger.error("", e);
                }
            });
        }
    }
}
