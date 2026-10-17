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

        int centerX = this.width / 2;
        int buttonWidth = 200;
        int buttonX = centerX - (buttonWidth / 2);
        
        int tabWidth = 100;
        int totalTabsWidth = 2 * tabWidth + 10;
        int tabStartX = centerX - (totalTabsWidth / 2);

        this.addDrawableChild(ButtonWidget.builder(Text.of("OGÓLNE"), b -> switchTab(0)).dimensions(tabStartX, 10, tabWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.of("STATYSTYKI"), b -> switchTab(1)).dimensions(tabStartX + tabWidth + 10, 10, tabWidth, 20).build());

        int currentY = 45;
        int spacing = 24;

        if (activeTab == 0) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.isEnabled = !HudConfig.isEnabled;
                button.setMessage(Text.of("Cały HUD: " + (HudConfig.isEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());
            
            currentY += spacing;

            this.addDrawableChild(new SliderWidget(buttonX, currentY, buttonWidth, 20, 
                Text.of(String.format("Skala HUD: %.1fx", HudConfig.hudScale)), (HudConfig.hudScale - 0.5f) / 1.5f) {
                
                @Override
                protected void updateMessage() {
                    this.setMessage(Text.of(String.format("Skala HUD: %.1fx", HudConfig.hudScale)));
                }

                @Override
                protected void applyValue() {
                    float rawScale = 0.5f + (float) this.value * 1.5f;
                    HudConfig.hudScale = Math.round(rawScale * 10.0f) / 10.0f;
                }
            });
            
            currentY += spacing;

            this.addDrawableChild(ButtonWidget.builder(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showSkin = !HudConfig.showSkin;
                button.setMessage(Text.of("Model Skina 3D: " + (HudConfig.showSkin ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());
            
            currentY += spacing;

            this.addDrawableChild(ButtonWidget.builder(Text.of("Przedmioty w rękach: " + (HudConfig.showItems ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showItems = !HudConfig.showItems;
                button.setMessage(Text.of("Przedmioty w rękach: " + (HudConfig.showItems ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());

        } else if (activeTab == 1) {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Nick nad HUDem: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showName = !HudConfig.showName;
                button.setMessage(Text.of("Nick nad HUDem: " + (HudConfig.showName ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());
            
            currentY += spacing;

            this.addDrawableChild(ButtonWidget.builder(Text.of(getHpButtonText()), button -> {
                HudConfig.hpDisplayMode = (HudConfig.hpDisplayMode + 1) % 3;
                button.setMessage(Text.of(getHpButtonText()));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());
            
            currentY += spacing;

            this.addDrawableChild(ButtonWidget.builder(Text.of(getArmorButtonText()), button -> {
                HudConfig.armorDisplayMode = (HudConfig.armorDisplayMode + 1) % 3;
                button.setMessage(Text.of(getArmorButtonText()));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());

            currentY += spacing;

            this.addDrawableChild(ButtonWidget.builder(Text.of("Błysk modelu po hicie: " + (HudConfig.showHurtFlash ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), button -> {
                HudConfig.showHurtFlash = !HudConfig.showHurtFlash;
                button.setMessage(Text.of("Błysk modelu po hicie: " + (HudConfig.showHurtFlash ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
            }).dimensions(buttonX, currentY, buttonWidth, 20).build());
        }
    }

    private String getHpButtonText() {
        if (HudConfig.hpDisplayMode == 0) return "Styl HP: §eNormalne Serca";
        if (HudConfig.hpDisplayMode == 1) return "Styl HP: §eProcenty (%)";
        return "Styl HP: §eHP: Liczby";
    }

    private String getArmorButtonText() {
        if (HudConfig.armorDisplayMode == 0) return "Styl Armora: §eNormalne Ikony";
        if (HudConfig.armorDisplayMode == 1) return "Styl Armora: §eProcenty (%)";
        return "Styl Armora: §eArmor: Liczby";
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.init();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderInGameBackground(context);

        int boxWidth = (int) (160 * HudConfig.hudScale);
        int boxHeight = (int) (95 * HudConfig.hudScale);

        context.fill(HudConfig.hudX, HudConfig.hudY, HudConfig.hudX + boxWidth, HudConfig.hudY + boxHeight, 0x66000000);
        context.drawText(this.textRenderer, "§e Złap i przesuń §f", HudConfig.hudX + 5, HudConfig.hudY + 5, 0xFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int boxWidth = (int) (160 * HudConfig.hudScale);
        int boxHeight = (int) (95 * HudConfig.hudScale);

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
