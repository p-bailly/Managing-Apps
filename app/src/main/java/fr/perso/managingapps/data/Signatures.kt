package fr.perso.managingapps.data

import java.security.MessageDigest

/** Empreinte SHA-256 d'un certificat, en hexadécimal minuscule sans séparateur. */
fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

/** Empreinte écrite à la main ramenée à la forme de [sha256Hex] (« 0B:D9:… » → « 0bd9… »). */
fun normalizeSha256(digest: String): String = digest.replace(":", "").replace(" ", "").lowercase()

/** L'APK est signé par le certificat attendu (parmi ceux qu'Android a lus dans l'archive, [actual]). */
fun signedBy(expected: String, actual: Collection<String>): Boolean =
    normalizeSha256(expected).let { e -> e.length == 64 && actual.any { normalizeSha256(it) == e } }
