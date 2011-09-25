package de.plushnikov.intellij.lombok.processor.clazz;

import com.intellij.openapi.project.Project;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.StringBuilderSpinAllocator;
import de.plushnikov.intellij.lombok.UserMapKeys;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import de.plushnikov.intellij.lombok.util.PsiClassUtil;
import de.plushnikov.intellij.lombok.util.PsiFieldUtil;
import de.plushnikov.intellij.lombok.util.PsiMethodUtil;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Inspect and validate @ToString lombok annotation on a class
 * Creates equals/hashcode method for fields of this class
 *
 * @author Plushnikov Michail
 */
public class EqualsAndHashCodeProcessor extends AbstractLombokClassProcessor {

  private static final String CLASS_NAME = EqualsAndHashCode.class.getName();
  public static final String EQUALS_METHOD_NAME = "equals";
  public static final String HASH_CODE_METHOD_NAME = "hashCode";
  public static final String CAN_EQUAL_METHOD_NAME = "canEqual";

  public EqualsAndHashCodeProcessor() {
    super(CLASS_NAME, PsiMethod.class);
  }

  @Override
  protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    final boolean result = validateAnnotationOnRigthType(psiClass, builder);
    if (result) {
      validateExistingMethods(psiClass, builder);
    }
    final Collection<String> excludeProperty = PsiAnnotationUtil.getAnnotationValues(psiAnnotation, "exclude", String.class);
    final Collection<String> ofProperty = PsiAnnotationUtil.getAnnotationValues(psiAnnotation, "of", String.class);

    if (!excludeProperty.isEmpty() && !ofProperty.isEmpty()) {
      builder.addWarning("exclude and of are mutually exclusive; the 'exclude' parameter will be ignored");//TODO add QuickFix  : remove all exclude params
    } else {
      validateExcludeParam(psiClass, builder, excludeProperty);
    }
    validateOfParam(psiClass, builder, ofProperty);

    validateCallSuperParam(psiAnnotation, psiClass, builder, "equals/hashCode");
    validateCallSuperParamForObject(psiAnnotation, psiClass, builder);

    return result;
  }

  protected void validateCallSuperParamForObject(PsiAnnotation psiAnnotation, PsiClass psiClass, ProblemBuilder builder) {
    Boolean callSuperProperty = PsiAnnotationUtil.getAnnotationValue(psiAnnotation, "callSuper", Boolean.class);
    if (null != callSuperProperty && callSuperProperty) {
      final PsiClass superClass = psiClass.getSuperClass();
      if (null != superClass && CommonClassNames.JAVA_LANG_OBJECT.equals(superClass.getQualifiedName())) {
        builder.addError("Generating equals/hashCode with a supercall to java.lang.Object is pointless.");//TODO add QuickFix : set callSuper param = false
      }
    }
  }

  protected boolean validateAnnotationOnRigthType(@NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    boolean result = true;
    if (psiClass.isAnnotationType() || psiClass.isInterface() || psiClass.isEnum()) {
      builder.addError("@EqualsAndHashCode is only supported on a class type");
      result = false;
    }
    return result;
  }

  protected boolean validateExistingMethods(@NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    boolean result = true;

    if (areMethodsAlreadyExists(psiClass)) {
      final boolean needsCanEqual = shouldGenerateCanEqual(psiClass);
      builder.addWarning(String.format("Not generating equals%s: A method with one of those names already exists. (Either all or none of these methods will be generated).",
          needsCanEqual ? ", hashCode and canEquals" : " and hashCode"));
      return false;
    }

    return result;
  }

  private boolean areMethodsAlreadyExists(@NotNull PsiClass psiClass) {
    final PsiMethod[] classMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);
    return PsiMethodUtil.hasMethodByName(classMethods, EQUALS_METHOD_NAME, HASH_CODE_METHOD_NAME, CAN_EQUAL_METHOD_NAME);
  }

  protected <Psi extends PsiElement> void processIntern(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<Psi> target) {
    target.addAll((Collection<? extends Psi>) createEqualAndHashCode(psiClass, psiAnnotation));
  }

  protected Collection<PsiMethod> createEqualAndHashCode(PsiClass psiClass, PsiElement psiNavTargetElement) {
    if (areMethodsAlreadyExists(psiClass)) {
      return Collections.emptyList();
    }
    Collection<PsiMethod> result = new ArrayList<PsiMethod>(3);
    result.add(createEqualsMethod(psiClass, psiNavTargetElement));
    result.add(createHashCodeMethod(psiClass, psiNavTargetElement));

    final boolean shouldGenerateCanEqual = shouldGenerateCanEqual(psiClass);
    if (shouldGenerateCanEqual) {
      result.add(createCanEqualMethod(psiClass, psiNavTargetElement));
    }

    Collection<PsiField> equalsAndHashCodeFields = PsiFieldUtil.filterFieldsByModifiers(psiClass.getFields(), PsiModifier.STATIC, PsiModifier.TRANSIENT);
    UserMapKeys.addReadUsageFor(equalsAndHashCodeFields);

    return result;
  }

  private boolean shouldGenerateCanEqual(@NotNull PsiClass psiClass) {
    boolean result = true;
    //needsCanEqual = !isFinal || !isDirectDescendantOfObject
    final PsiClass superClass = psiClass.getSuperClass();
    if (null == superClass && psiClass.hasModifierProperty(PsiModifier.FINAL)) {
      result = false;
    }
    if (null != superClass && psiClass.hasModifierProperty(PsiModifier.FINAL)) {
      final Project project = psiClass.getProject();
      final PsiManager manager = psiClass.getContainingFile().getManager();
      final PsiClassType javaLangObject = PsiType.getJavaLangObject(manager, GlobalSearchScope.projectScope(project));

      result = !superClass.equals(javaLangObject.resolve());
    }
    return result;
  }

  @NotNull
  private PsiMethod createEqualsMethod(@NotNull PsiClass psiClass, @NotNull PsiElement psiNavTargetElement) {
    final StringBuilder builder = StringBuilderSpinAllocator.alloc();
    try {
      builder.append("@java.lang.Override ");
      builder.append("public boolean ").append(EQUALS_METHOD_NAME).append("(final java.lang.Object other)");
      builder.append("{ return super.equals(other); }");

      return PsiMethodUtil.createMethod(psiClass, builder.toString(), psiNavTargetElement);
    } finally {
      StringBuilderSpinAllocator.dispose(builder);
    }
  }

  @NotNull
  private PsiMethod createHashCodeMethod(@NotNull PsiClass psiClass, @NotNull PsiElement psiNavTargetElement) {
    final StringBuilder builder = StringBuilderSpinAllocator.alloc();
    try {
      builder.append("@java.lang.Override ");
      builder.append("public int ").append(HASH_CODE_METHOD_NAME).append("()");
      builder.append("{ return super.hashCode(); }");

      return PsiMethodUtil.createMethod(psiClass, builder.toString(), psiNavTargetElement);
    } finally {
      StringBuilderSpinAllocator.dispose(builder);
    }
  }

  @NotNull
  private PsiMethod createCanEqualMethod(@NotNull PsiClass psiClass, @NotNull PsiElement psiNavTargetElement) {
    final StringBuilder builder = StringBuilderSpinAllocator.alloc();
    try {
      builder.append("public boolean ").append(CAN_EQUAL_METHOD_NAME).append("(final java.lang.Object other)");
      builder.append("{ return other instanceof ").append(psiClass.getName()).append("; }");

      return PsiMethodUtil.createMethod(psiClass, builder.toString(), psiNavTargetElement);
    } finally {
      StringBuilderSpinAllocator.dispose(builder);
    }
  }
}
