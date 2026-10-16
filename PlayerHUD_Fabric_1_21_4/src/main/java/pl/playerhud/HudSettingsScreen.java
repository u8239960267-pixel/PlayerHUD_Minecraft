package pl.playerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class HudSettingsScreen extends Screen {
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;

    public HudSettingsScreen() {
        super(Text.of("Ustawienia Player HUD"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(
                Text.of("HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")),
                button -> {
                    HudConfig.isEnabled = !HudConfig.isEnabled;
                    button.setMessage(Text.of("HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
                }
        ).dimensions(10, 10, 140, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.of("Model 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")),
                button -> {
                    HudConfig.showSkin = !HudConfig.showSkin;
                    button.setMessage(Text.of("Model 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
                }
        ).dimensions(10, 35, 140, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        context.fill(HudConfig.hudX, HudConfig.hudY, HudConfig.hudX + 140, HudConfig.hudY + 75, 0x66000000);
        context.drawText(this.textRenderer, "§e Złap i przesuń §f", HudConfig.hudX + 5, HudConfig.hudY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseX >= HudConfig.hudX && mouseX <= HudConfig.hudX + 140 &&
                mouseY >= HudConfig.hudY && mouseY <= HudConfig.hudY + 75) {
                this.isDragging = true;
                this.dragStartX = (int) mouseX - HudConfig.hudX;
                this.dragStartY = (int) mouseY - HudConfig.hudY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.isDragging) {
            HudConfig.hudX = (int) mouseX - this.dragStartX;
            HudConfig.hudY = (int) mouseY - this.dragStartY;
        }
    }
}
