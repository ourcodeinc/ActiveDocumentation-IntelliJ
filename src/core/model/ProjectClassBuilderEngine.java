package core.model;

import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.List;

// This class is utilized when traversing ASTs, so during the traversal, the types that are used get stored for processing in web-client

public class ProjectClassBuilderEngine {

    private JsonObject ct = new JsonObject();
    private ProjectClassHierarchyBuilder projectClassHierarchyBuilder;
    private Project project;

    /**
     * constructor
     * @param classTable empty JSON object
     */
    public ProjectClassBuilderEngine(JsonObject classTable){
        ct = classTable;
        projectClassHierarchyBuilder = new ProjectClassHierarchyBuilder(ct);
        project = ProjectManager.getInstance().getOpenProjects()[0];
    }

    public JsonObject getClassTable() {
        return ct;
    }

    public static String getTypeOfImportantElements(PsiElement element){
        // These are the three ways that types can be found in a Java file
        if (element instanceof PsiExpression) {
            PsiType pt = ((PsiExpression) element).getType();
            if (pt != null) {
                return pt.getCanonicalText();
            }
        }
        PsiType pt2 = PsiUtil.getTypeByPsiElement(element);
        if (pt2 != null) {
            return pt2.getCanonicalText();
        }
        if (element instanceof PsiClass) {
            return ((PsiClass) element).getQualifiedName();
        }
        return null;
    }

    public void doStuff(PsiElement element) {

        if (element instanceof PsiExpression) {
            PsiType pt = ((PsiExpression) element).getType();
            if (pt != null) {
                tryToRun(pt);
            }
        }
        PsiType pt2 = PsiUtil.getTypeByPsiElement(element);

        if (pt2 != null) {
            tryToRun(pt2);
        }

        if (element instanceof PsiClass) {
            PsiClass elemAsPsiClass = (PsiClass) element;
            String qName = elemAsPsiClass.getQualifiedName();
            if (qName != null && qName.endsWith(">")){
                List<String> classNameList = seperateGenerics(qName);
                for (String classNameFromClassList : classNameList) {
                    tryToRunClass(getClass(classNameFromClassList));
                }
            }else{
                tryToRunClass(((PsiClass) element));
            }
        }

    }

    // Extracted Method
    private void tryToRun(PsiType pt) {
        if(pt.getCanonicalText().endsWith(">")){
            List<String> classNameList = seperateGenerics(pt.getCanonicalText());
            for(String classNameFromClassList :classNameList){
                tryToRunClass(getClass(classNameFromClassList));
            }
        }else{
            tryToRunClass(getClass(pt.getCanonicalText()));
        }
    }

    private void tryToRunClass(PsiClass clazz) {
        if (clazz == null) {
            return;
        }
        projectClassHierarchyBuilder.processClass(clazz);
    }

    private void tryToRunClass(String canText) {
        if (canText == null) {
            return;
        }
        Class clazz;
        try {
            clazz = Class.forName(canText);
            // System.out.println("\tCanText: " + canText);
            // System.out.println("\t" + Class.forName(canText));
        } catch (ClassNotFoundException e) {
            clazz = null;
        }

        if (clazz != null) {
            projectClassHierarchyBuilder.processClass(clazz);
        }
    }

    public List<String> seperateGenerics(String q){
        List<String> li = new ArrayList<>();
        if(q == null){
            return li;
        }
        int i1 = q.indexOf('<');
        if(i1 < 0){
            return li;
        }
        int i2 = q.lastIndexOf('>');
        li.add(q.substring(0, i1));
        li.addAll(seperateGenerics(q.substring(i1 + 1, i2)));
        return li;
    }

    public PsiClass getClass(String q) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        PsiClass c = psiFacade.findClass(q, GlobalSearchScope.allScope(project));
        // System.out.println("checkClass123 " + q + " ==> " + c);
        return c;
    }

}
