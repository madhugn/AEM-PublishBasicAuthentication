package com.adobe.aem.madhugubby.servlets;

import com.adobe.aem.madhugubby.service.CommonService;
import com.adobe.aem.madhugubby.util.Constants;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Base64;

import static com.adobe.aem.madhugubby.util.Constants.CONFIRMATION_STRING;
import static com.adobe.aem.madhugubby.util.Constants.SITE_NAME;

@Component(name = "User Registration Confirmation Servlet", service = {Servlet.class}, property = {
        "sling.servlet.resourceTypes=api/v1/registration/confirmation", "sling.servlet.methods=GET","sling.servlet.methods=POST",
        "sling.servlet.extensions=do"})
public class RegistrationConfirmationServlet extends SlingAllMethodsServlet {
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 6385208307219405663L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationConfirmationServlet.class);
    @Reference
    transient CommonService commonService;

    private void registerUser(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse) throws IOException, RepositoryException {
        String confirmationString = servletRequest.getParameter(CONFIRMATION_STRING);
        String microSiteName = servletRequest.getParameter(SITE_NAME);
        if (confirmationString != null && microSiteName != null) {
            byte[] decodedBytesUserPath = Base64.getDecoder().decode(confirmationString);
            String decodedUserPath = new String(decodedBytesUserPath);
            ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            Authorizable authorizable = userManager.getAuthorizableByPath(decodedUserPath);
            if (authorizable == null) {
                servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=failure");
            } else {
                byte[] decodedBytesSiteName = Base64.getDecoder().decode(microSiteName);
                String decodedSiteName = new String(decodedBytesSiteName);
                Group group = (Group) userManager.getAuthorizable(Constants.EVP_CUG_GROUP.replaceAll("<siteName>", decodedSiteName));
                if (group == null) {
                    LOGGER.warn("The user group is invalid {}. CUG affiliation failed for the user {}", decodedSiteName, decodedUserPath);
                    servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=failure");
                    return;
                } else if (group.isGroup()) {
                    group.addMember(authorizable);
                    resourceResolver.commit();
                    LOGGER.info("The user group {}. is updated the user {} successfully", decodedSiteName, decodedUserPath);
                    servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=success");
                } else {
                    LOGGER.warn("The user group specified is not a group {}. CUG update failed for the user {}", microSiteName, confirmationString);
                    servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=failure");
                }
            }
        } else {
            LOGGER.warn("The user registration for {} can't be completed. Please check the error log for more details ", servletRequest.getParameter("confirmationString"));
            servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=failure");
            return;
        }
    }

    @Override
    protected void doGet(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException {
        String confirmationString = servletRequest.getParameter(CONFIRMATION_STRING);
        String siteName = servletRequest.getParameter(SITE_NAME);
        servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?"+CONFIRMATION_STRING +"="+confirmationString+"&" +SITE_NAME + "="+siteName);
    }

    @Override
    protected void doPost(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException {
        try {
            registerUser(servletRequest, servletResponse);
        } catch (RepositoryException e) {
            servletResponse.setStatus(500);
            LOGGER.warn("The user registration for {} can't be completed due to an exception {}. ", servletRequest.getParameter("confirmationString"), e);
            servletResponse.sendRedirect("/content/wknd/registrationCompletion.html?message=failure");
        }
    }
}
