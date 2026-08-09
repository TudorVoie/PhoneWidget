import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Communications;
import Toybox.System;

class PhoneWidgetView extends WatchUi.View {

    var phoneBattery = "---";

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
        // setLayout(Rez.Layouts.MainLayout(dc));
    }

    // Called when this View is brought to the foreground. Restore
    // the state of this View and prepare it to be shown. This includes
    // loading resources into memory.
    function onShow() as Void {
        System.println("Shown");
        // Listen for replies from the phone
        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));

        // Ask the phone for battery
        Communications.transmit({"cmd" => "battery"}, {}, new TxListener());
    
    }

    function onPhoneMessage(msg as Communications.PhoneAppMessage) as Void {
        System.println("got reply: " + msg.data);

        var data = msg.data;
        if (data instanceof Dictionary && data.hasKey("battery")) {
            phoneBattery = data["battery"].toString();
            WatchUi.requestUpdate();
        }
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {
        // Call the parent onUpdate function to redraw the layout
        // View.onUpdate(dc);
        System.println("update");
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        dc.drawText(dc.getWidth() / 2, dc.getHeight() / 2, Graphics.FONT_LARGE, phoneBattery + "%", Graphics.TEXT_JUSTIFY_CENTER);
    }

    // Called when this View is removed from the screen. Save the
    // state of this View here. This includes freeing resources from
    // memory.
    function onHide() as Void {
    }

}
