package com.adobe.aem.madhugubby.service.impl;

import com.adobe.aem.madhugubby.service.XlStudioEmailService;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
@Component(service = XlStudioEmailService.class)
public class XlStudioEmailServiceImpl implements XlStudioEmailService {
    @Reference
    private MessageGatewayService messageGatewayService;
    private final Logger logger = LoggerFactory.getLogger(getClass());


    @Override
    public void sendEmail(String subject, String message, String recipientMailId) throws EmailException {
        Email email = new HtmlEmail();
        email.addTo(recipientMailId);
        email.setSubject(subject);
        email.setMsg(message);
        MessageGateway<Email> messageGateway = messageGatewayService.getGateway(HtmlEmail.class);
        if (messageGateway != null) {
            logger.debug("sending out email");
            messageGateway.send((Email) email);
        } else {
            logger.error("The message gateway could not be retrieved.");
        }
    }
}
