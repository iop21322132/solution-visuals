package farvix.solution.client.modules.impl.visuals;

import farvix.solution.client.modules.Module;
import farvix.solution.client.modules.api.ModuleCategory;
import farvix.solution.client.modules.api.ModuleInfo;

/**
 * Показывает ваш собственный ник над головой в режиме от третьего лица (F5)
 * Идеально для контент-мейкеров, скриншотов и ролевых серверов
 */
@ModuleInfo(name = "Show My Name", category = ModuleCategory.VISUALS,
        description = "Показывает ваш ник в режиме F5")
public class ShowMyName extends Module {

    public ShowMyName() {
    }

    /**
     * Проверяет, должен ли отображаться ник игрока
     * @return true если модуль включен
     */
    public boolean shouldShowOwnName() {
        return isEnabled();
    }
}
