import Toybox.Graphics;
import Toybox.WatchUi;
import Toybox.System;
import Toybox.Application;
import Toybox.Timer;
import Toybox.Communications;

class PhoneWidgetGarminView extends WatchUi.View {

    var phoneBattery = "---";
    var ringerMode = "---";

    function initialize() {
        View.initialize();
    }

    // Load your resources here
    function onLayout(dc as Dc) as Void {
    }

    
    function onShow() as Void {
        var listener = new $.CommListener();
        Communications.transmit("sendinfo", null, listener);
    }

    var pornit = 0;
    // var tick = 0;
    var myTimer = new Timer.Timer();

    function timerCallback() as Void {
        pornit = 1;
        // tick = tick + 1;
        var listener = new $.CommListener();
        Communications.transmit("sendinfo", null, listener);
        WatchUi.requestUpdate();
    }

    // Update the view
    function onUpdate(dc as Dc) as Void {

        var strings = (Application.getApp() as PhoneWidgetGarminApp).getStrings();
        var raw = strings[0];

        if (raw != null && !raw.equals("")) {
            if (raw.find("BATTERY:") == 0) {
                var value = raw.substring(8, raw.length());
                var num = value.toNumber();
                if (num != null) {
                    phoneBattery = num.toString() + "%";
                }
            } else if (raw.find("RINGER:") == 0) {
                ringerMode = raw.substring(7, raw.length());
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

        // dc.drawText(width / 2, 175, Graphics.FONT_XTINY, tick.toString(), Graphics.TEXT_JUSTIFY_CENTER);

        var haveBattery = !phoneBattery.equals("---");
        var haveRinger = !ringerMode.equals("---");

        if ((!haveBattery || !haveRinger) && pornit == 0) {
            pornit = 1;
            myTimer.start(method(:timerCallback), 1000, true);
        }

        if (haveBattery && haveRinger) {
            pornit = 0;
            myTimer.stop();
        }

        
    }

    
    function onHide() as Void {
        myTimer.stop();
        pornit = 0;
    }

}
