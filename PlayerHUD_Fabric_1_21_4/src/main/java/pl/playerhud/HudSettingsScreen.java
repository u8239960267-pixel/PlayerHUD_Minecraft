package pl.playerhud;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
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

        // Obliczanie pozycji, aby wyśrodkować menu i opcje
        int centerX = this.width / 2;
        int buttonWidth = 200;
        int buttonX = centerX - (buttonWidth / 2);

        // Wyśrodkowane zakładki bez numeracji z ikonami tekstowymi
        int tabWidth = 85;
        int totalTabsWidth = 4 * tabWidth + 3 * 5; // 4 zakładki po 85px + 3 przerwy po 5px = 355px
        int tabStartX = centerX - (totalTabsWidth / 2);

        this.addDrawableChild(ButtonWidget.builder(Text.of("⚙ OGÓLNE"), b -> switchTab(0)).dimensions(tabStartX, 10, tabWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("👤 MODEL 3D"), b -> switchTab(1)).dimensions(tabStartX + 90, 10, tabWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("❤️ STATY"), b -> switchTab(2)).dimensions(tabStartX + 180, 10, tabWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("📛 NICKI"), b -> switchTab(3)).dimensions(tabStartX + 270, 10, tabWidth, 20).build());

        // Zakładka 0: OGÓLNE
        if (activeTab == 0) {
            // Włącznik HUDa
            this.addDrawableChild(ButtonWidget.builder(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.isEnabled = !HudConfig.isEnabled;
                button.setMessage(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, 50, buttonWidth, 20).build());

            // NOWOŚĆ: Suwak skali (Zakres od 0.5x do 2.0x)
            this.addDrawableChild(new SliderWidget(buttonX, 80, buttonWidth, 20, 
                Text.of(String.format("Skala HUD: %.1fx", HudConfig.hudScale)), (HudConfig.hudScale - 0.5f) / 1.5f) {
                
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.of(String.format("Skala HUD: %.1fx", HudConfig.hudScale)));
                }

                @Override
                protected void applyValue() {
                    // Przeliczanie wartości suwaka (0.0 do 1.0) na skale (0.5 do 2.0)
                    float rawScale = 0.5f + (float) this.value * 1.5f;
                    // Zaokrąglenie do jednego miejsca po przecinku (np. 1.1x, 1.2x)
                    HudConfig.hudScale = Math.round(rawScale * 10.0f) / 10.0f;
                }
            });
        } 
        // Zakładka 1: MODEL 3D
        else if (activeTab == 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showSkin = !HudConfig.showSkin;
                button.setMessage(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, 50, buttonWidth, 20).build());
        } 
        // Zakładka 2: STATY
        else if (activeTab == 2) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Minecraftowe Serca: " + (HudConfig.showHearts ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showHearts = !HudConfig.showHearts;
                button.setMessage(Text.of("Minecraftowe Serca: " + (HudConfig.showHearts ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, 50, buttonWidth, 20).build());

            this.addDrawableChild(ButtonWidget.builder(Text.of("Minecraftowy Armor: " + (HudConfig.showArmor ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showArmor = !HudConfig.showArmor;
                button.setMessage(Text.of("Minecraftowy Armor: " + (HudConfig.showArmor ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, 75, buttonWidth, 20).build());
        } 
        // Zakładka 3: NICKI
        else if (activeTab == 3) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Nick nad Statystykami: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showName = !HudConfig.showName;
                button.setMessage(Text.of("Nick nad Statystykami: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, 50, buttonWidth, 20).build());
        }
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);

        // Skalowanie podglądu przesuwania (szary kwadrat) zgodnie z wybraną skalą
        int boxWidth = (int) (150 * HudConfig.hudScale);
        int boxHeight = (int) (80 * HudConfig.hudScale);

        context.fill(HudConfig.hudX, HudConfig.hudY, HudConfig.hudX + boxWidth, HudConfig.hudY + boxHeight, 0x66000000);
        context.drawText(this.textRenderer, "§e Złap i przesuń §f", HudConfig.hudX + 5, HudConfig.hudY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int boxWidth = (int) (150 * HudConfig.hudScale);
        int boxHeight = (int) (80 * HudConfig.hudScale);

        if (button == 0 && mouseX >= HudConfig.hudX && mouseX <= HudConfig.hudX + boxWidth && mouseY >= HudConfig.hudY && mouseY <= HudConfig.hudY + boxHeight) {
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
