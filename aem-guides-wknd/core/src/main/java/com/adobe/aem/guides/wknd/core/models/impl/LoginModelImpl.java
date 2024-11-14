package com.adobe.aem.guides.wknd.core.models.impl;

import com.adobe.aem.guides.wknd.core.models.LoginModel;
import com.adobe.aem.madhugubby.service.EnvVariablesConfig;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {LoginModel.class},
        resourceType = {LoginModelImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class LoginModelImpl implements LoginModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationModelImpl.class);
    protected static final String RESOURCE_TYPE = "wknd/components/vision-portal/login";

    @OSGiService
    EnvVariablesConfig envVariables;
    @Override
    public String getSsoUrl() {
        return envVariables.getIdpUrl();
    }
}
