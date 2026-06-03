package farvix.solution.mixins;

import net.minecraft.client.option.ServerList;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Shadow
    private List<ServerInfo> servers;

    // В 1.21.4 метод load() был заменен на loadFile()
    @Inject(method = "loadFile()V", at = @At("RETURN"))
    private void onLoaded(CallbackInfo ci) {
        String targetAddr = "mc.toolrise.space";
        String targetName = "Лучший сервер!";
        
        // Используем итератор вместо лямбды для стабильности трансформации
        // Профессиональный подход: удаляем дубликаты перед добавлением
        java.util.Iterator<ServerInfo> iterator = servers.iterator();
        while (iterator.hasNext()) {
            ServerInfo info = iterator.next();
            if (info.address != null && info.address.toLowerCase().contains(targetAddr.toLowerCase())) {
                iterator.remove();
            }
        }

        // Добавляем наш сервер на первое место (индекс 0)
        servers.add(0, new ServerInfo(targetName, targetAddr, ServerInfo.ServerType.OTHER));
    }
}