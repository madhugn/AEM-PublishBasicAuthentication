package com.adobe.aem.madhugubby.service.impl;

import com.adobe.aem.madhugubby.service.CommonService;
import com.adobe.aem.madhugubby.util.Constants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

@Component(immediate = true, service = CommonService.class)
public class CommonServiceImpl implements CommonService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonService.class);

    @Reference
    ResourceResolverFactory resolverFactory;


    /**
     * This method is for Write User to fetch resource resolver Please close the
     * resolver object at the end of the calling method
     */
    @Override
    public ResourceResolver getWriteResourceResolver() {
        HashMap<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, Constants.WRITE_SERVICE);
        ResourceResolver resolver = null;
        try {
            resolver = resolverFactory.getServiceResourceResolver(param);
            LOGGER.debug("ResourceResolver is {}", resolver);
        } catch (LoginException e) {
            LOGGER.error("LoginException", e);
        }
        return resolver;
    }

    /**
     * This method terminates provided resource resolver session.
     */
    @Override
    public void terminateResourceResolver(ResourceResolver resourceResolver) {
        if (resourceResolver != null) {
            resourceResolver.close();
        }
    }
}
