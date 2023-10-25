package tw.iancheng.i18nsync.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;


public class SyncI18nText extends AnAction {
    String apiKey = "";
    String model = "gpt-3.5-turbo";
    double temperature = 0.7;

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
                selectedText = checkAndTrimInputFormat(selectedText);

                if (selectedText.isEmpty()) {
                    Messages.showErrorDialog("Invalid input format." +
                            " Please enter text that conforms to the i18n format.", "Format Error");
                    return;
                }

                VirtualFile baseDir = event.getData(CommonDataKeys.VIRTUAL_FILE);

                if (baseDir != null) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select file for translation");
                    fileChooser.setCurrentDirectory(new File(baseDir.getPath()));

                    FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files",
                            "txt", "properties");
                    fileChooser.setFileFilter(filter);
                    int result = fileChooser.showOpenDialog(null);

                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        String currentFilename = baseDir.getName();
                        String filename = selectedFile.getName();

                        if (!filename.equals(currentFilename)) {
                            String[] language = new String[] {"English", "Traditional Chinese"};
                            int index = Messages.showDialog("Select target language:", "Target Language",
                                    language, 0, null);
                            String targetLanguage = language[index];
                            String message = callChatGPT(selectedText, targetLanguage);
                            String absolutePath = selectedFile.getAbsolutePath();

                            if (message.isEmpty() || !writeMessageToFile(message, absolutePath)) {
                                Messages.showErrorDialog("Failed to write the message to the file: "
                                        + absolutePath, "Write Message Error");
                                return;
                            }
                        } else {
                            Messages.showErrorDialog("Can't select current file.", "Choose File Error");
                            return;
                        }

                        Messages.showInfoMessage("Translate successful!", "Congratulation");
                    }
                }
            }
        }
    }

    private String checkAndTrimInputFormat(String selectedText) {
        String[] lines = selectedText.split("\n");
        StringBuilder trimText = new StringBuilder();

        for (String line : lines) {
            if (line.contains("=")) {
                String[] parts = line.split("=");
                if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                    return "";
                } else {
                    trimText.append(parts[0].trim()).append("=").append(parts[1].trim()).append(" ");
                }
            } else {
                return "";
            }
        }

        return trimText.toString();
    }

    private String callChatGPT(String selectedText, String targetLanguage) {
        try {
            URL url = new URL("https://api.openai.com/v1/chat/completions");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String prompt = "Translate the following text from English to " + targetLanguage
                    + " and only translate the text after equal: " + selectedText;
            String requestBody = "{"
                    + "\"model\": \"" + model + "\","
                    + "\"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}],"
                    + "\"temperature\": " + temperature
                    + "}";

            OutputStream os = connection.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
            os.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return parseMessage(response.toString());
            } else {
                Messages.showErrorDialog("HTTP Request Failed with Response Code " + responseCode,
                        "Call API Fail");
            }
        } catch (Exception e) {
            Messages.showErrorDialog("Something wrong when calling ChatGPT.", "Call API Fail");
        }

        return "";
    }

    private String parseMessage(String response) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonResponse = (JSONObject) jsonParser.parse(response);
            JSONArray choices = (JSONArray) jsonResponse.get("choices");
            JSONObject choice = (JSONObject) choices.get(0);
            JSONObject message = (JSONObject) choice.get("message");

            return (String) message.get("content");
        } catch (ParseException e) {
            Messages.showErrorDialog("Something wrong when parsing message.",
                    "Parse Message Fail");

            return "";
        }
    }

    private boolean writeMessageToFile(String message, String absolutePath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(absolutePath));

            writer.write(message);
            writer.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}