package ru.spbau.mit.optimizer;

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BoxedBasicValue extends BasicValue {
    private final HashSet<AbstractInsnNode> associatedInsns = new HashSet<AbstractInsnNode>();
    private final AbstractInsnNode boxingInsn;
    private final Type basicType;
    private final Type boxedType;
    private final boolean isFromProgression;
    private boolean wasUnboxed = false;


    private static Type changeType(Type type) {
        assert (type.getSort() == Type.OBJECT);
        String internalName = type.getInternalName();

        return Type.getObjectType(internalName + "Boxed");
    }

    public BoxedBasicValue(Type type, Type boxedType, AbstractInsnNode insnNode, boolean isFromProgression) {
        super(changeType(type));
        basicType = type;
        this.boxedType = boxedType;
        associatedInsns.add(insnNode);
        boxingInsn = insnNode;
        this.isFromProgression = isFromProgression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoxedBasicValue that = (BoxedBasicValue) o;

        if (!getType().equals(that.getType())) return false;

        return (boxingInsn == ((BoxedBasicValue) o).boxingInsn);
    }

    public List<AbstractInsnNode> getAssociatedInsns() {
        return new ArrayList<AbstractInsnNode>(associatedInsns);
    }

    public void addInsn(AbstractInsnNode insnNode) {
        associatedInsns.add(insnNode);
    }

    public Type getBasicType() {
        return basicType;
    }

    public Type getBoxedType() {
        return boxedType;
    }

    public boolean wasUnboxed() {
        return wasUnboxed;
    }

    public void setWasUnboxed(boolean wasUnboxed) {
        this.wasUnboxed = wasUnboxed;
    }

    public boolean isFromProgression() {
        return isFromProgression;
    }

    public AbstractInsnNode getBoxingInsn() {
        return boxingInsn;
    }
}
