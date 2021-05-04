import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class MineRulesForFileAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        final Editor editor = event.getRequiredData(CommonDataKeys.EDITOR);
        final Document document = editor.getDocument();
        String path = event.getRequiredData(CommonDataKeys.PSI_FILE).getVirtualFile().getCanonicalPath();
        Project project = event.getData(CommonDataKeys.PROJECT);

        PsiFile file = event.getRequiredData(CommonDataKeys.PSI_FILE);
        PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
        String selectedText = (elementAt != null) ? elementAt.getText() : "";

        Caret primaryCaret = editor.getCaretModel().getPrimaryCaret();
        int startOffset = primaryCaret.getSelectionStart();
        int startOffsetLineSelection = document.getLineStartOffset(document.getLineNumber(startOffset));
        if (project != null) {
            FileChangeManager.getInstance().sendMessage(MessageProcessor.encodeData(new Object[]{
                    WebSocketConstants.SEND_ELEMENT_INFO_FOR_MINE_RULES,
                    MessageProcessor.encodeElementInfoForMineRules(new Object[]{path, Integer.toString(startOffset),
                            Integer.toString(startOffset - startOffsetLineSelection),
                            Integer.toString(document.getLineNumber(startOffset)),
                            selectedText
                    })
            }).toString());

            DoiProcessing doiClass = DoiProcessing.getInstance();
            FileChangeManager.getInstance().sendMessage(MessageProcessor.encodeData(new Object[]{
                    WebSocketConstants.SEND_DOI_INFORMATION, MessageProcessor.encodeDoiInformation(
                    new Object[]{doiClass.getVisitedFiles(), doiClass.getSearchHistory(),
                            doiClass.getVisitedElements()}
            )}).toString());

            FileChangeManager.getInstance().sendMessage(MessageProcessor.encodeData(new Object[]{
                    WebSocketConstants.SEND_REQUEST_MINE_RULES_FOR_ELEMENT, ""}).toString());
        }
    }
}
