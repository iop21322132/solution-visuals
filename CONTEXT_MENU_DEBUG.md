# Context Menu Debug Guide

## Current Status
Right-click context menu for modules in ClickGUI is not working. Clicks are not reaching `ModuleComponent.mouseClicked()`.

## Debug Logging Added

### 1. InterfaceScreen.mouseClicked() - BLUE messages
Shows:
- Button number
- Raw mouse coordinates (before transformation)
- Transformed coordinates (after scale transformation)
- Whether click is blocked in topbar zone (RED message)

### 2. ModuleScreen.mouseClicked() - YELLOW messages
Shows:
- Button number
- Mouse coordinates received
- Confirms click reached ModuleScreen

### 3. ModuleComponent.mouseClicked() - GREEN messages
Shows:
- Module name that was clicked
- Hovered state
- Confirms click reached the specific module

## Testing Steps

1. **Compile and run the mod**
   ```bash
   ./gradlew build
   ```

2. **Open ClickGUI** (press the keybind)

3. **Right-click on a module** (e.g., "Potions")

4. **Check chat for debug messages**:
   - If you see BLUE message only → Click is being blocked in InterfaceScreen
   - If you see BLUE + YELLOW → Click reaches ModuleScreen but not ModuleComponent
   - If you see BLUE + YELLOW + GREEN → Click reaches ModuleComponent (success!)

## Expected Behavior

When right-clicking on a module:
1. Context menu should appear with:
   - ⚙️ Настроить (Settings)
   - 🗑️ Удалить (Disable)
2. Clicking "Удалить" should disable the module
3. Clicking "Настроить" should show a message (settings not implemented yet)

## Known Issues

1. **Topbar blocking**: Clicks in the top 24px area (where searchbar is) are blocked
   - This is intentional to prevent opening menu when clicking searchbar
   - Modules in this area won't respond to right-click

2. **Coordinate transformation**: The GUI has scale animation
   - Mouse coordinates must be transformed: `(mouseX - centerX) / scale + centerX`
   - If transformation is wrong, clicks will miss the modules

3. **Hover detection**: ModuleComponent checks if mouse is over it
   - Uses `getTotalHeight()` which includes expanded settings
   - If hover is not detected, right-click won't work

## Possible Fixes

### If click doesn't reach ModuleScreen:
- Check `clickedOnMainGUI` condition in InterfaceScreen
- Check `inTopbar` condition - might be blocking too much area
- Verify transformed coordinates are correct

### If click doesn't reach ModuleComponent:
- Check that ModuleScreen is calling `component.mouseClicked()` for all components
- Verify component coordinates match mouse coordinates
- Check if scroll offset is applied correctly

### If hover is not detected:
- Add debug logging to `ModuleComponent.render()` to show component bounds
- Check if `getTotalHeight()` is calculated correctly
- Verify mouse coordinates match component position

## Files Modified

1. `src/main/java/farvix/solution/api/ui/clickgui/InterfaceScreen.java`
   - Added debug logging for right-click
   - Shows coordinate transformation

2. `src/main/java/farvix/solution/api/ui/clickgui/impl/module/ModuleScreen.java`
   - Added debug logging for right-click
   - Confirms click reaches ModuleScreen

3. `src/main/java/farvix/solution/api/ui/clickgui/ModuleComponent.java`
   - Already has debug logging for right-click
   - Shows module name and hover state

## Next Steps

After testing, report which debug messages appear in chat. This will help identify where the click is being lost.
