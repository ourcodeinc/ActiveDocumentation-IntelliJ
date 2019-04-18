import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.java_websocket.*;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/*
 * edited by saharmehrpour
 */

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 * When sendToAll is called, the message will be placed on a backlog of messages.
 * If the connection is open, the messages on the backlog will be sent out, so
 * the order in which the messages are told to be sent is preserved.
 */
public class ChatServer extends WebSocketServer {

    private List<String> backedUpMessages = new LinkedList<>();
    private FileChangeManager manager; // to process received messages

    /**
     *
     * @param port = 8887
     * @throws UnknownHostException
     */
    ChatServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    void setManager(FileChangeManager fcm) {
        manager = fcm;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
        System.out.println("(onOpen) " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");

        // check whether the project is changed
        manager.checkChangedProject();

        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "ENTER", (conn + " has entered the room!")}).toString());

//        for (int i = 0; i < Math.min(5, manager.getSrcml().fileNumber); i++) {
        for (int i = 0; i < manager.getSrcml().fileNumber; i++) {
            this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "XML",
                    MessageProcessor.encodeXMLData(new Object[]{manager.getSrcml().getPaths().get(i), manager.getSrcml().getXmls().get(i)})
            }).toString());
        }

        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", manager.getAllRules()}).toString());
        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", manager.getAllTags()}).toString());
        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", manager.generateProjectHierarchyAsJSON()}).toString());
        this.sendInitialMessages(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT", manager.projectPath}).toString());

        sendBackedUpMessages();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        this.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "LEFT", (conn + " has left the room!")}).toString());
        System.out.println("(onClose) " + conn + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        JsonParser parser = new JsonParser();
        final JsonObject messageAsJson = parser.parse(message).getAsJsonObject();
        System.out.println("(onMessage) " /*+ messageAsJson*/);
        System.out.println(message);
        manager.processReceivedMessages(messageAsJson);

    }

    @Override
    public void onFragment(WebSocket conn, Framedata fragment) {
        System.out.println("(onFragment) " /*+ "received fragment: " + fragment*/);
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("(onError) ");
        ex.printStackTrace();
    }

    /**
     * @param text The String to send across the network.
     */
    void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        backedUpMessages.add(text);
        if (con.size() == 0) {
            System.out.println("(sendToAll) " + "Putting message on hold since there's no connection.");
        } else {
            sendMessage(con);
        }
    }

    private void sendBackedUpMessages() {

        Collection<WebSocket> con = connections();
        if (con.size() == 0) {
            if (backedUpMessages.size() > 0) {
                System.out.println("(sendBackedUpMessages) " + "Can't clear out backlog since there's no connection right now.");
            }
        } else {
            sendMessage(con);
        }
    }


    /**
     * extracted local method
     * send the message to all connections
     *
     * @param con list of connections
     */
    private void sendMessage(Collection<WebSocket> con) {
        while (con.size() != 0 && !backedUpMessages.isEmpty()) {
            String itemToSend = backedUpMessages.get(0);
            synchronized (con) {
                for (WebSocket c : con) {
                    c.send(itemToSend);
                    System.out.println("(sendMessage) " /*+ "Server sent: " + itemToSend*/);
                }
            }
            backedUpMessages.remove(0);
        }
    }


    private void sendInitialMessages(WebSocket con, String message) {
        con.send(message);
    }
}
