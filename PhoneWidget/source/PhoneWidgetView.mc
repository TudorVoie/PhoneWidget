import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.Communications;
import Toybox.System;

class PhoneWidgetView extends WatchUi.View {

    var phoneBattery = "---";
    var phoneName = "---";
    var requestSent = false;
    var charghing = 0;

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
        // setLayout(Rez.Layouts.MainLayout(dc));
    }

    // Called when this View is brought to the foreground.
    function onShow() as Void {
        System.println("Shown");

        // Listen for replies from the phone
        Communications.registerForPhoneAppMessages(method(:onPhoneMessage));

        // Only request once per app lifetime - onShow can fire repeatedly
        // (waking from sleep, switching back from another view, etc.)
        // and re-transmitting every time fills the transmit queue fast.
        if (!requestSent) {
            requestSent = true;
            Communications.transmit({"cmd" => "info"}, {}, new TxListener());
        }
    }

    function onPhoneMessage(msg as Communications.PhoneAppMessage) as Void {
        System.println("got reply: " + msg.data);

        var data = msg.data;
        if (data instanceof Dictionary) {
            if (data.hasKey("battery")) {
                phoneBattery = data["battery"].toString();
            }
            if (data.hasKey("name")) {
                phoneName = data["name"].toString();
            }
            if(data.hasKey("charge")) {
                charghing = data["charge"].toNumber();
            }
            WatchUi.requestUpdate();
        }
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {
        System.println("update");
        dc.setColor(Graphics.COLOR_WHITE, Graphics.COLOR_BLACK);
        dc.clear();

        dc.drawText(dc.getWidth() / 2, dc.getHeight() / 2 - 45, Graphics.FONT_TINY, phoneName, Graphics.TEXT_JUSTIFY_CENTER);

        try {
            var percentage = phoneBattery.toNumber();
            if(percentage < 20) {
                dc.setColor(Graphics.COLOR_RED, Graphics.COLOR_BLACK);
            } else if(percentage > 89) {
                dc.setColor(Graphics.COLOR_GREEN, Graphics.COLOR_BLACK);
            }
        } catch (e) {
            
        }
        
        

        dc.drawText(dc.getWidth() / 2, dc.getHeight() / 2, Graphics.FONT_LARGE, phoneBattery + "%", Graphics.TEXT_JUSTIFY_CENTER);
        if(charghing == 1) {
            dc.setColor(Graphics.COLOR_YELLOW, Graphics.COLOR_BLACK);
            dc.drawText(dc.getWidth() / 2, dc.getHeight() / 2 + 35, Graphics.FONT_TINY, "Charghing", Graphics.TEXT_JUSTIFY_CENTER);
        }
    }

    // Called when this View is removed from the screen.
    function onHide() as Void {
    }

}
