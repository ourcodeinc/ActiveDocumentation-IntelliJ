/*
 * This class creates the context menu for Feature Selection
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
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

public class FeatureSelectionAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Document document = editor.getDocument();

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int startOffset = primaryCaret.getSelectionStart();
        int endOffset = primaryCaret.getSelectionEnd();
        int startOffsetLineSelection = document.getLineStartOffset(document.getLineNumber(startOffset));
        int endOffsetLineSelection = document.getLineEndOffset(document.getLineNumber(endOffset));

        String path = event.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile().getCanonicalPath();

        Project project = event.getData(CommonDataKeys.PROJECT);
        if (project != null) {
            FileChangeManager.getInstance().sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FEATURE_SELECTION",
                    MessageProcessor.encodeSelectedFragment(new Object[]{path, Integer.toString(startOffset),
                            Integer.toString(endOffset),
                            Integer.toString(startOffset - startOffsetLineSelection),
                            Integer.toString(document.getLineNumber(startOffset)),
                            document.getText(new TextRange(startOffsetLineSelection, endOffsetLineSelection)),
                            primaryCaret.getSelectedText()
                    })
            }).toString());
        }
    }
}
