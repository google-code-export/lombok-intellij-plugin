package de.plushnikov.intellij.lombok.processor.clazz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.psi.Modifier;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import de.plushnikov.intellij.lombok.LombokConstants;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import de.plushnikov.intellij.lombok.processor.field.GetterFieldProcessor;
import de.plushnikov.intellij.lombok.util.LombokProcessorUtil;
import de.plushnikov.intellij.lombok.util.PsiAnnotationUtil;
import de.plushnikov.intellij.lombok.util.PsiClassUtil;
import de.plushnikov.intellij.lombok.util.PsiMethodUtil;
import de.plushnikov.intellij.lombok.util.PsiPrimitiveTypeFactory;
import lombok.Getter;
import lombok.core.TransformationsUtil;

/**
 * Inspect and validate @Getter lombok annotation on a class
 * Creates getter methods for fields of this class
 *
 * @author Plushnikov Michail
 */
public class GetterProcessor extends AbstractLombokClassProcessor {

  private final GetterFieldProcessor fieldProcessor = new GetterFieldProcessor();

  public GetterProcessor() {
    super(Getter.class, PsiMethod.class);
  }

  @Override
  protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    final boolean result = validateAnnotationOnRigthType(psiClass, builder) && validateVisibility(psiAnnotation);

    final Boolean lazy = PsiAnnotationUtil.getAnnotationValue(psiAnnotation, "lazy", Boolean.class);
    if (null != lazy && lazy) {
      builder.addWarning("'lazy' is not supported for @Getter on a type");
    }

    return result;
  }

  protected boolean validateAnnotationOnRigthType(@NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    boolean result = true;
    if (psiClass.isAnnotationType() || psiClass.isInterface()) {
      builder.addError("'@Getter' is only supported on a class, enum or field type");
      result = false;
    }
    return result;
  }

  protected boolean validateVisibility(@NotNull PsiAnnotation psiAnnotation) {
    final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
    return null != methodVisibility;
  }

  protected <Psi extends PsiElement> void processIntern(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<Psi> target) {
    @Modifier final String methodVisibility = LombokProcessorUtil.getMethodModifier(psiAnnotation);
    if (methodVisibility != null) {
      target.addAll((Collection<? extends Psi>) createFieldGetters(psiClass, methodVisibility));
    }
  }

  @NotNull
  public Collection<PsiMethod> createFieldGetters(@NotNull PsiClass psiClass, @Modifier @NotNull String methodModifier) {
    Collection<PsiMethod> result = new ArrayList<PsiMethod>();
    final PsiMethod[] classMethods = PsiClassUtil.collectClassMethodsIntern(psiClass);

    final PsiType booleanType = PsiPrimitiveTypeFactory.getInstance().getBooleanType();
    for (PsiField psiField : psiClass.getFields()) {
      boolean createGetter = true;
      PsiModifierList modifierList = psiField.getModifierList();
      if (null != modifierList) {
        //Skip static fields.
        createGetter = !modifierList.hasModifierProperty(PsiModifier.STATIC);
        //Skip fields having Getter annotation already
        createGetter &= !hasFieldProcessorAnnotation(modifierList);
        //Skip fields that start with $
        createGetter &= !psiField.getName().startsWith(LombokConstants.LOMBOK_INTERN_FIELD_MARKER);
        //Skip fields if a method with same name already exists
        final Collection<String> methodNames = TransformationsUtil.toAllGetterNames(psiField.getName(), booleanType.equals(psiField.getType()));
        createGetter &= !PsiMethodUtil.hasMethodByName(classMethods, methodNames);
      }
      if (createGetter) {
        result.add(fieldProcessor.createGetterMethod(psiField, methodModifier));
      }
    }
    return result;
  }

  private boolean hasFieldProcessorAnnotation(PsiModifierList modifierList) {
    boolean hasSetterAnnotation = false;
    for (PsiAnnotation fieldAnnotation : modifierList.getAnnotations()) {
      hasSetterAnnotation |= fieldProcessor.acceptAnnotation(fieldAnnotation, PsiMethod.class);
    }
    return hasSetterAnnotation;
  }

}