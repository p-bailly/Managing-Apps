package fr.perso.managingapps

import fr.perso.managingapps.data.InstalledVersion
import fr.perso.managingapps.data.RemoteApk
import fr.perso.managingapps.data.TrackedApp
import org.junit.Assert.assertEquals
import org.junit.Test

class SortedForDisplayTest {
    private fun row(name: String, published: Long?, installedAt: Long?) = AppRow(
        app = TrackedApp(name, "p-bailly/$name", "fr.perso.$name"),
        installed = installedAt?.let { InstalledVersion("1.0", 1, lastUpdateTime = it) },
        remote = published?.let { RemoteApk(size = 1, updatedAt = it, downloadUrl = "", viaApi = false) },
    )

    private fun List<AppRow>.names() = sortedForDisplay().map { it.app.name }

    @Test
    fun `du plus récent au plus ancien`() {
        val rows = listOf(
            row("ancienne", published = 100, installedAt = 200),
            row("récente", published = 300, installedAt = 400),
            row("milieu", published = 200, installedAt = 300),
        )
        assertEquals(listOf("récente", "milieu", "ancienne"), rows.names())
    }

    @Test
    fun `les mises à jour d'abord, même plus anciennes`() {
        val rows = listOf(
            row("à jour récente", published = 500, installedAt = 600),
            row("à mettre à jour ancienne", published = 100, installedAt = 50),
            row("à jour ancienne", published = 200, installedAt = 300),
            row("à mettre à jour récente", published = 400, installedAt = 350),
        )
        assertEquals(
            listOf("à mettre à jour récente", "à mettre à jour ancienne", "à jour récente", "à jour ancienne"),
            rows.names(),
        )
    }

    @Test
    fun `sans date de publication, à la fin dans l'ordre d'origine`() {
        val rows = listOf(
            row("inconnue 1", published = null, installedAt = 10),
            row("publiée", published = 100, installedAt = 200),
            row("inconnue 2", published = null, installedAt = null),
        )
        assertEquals(listOf("publiée", "inconnue 1", "inconnue 2"), rows.names())
    }
}
