# Jahia SAML Groups
Update user membership after SAML login

## Configuration
You can update the configuration thanks to the [Felix console](http://localhost:8080/tools/osgi/console/configMgr).
*org.jahiacommunity.saml.groups.GroupProcessor.cfg*
```
login-attribute-assignement=login
groups-attribute-assignement=groups
```
or with CURL command:
```
curl -s --user root:root -X POST http://localhost:8080/modules/api/commands -d "\
    config:edit org.jahiacommunity.saml.groups.GroupProcessor.cfg; \
    config:property-set login-attribute-assignement login; \
    config:property-set groups-attribute-assignement groups; \
    config:update;"
```

**Important**: Jahia groups must exist.
