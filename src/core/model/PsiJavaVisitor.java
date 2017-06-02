// https://github.com/cmf/psiviewer/blob/master/src/idea/plugin/psiviewer/util/IntrospectionUtil.java

package core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.psi.*;

import java.util.ArrayList;
import java.util.HashMap;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.List;


// visitor for java files

public class PsiJavaVisitor implements TreeVisitor {

    HashMap<PsiElement, JsonObject> map = new HashMap<>();
    private final HashSet<Class> acceptableClasses = generateAcceptableClasses();
    private final HashSet<String> unacceptableVars = generateUnacceptableVars();
    private JsonObject projectClassTable;
    private ProjectClassBuilderEngine projectClassBuilderEngine;

    /**
     * Constructor
     * @param projectClassTable empty JSON
     */
    public PsiJavaVisitor(JsonObject projectClassTable){
        this.projectClassTable = projectClassTable;
        projectClassBuilderEngine = new ProjectClassBuilderEngine(this.projectClassTable);
    }

    /**
     *
     * @param psiElement (Java) files, elements
     * @param jsonNode in all calls, empty JSON object
     */
    @Override
    public void visit(PsiElement psiElement, JsonObject jsonNode) {

        if (psiElement == null) {
            return;
        }

        if (isIgnorePsiElement(psiElement)) {
            return;
        }

        // SHOULD NOT USE UNLESS YOU ARE BUILDING THE PSI CLASS HIERARCHY IF INTELLIJ ISSUED SOME NEW FEATURES
        // / UPDATES (use the project that is a .zip file on GitHub)
        if (PsiPreCompEngine.recomputePsiClassTable) {
            PsiPreCompEngine.doStuff(psiElement);
        }

        projectClassBuilderEngine.doStuff(psiElement);

        // take the new json object and give it an empty children array
        jsonNode.add("children", new JsonArray());
        jsonNode.add("properties", extractData(psiElement));
        map.put(psiElement, jsonNode);

        // get the parent and add node to the parent's children
        PsiElement parent = psiElement.getParent();
        if (parent != null && map.containsKey(parent)) {
            map.get(parent).get("children").getAsJsonArray().add(jsonNode);
        }

        for (PsiElement child : psiElement.getChildren()) {
            // System.out.println(child.getText());
            visit(child, new JsonObject());
        }

    }

    public boolean isIgnorePsiElement(PsiElement element) {
        return (element instanceof PsiWhiteSpace);
    }

    public JsonObject extractData(PsiElement element) {

        JsonObject properties = new JsonObject();

        // properties that all elements will have
        properties.addProperty("psiType", element.getClass().getSimpleName());
        String specialType = ProjectClassBuilderEngine.getTypeOfImportantElements(element);
        if(specialType != null){
            properties.addProperty("type", specialType);
            if(!projectClassTable.has(specialType)){
                projectClassBuilderEngine.doStuff(element);
            }
        }

        // check annotations
//        if(element instanceof PsiAnnotationOwner){
//            //System.out.println("---------Annotate----------");
//            //System.out.println(element.getParent());
//            //System.out.println(element);
//            PsiAnnotationOwner psiAnnotationOwner =(PsiAnnotationOwner)element;
//            List<String> li = new ArrayList<>();
//            for(PsiAnnotation pa : psiAnnotationOwner.getAnnotations()){
//                if(pa.getText().trim().startsWith("@")) {
//                    //System.out.println("\t => " + pa.getText());
//                    li.add(pa.getText().trim());
//                }
//            }
//            if(li.size() > 0) {
//                JsonObject parJsonProp = map.get(element.getParent()).get("properties").getAsJsonObject();
//                parJsonProp.add("annotations", new JsonArray());
//                for(String s : li) {
//                    parJsonProp.get("annotations").getAsJsonArray().add(s);
//                }
//            }
//
//            if(element.getParent() instanceof PsiClass){
//
//            }
//
//            // System.out.println("-------------------");
//        }

        // Extract data with IntrospectionUtil. Get only the primitive types
        // so we can represent the data in the Json structure we build
        PropertyDescriptor[] propertyDescriptors = IntrospectionUtil.getProperties(element.getClass());
        for (PropertyDescriptor pd : propertyDescriptors) {
            if (isClassAcceptable(pd.getPropertyType()) && isVariableAcceptable(pd.getName())) {

                Object val = IntrospectionUtil.getValue(element, pd);

                if (val instanceof String) {
                    properties.addProperty(pd.getName(), (String) val);
                } else if (val instanceof Integer) {
                    properties.addProperty(pd.getName(), (Integer) val);
                } else if (val instanceof Double) {
                    properties.addProperty(pd.getName(), (Double) val);
                } else if (val instanceof Byte) {
                    properties.addProperty(pd.getName(), (Byte) val);
                } else if (val instanceof Boolean) {
                    properties.addProperty(pd.getName(), (Boolean) val);
                } else if (val instanceof Character) {
                    properties.addProperty(pd.getName(), (Character) val);
                } else if (val instanceof Short) {
                    properties.addProperty(pd.getName(), (Short) val);
                } else if (val instanceof Long) {
                    properties.addProperty(pd.getName(), (Long) val);
                } else if (val instanceof Float) {
                    properties.addProperty(pd.getName(), (Float) val);
                //} else if (val == null) {
                } else {
                    System.out.println("An unhandled primitive?! " + val);
                }
            }
        }

        return properties;

    }

    // these four functions help filter out some of the less useful info the IDE provides

    public boolean isVariableAcceptable(String s) {
        return !unacceptableVars.contains(s);
    }

    public boolean isClassAcceptable(Class clazz) {
        // return true;
        return acceptableClasses.contains(clazz);
    }

    private HashSet<Class> generateAcceptableClasses() {
        HashSet<Class> output = new HashSet<>();
        output.add(Boolean.class);
        output.add(Character.class);
        output.add(Byte.class);
        output.add(Short.class);
        output.add(Integer.class);
        output.add(Long.class);
        output.add(Float.class);
        output.add(Double.class);
        output.add(String.class);

        output.add(boolean.class);
        output.add(char.class);
        output.add(byte.class);
        output.add(short.class);
        output.add(int.class);
        output.add(long.class);
        output.add(float.class);
        output.add(double.class);

        return output;
    }

    public HashSet<String> generateUnacceptableVars() {
        HashSet<String> output = new HashSet<>();

        output.add("userDataString");

        return output;
    }

}