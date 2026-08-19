import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.System;
import Toybox.Application;

class PhoneWidgetGarminView extends WatchUi.View {

    var phoneBattery = "---";
    var ringerMode = "---";

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() as Void {
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {

        var strings = (Application.getApp() as PhoneWidgetGarminApp).getStrings();
        var raw = strings[0];

        // Only update phoneBattery if we actually have a parseable number.
        // toNumber() returns null (not an exception) on empty/invalid input,
        // so we have to check for that explicitly.
        if (raw != null && !raw.equals("")) {
            var num = raw.toNumber();
            if (num != null) {
                phoneBattery = num.toString() + "%";
            }
        }

        var width = dc.getWidth();

        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);

        dc.drawText(width / 2, 40, Graphics.FONT_SMALL, "Battery", Graphics.TEXT_JUSTIFY_CENTER);
        dc.drawText(width / 2, 75, Graphics.FONT_XTINY, phoneBattery, Graphics.TEXT_JUSTIFY_CENTER);

        dc.drawText(width / 2, 125, Graphics.FONT_SMALL, "Ringer", Graphics.TEXT_JUSTIFY_CENTER);
        dc.drawText(width / 2, 150, Graphics.FONT_XTINY, ringerMode, Graphics.TEXT_JUSTIFY_CENTER);
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() as Void {
    }

}
