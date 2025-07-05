package io.github.frankois944.spmForKmp.definition.exported

import java.io.Serializable

public interface ExportedPackageConfig : Serializable {
    public var isStatic: Boolean
    public var name: String?
    public var includeProduct: List<String>
}
