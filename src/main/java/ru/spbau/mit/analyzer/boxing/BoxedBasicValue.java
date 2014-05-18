package ru.spbau.mit.analyzer.boxing;

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.HashMap;
import java.util.Map;

public class BoxedBasicValue extends BasicValue {

    public static final BasicValue PROGRESSION_ITERATOR = new BasicValue(Type.getObjectType("java/util/Iterator"));
    private static Map<Type, BoxedBasicValue> instances = new HashMap<Type, BoxedBasicValue>();

    private final boolean isFromRange;

    public static BoxedBasicValue instance(Type type) {
        if (!instances.containsKey(type)) {
            instances.put(type, new BoxedBasicValue(type));
        }

        return instances.get(type);
    }

    public BoxedBasicValue(Type type) {
        super(type);
        isFromRange = false;
    }

    public BoxedBasicValue(Type type, boolean isFromRange) {
        super(type);
        this.isFromRange = isFromRange;
    }

    public boolean isFromRange() {
        return isFromRange;
    }
}
