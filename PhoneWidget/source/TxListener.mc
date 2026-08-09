using Toybox.Communications;
using Toybox.System;

class TxListener extends Communications.ConnectionListener {

    function initialize() {
        ConnectionListener.initialize();
    }

    function onComplete() {
        System.println("Sent!");
    }

    function onError() {
        System.println("Failed!");
    }
}