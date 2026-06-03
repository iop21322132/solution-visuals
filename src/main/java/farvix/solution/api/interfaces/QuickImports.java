package farvix.solution.api.interfaces;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;

import farvix.solution.api.render.rect.impl.Arc;
import farvix.solution.api.render.rect.impl.Blur;
import farvix.solution.api.render.rect.impl.Glass;
import farvix.solution.api.render.rect.impl.Image;
import farvix.solution.api.render.rect.impl.Rectangle;
import farvix.solution.api.render.rect.impl.RoundedTexture;

public interface QuickImports {
    // Ленивая инициализация для избежания краша при раннем доступе
    MinecraftClient mc = MinecraftClient.getInstance();
    
    // Используем методы вместо полей для ленивой инициализации
    static RenderTickCounter getTickCounter() {
        return mc.getRenderTickCounter();
    }

    // Holder pattern для ленивой инициализации - инициализируется только при первом обращении
    class RectangleHolder {
        static final Rectangle INSTANCE = new Rectangle();
    }
    
    class BlurHolder {
        static final Blur INSTANCE = new Blur();
    }
    
    class GlassHolder {
        static final Glass INSTANCE = new Glass();
    }
    
    class ArcHolder {
        static final Arc INSTANCE = new Arc();
    }
    
    class ImageHolder {
        static final Image INSTANCE = new Image();
    }
    
    class RoundedTextureHolder {
        static final RoundedTexture INSTANCE = new RoundedTexture();
    }
    
    // Поля с ленивой инициализацией через Holder pattern
    Rectangle rectangle = RectangleHolder.INSTANCE;
    Blur blur = BlurHolder.INSTANCE;
    Glass glass = GlassHolder.INSTANCE;
    Arc arc = ArcHolder.INSTANCE;
    Image image = ImageHolder.INSTANCE;
    RoundedTexture roundedTexture = RoundedTextureHolder.INSTANCE;
}
