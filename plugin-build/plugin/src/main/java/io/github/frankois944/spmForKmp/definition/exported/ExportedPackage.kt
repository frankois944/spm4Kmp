package io.github.frankois944.spmForKmp.definition.exported

import java.io.Serializable

internal class ExportedPackage :
    ExportedPackageConfig,
    Serializable {
    override var isStatic: Boolean = true
    override var name: String? = null

    private companion object {
        private const val serialVersionUID: Long = 1
    }
}
