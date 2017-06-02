// SHOULD NOT USE UNLESS YOU ARE BUILDING THE PSI CLASS HIERARCHY IF INTELLIJ ISSUED SOME NEW FEATURES / UPDATES
package core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.psi.util.PsiUtil;

// This class helps build the relationship of all the Psi- classes with the special project that is included as a .zip on the GitHub page.

public class PsiClassHierarchyBuilder {

    private JsonObject classTable;

    public PsiClassHierarchyBuilder(JsonObject cTable){
        classTable = cTable;
    }

    public void processClass(Class clazz){

        String simpName = getSimpleName(clazz);

        ////////////////
        if(classTable.has(simpName)){
            return;
        }

        JsonObject val = new JsonObject();
        val.addProperty("name", clazz.getName());
        val.addProperty("simpleName", simpName);

        classTable.add(simpName, val);

        if(clazz.isInterface()){
            val.addProperty("type", "interface");
            JsonArray imp = new JsonArray();
            for(Class i : clazz.getInterfaces()){
                imp.add(getSimpleName(i));
                processClass(i);
            }
            val.add("extends", imp);
        }else{
            val.addProperty("type", "class");
            if(clazz.getSuperclass() != null) {
                val.addProperty("extends", getSimpleName(clazz.getSuperclass()));
                processClass(clazz.getSuperclass());
            }
            JsonArray imp = new JsonArray();
            for(Class i : clazz.getInterfaces()){
                imp.add(getSimpleName(i));
                processClass(i);
            }
            val.add("implements", imp);
        }

    }

    public String getSimpleName(Class clazz){
        if(clazz.getSimpleName().equals("Stub")){
            return getStubName(clazz);
        }else{
            return clazz.getSimpleName();
        }
    }

    public String getStubName(Class clazz){
        Class sup = clazz.getSuperclass();
        if(sup != null){
            return sup.getSimpleName() + "$" + "Stub";
        }
        return "";
    }

    public boolean isIgnorePsiElement(PsiElement element){
        if(element instanceof PsiWhiteSpace){
            return true;
        }
        return false;
    }
}
