package com.tiertagger.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TierScreen extends Screen {
    private final TierDataStore store;
    private EditBox searchBox;

    public TierScreen(TierDataStore store) {
        super(Component.literal("TierTagger"));
        this.store = store;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(this.font, this.width / 2 - 140, 35, 280, 20,
                Component.literal("Search player"));
        searchBox.setValue("");
        addRenderableWidget(searchBox);
        setInitialFocus(searchBox);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);
        graphics.text(this.font, "TierTagger", this.width / 2 - 40, 12, 0xFFFFFFFF, true);

        List<TierEntry> results = store.search(searchBox == null ? "" : searchBox.getValue());
        int y = 70;
        int shown = 0;
        for (TierEntry entry : results) {
            String line = entry.player() + " | " + entry.gamemode() + " | " + entry.tier();
            graphics.text(this.font, line, this.width / 2 - 140, y, 0xFFFFFFFF, false);
            y += 16;
            if (++shown >= 18) break;
        }
        if (results.isEmpty()) {
            graphics.text(this.font, "No tier found", this.width / 2 - 40, 75, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
