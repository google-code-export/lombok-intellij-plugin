package de.plushnikov.intellij.lombok.processor.clazz;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import de.plushnikov.intellij.lombok.psi.MyLightMethod;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Plushnikov Michail
 */
public class EqualsAndHashCodeProcessor extends AbstractLombokClassProcessor {

  private static final String CLASS_NAME = EqualsAndHashCode.class.getName();

  public EqualsAndHashCodeProcessor() {
    super(CLASS_NAME, PsiMethod.class);
  }

  public <Psi extends PsiElement> boolean process(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation, @NotNull List<Psi> target) {
    Project project = psiClass.getProject();
    PsiManager manager = psiClass.getContainingFile().getManager();
    PsiElementFactory elementFactory = JavaPsiFacade.getInstance(project).getElementFactory();

    PsiMethod equalsMethod = createEqualsMethod(psiClass, elementFactory);
    target.add((Psi) new MyLightMethod(manager, equalsMethod, psiClass));

    PsiMethod hashcodeMethod = createHashCodeMethod(psiClass, elementFactory);
    target.add((Psi) new MyLightMethod(manager, hashcodeMethod, psiClass));

    return true;
  }

  private PsiMethod createEqualsMethod(PsiClass psiClass, PsiElementFactory elementFactory) {
    return elementFactory.createMethodFromText(
        "public boolean equals(Object o) { return super.equals(o); }",
        psiClass);
  }

  private PsiMethod createHashCodeMethod(PsiClass psiClass, PsiElementFactory elementFactory) {
    return elementFactory.createMethodFromText(
        "public int hashCode() { return super.hashCode(); }",
        psiClass);
  }

}
