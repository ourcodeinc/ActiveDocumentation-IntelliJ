package core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.List;

// This class actually puts the particular classes into the classTable and also processes superclasses and interfaces

public class ProjectClassHierarchyBuilder {

    private JsonObject classTable;

    /**
     * constructor
     * @param cTable empty JSON object
     */
    public ProjectClassHierarchyBuilder(JsonObject cTable) {
        classTable = cTable;
    }

    public void processClass(PsiClass clazz) {

        if (classTable.has(clazz.getQualifiedName())) {
            return;
        }

        String simpName = clazz.getName();

        JsonObject val = new JsonObject();
        val.addProperty("name", clazz.getQualifiedName());
        val.addProperty("simpleName", simpName);

        String className = clazz.getQualifiedName();
        if(className == null){
            return;
        }

        classTable.add(className, val);

        // find the PsiModifierList
        List<String> anList = new ArrayList<>();
        for(PsiAnnotation an : clazz.getModifierList().getAnnotations()){
            if(an.getText().trim().startsWith("@")){
                anList.add(an.getText().trim());
            }
        }
        if(anList.size() > 0){
            val.add("annotations", new JsonArray());
            for(String anString : anList){
                val.get("annotations").getAsJsonArray().add(anString);
            }
        }

        // get info based on whether class is a class or interface
        if (clazz.isInterface()) {
            val.addProperty("type", "interface");
            JsonArray imp = new JsonArray();
            for (PsiClass i : clazz.getInterfaces()) {
                if (i != null && i.getQualifiedName() != null) {
                    imp.add(i.getQualifiedName());
                    processClass(i);
                }
            }
            val.add("extends", imp);
        } else {
            val.addProperty("type", "class");
//            if (clazz.getSuperClass() != null) {
//                val.addProperty("extends", clazz.getSuperClass().getQualifiedName());
//                processClass(clazz.getSuperClass());
//            }
            JsonArray imp = new JsonArray();
            for (PsiClass i : clazz.getInterfaces()) {
                if (i != null && i.getQualifiedName() != null) {
                    imp.add(i.getQualifiedName());
                    processClass(i);
                }
            }
            val.add("implements", imp);
        }

    }

    public void processClass(Class clazz) {

        if (classTable.has(clazz.getCanonicalName())) {
            return;
        }

        String simpName = clazz.getSimpleName();

        JsonObject val = new JsonObject();
        val.addProperty("name", clazz.getCanonicalName());
        val.addProperty("simpleName", simpName);

        classTable.add(clazz.getCanonicalName(), val);

        if (clazz.isInterface()) {
            val.addProperty("type", "interface");
            JsonArray imp = new JsonArray();
            for (Class i : clazz.getInterfaces()) {
                imp.add(i.getCanonicalName());
                processClass(i);
            }
            val.add("extends", imp);
        } else {
            val.addProperty("type", "class");
            if (clazz.getSuperclass() != null) {
                val.addProperty("extends", clazz.getSuperclass().getCanonicalName());
                processClass(clazz.getSuperclass());
            }
            JsonArray imp = new JsonArray();
            for (Class i : clazz.getInterfaces()) {
                imp.add(i.getCanonicalName());
                processClass(i);
            }
            val.add("implements", imp);
        }

    }

}


/*
*
* ///////////////
        PsiTypeElement gg = null;
        gg.getType().getCanonicalText();

        PsiJavaCodeReferenceElement f = null;
        f.getCanonicalText(); // https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/psi/PsiReference.java

        PsiIdentifierImpl d = null;
        PsiUtil.getTypeByPsiElement(d);
        d.getTokenType();
        d.getElementType();
*
* */