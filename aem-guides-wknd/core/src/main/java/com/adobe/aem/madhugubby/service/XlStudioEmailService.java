package com.adobe.aem.madhugubby.service;

import org.apache.commons.mail.EmailException;

import java.util.Map;

public interface XlStudioEmailService {
    /**
     * This method is for sending emails
     */
   void sendEmail(String subject, String message, String recipientMailId) throws EmailException;
}
