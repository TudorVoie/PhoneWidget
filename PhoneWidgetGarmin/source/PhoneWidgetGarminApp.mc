import Toybox.Application;
import Toybox.Lang;
import Toybox.WatchUi;
import Toybox.System;
import Toybox.Communications;

class PhoneWidgetGarminApp extends Application.AppBase {

    private var _phoneMethod as Method(msg as PhoneAppMessage) as Void;
    private const _strings = ["","","","",""];

    function initialize() {
        AppBase.initialize();

        _phoneMethod = method(:onPhone);
        if (Communications has :registerForPhoneAppMessages) {
            Communications.registerForPhoneAppMessages(_phoneMethod);
        }
    }

    // onStart() is called on application start up
    function onStart(state as Dictionary?) as Void {
    }

    // onStop() is called when your application is exiting
    function onStop(state as Dictionary?) as Void {
    }

    // Return the initial view of your application here
    function getInitialView() as [Views] or [Views, InputDelegates] {
        return [ new PhoneWidgetGarminView(), new PhoneWidgetGarminDelegate() ];
    }

    public function onPhone(msg as PhoneAppMessage) as Void {
        var data = msg.data;

        if (data != null) {
            for (var i = (_strings.size() - 1); i > 0; i--) {
                _strings[i] = _strings[i-1];
            }
            _strings[0] = data.toString();
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
