import Toybox.WatchUi;
import Toybox.System;

class PhoneWidgetDelegate extends WatchUi.BehaviorDelegate {

    var view;

    function initialize(view) {
        BehaviorDelegate.initialize();
        self.view = view;
    }

    // Called for the physical Start/Enter button on button-based devices like the fr55.
    // BehaviorDelegate maps this to onSelect() automatically across most Garmin devices,
    // which is generally more portable than binding onKey(KEY_ENTER) directly.
    function onSelect() {
        view.nextPage();
        return true;
    }

    function onKey(keyEvent) {
        if(keyEvent.getKey() == WatchUi.KEY_UP) {
            if(view.page == 1) {
                view.sendSilent();
                return true;
            }
        }

        //return false;
        return BehaviorDelegate.onKey(keyEvent);
    }

    // Optional: use the Back button to go to the previous page instead of exiting the widget.
    // Comment this out if you'd rather Back close the widget as usual.
    
}
