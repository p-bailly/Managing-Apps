package fr.perso.managingapps.data

import android.content.Context

/** Réglages de l'appli. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    /** Jeton GitHub, nécessaire pour les dépôts privés (Nimbus). */
    var githubToken: String?
        get() = prefs.getString("githubToken", null)?.takeIf { it.isNotBlank() }
        set(value) {
            prefs.edit().putString("githubToken", value?.trim()?.takeIf { it.isNotEmpty() }).apply()
        }
}
