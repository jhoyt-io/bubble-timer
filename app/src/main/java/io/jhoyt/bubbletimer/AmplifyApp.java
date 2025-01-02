package io.jhoyt.bubbletimer;

import android.app.Application;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.configuration.AmplifyOutputs;

public class AmplifyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Amplify.addPlugin(new AWSCognitoAuthPlugin());
            Amplify.configure(AmplifyOutputs.fromResource(R.raw.amplify_outputs), getApplicationContext());
            Log.i("AmplifyApp", "Initialized Amplify");
        } catch (AmplifyException e) {
            Log.e("AmplifyApp", "Could not initialize Amplify", e);
        }
    }
}
