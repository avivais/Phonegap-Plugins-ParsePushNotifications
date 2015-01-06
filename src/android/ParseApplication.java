package com.stratogos.cordova.parsePushNotifications;

import android.app.Application;
import com.parse.Parse;
import com.parse.ParseUser;
import com.parse.ParseACL;
import com.parse.ParseInstallation;

public class ParseApplication extends Application {

    public ParseApplication(){
        super();
    }

    public void onCreate(){
        Parse.initialize(getApplicationContext(), "r7aHNhWLVVZeLMj36TngO5j8pUrgAtx3CkwhGuW6", "WXBNgQEIplGqUfDGQliScPXvPo21mzWPi1Z2wttb");
        ParseInstallation.getCurrentInstallation().saveInBackground();
        ParseUser.enableAutomaticUser();
        ParseACL defaultACL = new ParseACL();
        ParseACL.setDefaultACL(defaultACL, true);
    }
}
