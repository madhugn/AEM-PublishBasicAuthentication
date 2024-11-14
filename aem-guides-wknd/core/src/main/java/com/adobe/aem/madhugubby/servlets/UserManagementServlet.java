package com.adobe.aem.madhugubby.servlets;

import com.adobe.aem.madhugubby.service.CommonService;
import com.adobe.aem.madhugubby.service.XlStudioEmailService;
import com.day.cq.commons.Externalizer;
import org.apache.commons.mail.EmailException;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This servlet must be accessible only on publish. This is used to view user details like list of users, membership and allow deletion of the user
 * GET: for a given user id, return the user profile details and group membership. Additional request param: userId=, operation=getDetails
 * GET: for a given group id, return all the user principals who are part of this group.Additional request param:  groupId=, operation=getDetails
 * GET: for a given user id, initiate the password reset functionality. Additional request params: userId=, operation=resetPassword
 * POST: delete a user with a user principal as a parameter. Additional request param:  delete
 * POST: update a user with password (reset). Additional request param: operation=passwordReset
 * POST: update a user with profile details. Additional request param: operation=updateDetails
 * POST: update a user with profile details. Additional request param: operation=deleteUser //Must not be publicly available
 */
@Component(name = "User Management Servlet", service = {Servlet.class}, property = {
        "sling.servlet.resourceTypes=api/v1/userManagement", "sling.servlet.methods=POST", "sling.servlet.methods=GET",
        "sling.servlet.extensions=do"})
public class UserManagementServlet extends SlingAllMethodsServlet {
    /**
     * The Constant serialVersionUID.
     */
    private static final long serialVersionUID = 638582347219405663L;

    private static final Logger LOGGER = LoggerFactory.getLogger(UserManagementServlet.class);

    @Reference
    Externalizer externalizer;

    @Reference
    CommonService commonService;

    @Reference
    private XlStudioEmailService xlStudioEmailService;

