package com.ray.processor

import com.cnstrong.annotations.ServiceRepository
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@AutoService(Processor::class)
class ServiceRepositoryProcessor : AbstractProcessor() {

    private lateinit var elementUtil: Elements
    private lateinit var messager: Messager
    private lateinit var filerUtil: Filer

    private lateinit var typeUtils: Types

    private val projectNameKey = "projectName"

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        elementUtil = processingEnv!!.elementUtils
        filerUtil = processingEnv.filer
        messager = processingEnv.messager
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf<String>().apply {
            add(ServiceRepository::class.java.canonicalName)
        }
    }

    /**
     * kapt {
    arguments {
    arg("projectName", "xxx")
    }
    }
     */
    //在process方法中 String resultPath = processingEnv.getOptions().get(projectNameKey);
    override fun getSupportedOptions(): MutableSet<String> {
        return mutableSetOf<String>().apply {
            add(projectNameKey)
        }
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }


    override fun process(
        typeElements: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {

        if (typeElements.isNullOrEmpty() || roundEnvironment == null) {
            return false
        }

        val annotationElementSet =
            roundEnvironment.getElementsAnnotatedWith(ServiceRepository::class.java)

        annotationElementSet.forEach {
            if (it != null && it.kind == ElementKind.INTERFACE) {
                val annotation = it.getAnnotation(ServiceRepository::class.java)
                if (annotation != null) {
                    generationImpl(it as TypeElement, annotation)
                }
            }
        }
        return true
    }

    private fun generationImpl(element: TypeElement, annotation: ServiceRepository) {

        val packageName = elementUtil.getPackageOf(element).qualifiedName.toString()

        val targetName =
            annotation.altName.ifEmpty { element.simpleName.toString() + "Repository" }


        val closedElements = mutableListOf<ExecutableElement>().apply {
            element.enclosedElements?.forEach {
                if (it != null && it.kind == ElementKind.METHOD) {
                    this.add(it as ExecutableElement)
                }
            }
        }

        val funcSpecs = mutableListOf<FunSpec>().apply {

            val serviceClassName = ClassName(packageName, element.simpleName.toString())

            val httpMember = MemberName("com.ray.network", "createService")

            closedElements.forEach {

                val funBuilder = FunSpec.builder(it.simpleName.toString())

                val variableElements = it.parameters

                var hasSuspend = false

                if (it.parameters != null && it.parameters.size > 0) {
                    val lastVariableElement = it.parameters[it.parameters.lastIndex]
                    if (lastVariableElement.asType().kind == TypeKind.DECLARED) {
                        val actualType =
                            (lastVariableElement.asType() as DeclaredType).asElement() as TypeElement
                        val actualClassName = ClassName(
                            elementUtil.getPackageOf(actualType).qualifiedName.toString(),
                            actualType.simpleName.toString()
                        )
                        hasSuspend =
                            actualClassName == ClassName("kotlin.coroutines", "Continuation")
                        val typeArguments =
                            (lastVariableElement.asType() as DeclaredType).typeArguments
                        if (typeArguments != null && typeArguments.size > 0) {
                            val targetMirror = typeArguments[0]
                            funBuilder.returns(getTypeName(targetMirror))
                        }
                    }
                }

                if (!hasSuspend) {
                    funBuilder.returns(getTypeName(it.returnType))
                } else {
                    funBuilder.addModifiers(KModifier.SUSPEND)
                }

                val lastTargetIndex = variableElements?.takeIf { it.size > 0 }
                    ?.run { if (hasSuspend) this.size - 1 else this.size } ?: 0

                variableElements.forEachIndexed { index, variableElement ->
                    if (variableElement != null && index < lastTargetIndex) {
                        funBuilder.addParameter(
                            variableElement.simpleName.toString(),
                            getTypeName(variableElement.asType())
                        )
                    }
                }

                val stringBuilder = StringBuilder()

                if (it.returnType.kind == TypeKind.DECLARED) {
                    stringBuilder.append("return ")
                }
                stringBuilder.append("%M(%T::class.java).${it.simpleName}")
                stringBuilder.append("(")
                variableElements.forEachIndexed { index, variableElement ->
                    if (variableElement != null && index < lastTargetIndex) {
                        stringBuilder.append(variableElement.simpleName.toString())
                        stringBuilder.append(",")
                    }
                }
                stringBuilder.append(")")

                funBuilder.addStatement(
                    stringBuilder.toString(), httpMember, serviceClassName
                )

                this.add(funBuilder.build())
            }
        }

        val file = FileSpec.builder(packageName, targetName).addType(
            TypeSpec.classBuilder(targetName).addFunctions(funcSpecs).build()
        ).build()

        file.writeTo(filerUtil)
    }

    private fun getTypeName(typeMirror: TypeMirror): TypeName {

        return when (typeMirror.kind) {
            TypeKind.INT -> INT
            TypeKind.CHAR -> CHAR
            TypeKind.BYTE -> BYTE
            TypeKind.SHORT -> SHORT
            TypeKind.LONG -> LONG
            TypeKind.FLOAT -> FLOAT
            TypeKind.DOUBLE -> DOUBLE
            TypeKind.BOOLEAN -> BOOLEAN
            TypeKind.VOID -> UNIT
            TypeKind.DECLARED -> {
                val actualElement = (typeMirror as DeclaredType).asElement() as TypeElement
                val actualClassName = ClassName(
                    elementUtil.getPackageOf(actualElement).qualifiedName.toString(),
                    actualElement.simpleName.toString()
                )
                val typeNames = mutableListOf<TypeName>()
                typeMirror.typeArguments?.forEach {
                    it?.run {
                        typeNames.add(getTypeName(it))
                    }
                }

                when (actualClassName) {
                    ClassName("java.lang", "String") -> STRING
                    ClassName("java.lang", "Object") -> ANY
                    ClassName("java.lang", "Enum") -> ENUM
                    ClassName("java.util", "List") -> if (typeNames.size > 0) LIST.parameterizedBy(
                        typeNames
                    ) else LIST
                    ClassName("java.util", "Map") -> if (typeNames.size > 0) MAP.parameterizedBy(
                        typeNames
                    ) else MAP
                    else -> if (typeNames.size > 0) actualClassName.parameterizedBy(typeNames) else actualClassName
                }
            }

            TypeKind.ARRAY -> {
                val arrayType = (typeMirror as ArrayType)
                return when (arrayType.componentType.kind) {
                    TypeKind.INT -> INT_ARRAY
                    TypeKind.CHAR -> CHAR_ARRAY
                    TypeKind.BYTE -> BYTE_ARRAY
                    TypeKind.SHORT -> SHORT_ARRAY
                    TypeKind.LONG -> LONG_ARRAY
                    TypeKind.FLOAT -> FLOAT_ARRAY
                    TypeKind.DOUBLE -> DOUBLE_ARRAY
                    TypeKind.BOOLEAN -> BOOLEAN_ARRAY
                    TypeKind.DECLARED -> {
                        return ClassName(
                            "kotlin",
                            "Array"
                        ).parameterizedBy(getTypeName(arrayType.componentType))
                    }
                    else -> ANY
                }
            }

            //处理不是很恰当
            TypeKind.WILDCARD -> {
                val wildType = typeMirror as WildcardType
                val superMirror = wildType.superBound
                getTypeName(superMirror)
            }

            else -> ANY
        }
    }
}