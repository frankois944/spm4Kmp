package io.github.frankois944.spmForKmp.dump


import com.fasterxml.jackson.annotation.JsonProperty
import android.support.annotation.Keep

@Keep
internal data class SourceControl(
    @JsonProperty("identity")
    val identity: String?,
    @JsonProperty("location")
    val location: Location?,
    @JsonProperty("productFilter")
    val productFilter: Any?,
    @JsonProperty("requirement")
    val requirement: Requirement?
)
