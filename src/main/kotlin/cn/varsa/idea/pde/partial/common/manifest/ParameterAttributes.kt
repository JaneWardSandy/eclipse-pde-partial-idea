package cn.varsa.idea.pde.partial.common.manifest

class ParameterAttributes(val attribute: Map<String, String>, val directive: Map<String, String>) {

  private fun toStringAttribute(): String =
    if (attribute.isEmpty()) "" else attribute.entries.joinToString(";", ";") { "${it.key}=${it.value}" }

  private fun toStringDirective(): String =
    if (directive.isEmpty()) "" else directive.entries.joinToString(";", ";") { "${it.key}:=${it.value}" }

  override fun toString(): String = toStringAttribute() + toStringDirective()
}
