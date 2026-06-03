package farvix.solution.mixins;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Item;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import farvix.solution.Client;
import farvix.solution.client.modules.impl.environment.ItemScroller;
import farvix.solution.client.modules.impl.visuals.ItemHighlight;

import java.util.HashSet;
import java.util.Set;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow protected Slot focusedSlot;
    @Shadow protected abstract Slot getSlotAt(double x, double y);
    @Shadow protected abstract void onMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);

    // Слоты которые уже были обработаны в текущем drag-сессии
    private final Set<Integer> scrollerProcessedSlots = new HashSet<>();
    private boolean scrollerDragging = false;
    private int scrollerButton = 1; // 0=ЛКМ, 1=ПКМ

    // ── ItemHighlight: средняя кнопка ─────────────────────────────────────────

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {
        // ItemHighlight — средняя кнопка
        if (button == 2) {
            ItemHighlight module = getHighlightModule();
            if (module != null && module.isEnabled()) {
                Slot slot = focusedSlot;
                if (slot != null && !slot.getStack().isEmpty()) {
                    Item item = slot.getStack().getItem();
                    if (module.getHighlightedItems().contains(item)) {
                        module.getHighlightedItems().remove(item);
                    } else {
                        module.getHighlightedItems().add(item);
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // ItemScroller — только Shift + ЛКМ для начала drag
        if (button == 0 && isShiftHeld()) {
            ItemScroller scroller = getScrollerModule();
            if (scroller != null && scroller.isEnabled()) {
                Slot slot = getSlotAt(mouseX, mouseY);
                
                // Проверяем что слот валидный и имеет предмет
                if (slot != null && slot.hasStack()) {
                    // В креативе некоторые слоты имеют отрицательный id - пропускаем их
                    // Также проверяем что слот можно взять (canTakeItems)
                    if (slot.id >= 0 && slot.canTakeItems(net.minecraft.client.MinecraftClient.getInstance().player)) {
                        scrollerProcessedSlots.clear();
                        scrollerDragging = true;
                        scrollerButton = 0; // Только ЛКМ

                        onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                        scrollerProcessedSlots.add(slot.id);
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(double mouseX, double mouseY, int button,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (button == 0) { // Только ЛКМ
            scrollerDragging = false;
            scrollerProcessedSlots.clear();
        }
    }

    // Отслеживаем движение мыши через render — надёжнее чем mouseDragged
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!scrollerDragging) return;

        ItemScroller scroller = getScrollerModule();
        if (scroller == null || !scroller.isEnabled()) { scrollerDragging = false; return; }

        // Если кнопка отпущена — сбрасываем (только ЛКМ)
        long handle = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        if (GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            scrollerDragging = false;
            scrollerProcessedSlots.clear();
            return;
        }

        if (!isShiftHeld()) { scrollerDragging = false; return; }

        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot != null && slot.hasStack() && !scrollerProcessedSlots.contains(slot.id)) {
            // Проверяем что слот можно взять
            if (slot.canTakeItems(net.minecraft.client.MinecraftClient.getInstance().player)) {
                onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                scrollerProcessedSlots.add(slot.id);
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(double mouseX, double mouseY, int button,
                                 double deltaX, double deltaY,
                                 CallbackInfoReturnable<Boolean> cir) {
        if (button != 0 || !scrollerDragging) return; // Только ЛКМ

        ItemScroller scroller = getScrollerModule();
        if (scroller == null || !scroller.isEnabled()) return;
        if (!isShiftHeld()) { scrollerDragging = false; return; }

        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot != null && slot.hasStack() && !scrollerProcessedSlots.contains(slot.id)) {
            // Проверяем что слот можно взять
            if (slot.canTakeItems(net.minecraft.client.MinecraftClient.getInstance().player)) {
                onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
                scrollerProcessedSlots.add(slot.id);
            }
        }

        cir.setReturnValue(true);
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemHighlight module = getHighlightModule();
        if (module == null || !module.isEnabled()) return;
        var stack = slot.getStack();
        if (stack.isEmpty()) return;
        if (!module.getHighlightedItems().contains(stack.getItem())) return;
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, module.getHighlightColor());
    }

    // ── ItemScroller: колесо мыши над слотом ─────────────────────────────────
    // ОТКЛЮЧЕНО: функция перемещения предметов через колесо убрана
    /*
    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY,
                                  double horizontalAmount, double verticalAmount,
                                  CallbackInfoReturnable<Boolean> cir) {
        ItemScroller scroller = getScrollerModule();
        if (scroller == null || !scroller.isEnabled()) return;

        // Не обрабатываем скролл если открыт ClickGUI
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc.currentScreen instanceof farvix.solution.api.ui.clickgui.InterfaceScreen) {
            return;
        }

        Slot slot = getSlotAt(mouseX, mouseY);
        if (slot == null || !slot.hasStack()) return;

        // Колесо — переместить 1 предмет (или весь стак с Shift)
        onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
        cir.setReturnValue(true);
    }
    */

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isShiftHeld() {
        long handle = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
            || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private ItemHighlight getHighlightModule() {
        if (Client.getInstance() == null) return null;
        return Client.getInstance().getModuleManager().get(ItemHighlight.class);
    }

    private ItemScroller getScrollerModule() {
        if (Client.getInstance() == null) return null;
        return Client.getInstance().getModuleManager().get(ItemScroller.class);
    }
}
