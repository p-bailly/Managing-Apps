package fr.perso.managingapps.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignaturesTest {
    private val scrabble = "0bd9375bf39cc9bad5f55b5a484646509f8174765c66476c7b6885bc8685178c"

    @Test
    fun `empreinte SHA-256 en hexadécimal minuscule`() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(ByteArray(0)))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc".toByteArray()))
    }

    @Test
    fun `accepte l'empreinte attendue, écrite comme keytool ou non`() {
        assertTrue(signedBy(scrabble, listOf(scrabble)))
        assertTrue(signedBy(scrabble.uppercase().chunked(2).joinToString(":"), listOf(scrabble)))
        assertTrue(signedBy(scrabble, listOf("00".repeat(32), scrabble)))
    }

    @Test
    fun `refuse une autre clé, aucune clé ou une empreinte attendue mal écrite`() {
        assertFalse(signedBy(scrabble, listOf("00".repeat(32))))
        assertFalse(signedBy(scrabble, emptyList()))
        assertFalse(signedBy("", listOf("")))
        assertFalse(signedBy(scrabble.take(40), listOf(scrabble.take(40))))
    }

    @Test
    fun `chaque appli suivie a une empreinte complète`() {
        for (app in TRACKED_APPS) {
            val e = normalizeSha256(app.certSha256)
            assertTrue("${app.name} : $e", e.length == 64 && e.all { it in "0123456789abcdef" })
        }
    }
}
