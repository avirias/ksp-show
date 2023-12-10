package com.avirias.processor

import com.avirias.Decorator
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

class DecoratorProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = Decorator::class.qualifiedName?.let {
            resolver.getSymbolsWithAnnotation(it)
        }

        symbols?.filter {
            it is KSClassDeclaration && it.validate()
        }?.forEach {
            it.accept(DecoratorVisitor(codeGenerator, logger), Unit)
        }
        return symbols?.filter { !it.validate() }?.toList() ?: emptyList()
    }
}