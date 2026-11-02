package skills_overhaul.ui.host;

import org.lwjgl.input.Keyboard;

public class SkillsHotkeyGuardTest {
    public static void main(String[] args) {
        int k = Keyboard.KEY_K;

        // shouldOpen(consumed, keyDown, eventKey, configuredKey, hasDialog, showingMenu, coreTab)
        assert SkillsHotkeyListener.shouldOpen(false, true, k, k, false, false, false)
                : "opens on unconsumed key-down of the configured key on the map";

        assert !SkillsHotkeyListener.shouldOpen(true, true, k, k, false, false, false) : "consumed";
        assert !SkillsHotkeyListener.shouldOpen(false, false, k, k, false, false, false) : "key-up";
        assert !SkillsHotkeyListener.shouldOpen(false, true, Keyboard.KEY_J, k, false, false, false) : "other key";
        assert !SkillsHotkeyListener.shouldOpen(false, true, k, k, true, false, false) : "interaction dialog open";
        assert !SkillsHotkeyListener.shouldOpen(false, true, k, k, false, true, false) : "menu showing";
        assert !SkillsHotkeyListener.shouldOpen(false, true, k, k, false, false, true) : "core tab open";

        // A rebound key opens on itself and no longer on K.
        int f8 = Keyboard.KEY_F8;
        assert SkillsHotkeyListener.shouldOpen(false, true, f8, f8, false, false, false) : "configured F8";
        assert !SkillsHotkeyListener.shouldOpen(false, true, k, f8, false, false, false) : "K when F8 configured";

        System.out.println("ALL PASSED");
    }
}
