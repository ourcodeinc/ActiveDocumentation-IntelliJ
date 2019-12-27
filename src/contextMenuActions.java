/*
 * @author : Sahar Mehrpour
 * created on Dec 26, 2019
 */

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class contextMenuActions extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Document document = editor.getDocument();

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int start = primaryCaret.getSelectionStart();
        int end = primaryCaret.getSelectionEnd();
        String path = event.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile().getCanonicalPath();

        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            project.getComponent(FileChangeManager.class).sendFeatureSelectionData(path, Integer.toString(start),
                    Integer.toString(end), Integer.toString(document.getLineNumber(start)), primaryCaret.getSelectedText());
        }

    }
}
