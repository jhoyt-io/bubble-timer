package io.jhoyt.bubbletimer;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.configuration.AmplifyOutputs;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class BubbleTimerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Configure Amplify plugins
        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(AmplifyOutputs.fromResource(R.raw.amplify_outputs), getApplicationContext());
            Log.i("BubbleTimerApplication", "Initialized Amplify");
        } catch (AmplifyException e) {
            Log.e("BubbleTimerApplication", "Could not initialize Amplify", e);
        }
    }
} 