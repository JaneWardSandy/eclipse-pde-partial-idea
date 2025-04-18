package cn.varsa.idea.pde.partial.plugin.kotlin.support

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.*

fun KtTypeReference.classForRefactor(): KtClass? {
  // Use the K2 analyze block. 'this' inside refers to KtAnalysisSession.
  // The KtTypeReference itself is the context element for analysis.
  return analyze(this) {
    // 1. Resolve this KtTypeReference to a KtType (K2 representation)
    // Use this@classForRefactor to refer to the KtTypeReference extension receiver
    val ktType = this@classForRefactor.type

    // 2. Get the symbol declared by this type.
    // Use expandedClassSymbol to resolve through type aliases to the actual class/object.
    val classSymbol: KaClassLikeSymbol? = ktType.expandedSymbol

    // 3. Get the PSI source element (KtDeclaration) directly from the symbol.
    // This replaces DescriptorToSourceUtilsIde.getAnyDeclaration.
    val psiDeclaration = classSymbol?.psi

    // 4. Ensure the resulting PSI is specifically a KtClass
    // (expandedClassSymbol could potentially point to a KtObjectDeclaration too,
    // depending on your exact needs, you might keep it KtClassLikeDeclaration or cast)
    psiDeclaration as? KtClass
  }
}
