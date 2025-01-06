package io.github.frankois944.spmForKmp.dump


import com.fasterxml.jackson.annotation.JsonProperty
import android.support.annotation.Keep

@Keep
internal data class Product(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("settings")
    val settings: List<Any?>?,
    @JsonProperty("targets")
    val targets: List<String?>?,
    @JsonProperty("type")
    val type: Type?
)
