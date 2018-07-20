package com.haulmont.cuba.core.global.queryconditions;

import com.haulmont.bali.util.Dom4j;
import org.dom4j.Element;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(ConditionXmlLoader.NAME)
public class ConditionXmlLoader {

    public static final String NAME = "cuba_ConditionXmlLoader";

    private Map<String, Function<Element, Condition>> factories = new LinkedHashMap<>();

    public ConditionXmlLoader() {
        factories.put("and",
                element -> {
                    if (element.getName().equals("and")) {
                        Condition condition = new LogicalCondition(LogicalCondition.Type.AND);
                        for (Element el : element.elements()) {
                            ((LogicalCondition) condition).getConditions().add(fromXml(el));
                        }
                        return condition;
                    } else {
                        return null;
                    }
        });
        factories.put("or",
                element -> {
                    if (element.getName().equals("or")) {
                        Condition condition = new LogicalCondition(LogicalCondition.Type.OR);
                        for (Element el : element.elements()) {
                            ((LogicalCondition) condition).getConditions().add(fromXml(el));
                        }
                        return condition;
                    } else {
                        return null;
                    }
        });
        factories.put("jpql",
                element -> {
                    if (element.getName().equals("jpql")) {
                        List<PropertyCondition.Entry> entries = element.elements().stream()
                                .map(el -> new PropertyCondition.Entry(el.getName(), el.getText()))
                                .collect(Collectors.toList());
                        return new JpqlCondition(entries);
                    }
                    return null;
        });
    }

    public void addFactory(String name, Function<Element, Condition> factory) {
        factories.put(name, factory);
    }

    public void removeFactory(String name) {
        factories.remove(name);
    }

    public Condition fromXml(String xml) {
        Element element = Dom4j.readDocument(xml).getRootElement();
        return fromXml(element);
    }

    public Condition fromXml(Element element) {
        for (Function<Element, Condition> factory : factories.values()) {
            Condition condition = factory.apply(element);
            if (condition != null)
                return condition;
        }
        throw new RuntimeException("Cannot create condition for element " + element.getName());
    }
}
