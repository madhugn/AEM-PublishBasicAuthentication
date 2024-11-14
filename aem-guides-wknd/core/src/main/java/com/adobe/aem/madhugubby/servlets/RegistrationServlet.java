package com.adobe.aem.madhugubby.servlets;

import com.adobe.aem.madhugubby.service.CommonService;
import com.adobe.aem.madhugubby.service.XlStudioEmailService;
import com.day.cq.commons.Externalizer;
import org.apache.commons.mail.EmailException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.AuthorizableExistsException;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.jackrabbit.value.StringValue;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Base64;

import static com.adobe.aem.madhugubby.util.Constants.*;

@Component(name = "New User Registration Servlet", service = { Servlet.class }, property = {
        "sling.servlet.resourceTypes=api/v1/registration/new", "sling.servlet.methods=POST","sling.servlet.methods=GET",
        "sling.servlet.extensions=do" })
public class RegistrationServlet extends SlingAllMethodsServlet {
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 6385208307219405663L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationServlet.class);

    @Reference
    Externalizer externalizer;

    @Reference
    CommonService commonService;

    @Reference
    private XlStudioEmailService xlStudioEmailService;

    @Override
    protected void doPost(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException, ServletException {
        try {
           Authorizable newUser =  initiateRegistration(servletRequest, servletResponse);
           if (newUser != null) {
               String userPath = newUser.getPath();
               String confirmationString = Base64.getEncoder().encodeToString(userPath.getBytes());
               String siteName = servletRequest.getParameter(SITE_NAME);
               if (siteName != null) {
                   siteName = Base64.getEncoder().encodeToString(siteName.getBytes());
               } else {
                   siteName = "default";//TODO: A default user group\website must be redirected to upon this param missing
               }
               String finalQueryString = "?confirmationString="+confirmationString+"&siteName="+siteName;
               sendEmail(servletRequest, finalQueryString);
               servletResponse.setStatus(200);
               servletResponse.sendRedirect("/content/wknd/register.html?message=success");
           }
        } catch (AuthorizableExistsException aex) {
            LOGGER.error("user registration failed as the user ID already exists", aex);
            servletResponse.sendRedirect("/content/wknd/register.html?message=userAlreadyExists");
        }
        catch (RepositoryException|IllegalArgumentException e) {
            LOGGER.error("user registration failed due to the error", e);
            servletResponse.sendRedirect("/content/wknd/register.html?message=serverError");
        }


    }

    private Authorizable initiateRegistration(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse) throws IOException, RepositoryException, ServletException {
        String userName = servletRequest.getParameter("username"); //Username must be email address as well
        String password = servletRequest.getParameter("password");
        String passwordConfirm = servletRequest.getParameter("passwordConfirm");
        String firstName = servletRequest.getParameter("firstName");
        //String lastName = servletRequest.getParameter("lastName");
        String siteName = servletRequest.getParameter("siteName");//TODO: Set this value at user profile level
        ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);

        if (password==null || passwordConfirm == null) {
            servletResponse.sendRedirect("/content/wknd/register.html?message=passwordEmpty");
            return null;
        }

        if (!password.equals(passwordConfirm)){
            servletResponse.sendRedirect("/content/wknd/register.html?message=passwordMismatch");
            return null;
        }
        if (userName != null && userName.indexOf("@")>0 && userName.indexOf(".")>0) {
            if (userName.contains("@adobe.com")) { // Do not allow @adobe.com domain users to register on the site
                servletResponse.sendRedirect("/content/wknd/register.html?message=adobeSSOLoginOnly");
                return null;
            }

            if (!isDomainWhitelisted(resourceResolver, userName)) {
                servletResponse.sendRedirect("/content/wknd/register.html?message=domainNotRegistered");
                return null;
            } else {
                Authorizable authorizable = userManager.createUser(userName, password, new PrincipalImpl(userName), USER_CREATION_PATH);
                authorizable.setProperty("email", new StringValue(userName));
                authorizable.setProperty("firstName", new StringValue(firstName));
                String role = servletRequest.getParameter("role");
                if (role != null && !role.equalsIgnoreCase("choose") );
                {
                    authorizable.setProperty("role", new StringValue(role));
                }
                resourceResolver.commit();
                return authorizable;
            }
        }
        else {
            servletResponse.sendRedirect("/content/wknd/register.html?message=invalidEmail");
        }
        return  null;
    }

    /**
     * This method checks if the current domain is whitelisted on aem side so that only the allowed domain users can create account
     * The domain must be registered under /content/wknd/configuration/allowed-domains
     * @param resourceResolver
     * @param userName
     * @return
     */
    private boolean isDomainWhitelisted(ResourceResolver resourceResolver, String userName) {
        //check if email domain matches that of what is registered on AEM.
        String domainName = userName.substring(userName.indexOf("@")+1, userName.length());
        Resource domainResource = resourceResolver.getResource(ALLOWED_DOMAIN_CONFIG_PATH+FORWARD_SLASH+domainName);
        return domainResource != null;
    }

    @Override
    protected void doGet(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException {
        servletResponse.getWriter().write("Registration End point is running");
    }

    private void sendEmail(SlingHttpServletRequest servletRequest, String finalQueryString)  {
        String subjectLine = "Registration completion for the Adobe Partnership Site";
        ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
        String serverUrl = externalizer.publishLink(resourceResolver, "/");
        String msgBody = "Dear "+ servletRequest.getParameter("firstName") + ", <br> Please complete the Adobe Partnership website registration by clicking this link: " + serverUrl+"/content/wknd/servletEndPoints/registrationConfirmation.do"+finalQueryString + "<br>Regards, <br>Adobe XLStudio Team";
        String recipient = servletRequest.getParameter("username");
        LOGGER.warn("User Registration Link:{}", serverUrl+"/content/wknd/servletEndPoints/registrationConfirmation.do"+finalQueryString);//This is for debugging and to manually activate a user if a user does not receive an email
        try {
            xlStudioEmailService.sendEmail(subjectLine,msgBody, recipient);
        } catch (EmailException e) {
            LOGGER.error("Sending email to the recipient {} failed due to the error {}", recipient, e);
        }
    }
}
