import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;
import Toybox.System;
import Toybox.Communications;
import Toybox.Background;
import Toybox.Time;

(:glance, :background)
class PhoneWidgetGarminApp extends Application.AppBase {

    private var _phoneMethod as Method(msg as PhoneAppMessage) as Void;
    private const _strings = ["","","","",""];

    function initialize() {
        AppBase.initialize();
        _phoneMethod = method(:onPhone);

        /*
        if (Communications has :registerForPhoneAppMessages) {
            Communications.registerForPhoneAppMessages(_phoneMethod);
        }
        */
    }

    // onStart() is called on application start up
    function onStart(state as Dictionary?) as Void {
    }

    // onStop() is called when your application is exiting
    function onStop(state as Dictionary?) as Void {
    }

    // Return the initial view of your application here
    function getInitialView() as [Views] or [Views, InputDelegates] {
        if (Communications has :registerForPhoneAppMessages) {
            Communications.registerForPhoneAppMessages(_phoneMethod);
        }
        if (Toybox has :Background) {
            Background.registerForTemporalEvent(new Time.Duration(5 * 60));
            if (Background has :registerForPhoneAppMessageEvent) {
                Background.registerForPhoneAppMessageEvent();
            }
        }
        return [ new PhoneWidgetGarminView(), new PhoneWidgetGarminDelegate() ];
    }

    function getServiceDelegate() as [System.ServiceDelegate] {
        return [ new PhoneWidgetGarminServiceDelegate() ];
    }

    function getGlanceView() {
        return [ new PhoneWidgetGarminGlanceView() ];
    }

    

    public function onPhone(msg as PhoneAppMessage) as Void {
        var data = msg.data;

        if (data != null) {
            var text = data.toString();

            for (var i = (_strings.size() - 1); i > 0; i--) {
                _strings[i] = _strings[i-1];
            }
            _strings[0] = text;

            // Persist for the glance
            if (text.find("BATTERY:") == 0) {
                var num = text.substring(8, text.length()).toNumber();
                if (num != null) {
                    Application.Storage.setValue("battery", num);
                }
            } else if (text.find("RINGER:") == 0) {
                Application.Storage.setValue("ringer", text.substring(7, text.length()));
            }
        }

        WatchUi.requestUpdate();
    }

    public function getPhoneMethod() as Method(msg as PhoneAppMessage) as Void {
        return _phoneMethod;
    }

    public function getStrings() as Array<String> {
        return _strings;
    }

}
