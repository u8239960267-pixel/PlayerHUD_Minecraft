package pl.playerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

public class HudSettingsScreen extends Screen {
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int activeTab = 0;

    public HudSettingsScreen() {
        super(Text.of("Ustawienia Player HUD"));
    }

    @Override
    protected void init() {
        this.clearChildren();

        this.addDrawableChild(ButtonWidget.builder(Text.of("1. OGÓLNE"), b -> switchTab(0)).dimensions(10, 10, 85, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("2. MODEL 3D"), b -> switchTab(1)).dimensions(100, 10, 85, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("3. STATY"), b -> switchTab(2)).dimensions(190, 10, 85, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("4. NICKI"), b -> switchTab(3)).dimensions(280, 10, 85, 20).build());

        if (activeTab == 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.isEnabled = !HudConfig.isEnabled;
                button.setMessage(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(20, 45, 180, 20).build());
        } 
        else if (activeTab == 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showSkin = !HudConfig.showSkin;
                button.setMessage(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(20, 45, 180, 20).build());
        } 
        else if (activeTab == 2) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Minecraftowe Serca: " + (HudConfig.showHearts ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showHearts = !HudConfig.showHearts;
                button.setMessage(Text.of("Minecraftowe Serca: " + (HudConfig.showHearts ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(20, 45, 180, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.of("Minecraftowy Armor: " + (HudConfig.showArmor ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showArmor = !HudConfig.showArmor;
                button.setMessage(Text.of("Minecraftowy Armor: " + (HudConfig.showArmor ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(20, 70, 180, 20).build());
        } 
        else if (activeTab == 3) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Nick nad Statystykami: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showName = !HudConfig.showName;
                button.setMessage(Text.of("Nick nad Statystykami: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(20, 45, 180, 20).build());
        }
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);

        context.fill(HudConfig.hudX, HudConfig.hudY, HudConfig.hudX + 150, HudConfig.hudY + 80, 0x66000000);
        context.drawText(this.textRenderer, "§e Złap i przesuń §f", HudConfig.hudX + 5, HudConfig.hudY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= HudConfig.hudX && mouseX <= HudConfig.hudX + 150 && mouseY >= HudConfig.hudY && mouseY <= HudConfig.hudY + 80) {
            this.isDragging = true;
            this.dragStartX = (int) mouseX - HudConfig.hudX;
            this.dragStartY = (int) mouseY - HudConfig.hudY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.isDragging = false;
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
