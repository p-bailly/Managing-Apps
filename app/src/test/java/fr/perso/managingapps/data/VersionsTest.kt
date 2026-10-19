package fr.perso.managingapps.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class VersionsTest {
    private val published = Instant.parse("2026-10-19T16:15:58Z").toEpochMilli()
    private val remote = RemoteApk(size = 1, updatedAt = published, downloadUrl = "", viaApi = false)

    private fun installedAt(time: Long) = InstalledVersion("1.0.16", 16, lastUpdateTime = time)

    @Test
    fun `pas installée`() {
        assertEquals(UpdateStatus.NOT_INSTALLED, updateStatus(null, remote))
    }

    @Test
    fun `installée après la publication de l'APK`() {
        assertEquals(UpdateStatus.UP_TO_DATE, updateStatus(installedAt(published + 60_000), remote))
    }

    @Test
    fun `installée avant la publication de l'APK`() {
        assertEquals(UpdateStatus.UPDATE_AVAILABLE, updateStatus(installedAt(published - 60_000), remote))
    }

    @Test
    fun `release lue depuis la réponse de GitHub`() {
        val apk = parseRelease(RELEASE, "Nimbus.apk")!!
        assertEquals(19214431L, apk.size)
        assertEquals(published, apk.updatedAt)
        assertEquals("https://api.github.com/repos/p-bailly/Nimbus/releases/assets/656763103", apk.downloadUrl)
        assertTrue(apk.viaApi)
    }

    @Test
    fun `sans nom de fichier, le premier APK`() {
        assertEquals(
            "https://api.github.com/repos/p-bailly/Nimbus/releases/assets/656763103",
            parseRelease(RELEASE, null)!!.downloadUrl,
        )
    }

    @Test
    fun `fichier absent de la release`() {
        assertNull(parseRelease(RELEASE, "Scan.apk"))
        assertNull(parseRelease("""{"tag_name": "apk", "assets": []}""", null))
    }

    private companion object {
        // Extrait d'une vraie réponse de GET /repos/p-bailly/Nimbus/releases/latest.
        val RELEASE = """
            {
              "tag_name": "apk",
              "published_at": "2026-10-19T12:47:49Z",
              "assets": [
                {
                  "url": "https://api.github.com/repos/p-bailly/Nimbus/releases/assets/656763000",
                  "name": "notes.txt",
                  "size": 120,
                  "updated_at": "2026-10-19T12:47:49Z"
                },
                {
                  "url": "https://api.github.com/repos/p-bailly/Nimbus/releases/assets/656763103",
                  "name": "Nimbus.apk",
                  "size": 19214431,
                  "updated_at": "2026-10-19T16:15:58Z"
                }
              ]
            }
        """.trimIndent()
    }
}
