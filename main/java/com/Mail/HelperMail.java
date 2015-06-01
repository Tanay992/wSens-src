package com.Mail;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

//TODO:
//Currently we have enabled access for "Less Secure Apps" for our wearsens gmail account
//Later use a stronger authentication process in this class, so we don't need to enable this access


//Class Usage:
//Instantiate a HelperMail object by passing the arguments (don't use default constructor)
//Call obj.createEmailMessage(), handling the exceptions (you can ignore the return value)
//Call obj.sendEmail(), handling the exceptions.


public class HelperMail {

    final String emailPort = "587";// gmail's smtp port - change for another mail server
    final String smtpAuth = "true";
    final String starttls = "true";
    final String emailHost = "smtp.gmail.com";

    String fromEmail;
    String fromPassword;
    List<String> toEmailList;
    String emailSubject;
    String emailBody;

    Properties emailProperties;
    Session mailSession;
    MimeMessage emailMessage;

    public HelperMail() {

    }


    //Create a Helper object with the required fields.
    public HelperMail(String fromEmail, String fromPassword,
                 List toEmailList, String emailSubject, String emailBody) {
        this.fromEmail = fromEmail;
        this.fromPassword = fromPassword;
        this.toEmailList = toEmailList;
        this.emailSubject = emailSubject;
        this.emailBody = emailBody;

        emailProperties = System.getProperties();
        emailProperties.put("mail.smtp.port", emailPort);
        emailProperties.put("mail.smtp.auth", smtpAuth);
        emailProperties.put("mail.smtp.starttls.enable", starttls);
        Log.i("HelperMail", "Mail server properties set.");
    }

    //Creates and returns an email message (MimeMessage) out of the Gmail object.
    public MimeMessage createEmailMessage() throws AddressException,
            MessagingException, UnsupportedEncodingException {

        mailSession = Session.getDefaultInstance(emailProperties, null);
        mailSession.setDebug(true);
        emailMessage = new MimeMessage(mailSession);

        emailMessage.setFrom(new InternetAddress(fromEmail, fromEmail));
        for (String toEmail : toEmailList) {
            Log.i("HelperMail","toEmail: "+toEmail);
            emailMessage.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(toEmail));
        }

        emailMessage.setSubject(emailSubject);
        emailMessage.setSentDate(new Date());
        //emailMessage.setContent(emailBody, "text/html");// for a html email
        emailMessage.setText(emailBody);// for a text email
        Log.i("HelperMail", "Email Message created.");
        return emailMessage;
    }

    public void sendEmail() throws AddressException, MessagingException {

        Transport transport = mailSession.getTransport("smtp");
        transport.connect(emailHost, fromEmail, fromPassword);
        Log.i("HelperMail","allrecipients: "+emailMessage.getAllRecipients());
        transport.sendMessage(emailMessage, emailMessage.getAllRecipients());
        transport.close();
        Log.i("HelperMail", "Email sent successfully.");
    }

}
