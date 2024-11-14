package com.adobe.aem.guides.wknd.core.models.impl;

import com.adobe.aem.guides.wknd.core.models.PasswordUpdateModel;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.aem.madhugubby.util.Constants.*;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {PasswordUpdateModel.class},
        resourceType = {PasswordUpdateModelImpl.RESOURCE_TYPE},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PasswordUpdateModelImpl implements PasswordUpdateModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordUpdateModelImpl.class);
    protected static final String RESOURCE_TYPE = "wknd/components/vision-portal/resetPassword";

    @Self
    private SlingHttpServletRequest request;

    @Override
    public String getUserId() {
        return request.getParameter(USER_ID);
    }
    @Override
    public String getStatus() {
        if (request.getParameter(USER_ID) != null) {
            return "confirmation";
        } else {
            return "completion";
        }
    }
}
