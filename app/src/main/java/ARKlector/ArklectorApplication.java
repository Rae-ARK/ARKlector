package ARKlector;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ARKlector.data.AppDatabase;
import ARKlector.service.Preferences;

/**
 * Process-scoped singletons live here: the Room database and the {@link Preferences}
 * wrapper around {@link SharedPreferences}. Nothing outside this class constructs either
 * directly — see docs/foundational/design.md rule 1 (services own resources).
 */
public final class ArklectorApplication extends Application {

    private AppDatabase database;
    private Preferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.open(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        preferences = new Preferences(prefs);
    }

    public AppDatabase database() {
        return database;
    }

    public Preferences preferences() {
        return preferences;
    }
}
