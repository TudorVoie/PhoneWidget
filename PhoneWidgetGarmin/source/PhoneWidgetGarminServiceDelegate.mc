import Toybox.Application;
import Toybox.Background;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;

(:background)
class PhoneWidgetGarminServiceDelegate extends System.ServiceDelegate {

    function initialize() {
        ServiceDelegate.initialize();
    }

    function sendWatchBattery() as Void {
        var battery = System.getSystemStats().battery.toNumber();
        Communications.transmit("WATCHBAT:" + battery.toString(), null, new PhoneWidgetGarminBgListener());
    }

    // Every ~5 min: ask the phone for fresh values. The reply wakes
    // onPhoneAppMessage below.
    function onTemporalEvent() as Void {
        sendWatchBattery();   // doar asta, fără "sendinfo"
    }

    // Phone pushed a message (on change, or in reply to "sendinfo")
    function onPhoneAppMessage(msg as Communications.PhoneAppMessage) as Void {
        
        var data = msg.data;
        if (data != null) {
            var text = data.toString();

            if (text.equals("getwatchbat")) {
                sendWatchBattery();
                return; // listener-ul apelează Background.exit după transmisie
            }

            if (text.find("BATTERY:") == 0) {
                var num = text.substring(8, text.length()).toNumber();
                if (num != null) {
                    Application.Storage.setValue("battery", num);
                }
            } else if (text.find("RINGER:") == 0) {
                Application.Storage.setValue("ringer", text.substring(7, text.length()));
            }
        }
        Background.exit(null);
    }
        
}

(:background)
class PhoneWidgetGarminBgListener extends Communications.ConnectionListener {
    function initialize() {
        Communications.ConnectionListener.initialize();
    }
    function onComplete() as Void {
        Background.exit(null);
    }
    function onError() as Void {
        Background.exit(null);
    }
}