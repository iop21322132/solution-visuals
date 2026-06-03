package farvix.solution.client.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import farvix.solution.api.util.game.ChatUtility;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Хранит заметки игрока (до 100) и обрабатывает команды .note
 */
@Getter
public class NoteManager {

    public static final int MAX_NOTES = 100;

    private static NoteManager instance;

    private final List<String> notes = new ArrayList<>();
    private final File notesFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public NoteManager() {
        instance = this;
        notesFile = new File(
                FabricLoader.getInstance().getGameDir().toFile(),
                "solution-configs/notes.json");
        load();
    }

    public static NoteManager getInstance() {
        return instance;
    }

    public List<String> getNotesView() {
        return Collections.unmodifiableList(notes);
    }

    /**
     * @return true если команда обработана и не должна уходить на сервер
     */
    public boolean handleCommand(String raw) {
        String msg = raw.trim();
        String lower = msg.toLowerCase();

        if (!lower.startsWith(".note")) {
            return false;
        }

        if (lower.equals(".note") || lower.equals(".note help")) {
            sendHelp();
            return true;
        }

        if (lower.startsWith(".note add ")) {
            String text = msg.substring(".note add ".length()).trim();
            if (text.isEmpty()) {
                ChatUtility.sendError("Укажите текст: .note add <текст>");
            } else {
                addNote(text);
            }
            return true;
        }

        if (lower.startsWith(".note del ")) {
            String numStr = msg.substring(".note del ".length()).trim();
            try {
                int index = Integer.parseInt(numStr);
                deleteNote(index);
            } catch (NumberFormatException e) {
                ChatUtility.sendError("Укажите номер от 1 до " + MAX_NOTES + ": .note del <номер>");
            }
            return true;
        }

        ChatUtility.sendError("Неизвестная команда. Введите .note help");
        return true;
    }

    public void addNote(String text) {
        if (notes.size() >= MAX_NOTES) {
            ChatUtility.sendError("Достигнут лимит заметок (" + MAX_NOTES + ")");
            return;
        }
        notes.add(text);
        save();
        ChatUtility.send("Заметка #" + notes.size() + " добавлена");
    }

    public void deleteNote(int oneBasedIndex) {
        if (oneBasedIndex < 1 || oneBasedIndex > notes.size()) {
            ChatUtility.sendError("Заметка #" + oneBasedIndex + " не найдена (всего: " + notes.size() + ")");
            return;
        }
        String removed = notes.remove(oneBasedIndex - 1);
        save();
        ChatUtility.send("Удалена заметка #" + oneBasedIndex + ": " + truncate(removed, 40));
    }

    private void sendHelp() {
        ChatUtility.send("Команды Note:");
        ChatUtility.send(".note add <текст> — добавить заметку");
        ChatUtility.send(".note del <1-" + MAX_NOTES + "> — удалить по номеру");
        ChatUtility.send(".note help — эта справка");
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    public void save() {
        try {
            File parent = notesFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (String note : notes) {
                arr.add(note);
            }
            root.add("notes", arr);
            try (FileWriter writer = new FileWriter(notesFile)) {
                gson.toJson(root, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void load() {
        notes.clear();
        if (!notesFile.exists()) {
            return;
        }
        try (FileReader reader = new FileReader(notesFile)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("notes")) {
                return;
            }
            JsonArray arr = root.getAsJsonArray("notes");
            for (int i = 0; i < arr.size() && notes.size() < MAX_NOTES; i++) {
                notes.add(arr.get(i).getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
