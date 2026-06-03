package farvix.solution.api.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Простой менеджер друзей — хранит никнеймы в памяти.
 * Сохранение через ConfigManager.
 */
public class FriendManager {

    private static final Set<String> friends = new HashSet<>();

    public static void addFriend(String name) {
        if (name != null && !name.isEmpty()) friends.add(name);
    }

    public static void removeFriend(String name) {
        if (name != null) friends.removeIf(n -> n.equalsIgnoreCase(name));
    }

    public static boolean isFriend(String name) {
        if (name == null) return false;
        return friends.stream().anyMatch(n -> n.equalsIgnoreCase(name));
    }

    public static Set<String> getFriends() {
        return friends;
    }
}
