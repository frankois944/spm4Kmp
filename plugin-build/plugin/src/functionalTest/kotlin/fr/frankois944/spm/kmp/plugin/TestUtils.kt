package fr.frankois944.spm.kmp.plugin

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.condition.OS

fun assumeMacos() {
    assumeTrue(OS.MAC.isCurrentOs)
}

fun assumeLinux() {
    assumeTrue(OS.LINUX.isCurrentOs)
}
