package fr.perso.managingapps.data

import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/** Erreur dont le message s'affiche tel quel. */
class GitHubException(message: String) : IOException(message)

/** Releases GitHub, sans bibliothèque : HttpURLConnection et org.json d'Android. */
class GitHub(private val token: () -> String?) {

    /**
     * Dernier APK publié pour [app]. Sans jeton, par le lien de téléchargement public de GitHub :
     * sans jeton, l'API n'accepte que 60 requêtes par heure, partagées par tous les appareils
     * derrière la même adresse IP.
     */
    fun latestApk(app: TrackedApp): RemoteApk {
        if (token() == null && app.apkName != null) {
            publicApk(app.repo, app.apkName)?.let { return it }
        }
        return apiApk(app)
    }

    /** Taille et date du fichier derrière « releases/latest/download » ; null si GitHub ne donne pas la date. */
    private fun publicApk(repo: String, apkName: String): RemoteApk? {
        val url = "https://github.com/$repo/releases/latest/download/$apkName"
        // Requête HEAD : les redirections jusqu'au fichier sont suivies, sans rien télécharger.
        val connection = open(url, api = false).apply { requestMethod = "HEAD" }
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> throw GitHubException(
                    "Pas de $apkName dans la dernière release (ou dépôt privé : ajoute un jeton GitHub ⚙)"
                )
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> throw GitHubException("GitHub refuse les requêtes, réessaie plus tard")
                else -> throw GitHubException("Erreur GitHub ${connection.responseCode}")
            }
            val updatedAt = connection.lastModified.takeIf { it > 0 } ?: return null
            return RemoteApk(size = connection.contentLengthLong, updatedAt = updatedAt, downloadUrl = url, viaApi = false)
        } finally {
            connection.disconnect()
        }
    }

    /** Par l'API : avec un jeton (dépôts privés, 5 000 requêtes par heure), ou sans nom de fichier connu. */
    private fun apiApk(app: TrackedApp): RemoteApk {
        val connection = open("https://api.github.com/repos/${app.repo}/releases/latest", api = true)
        try {
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_UNAUTHORIZED -> throw GitHubException("Jeton GitHub refusé (expiré ?)")
                HttpURLConnection.HTTP_FORBIDDEN, 429 -> throw GitHubException(
                    if (connection.getHeaderField("x-ratelimit-remaining") == "0") {
                        "Limite de requêtes GitHub atteinte, réessaie plus tard"
                    } else {
                        "Accès refusé par GitHub"
                    }
                )
                // GitHub répond 404 aussi pour un dépôt privé consulté sans jeton.
                HttpURLConnection.HTTP_NOT_FOUND -> throw GitHubException(
                    if (token() == null) "Aucune release, ou dépôt privé : ajoute un jeton GitHub (⚙)" else "Aucune release publiée"
                )
                else -> throw GitHubException("Erreur GitHub ${connection.responseCode}")
            }
            val json = connection.inputStream.bufferedReader().use { it.readText() }
            return parseRelease(json, app.apkName)
                ?: throw GitHubException("Pas de ${app.apkName ?: "fichier .apk"} dans la dernière release")
        } finally {
            connection.disconnect()
        }
    }

    /** Télécharge [apk] dans [destination], en signalant l'avancement (de 0 à 1). */
    fun download(apk: RemoteApk, destination: File, onProgress: (Float) -> Unit) {
        var connection = open(apk.downloadUrl, api = apk.viaApi, accept = "application/octet-stream")
        if (apk.viaApi) {
            // L'API redirige vers un lien signé du stockage de GitHub, qui refuse l'en-tête Authorization :
            // la redirection est suivie à la main, sans le jeton.
            connection.instanceFollowRedirects = false
            if (connection.responseCode in 300..399) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                connection = open(location ?: throw GitHubException("Téléchargement impossible (redirection vide)"), api = false)
            }
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw GitHubException("Téléchargement impossible (erreur ${connection.responseCode})")
            }
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: apk.size
            destination.parentFile?.mkdirs()
            var received = 0L
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        if (total > 0) onProgress((received.toFloat() / total).coerceAtMost(1f))
                    }
                }
            }
            if (total > 0 && received != total) throw GitHubException("Téléchargement interrompu")
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, api: Boolean, accept: String = "application/vnd.github+json") =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            if (api) {
                setRequestProperty("Accept", accept)
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                token()?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
        }
}

/** APK de la release décrite par [json] (réponse de l'API GitHub) : [apkName], ou le premier « .apk ». */
internal fun parseRelease(json: String, apkName: String?): RemoteApk? {
    val assets = JSONObject(json).optJSONArray("assets") ?: return null
    val asset = (0 until assets.length()).map { assets.getJSONObject(it) }.firstOrNull {
        val name = it.getString("name")
        if (apkName != null) name == apkName else name.endsWith(".apk", ignoreCase = true)
    } ?: return null
    return RemoteApk(
        size = asset.optLong("size"),
        updatedAt = Instant.parse(asset.getString("updated_at")).toEpochMilli(),
        downloadUrl = asset.getString("url"),
        viaApi = true,
    )
}
