package ru.spbau.mit.optimizer.boxing;

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.HashMap;
import java.util.Map;

public class BoxedBasicValue extends BasicValue {

    public static final BasicValue PROGRESSION_ITERATOR = new BasicValue(Type.getObjectType("java/util/Iterator"));
    private static Map<Type, BoxedBasicValue> instances = new HashMap<>();

    public static BoxedBasicValue instance(Type type) {
        if (!instances.containsKey(type)) {
            instances.put(type, new BoxedBasicValue(type));
        }

        return instances.get(type);
    }

    private BoxedBasicValue(Type type) {
        super(type);
    }
}
