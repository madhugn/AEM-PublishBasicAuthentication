package com.adobe.aem.madhugubby.service.impl;

import com.adobe.aem.madhugubby.service.EnvVariablesConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(immediate = true, service = EnvVariablesConfig.class)
@Designate(ocd = EnvVariablesConfigImpl.Config.class)
public class EnvVariablesConfigImpl implements EnvVariablesConfig {

    @Activate
    EnvVariablesConfigImpl.Config config;

    @Override
    public String getIdpUrl() {
        return config.idpUrl();
    }

    @Override
    public String getEdgeConfigId() {
        return config.edgeConfigId();
    }

    @Override
    public String getLaunchURL() {
        return config.launchURL();
    }

    @Override
    public String getImsOrgId() {
        return config.imsOrgId();
    }

    @Override
    public String getClientSecret() {
        return config.clientSecret();
    }

    @Override
    public String getApiKey() {
        return config.apiKey();
    }

    @Override
    public String getCode() {
        return config.code();
    }

    @Override
    public String getFireFallUrl() {
        return config.fireFallUrl();
    }

    @Override
    public String getOauthEndPoint() {
        return config.oauthEndPoint();
    }

    @ObjectClassDefinition(name = "XLStudio Environment Variables access  config")
    public @interface Config {

        /**
         * Get ID URL
         *
         * @return the string [ ]
         */
        @AttributeDefinition(
                name = "Get IDP URL",
                description = "SAML IDP URL cofigured for the current environment"
        )
        String idpUrl() default "Please configure IDP URL in Environment Variables";

        /**
         * Get edgeConfigId
         *
         * @return the string [ ]
         */
        @AttributeDefinition(
                name = "Get edgeConfigId",
                description = "edgeConfigId for analytics configured for the current environment"
        )
        String edgeConfigId() ;

        /**
         * Get launchURL
         *
         * @return the string [ ]
         */
        @AttributeDefinition(
                name = "Get launchURL",
                description = "launchURL for analytics configured for the current environment"
        )
        String launchURL() ;

        @AttributeDefinition(
                name = "Get imsOrgId",
                description = "imsOrgId : this is  x-gw-ims-org-id header for oauthEndpoint"
        )
        String imsOrgId() default "AB91714A5A2739A50A494234@AdobeOrg";

        @AttributeDefinition(
                name = "Get clientSecret",
                description = "clientSecret is a header for oauthEndpoint"
        )
        String clientSecret() ;

        @AttributeDefinition(
                name = "Get apiKey",
                description = "apiKey is a header client_id header for oauthEndpoint"
        )
        String apiKey();

        @AttributeDefinition(
                name = "Get code",
                description = "code is a header code header for oauthEndpoint"
        )
                //TODO: delete the code before committing to GIT
        String code();

        @AttributeDefinition(
                name = "Get fireFallUrl",
                description = "fireFallUrl end point for fetching GenAI content"
        )
        String fireFallUrl();


        @AttributeDefinition(
                name = "Get oauthEndPoint",
                description = "oauthEndPoint end point for fetching bearer and refresh oauth tokens"
        )
        String oauthEndPoint() default "";
    }
}