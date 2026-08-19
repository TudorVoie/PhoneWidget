import Toybox.Application;
import Toybox.Communications;
import Toybox.Lang;
import Toybox.System;
import Toybox.WatchUi;

class CommListener extends Communications.ConnectionListener {

    //! Constructor
    public function initialize() {
        Communications.ConnectionListener.initialize();
    }

    //! Handle a communications operation completing
    public function onComplete() as Void {
        System.println("Transmit Complete");
    }

    //! Handle a communications operation erroring
    public function onError() as Void {
        System.println("Transmit Failed");
    }
}

class PhoneWidgetGarminDelegate extends WatchUi.BehaviorDelegate {

    public function initialize() {
        BehaviorDelegate.initialize();
    }

    // Fires on the physical "Start"/Select button
    public function onSelect() as Boolean {
        var listener = new $.CommListener();
        Communications.transmit("Hello World", null, listener);
        return true;
    }
}
