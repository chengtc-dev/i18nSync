package tw.iancheng.i18nsync.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


public class SyncI18nText extends AnAction {
    public SyncI18nText() {
        super("Sync I18n Text", "Sync I18n Text", AllIcons.Toolbar.Unknown);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);

        if (editor != null) {
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();

            if (selectedText != null) {
                String[] lines = selectedText.split("\n");
                Map<String, String> i18nMap = new HashMap<>();
                boolean isValid = true;

                // check input format
                for (String line : lines) {
                    if (line.contains("=")) {
                        String[] parts = line.split("=");
                        if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                            String key = parts[0].trim();
                            String value = parts[1].trim();
                            i18nMap.put(key, value);
                        } else {
                            isValid = false;
                            break;
                        }
                    } else {
                        isValid = false;
                        break;
                    }
                }

                if (isValid) {
                    System.out.println(i18nMap);
                } else {
                    Messages.showErrorDialog("Invalid input format. Please enter text that conforms to the i18n format.", "Error");
                }

            }
        }
    }

}