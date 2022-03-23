import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
//import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;


import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.io.*;
import java.util.concurrent.CopyOnWriteArraySet;

class ServerWebSockets {

    public static void main(String[] args) throws Exception {
        Server server = new Server(28563);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        try {
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

            wscontainer.addEndpoint(WS_Server.class);

            server.start();
            server.join();
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }
}

@ServerEndpoint("/")
public class WS_Server {
	private static  HashSet<Session> people = new HashSet<Session>();
	private static  HashSet<String> log = new HashSet<String>();
	private static  HashSet<String> conn = new HashSet<String>();
	private static  HashMap<String,String> con_people = new HashMap<String,String>();
	private static int curt = 0;
	//private  HashSet<S> newConnect = new HashSet<int>();
    private static int concurrentClientCount = 0;

    @OnClose
    public void onClose(Session session, CloseReason reason) throws IOException {
        concurrentClientCount--;
		var us = con_people.get(session.getId());
		for (var x: people) {
			System.out.println(con_people.get(x.getId()));
			if (!x.getId().equals(session.getId()))
				x.getBasicRemote().sendText("dis " + us);
		}
		conn.remove(con_people.get(session.getId()));
		con_people.remove(session.getId());		
		people.remove(session);
    }

    @OnError
    public void onError(Throwable t) {
        //concurrentClientCount--;
        System.out.println("Error: " + t.getMessage());
    }

    @OnOpen
    public void onConnect(Session session) {
        concurrentClientCount++;		
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException{
        System.out.println("Data received: " + message);
        String[] words = message.split(" ");
        String answer = "ERR Unknown operator\r\n";
		if (words[0].equals("reg")) {			
			String login = words[1];
			String pass = words[2];			
			File l = new File("logins.txt");
			FileReader reader = new FileReader(l);
			Scanner scan = new Scanner(reader);	
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] tek = line.split(" ");
                if (tek[0].equals(login)) {
					session.getBasicRemote().sendText("This login is already used");
					return;
                }
            }
            FileWriter writer = new FileWriter(l, true);
            writer.write(login + ' ' + pass + '\n');
            writer.close();
			answer = "User was registrated";
		}
		if (words[0].equals("log")) {
			answer = "Login or password is not right";
			String login = words[1];
			String pass = words[2];			
			File l = new File("logins.txt");
			FileReader reader = new FileReader(l);
			Scanner scan = new Scanner(reader);	
			while (scan.hasNextLine()) {
				String line = scan.nextLine();
				String[] tek = line.split(" ");
                if (tek[0].equals(login)) {
                    if (tek[1].equals(pass)) {
						session.getBasicRemote().sendText("PASS");
						log.add(login); 
						return;
					}
                }
            }
		}
		if (words[0].equals("connect")) {	
			if (words.length == 1) {				
				session.getBasicRemote().sendText("Error");
				conn.remove(con_people.get(session.getId()));
				con_people.remove(session.getId());
				people.remove(session);
				return;
			}
			else
				if (conn.contains(words[1])) {
					session.getBasicRemote().sendText("Error");
					return;
				}
				if (log.contains(words[1])) {	
					conn.add(words[1]);
					log.remove(words[1]);
					con_people.put(session.getId(), words[1]);					
					people.add(session);					
					for (var x: people) {						
						x.getBasicRemote().sendText("con " + words[1]);	
						if(!x.getId().equals(session.getId()))					
							session.getBasicRemote().sendText("con " + con_people.get(x.getId()));
					}
				}
				else {
					session.getBasicRemote().sendText("Error");
				}
			return;
		}
		if (words[0].equals("mes")) {
			 GregorianCalendar gcalendar = new GregorianCalendar();
			 String t = gcalendar.get(Calendar.HOUR) + ":" + gcalendar.get(Calendar.MINUTE);
				String s = words[1];
			for (var x : people) {
				for (int i = 2; i < words.length; i++) {
					s += words[i];
				}
				if (x.getId().equals(session.getId())) 
					x.getBasicRemote().sendText("mes my " + con_people.get(session.getId()) + ' ' + t + ' ' + s);
				else 
					x.getBasicRemote().sendText("mes other " + con_people.get(session.getId()) + ' ' + t + ' ' + s);
					
			}			
			System.out.println("Message from: " + con_people.get(session.getId()) + "; Data: " + s + "; Time: " + t);
			return;
		}
        session.getBasicRemote().sendText(answer);
    }
}
