import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.java_websocket.*;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.awt.*;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple WebSocketServer implementation. Keeps track of a "chatroom".
 * When sendToAll is called, the message will be placed on a backlog of messages.
 * If the connection is open, the messages on the backlog will be sent out, so
 * the order in which the messages are told to be sent is preserved.
 */
public class ChatServer extends WebSocketServer {

	private List<String> backedUpMessages = new LinkedList<>();

	/**
	 * only used in GrepServerToolWindowFactory.run()
	 * @param port = 8887
	 * @param initialMessage from  generateProjectHierarchyAsJSON()
	 * @throws UnknownHostException
	 */
	public ChatServer(int port, String initialMessage) throws UnknownHostException {
		super(new InetSocketAddress(port));
		backedUpMessages.add(initialMessage);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		// this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
		System.out.println("(onOpen) " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!");
		sendBackedUpMessages();
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		this.sendToAll(conn + " has left the room!");
		System.out.println("(onClose) " + conn + " has left the room!");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		this.sendToAll(message);
		System.out.println("(onMessage) " + conn + ": " + message);

		JsonParser parser = new JsonParser();
		final JsonObject messageAsJson = parser.parse(message).getAsJsonObject();
		final JsonObject theDataFromTheMessage = messageAsJson.get("data").getAsJsonObject();

		// Only one command is handled

		if (messageAsJson.get("command").getAsString().equals("JUMP_TO_CLASS_WITH_LINE_NUM")) {
			EventQueue.invokeLater(() -> {
				String fileToFocusOn = theDataFromTheMessage.get("fileName").getAsString();
				int indexToFocusOn = theDataFromTheMessage.get("lineNumber").getAsInt(); // the character index
				Project currentProject = ProjectManager.getInstance().getOpenProjects()[0];
				VirtualFile theVFile = FilenameIndex.getVirtualFilesByName(currentProject, fileToFocusOn,
						GlobalSearchScope.projectScope(currentProject)).iterator().next();
				FileEditorManager.getInstance(currentProject).openFile(theVFile, true);
				Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
				theEditor.getCaretModel().moveToOffset(indexToFocusOn);
				theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
			});
		}
	}

	@Override
	public void onFragment(WebSocket conn, Framedata fragment) {
		System.out.println("(onFragment) " + "received fragment: " + fragment);
	}


	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("(onError) ");
		ex.printStackTrace();
//		if( conn != null ) {
//			// some errors like port binding failed may not be assignable to a specific web-socket
//		}
	}

	/**
	 * Sends <var>text</var> to all currently connected WebSocket clients.
	 *
	 * @param text The String to send across the network.
	 */
	public void sendToAll(String text) {
		Collection<WebSocket> con = connections();
		backedUpMessages.add(text);
		if (con.size() == 0) {
			System.out.println("(sendToAll) " + "Putting message on hold since there's no connection.");
		} else {
			sendMessage(con);
		}
	}

	public void sendBackedUpMessages() {

		Collection<WebSocket> con = connections();
		if (con.size() == 0) {
			if (backedUpMessages.size() > 0) {
				System.out.println("(sendBackedUpMessages) "
						+ "Can't clear out backlog since there's no connection right now.");
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
					System.out.println("(sendMessage) " + "Server sent: " + itemToSend);
				}
			}
			backedUpMessages.remove(0);
		}
	}
}
