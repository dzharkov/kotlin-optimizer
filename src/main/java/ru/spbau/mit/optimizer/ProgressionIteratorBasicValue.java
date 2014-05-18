package ru.spbau.mit.optimizer;

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

public class ProgressionIteratorBasicValue extends BasicValue {
    private final Type boxedType;

    public ProgressionIteratorBasicValue(Type type, Type boxedType) {
        super(type);
        this.boxedType = boxedType;
    }

    public Type getBoxedType() {
        return boxedType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProgressionIteratorBasicValue that = (ProgressionIteratorBasicValue) o;

        if (boxedType != null ? !boxedType.equals(that.boxedType) : that.boxedType != null) return false;

        return true;
    }
}
