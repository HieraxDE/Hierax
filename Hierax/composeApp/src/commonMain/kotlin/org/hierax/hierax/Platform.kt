package org.hierax.hierax

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform