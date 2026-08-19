import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.System;

class PhoneWidgetGarminView extends WatchUi.View {

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
        setLayout(Rez.Layouts.MainLayout(dc));
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() as Void {
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {
        var strings = (Application.getApp() as PhoneWidgetGarminApp).getStrings();
        var width = dc.getWidth();

        // Clear background
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_TRANSPARENT);

        // Connection status - true when the phone (and Connect IQ app) is reachable
        var phoneConnected = System.getDeviceSettings().phoneConnected;
        var statusText = phoneConnected ? "Connected" : "Not Connected";

        dc.drawText(width / 2, 30, Graphics.FONT_SMALL, statusText, Graphics.TEXT_JUSTIFY_CENTER);

        // Last message received from the phone
        var lastMessage = strings[0].equals("") ? "No message yet" : strings[0];
        dc.drawText(width / 2, 70, Graphics.FONT_SMALL, lastMessage, Graphics.TEXT_JUSTIFY_CENTER);

        // Hint for the user
        dc.drawText(width / 2, 120, Graphics.FONT_XTINY, "Press START to send", Graphics.TEXT_JUSTIFY_CENTER);

        // Call the parent onUpdate function to redraw the layout
        View.onUpdate(dc);
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() as Void {
    }

}
