package core.model;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.PsiTypeElementImpl;
import com.intellij.psi.impl.source.tree.java.PsiDeclarationStatementImpl;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Change the variable recomputePsiClassTable if you want to recompute the PSI class table
 * Should only be used with the special project (.zip on GitHub)
 * Normally should be false. Set to true if running with that project
 */
public class PsiPreCompEngine {

    public static JsonObject ct = new JsonObject();
    public static boolean recomputePsiClassTable = false;

    public static void doStuff(PsiElement element){

        if(element.getContainingFile().getName().contains("TWF")){
            if(element instanceof PsiDeclarationStatementImpl){
                PsiDeclarationStatementImpl pdsi = (PsiDeclarationStatementImpl) element;
                System.out.println(pdsi.getElementType().toString());
                Object x = pdsi.getFirstChild().getChildren()[1];
                if(x instanceof PsiTypeElementImpl){
                    PsiTypeElementImpl y = (PsiTypeElementImpl) x;
                    // System.out.println("\t"+y.getText());
                    // System.out.println("\t"+y.getType().toString());
                    String canText = y.getType().getCanonicalText();

                    PsiClassHierarchyBuilder psiClassHierarchyBuilder = new PsiClassHierarchyBuilder(ct);
                    Class clazz;
                    try{
                        getClass(canText);
                        clazz = Class.forName(canText);
                        // System.out.println("\tCanText: " + canText);
                        // System.out.println("\t"+Class.forName(canText));
                    } catch (ClassNotFoundException e) {
                        clazz = null;
                    }

                    if(clazz != null){
                        psiClassHierarchyBuilder.processClass(clazz);
                    }

                }
            }
        }
    }

    public static PsiClass getClass(String q) {

        Project p = ProjectManager.getInstance().getOpenProjects()[0];
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(p);
        PsiClass c = psiFacade.findClass(q, GlobalSearchScope.allScope(p));
        // System.out.println("checkClass123 " + q + " ==> " + c);
        return c;
    }
}
