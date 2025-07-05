package io.github.frankois944.spmForKmp.definition.exported

import java.io.Serializable

internal class ExportedPackage :
    ExportedPackageConfig,
    Serializable {
    override var isStatic: Boolean = true
    override var name: String? = null
    override var includeProduct: List<String> = emptyList()

    private companion object {
        private const val serialVersionUID: Long = 2
    }
}
