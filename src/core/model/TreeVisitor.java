package core.model;

import com.google.gson.JsonObject;
import com.intellij.psi.PsiElement;

// interface to visit PsiElements and to simultaneously build a corresponding tree with JSON for the web-client

public interface TreeVisitor {
    void visit(PsiElement psiElement, JsonObject node);
}
