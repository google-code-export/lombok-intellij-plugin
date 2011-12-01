package de.plushnikov.intellij.lombok.processor.clazz.constructor;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import de.plushnikov.intellij.lombok.problem.ProblemBuilder;
import de.plushnikov.intellij.lombok.processor.LombokProcessorUtil;
import lombok.AllArgsConstructor;

/**
 * @author Plushnikov Michail
 */
public class AllArgsConstructorProcessor extends AbstractConstructorClassProcessor {

  private static final String CLASS_NAME = AllArgsConstructor.class.getName();

  public AllArgsConstructorProcessor() {
    super(CLASS_NAME, PsiMethod.class);
  }

  @Override
  protected boolean validate(@NotNull PsiAnnotation psiAnnotation, @NotNull PsiClass psiClass, @NotNull ProblemBuilder builder) {
    boolean result;

    result = super.validate(psiAnnotation, psiClass, builder);

    final Collection<PsiField> allNotInitializedNotStaticFields = getAllNotInitializedAndNotStaticFields(psiClass);
    final String staticConstructorName = getStaticConstructorName(psiAnnotation);
    if (!validateIsConstructorDefined(psiClass, staticConstructorName, allNotInitializedNotStaticFields, builder)) {
      result = false;
    }
    return result;
  }

  protected <Psi extends PsiElement> void processIntern(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<Psi> target) {
    final String methodVisibility = LombokProcessorUtil.getAccessVisibity(psiAnnotation);
    if (null != methodVisibility) {
      final Collection<PsiField> allNotInitializedNotStaticFields = getAllNotInitializedAndNotStaticFields(psiClass);

      target.addAll((Collection<? extends Psi>) createConstructorMethod(psiClass, methodVisibility, psiAnnotation, allNotInitializedNotStaticFields));
    }
  }

}