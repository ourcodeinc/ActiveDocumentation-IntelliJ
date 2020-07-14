/*
 * written by saharmehrpour
 * This class creates the chat server using websockets.
 */

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 * When sendToAll is called, the message will be placed on a backlog of messages.
 * If the connection is open, the messages on the backlog will be sent out, so
 * the order in which the messages are told to be sent is preserved.
 */
public class ChatServer extends WebSocketServer {

    /**
     * @param port = 8887
     * @throws UnknownHostException if it fails to build a connection
     */
    ChatServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("(onOpen) " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");

        FileChangeManager manager = FileChangeManager.getInstance();

        // check whether the project is changed
        manager.checkChangedProject();
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "ENTER", (conn + " connected to ActiveDocumentation")}).toString());
        for (int i = 0; i < manager.getSrcml().fileNumber; i++) {
            this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "XML",
                    MessageProcessor.encodeXMLData(new Object[]{manager.getSrcml().getPaths().get(i), manager.getSrcml().getXmls().get(i)})
            }).toString());
        }
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", FollowAndAuthorRulesProcessor.getInstance().getRuleTable()}).toString());
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", FollowAndAuthorRulesProcessor.getInstance().getTagTable()}).toString());
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", manager.generateProjectHierarchyAsJSON()}).toString());
        this.sendAMessage(conn, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_PATH", manager.projectPath}).toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        this.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "LEFT", (conn + " disconnected")}).toString());
        System.out.println("(onClose) " + conn + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JsonParser parser = new JsonParser();
        final JsonObject messageAsJson = parser.parse(message).getAsJsonObject();

        FollowAndAuthorRulesProcessor faw = FollowAndAuthorRulesProcessor.getInstance();
        MiningRulesProcessor mr = MiningRulesProcessor.getInstance();

        if (faw.wsMessages.contains(messageAsJson.get("command").getAsString())) {
            faw.processReceivedMessages(messageAsJson);
        }

        else if (mr.wsMessages.contains(messageAsJson.get("command").getAsString())) {
            mr.processReceivedMessages(messageAsJson);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("(onError) ");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("(Started) ");
    }

    /**
     * @param text The String to send across the network.
     */
    void sendToAll(String text) {
        Collection<WebSocket> conn = getConnections();
        if (conn.size() == 0) {
            System.out.println("(sendToAll) " + "There's no connection.");
        } else {
            for (WebSocket c : conn) {
                c.send(text);
                System.out.println("(sendMessage) ");
            }
        }
    }

    private void sendAMessage(WebSocket con, String message) {
        con.send(message);
    }
}
