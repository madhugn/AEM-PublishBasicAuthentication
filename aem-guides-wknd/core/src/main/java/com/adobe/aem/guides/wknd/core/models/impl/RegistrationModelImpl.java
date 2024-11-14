package com.adobe.aem.guides.wknd.core.models.impl;

import com.adobe.aem.guides.wknd.core.models.RegistrationModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.aem.madhugubby.util.Constants.CONFIRMATION_STRING;
import static com.adobe.aem.madhugubby.util.Constants.SITE_NAME;
@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {RegistrationModel.class},
        resourceType = {RegistrationModelImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class RegistrationModelImpl implements RegistrationModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationModelImpl.class);
    protected static final String RESOURCE_TYPE = "wknd/components/vision-portal/registrationConfirmation";

    @Self
    private SlingHttpServletRequest request;

    @Override
    public String getConfirmationString() {
        return request.getParameter(CONFIRMATION_STRING);
    }
    @Override
    public String getMicroSiteName() {
        return request.getParameter(SITE_NAME);
    }
    @Override
    public String getStatus() {
        if (request.getParameter(CONFIRMATION_STRING) != null && request.getParameter(SITE_NAME) != null) {
            return "confirmation";
        } else {
            return "completion";
        }
    }
}
