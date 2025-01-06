package io.github.frankois944.spmForKmp.dump


import com.fasterxml.jackson.annotation.JsonProperty
import android.support.annotation.Keep

@Keep
internal data class Platform(
    @JsonProperty("options")
    val options: List<Any?>?,
    @JsonProperty("platformName")
    val platformName: String?,
    @JsonProperty("version")
    val version: String?
)
