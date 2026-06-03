package farvix.solution.client.modules.impl.environment;

import farvix.solution.api.settings.impl.StringSetting;
import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

@ModuleInfo(name = "Streamer", category = ModuleCategory.ENVIRONMENT,
        description = "Скрывает ваш ник для стримов")
public class Streamer extends Module {

    public final StringSetting alias = new StringSetting("Псевдоним", "Псевдоним для замены ника", "Streamer", this);

    public String replace(String text) {
        if (!isEnabled() || text == null) return text;
        String realNick = mc.getSession() != null ? mc.getSession().getUsername() : null;
        if (realNick == null || realNick.isEmpty()) return text;
        String a = alias.getText();
        if (a == null || a.isEmpty()) return text;
        return text.replaceAll("(?i)" + java.util.regex.Pattern.quote(realNick), a);
    }
}
