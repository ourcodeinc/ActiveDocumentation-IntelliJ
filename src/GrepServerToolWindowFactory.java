/*
 * This module creates the GUI for the plugin
 * and process the info (psiJavaFiles) to produce contents
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.psi.*;
import org.java_websocket.WebSocketImpl;
import org.jetbrains.annotations.NotNull;
import com.intellij.ui.content.*;

//import java.awt.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

//import core.model.PsiJavaVisitor;
//import core.model.PsiPreCompEngine;

public class GrepServerToolWindowFactory implements ToolWindowFactory {

    private ChatServer s;
    private static List<VirtualFile> ignoredFilesList = null;


    private JButton button;
    private JPanel panel;

    /**
     * This function creates the GUI for the plugin.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {


        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);


        button.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                }
        );

        System.out.println("(createToolWindowContent) " + project.getBaseDir());

        ignoredFilesList = getIgnoredFilesList(project.getBaseDir());

        // Use ApplicationManager.getApplication().runReadAction() to run things on a new thread with IntelliJ.
        // Don't use the Java Thread library.
        // Here, the ChatServer is initialized and continues to run once the plugin has been opened.

        try {
            WebSocketImpl.DEBUG = false;
            int port = 8887; // 843 flash policy port

            // generateProjectHierarchyAsJSON() populates initialClassTable as well
            s = new ChatServer(port, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "INITIAL_PROJECT_HIERARCHY",
                    generateProjectHierarchyAsJSON()}).toString());

        } catch (Exception e) {
            e.printStackTrace();

        }

        // This will allow file changes to be sent to the web client
        FileChangeManager fcm = new FileChangeManager(s);
        fcm.initComponent();

    }


    // traverses the project hierarchy (the directories of the project that the user is working on)
    // when it hits a file that is not a directory, the getASTAsJSON function is called on that file
    // returns a JSON object that has the entire project hierarchy with individual files that have a property called 'ast'
    private static JsonObject generateProjectHierarchyAsJSON() {

        // start off with root
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

        // json version of root
        JsonObject jsonRootDirectory = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("canonicalPath", rootDirectoryVirtualFile.getCanonicalPath());
        properties.addProperty("parent", "");
        properties.addProperty("name", rootDirectoryVirtualFile.getNameWithoutExtension());
        properties.addProperty("isDirectory", true);
        jsonRootDirectory.add("children", new JsonArray());
        jsonRootDirectory.add("properties", properties);

        // set up a hashmap for the traversal
        HashMap<String, JsonObject> canonicalToJsonMap = new HashMap<>();
        canonicalToJsonMap.put(rootDirectoryVirtualFile.getCanonicalPath(), jsonRootDirectory);

        // set up queue
        java.util.List<VirtualFile> q = new ArrayList<>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            java.util.List<VirtualFile> new_q = new ArrayList<>();
            for (VirtualFile item : q) {
                // System.out.println(item.getName());
                if (shouldIgnoreFile(item)) {
                    continue;
                }
                //System.out.println("(generateProjectHierarchyAsJSON) " + "Included: " + item.getCanonicalPath());
                for (VirtualFile childOfItem : item.getChildren()) {
                    if (shouldIgnoreFile(childOfItem)) {
                        continue;
                    }
                    new_q.add(childOfItem);
                    JsonObject jsonChildOfItem = new JsonObject();
                    JsonObject propertiesOfChild = new JsonObject();
                    if (childOfItem.isDirectory()) {
                        propertiesOfChild.addProperty("canonicalPath", childOfItem.getCanonicalPath());
                        propertiesOfChild.addProperty("parent", item.getCanonicalPath());
                        propertiesOfChild.addProperty("name", childOfItem.getNameWithoutExtension());
                        propertiesOfChild.addProperty("isDirectory", true);
                        jsonChildOfItem.add("children", new JsonArray());
                        jsonChildOfItem.add("properties", propertiesOfChild);
                    } else {
                        propertiesOfChild.addProperty("canonicalPath", childOfItem.getCanonicalPath());
                        propertiesOfChild.addProperty("parent", item.getCanonicalPath());
                        propertiesOfChild.addProperty("name", childOfItem.getNameWithoutExtension());
                        propertiesOfChild.addProperty("isDirectory", false);
                        propertiesOfChild.addProperty("fileType", childOfItem.getFileType().getName());
                        jsonChildOfItem.add("properties", propertiesOfChild);
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(childOfItem);

                        if (psiFile != null) {

                            propertiesOfChild.addProperty("text", psiFile.getText());
                            //propertiesOfChild.add("ast", generateASTAsJSON(psiFile));
                            propertiesOfChild.addProperty("fileName", psiFile.getName());
                        }
                    }
                    canonicalToJsonMap.get(item.getCanonicalPath()).get("children").getAsJsonArray().add(jsonChildOfItem);
                    canonicalToJsonMap.put(childOfItem.getCanonicalPath(), jsonChildOfItem);
                }
            }
            q = new_q;
        }

        System.out.println("(generateProjectHierarchyAsJSON) " + jsonRootDirectory);
        return jsonRootDirectory;

    }

    // checks if we should ignore a file
    public static boolean shouldIgnoreFile(VirtualFile s) {
        if (ignoredFilesList == null) {
            return false;
        }
        for (VirtualFile vfile : ignoredFilesList) {
            if (vfile.getCanonicalPath().equals(s.getCanonicalPath())) {
                return true;
            } else if (isFileAChildOf(s, vfile)) {
                return true;
            }
        }

        return false;
    }

    // determines if one file/directory is stored somewhere down the line in another directory
    private static boolean isFileAChildOf(VirtualFile maybeChild, VirtualFile possibleParent) {
        final VirtualFile parent = possibleParent.getCanonicalFile();
        if (!parent.exists() || !parent.isDirectory()) {
            // this cannot possibly be the parent
            return false;
        }

        VirtualFile child = maybeChild.getCanonicalFile();
        while (child != null) {
            if (child.equals(parent)) {
                return true;
            }
            child = child.getParent();
        }
        // No match found, and we've hit the root directory
        return false;
    }

    // generates the list of files/folders to ignore
    public static List<VirtualFile> getIgnoredFilesList(VirtualFile bDir) {

        List<String> list = new ArrayList<>();
        list.add(".idea");
        list.add("out");
        list.add("website-client");

        List<VirtualFile> set = new ArrayList<>();

        for (String item : list) {
            VirtualFile vfile = bDir.findFileByRelativePath(item);
            if (vfile != null) {
                set.add(vfile);
            }
        }

        return set;
    }

}

