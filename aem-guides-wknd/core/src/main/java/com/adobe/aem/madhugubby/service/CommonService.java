package com.adobe.aem.madhugubby.service;
import org.apache.sling.api.resource.ResourceResolver;
public interface CommonService {
    /**
     * This method fetches the write resource resolver
     * TODO: in real application, you must create another method getReadResourceResolver that uses a separate service user
     */
    ResourceResolver getWriteResourceResolver();
    void terminateResourceResolver(ResourceResolver resourceResolver);
}
