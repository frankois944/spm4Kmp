package io.github.frankois944.spmForKmp.dump


import com.fasterxml.jackson.annotation.JsonProperty
import android.support.annotation.Keep

@Keep
internal data class PackageKind(
    @JsonProperty("root")
    val root: List<String?>?
)