    @Override
    protected void doPost(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException, ServletException {
        String operation = servletRequest.getParameter("operation");
        String mode = servletRequest.getParameter("mode");
        String userId = servletRequest.getParameter("userId");
        String returnMessage = "If you are seeing this message, the operation was not performed. Please contact administrator for a detailed log";
        if (operation != null && userId != null) {
            ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            User user = null;
            try {
                user = (User) userManager.getAuthorizable(userId);
            } catch (RepositoryException e) {
                LOGGER.error("Error while fetching the user ID", e);
                returnMessage = "Error while fetching the user ID" + e;
            }
            if (user != null) {
                switch (operation) {
                    case "passwordResetInitiate": {
                        /**
                         * This parameter creates password reset link and emails it to the user. This should be triggered when the user clicks forgot password, enters the user id and submits.
                         *  The clicking of this link will result in passwordChange operation. The URL with querystring ?operation=passwordResetInitiate must be authored on the HTML
                         */
                        String queryString = "/content/wknd/resetPassword.html?userId=" + userId;
                        try {
                            sendEmail(servletRequest, queryString, user.getProperty("firstName"));
                            returnMessage = "Password reset link has been emailed to the user : " + userId;
                        } catch (RepositoryException e) {
                            LOGGER.error("Password reset inititation email sending failed for the user ID {}", userId, e);
                            returnMessage = "Password reset inititation email sending failed for the user ID {}" + userId + " Exception is:" + e;
                            if (mode==null) {
                                servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=serverError");
                                return;
                            }
                        }
                        if (mode==null) {
                            servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=resetEmailSent");
                        }
                        break;
                    }
                    case "passwordChange": {
                        String password = servletRequest.getParameter("password");
                        String passwordConfirm = servletRequest.getParameter("passwordConfirm");
                        boolean canProceed = false;
                        if (password == null || passwordConfirm == null) {
                            returnMessage = "Password (s) can't be empty. Aborting the process";
                            if (mode==null) { 
                                servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=passwordMismatch" + "&userId=" + userId);
                                return;
                            }
                        }
                        if (!password.equals(passwordConfirm)) {
                            returnMessage = "Input passwords don't match. Aborting the process";
                            if (mode==null) {
                                servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=passwordMismatch" + "&userId=" + userId);
                                return;
                            }
                        } else {
                            try {
                                user.changePassword(password);
                                resourceResolver.commit();
                                returnMessage = "Password has been reset for the user:" + userId;
                            } catch (RepositoryException e) {
                                LOGGER.error("Password reset failed", e);
                                returnMessage = "password reset failed: " + e;
                                if (mode==null) {
                                    servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=serverError");
                                    return;
                                }
                            }
                        }

                        if (mode == null) {
                            servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=success");
                        }
                        break;
                    }
                    case "updateDetails": {
                        break;
                    }
                    case "deleteUser": {
                        String initiatorId = servletRequest.getParameter("initiatorId");
                        Resource allowedIdResource = resourceResolver.getResource("/content/wknd/servletEndPoints/userManagement");
                        if (allowedIdResource != null) {
                            ValueMap allowedIdsValueMap = allowedIdResource.getValueMap();
                            String[] allowedIds = allowedIdsValueMap.get("allowedIds", String[].class);
                            if (allowedIds != null && allowedIds.length > 0) {
                                if (Arrays.stream(allowedIds).anyMatch(initiatorId::equals)) {
                                    try {
                                        user.remove();
                                        resourceResolver.commit();
                                        LOGGER.info("The user {} is deleted {} successfully. This was initiated by the admin user {}", userId, initiatorId);
                                        servletResponse.getWriter().write("The user has been deleted successfully:" + user.getID());
                                        returnMessage = "The user has been deleted successfully:" + userId;
                                    } catch (RepositoryException e) {
                                        LOGGER.error("User deletion failed {}", userId, e);
                                        returnMessage = "User deletion failed:" + userId + "Exception is: " + e;
                                        servletResponse.getWriter().write("The user could not be deleted:" + e);
                                    }
                                }
                            }
                        }
                        break;
                    }
                    case "addUserToGroup": {//Add a user to a group
                        String groupId = servletRequest.getParameter("groupId");
                        if (groupId != null) {
                            try {
                                Group group = (Group) userManager.getAuthorizable(groupId);
                                if (group != null && group.isGroup()) {
                                    group.addMember(user);
                                    resourceResolver.commit();
                                    LOGGER.info("The user {} is added to the usergroup {} successfully", userId, groupId);
                                    returnMessage = "The user {} is added to the usergroup {} successfully: " + userId + "GroupID: " + groupId;
                                }
                            } catch (RepositoryException e) {
                                LOGGER.error("Error while fetching the user group {} for the operation {}", groupId, operation, e);
                                returnMessage = "Error while fetching the user group {} for the operation {}" + groupId + " operation: " + operation + ". Exception is:" + e;
                            }
                        } else {
                            LOGGER.error("User group is empty. Operation: addUserToGroups requires user group to be sent. Aborting addUserToGroup operation");
                            returnMessage = "User group is empty. Operation: addUserToGroups requires user group to be sent. Aborting addUserToGroup operation";
                        }
                        break;
                    }

                    case "removeUserFromGroup": {//Remove a user from a group
                        String groupId = servletRequest.getParameter("groupId");
                        if (groupId != null) {
                            try {
                                Group group = (Group) userManager.getAuthorizable(groupId);
                                if (group != null && group.isGroup()) {
                                    group.removeMember(user);
                                    resourceResolver.commit();
                                    LOGGER.info("The user {} is removed from the usergroup {} successfully", userId, groupId);
                                    returnMessage = "The user {} is removed from the usergroup {} successfully: " + userId + "GroupID: " + groupId;
                                }
                            } catch (RepositoryException e) {
                                LOGGER.error("Error while fetching the user group {} for the operation {}", groupId, operation, e);
                                returnMessage = "Error while fetching the user group {} for the operation {}" + groupId + " operation: " + operation + ". Exception is:" + e;
                            }
                        } else {
                            LOGGER.error("User group is empty. Operation: removeUserFromGroup requires user group to be sent. Aborting removeUserFromGroup operation");
                            returnMessage = "User group is empty. Operation: removeUserFromGroup requires user group to be sent. Aborting removeUserFromGroup operation";
                        }
                        break;
                    }
                    default: {
                        LOGGER.warn("Operation param {} did not match any of the crtieria. Aborting user update process", operation);
                        returnMessage = "Operation param {} did not match any of the crtieria. Aborting user update process " + operation;
                        break;
                    }
                }
            } else {
                LOGGER.error("User ID not found. Aborting user update process");
                returnMessage = "User ID not found. Aborting user update process";
                if (mode == null) {
                    servletResponse.sendRedirect("/content/wknd/resetPassword.html?message=invalidUserId");
                    return;
                }
            }
        } else {
            LOGGER.warn("Illegal user update process trigger as operation is null. Aborting user update process");
        }
        servletResponse.getWriter().write("The user details are:" + returnMessage);
    }


    @Override
    protected void doGet(SlingHttpServletRequest servletRequest, SlingHttpServletResponse servletResponse)
            throws IOException {
        String userId = servletRequest.getParameter("userId");
        String groupId = servletRequest.getParameter("groupId");
        StringBuilder returnValue = new StringBuilder();
        if (userId != null) {
            ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            try {
                Authorizable authorizable = userManager.getAuthorizable(userId);
                if (authorizable != null && !authorizable.isGroup()) {
                    Iterator<Group> groupAffiliation = authorizable.memberOf();
                    while (groupAffiliation.hasNext()) {
                        Authorizable a = groupAffiliation.next();
                        returnValue.append("Group membership from .memberOf(): \n");
                        returnValue.append("ID is: " + a.getID() + ". Principal.getName is: " + a.getPrincipal().getName());
                        returnValue.append(".\n  ");
                    }

                    Iterator<Group> groupAff = authorizable.declaredMemberOf();
                    while (groupAff.hasNext()) {
                        Authorizable a = groupAff.next();
                        returnValue.append("Group membership from .declaredMemberOf(): \n");
                        returnValue.append("ID is: " + a.getID() + ". Principal.getName is: " + a.getPrincipal().getName());
                        returnValue.append(".\n  ");
                    }

                    User user = (User) authorizable;
                    returnValue.append("User Details are: \n");
                    returnValue.append("User Path is: " + user.getPath() + "\n");
                    returnValue.append("First Name:" + user.getProperty("familyName") + "\n:");
                    returnValue.append("First Name:" + user.getProperty("givenName") + "\n:");
                    returnValue.append("Email:" + user.getID() + "\n:");
                    returnValue.append("Role:" + user.getProperty("role") + "\n:");

                } else {
                    LOGGER.error("The ID supplied is a group and not a user ID. Aborting {}", userId);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error while fetching the details for the principal {}  and error is {}", userId, e);
            }

        }
        if (groupId != null) {
            ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
            UserManager userManager = resourceResolver.adaptTo(UserManager.class);
            try {
                Authorizable authorizable = userManager.getAuthorizable(groupId);
                if (authorizable != null && authorizable.isGroup()) {
                    Group group = (Group) authorizable;
                    Iterator<Authorizable> members = group.getMembers();
                    while (members.hasNext()) {
                        Authorizable a = members.next();
                        returnValue.append("User from getMembers is: ");
                        returnValue.append("ID is: " + a.getID() + "Principal.getName is: " + a.getPrincipal().getName());
                        returnValue.append(".\n  ");
                    }
                    Iterator<Authorizable> declaredMembers = group.getDeclaredMembers();

                    while (declaredMembers.hasNext()) {
                        Authorizable a = declaredMembers.next();
                        returnValue.append("User from getDeclaredMembers is: ");
                        returnValue.append("ID=" + a.getID() + "and Principal Name is:" + a.getPrincipal().getName());
                        returnValue.append(".\n  ");
                    }
                } else {
                    LOGGER.error("The ID supplied is a user ID and not a group. Aborting {}", groupId);
                }
            } catch (RepositoryException e) {
                LOGGER.error("Error while fetching the details for the principal {}  and error is {}", groupId, e);
            }
        }
        servletResponse.getWriter().write("The user details are:" + returnValue.toString());
    }

    private void sendEmail(SlingHttpServletRequest servletRequest, String finalQueryString, Value[] firstNames) {
        String firstName = "Xl Studio User";
        if (firstNames != null && firstNames.length > 0) {
            Value v = firstNames[0];
            try {
                firstName = v.getString();
            } catch (RepositoryException e) {
                LOGGER.error("Can't extract first name for the user principal {} and the error is {}", finalQueryString, e);
            }
        }
        String subjectLine = "Password Reset Initialized";
        ResourceResolver resourceResolver = commonService.getWriteResourceResolver();
        String serverUrl = externalizer.publishLink(resourceResolver, "/");
        String msgBody = "Dear " + firstName + ", <br> Please complete the Adobe Partnership website password reset by clicking this link: " + serverUrl + finalQueryString + "<br>Regards, <br>Adobe XLStudio Team";
        String recipient = servletRequest.getParameter("userId");
        LOGGER.warn("User Password Reset Link:{}", serverUrl + finalQueryString);//This is for debugging and to manually activate a user if a user does not receive an email
        try {
            xlStudioEmailService.sendEmail(subjectLine, msgBody, recipient);
        } catch (EmailException e) {
            LOGGER.error("Sending email for Password Reset to the recipient {} failed due to the error {}", recipient, e);
        }
    }
}
