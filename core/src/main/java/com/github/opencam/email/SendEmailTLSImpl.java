package com.github.opencam.email;

import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import com.github.opencam.imagegrabber.Resource;

public class SendEmailTLSImpl implements SendEmail {

  private Session sess;
  InternetAddress from;

  public SendEmailTLSImpl(final String host, final int port, final String username, final String password, final String from) {
    final Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", Integer.toString(port));

    final Authenticator auth = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    };

    sess = Session.getInstance(props, auth);
    try {
      this.from = new InternetAddress(from);
    } catch (final AddressException e) {
      throw new RuntimeException("Problem creating from address", e);
    }
  }

  public void sendAlertEmail(final List<String> recipients, final String subject, final String body, final List<Resource> images) {
    final MimeMessage message = new MimeMessage(sess);
    try {
      message.setSubject(subject);
      message.setFrom(from);
      for (final String to : recipients) {
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      }

      final MimeMultipart multipart = new MimeMultipart("related");
      final BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(body, "text/html");

      multipart.addBodyPart(messageBodyPart);

      if (images != null) {
        int i = 0;
        for (final Resource stream : images) {
          final BodyPart imagePart = new MimeBodyPart();
          imagePart.setHeader("Content-ID", "attach-" + i);
          final DataSource fds = new ByteArrayDataSource(stream.getData(), stream.getMimeType());
          imagePart.setDataHandler(new DataHandler(fds));
          multipart.addBodyPart(imagePart);
          i++;
        }
      }

      message.setContent(multipart);
      Transport.send(message);
    } catch (final Exception e) {
      throw new RuntimeException("Unabe to send message", e);
    }
  }
}
