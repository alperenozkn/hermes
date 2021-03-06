package services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import controllers.MessageController;
import domain.Channel;
import domain.Log;
import domain.User;
import repositories.ChannelRepository;
import repositories.MessageRepository;
import repositories.SessionRepository;
import repositories.UserRepository;

@Component
public class AsyncMailReceiver {
	@Autowired
	private UserService userService;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ChannelService channelService;
	@Autowired
	private ChannelRepository channelRepository;
	@Autowired
	private SessionRepository sessionRepository;
	@Autowired
	private SessionService sessionService;
	@Autowired
	private MessageService messageService;
	@Autowired
	private MessageRepository messageRepository;
	@Autowired
	private LogService logService;
	@Autowired
	private MessageController sendController;
	
	@Scheduled(fixedDelay=10000)
	public void checkMails() {
	    System.out.println("[*]Mail receiver");
	    String host = "imap.gmail.com";
	    String mailStoreType = "imap";
	    String user = "hermesanonymousmessaging@gmail.com";
	    String password = "hgpturotvshvpxlk";
	    
	    try {
	        Properties properties = new Properties();
	        properties.put("mail.imap.host", host);
	        properties.put("mail.imap.port", "993");
	        properties.put("mail.imap.starttls.enable", "true");
	        Session emailSession = Session.getDefaultInstance(properties);
	    
	        Store store = emailSession.getStore("imaps");

	        store.connect(host, user, password);

	        Folder emailFolder = store.getFolder("INBOX");
	        emailFolder.open(Folder.READ_WRITE);

	        Message[] messages = emailFolder.search(new FlagTerm(new Flags(
                    Flags.Flag.SEEN), false));

	        addMailsToDatabase(messages);
	               
	        emailFolder.close(false);
	        store.close();

	        } catch (NoSuchProviderException e) {
	           e.printStackTrace();
	        } catch (MessagingException e) {
	           e.printStackTrace();
	        } catch (Exception e) {
	           e.printStackTrace();
	        }
	}
	
	private String getTextFromMessage(Message message) 
			throws MessagingException, IOException {
	    String result = "";
	    if (message.isMimeType("text/plain")) {
	        result = message.getContent().toString();
	    } else if (message.isMimeType("multipart/*")) {
	        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
	        result = getTextFromMimeMultipart(mimeMultipart);
	    }
	    String lines[] = result.split("\\r?\\n");
        if(lines.length >= 1)
        	result = lines[1];
	    return result;
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)  
			throws MessagingException, IOException{
	    String result = "";
	    int count = mimeMultipart.getCount();
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        if (bodyPart.isMimeType("text/plain")) {
	            result = result + "\n" + bodyPart.getContent();
	            break; // without break same text appears twice in my tests
	        } else if (bodyPart.isMimeType("text/html")) {
	            String html = (String) bodyPart.getContent();
	            result = result + "\n" + Jsoup.parse(html).text();
	        } else if (bodyPart.getContent() instanceof MimeMultipart){
	            result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
	        }
	    }
	    return result;
	}
	
	private void addMailsToDatabase(Message[] messages) throws MessagingException, IOException {
		System.out.println("[*]No. of Unread Messages : " + messages.length);
        

        for (int i = 0, n = messages.length; i < n; i++) {
           Message message = messages[i];
           System.out.println("---------------------------------");
           System.out.println("Email Number " + (i + 1));
           String title = message.getSubject().replace("Re: ","").replace("Re:","");
           System.out.println("Subject: " + title);
           System.out.println("From: " + message.getFrom()[0]);
           String text = getTextFromMessage(message);
           System.out.println("Text: " + text);
           String[] address = message.getFrom()[0].toString().split(" ");
           String mail = address[address.length - 1];
           mail = mail.replace("<","").replace(">","");
           System.out.println(mail);
           MimeMessage source = (MimeMessage) message;
           MimeMessage copy = new MimeMessage(source);
           User sender = userRepository.findByEmailIgnoreCase(mail);
           if(sender != null)
        	   System.out.println("DBYE KAYITLI");
           List<String> channelIds = sender.getChannelsList();
           Channel channel = null;
           
           for(String channelId : channelIds) {
        	   Channel itChannel = channelService.getById(channelId);
        	   if(itChannel.getName().compareToIgnoreCase(title) == 0) {
        		   channel = itChannel;
        		   break;
        	   }
           }
           if(channel == null)
        	   return;
           List<domain.Session> sessions = sessionRepository.findByChannelId(channel.getId());
           domain.Session session = null;
           for(domain.Session tempSession : sessions) {
        	   if(tempSession.isActive()) {
        		   session = tempSession;
        		   break;
        	   }
           }
           if(session == null)
        	   return;
           
           
           sendController.sendMsg(sender.getId(), text, session.getId(), channel.getId());
	   		
			Log newlog = new Log("UserID: " + sender.getId()+ " has sent a new message via mail to channel with ID: " + channel.getId() + " with session ID: " + session.getId());
			logService.saveOrUpdate(newlog);
           
        }
	}
	
}
