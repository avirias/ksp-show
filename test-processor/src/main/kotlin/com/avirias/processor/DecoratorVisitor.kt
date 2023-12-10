package com.avirias.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class DecoratorVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    private val functions = mutableListOf<FunctionData>()

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        classDeclaration.getDeclaredFunctions()
            .forEach { it.accept(this, Unit) }

        val packageName = classDeclaration.packageName.asString()
        val className = "Decorated$classDeclaration"

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(
                TypeSpec.classBuilder(className)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter(getParamSpecs(classDeclaration))
                            .build()
                    )
                    .addProperty(
                        PropertySpec
                            .builder("$classDeclaration".lowercase(), getTypeName(classDeclaration))
                            .addModifiers(KModifier.PRIVATE)
                            .initializer("$classDeclaration".lowercase())
                            .build()
                    )
                    .addSuperinterface(getTypeName(classDeclaration))
                    .addModifiers(KModifier.PUBLIC)
                    .addFunctions(getAllFunctions(classDeclaration))
                    .build()
            ).build()

        fileSpec.writeTo(codeGenerator, true)
    }

    private fun getAllFunctions(classDeclaration: KSClassDeclaration): Iterable<FunSpec> {
        return functions.map {
            FunSpec.builder(it.functionName)
                .addModifiers(KModifier.OVERRIDE)
                .returns(it.returnType!!.toTypeName())
                .addStatement("return ${classDeclaration.toString().lowercase()}.${it.functionName}()")
                .build()
        }
    }

    private fun getParamSpecs(classDeclaration: KSClassDeclaration): ParameterSpec {
        return ParameterSpec(
            "$classDeclaration".lowercase(), getTypeName(classDeclaration)
        )

    }

    private fun getTypeName(classDeclaration: KSClassDeclaration): TypeName {
        return ClassName(classDeclaration.packageName.toString(), "$classDeclaration")
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: Unit) {
        logger.info("functions", function)
        functions.add(FunctionData(functionName = function.toString()))
        function.returnType?.accept(this, Unit)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
        functions.firstOrNull {
            it.functionName == typeReference.parent.toString()
        }?.returnType = typeReference
    }
}

data class FunctionData(
    val functionName: String,
    var returnType: KSTypeReference? = null
)