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

    // Every ~5 min: ask the phone for fresh values. The reply wakes
    // onPhoneAppMessage below.
    function onTemporalEvent() as Void {
        Communications.transmit("sendinfo", null, new PhoneWidgetGarminBgListener());
    }

    // Phone pushed a message (on change, or in reply to "sendinfo")
    function onPhoneAppMessage(msg as Communications.PhoneAppMessage) as Void {
        var data = msg.data;
        if (data != null) {
            var text = data.toString();
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