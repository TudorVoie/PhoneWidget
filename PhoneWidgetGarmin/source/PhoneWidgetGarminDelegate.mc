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

    // How long (ms) counts as a "hold" vs a quick tap
    private const HOLD_THRESHOLD_MS = 600;
    private var _pressStartTime as Number = 0;

    public function initialize() {
        BehaviorDelegate.initialize();
    }

    // Fires the instant the Start/Select button goes down
    public function onKeyPressed(keyEvent as WatchUi.KeyEvent) as Boolean {
        if (keyEvent.getKey() == WatchUi.KEY_ENTER) {
            _pressStartTime = System.getTimer();
            return true;
        }
        return false;
    }

    // Fires when the Start/Select button is released - here we decide
    // whether it was a quick tap or a hold, and send accordingly
    public function onKeyReleased(keyEvent as WatchUi.KeyEvent) as Boolean {
        if (keyEvent.getKey() == WatchUi.KEY_ENTER) {
            var heldDuration = System.getTimer() - _pressStartTime;
            var listener = new $.CommListener();

            if (heldDuration >= HOLD_THRESHOLD_MS) {
                Communications.transmit("Held Hello World", null, listener);
            } else {
                Communications.transmit("Hello World", null, listener);
            }
            return true;
        }
        return false;
    }
}
