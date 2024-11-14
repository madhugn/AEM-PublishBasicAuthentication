package com.adobe.aem.madhugubby.servlets;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.Servlet;

@Component(service = { Servlet.class }, property = { "sling.servlet.methods=head",
        "sling.servlet.paths=/bin/permissioncheck" })
public class AuthcheckerServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try{
            //retrieve the requested URL
            String uri = request.getParameter("uri");
            //obtain the session from the request
            Session session = request.getResourceResolver().adaptTo(javax.jcr.Session.class);
            //perform the permissions check
            try {
                session.checkPermission(uri, Session.ACTION_READ);
                logger.info("authchecker says OK");
                response.setStatus(SlingHttpServletResponse.SC_OK);
            } catch(Exception e) {
                logger.info("authchecker says READ access DENIED!");
                response.setStatus(SlingHttpServletResponse.SC_FORBIDDEN);
            }
        }catch(Exception e){
            logger.error("authchecker servlet exception: " + e.getMessage());
        }
    }
}

