package ARKlector;

import android.app.Application;
import android.content.SharedPreferences;

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
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        preferences = new Preferences(prefs);
    }

    public AppDatabase database() {
        return database;
    }

    public Preferences preferences() {
        return preferences;
    }
}
