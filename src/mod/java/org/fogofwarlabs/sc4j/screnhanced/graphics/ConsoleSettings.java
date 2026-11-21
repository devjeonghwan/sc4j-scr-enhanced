package org.fogofwarlabs.sc4j.screnhanced.graphics;

import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrConsolePlacement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a map script sets on the console: whether the console is shown, the visibility and the top-left corner of each console dialog in the
 * classic screen, the picture drawn instead of the console art and the pictures of the resource icons. An item the script never set is unset,
 * so that the classic console ({@link ClassicConsoleGraphics}) shows what EUD memory gives for it; an item the script set wins over
 * EUD memory from then on. The change count grows with every change.
 */
public final class ConsoleSettings {
    /**
     * The number of resource icon frames: minerals, then the gas and the supply of the Zerg, Terran and Protoss.
     */
    public static final int RESOURCE_ICON_COUNT = 7;

    private static final int DIALOG_COUNT = ScrConsolePlacement.Dialog.values().length;

    private boolean consoleVisible;

    private final boolean @NotNull [] visibilitySet;
    private final boolean @NotNull [] visible;
    private final boolean @NotNull [] positionSet;
    private final int @NotNull [] lefts;
    private final int @NotNull [] tops;

    private @Nullable Picture art;
    private final @Nullable Picture @NotNull [] resourceIcons;

    private long changeCount;

    /**
     * Creates a {@link ConsoleSettings} instance with the console shown and nothing else set.
     */
    public ConsoleSettings() {
        this.consoleVisible = true;

        this.visibilitySet = new boolean[DIALOG_COUNT];
        this.visible       = new boolean[DIALOG_COUNT];
        this.positionSet   = new boolean[DIALOG_COUNT];
        this.lefts         = new int[DIALOG_COUNT];
        this.tops          = new int[DIALOG_COUNT];

        this.art           = null;
        this.resourceIcons = new Picture[RESOURCE_ICON_COUNT];

        this.changeCount = 0;
    }

    /**
     * @return the number of changes so far
     */
    public long getChangeCount() {
        return this.changeCount;
    }

    /**
     * @return {@code true} if the console is shown, {@code false} if the script hid it
     */
    public boolean isConsoleVisible() {
        return this.consoleVisible;
    }

    /**
     * Sets whether the console is shown.
     *
     * @param consoleVisible whether the console is shown
     */
    public void setConsoleVisible(boolean consoleVisible) {
        if (this.consoleVisible != consoleVisible) {
            this.consoleVisible = consoleVisible;
            this.changeCount++;
        }
    }

    /**
     * @return {@code true} if the script set the visibility or the position of a dialog, {@code false} otherwise
     */
    public boolean hasDialogSettings() {
        for (int index = 0; index < DIALOG_COUNT; index++) {
            if (this.visibilitySet[index] || this.positionSet[index]) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param dialog the dialog
     * @return {@code true} if the script set whether the dialog is shown, {@code false} otherwise
     */
    public boolean isDialogVisibilitySet(ScrConsolePlacement.@NotNull Dialog dialog) {
        return this.visibilitySet[dialog.ordinal()];
    }

    /**
     * @param dialog the dialog
     * @return {@code true} if the script showed the dialog or never set it, {@code false} if it hid it
     */
    public boolean isDialogVisible(ScrConsolePlacement.@NotNull Dialog dialog) {
        return !this.visibilitySet[dialog.ordinal()] || this.visible[dialog.ordinal()];
    }

    /**
     * Sets whether a dialog is shown.
     *
     * @param dialog  the dialog
     * @param visible whether the dialog is shown
     */
    public void setDialogVisible(ScrConsolePlacement.@NotNull Dialog dialog, boolean visible) {
        int index = dialog.ordinal();

        if (!this.visibilitySet[index] || this.visible[index] != visible) {
            this.visibilitySet[index] = true;
            this.visible[index]       = visible;
            this.changeCount++;
        }
    }

    /**
     * @param dialog the dialog
     * @return {@code true} if the script set the dialog's position, {@code false} otherwise
     */
    public boolean isDialogPositionSet(ScrConsolePlacement.@NotNull Dialog dialog) {
        return this.positionSet[dialog.ordinal()];
    }

    /**
     * @param dialog the dialog
     * @return the classic screen column of the dialog's left edge the script set, {@code 0} if it set none
     */
    public int getDialogLeft(ScrConsolePlacement.@NotNull Dialog dialog) {
        return this.lefts[dialog.ordinal()];
    }

    /**
     * @param dialog the dialog
     * @return the classic screen row of the dialog's top edge the script set, {@code 0} if it set none
     */
    public int getDialogTop(ScrConsolePlacement.@NotNull Dialog dialog) {
        return this.tops[dialog.ordinal()];
    }

    /**
     * Sets the top-left corner of a dialog in the classic screen.
     *
     * @param dialog the dialog
     * @param left   the screen column of its left edge
     * @param top    the screen row of its top edge
     */
    public void setDialogPosition(ScrConsolePlacement.@NotNull Dialog dialog, int left, int top) {
        int index = dialog.ordinal();

        if (!this.positionSet[index] || this.lefts[index] != left || this.tops[index] != top) {
            this.positionSet[index] = true;
            this.lefts[index]       = left;
            this.tops[index]        = top;
            this.changeCount++;
        }
    }

    /**
     * @return the picture drawn instead of the console art, or {@code null} for the installed art
     */
    public @Nullable Picture getArt() {
        return this.art;
    }

    /**
     * Sets the picture drawn instead of the console art.
     *
     * @param art the picture, or {@code null} for the installed art
     */
    public void setArt(@Nullable Picture art) {
        if (this.art != art) {
            this.art = art;
            this.changeCount++;
        }
    }

    /**
     * @param icon the resource icon frame, below {@link #RESOURCE_ICON_COUNT}
     * @return the picture drawn for the icon, or {@code null} for the icon of EUD memory or the installed icon
     * @throws IndexOutOfBoundsException if the frame is not a resource icon frame
     */
    public @Nullable Picture getResourceIcon(int icon) {
        return this.resourceIcons[icon];
    }

    /**
     * Sets the picture drawn for a resource icon.
     *
     * @param icon    the resource icon frame, below {@link #RESOURCE_ICON_COUNT}
     * @param picture the picture, or {@code null} for the icon of EUD memory or the installed icon
     * @throws IndexOutOfBoundsException if the frame is not a resource icon frame
     */
    public void setResourceIcon(int icon, @Nullable Picture picture) {
        if (this.resourceIcons[icon] != picture) {
            this.resourceIcons[icon] = picture;
            this.changeCount++;
        }
    }
}
