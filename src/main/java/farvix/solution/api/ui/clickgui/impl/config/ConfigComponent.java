package farvix.solution.api.ui.clickgui.impl.config;

import net.minecraft.client.gui.DrawContext;
import farvix.solution.Client;
import farvix.solution.api.TempColor;
import farvix.solution.api.config.Config;
import farvix.solution.api.render.font.Fonts;
import farvix.solution.api.render.rect.ShapeProperties;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfigComponent {
    
    private final Config config;
    private final ConfigScreen parent;
    
    private float x, y, width, height;
    
    public ConfigComponent(Config config, ConfigScreen parent) {
        this.config = config;
        this.parent = parent;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Фон компонента
        boolean hovered = isHovered(mouseX, mouseY);
        float guiAlpha = parent.getClickGUI().getGuiAlpha();
        
        ShapeProperties.create(context.getMatrices(), x, y, width, height)
                .round(8)
                .thickness(1)
                .outlineColor(TempColor.getClientColor().alpha(0.2 * guiAlpha).getRGB())
                .color(hovered ? TempColor.getClientColor().alpha(0.05 * guiAlpha).getRGB() : 
                       TempColor.getGuiBackground().alpha(0.3 * guiAlpha).getRGB())
                .build();
        
        // Название конфига
        Fonts.DEFAULT.get(16).drawString(context.getMatrices(), config.name, 
                x + 15, y + 12, parent.getFadeColor(TempColor.getTextPrimary().getRGB(), guiAlpha));
        
        // Дата изменения
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStr = sdf.format(new Date(config.lastModified));
        Fonts.DEFAULT.get(12).drawString(context.getMatrices(), "Last modified: " + dateStr, 
                x + 15, y + 30, parent.getFadeColor(TempColor.getTextSecondary().getRGB(), guiAlpha));
        
        // Кнопки
        float btnWidth = 70;
        float btnHeight = 25;
        float btnSpacing = 10;
        float btnY = y + (height - btnHeight) / 2;
        
        float loadBtnX = x + width - btnWidth * 2 - btnSpacing - 15;
        float deleteBtnX = x + width - btnWidth - 15;
        
        // Load button
        boolean loadHovered = isHovered(loadBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY);
        
        ShapeProperties.create(context.getMatrices(), loadBtnX, btnY, btnWidth, btnHeight)
                .round(6)
                .color(loadHovered ? TempColor.getClientColor().alpha(0.4 * guiAlpha).getRGB() : 
                       TempColor.getClientColor().alpha(0.3 * guiAlpha).getRGB())
                .build();
        
        Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "Load", 
                loadBtnX + btnWidth / 2, btnY + btnHeight / 2 - 3,
                parent.getFadeColor(TempColor.getTextPrimary().getRGB(), guiAlpha));
        
        // Delete button
        boolean deleteHovered = isHovered(deleteBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY);
        
        ShapeProperties.create(context.getMatrices(), deleteBtnX, btnY, btnWidth, btnHeight)
                .round(6)
                .color(deleteHovered ? new farvix.solution.api.util.color.FixColor(255, 50, 50).alpha(0.4 * guiAlpha).getRGB() : 
                       new farvix.solution.api.util.color.FixColor(255, 50, 50).alpha(0.2 * guiAlpha).getRGB())
                .build();
        
        Fonts.DEFAULT.get(14).drawCenteredString(context.getMatrices(), "Delete", 
                deleteBtnX + btnWidth / 2, btnY + btnHeight / 2 - 3,
                parent.getFadeColor(TempColor.getTextPrimary().getRGB(), guiAlpha));
    }
    
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;
        
        float btnWidth = 70;
        float btnHeight = 25;
        float btnSpacing = 10;
        float btnY = y + (height - btnHeight) / 2;
        
        float loadBtnX = x + width - btnWidth * 2 - btnSpacing - 15;
        float deleteBtnX = x + width - btnWidth - 15;
        
        // Load button
        if (isHovered(loadBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {
            Client.getInstance().getConfigManager().loadConfig(config.name);
        }
        
        // Delete button
        if (isHovered(deleteBtnX, btnY, btnWidth, btnHeight, mouseX, mouseY)) {
            Client.getInstance().getConfigManager().deleteConfig(config.name);
            parent.onConfigDeleted();
        }
    }
    
    private boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    private boolean isHovered(float x, float y, float width, float height, double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
    
    public void setX(float x) {
        this.x = x;
    }
    
    public void setY(float y) {
        this.y = y;
    }
    
    public void setWidth(float width) {
        this.width = width;
    }
    
    public void setHeight(float height) {
        this.height = height;
    }
}
