package ru.spbau.mit.optimizer;

import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.*;

class RedundantBoxingInterpreter extends BasicInterpreter {
    private Map<Integer, BoxedBasicValue> boxingPlaces = new HashMap<Integer, BoxedBasicValue>();
    private Set<BoxedBasicValue> candidatesBoxedValues = new HashSet<BoxedBasicValue>();
    private  InsnList insnList;

    RedundantBoxingInterpreter(InsnList insnList) {
        this.insnList = insnList;
    }

    private boolean isUnboxing(AbstractInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return false;
        }

        MethodInsnNode methodInsn = (MethodInsnNode) insn;

        return isClassBox(methodInsn.owner) && isUnboxingMethod(methodInsn.name);
    }

    private boolean isClassBox(String owner) {

        if (!owner.startsWith("java/lang/")) {
            return false;
        }

        String className = owner.substring("java/lang/".length());

        return (className.equals("Integer") ||
            className.equals("Double") ||
            className.equals("Long") ||
            className.equals("Char") ||
            className.equals("Byte") ||
            className.equals("Boolean")) ||
            className.endsWith("Number");
    }

    private boolean isUnboxingMethod(String name) {
        return name.endsWith("Value");
    }

    @Override
    public BasicValue newValue(Type type) {
        if (type == null) {
            return super.newValue(type);
        }
        if (type.getSort() == Type.OBJECT) {
            return new BasicValue(type);
        }

        return super.newValue(type);
    }

    private Type getProgressionIteratorType(Type type) {
        if (type.getSort() != Type.OBJECT) {
            return null;
        }

        String internalName = type.getInternalName();

        if (!internalName.startsWith("kotlin/")) {
            return null;
        }

        String iteratorType = internalName.substring(
                "kotlin/".length()
        );

        if (iteratorType.startsWith("Int")) {
            return Type.INT_TYPE;
        }
        if (iteratorType.startsWith("Long")) {
            return Type.LONG_TYPE;
        }
        if (iteratorType.startsWith("Byte")) {
            return Type.BYTE_TYPE;
        }
        if (iteratorType.startsWith("Char")) {
            return Type.CHAR_TYPE;
        }
        if (iteratorType.startsWith("Boolean")) {
            return Type.BOOLEAN_TYPE;
        }
        if (iteratorType.startsWith("Float")) {
            return Type.FLOAT_TYPE;
        }
        if (iteratorType.startsWith("Double")) {
            return Type.DOUBLE_TYPE;
        }

        return null;
    }

    private boolean isProgressionClass(String internalName) {
        return internalName.startsWith("kotlin/") && (
                internalName.endsWith("Progression") ||
                        internalName.endsWith("Range")
        );
    }

    private BoxedBasicValue createOrLoadBoxedValue(AbstractInsnNode insnNode, BasicValue value, List<? extends BasicValue> values, boolean isFromProgression) {
        int index = insnList.indexOf(insnNode);
        if (!boxingPlaces.containsKey(index)) {
            BoxedBasicValue boxedBasicValue = new BoxedBasicValue(value.getType(), values.get(0).getType(), insnNode, isFromProgression);
            candidatesBoxedValues.add(boxedBasicValue);
            boxingPlaces.put(index, boxedBasicValue);
        }

        return boxingPlaces.get(index);
    }

    @Override
    public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
        BasicValue value = super.naryOperation(insn, values);

        int opcode = insn.getOpcode();

        if (opcode == Opcodes.INVOKESTATIC &&
                isClassBox(((MethodInsnNode) insn).owner) &&
                ((MethodInsnNode) insn).name.equals("valueOf")) {

            return createOrLoadBoxedValue(insn, value, values, false);
        }

        if (isUnboxing(insn) &&
                values.get(0) instanceof BoxedBasicValue &&
                value.getType().equals(((BoxedBasicValue)values.get(0)).getBoxedType())) {
            BoxedBasicValue boxedBasicValue = (BoxedBasicValue) values.get(0);
            boxedBasicValue.addInsn(insn);
            boxedBasicValue.setWasUnboxed(true);
        }

        if (!(insn instanceof MethodInsnNode)) {
            return value;
        }

        MethodInsnNode methodInsnNode = (MethodInsnNode) insn;

        if (opcode == Opcodes.INVOKEINTERFACE &&
                values.get(0).getType() != null &&
                isProgressionClass(values.get(0).getType().getInternalName())
                && methodInsnNode.name.equals("iterator")) {
            Type progressionIteratorType = getProgressionIteratorType(values.get(0).getType());
            return new ProgressionIteratorBasicValue(value.getType(), progressionIteratorType);
        }

        if (opcode == Opcodes.INVOKEINTERFACE &&
                values.get(0) instanceof ProgressionIteratorBasicValue &&
                ((MethodInsnNode) insn).name.equals("next")) {
            return createOrLoadBoxedValue(insn, value, values, true);
        }

        return value;
    }



    @Override
    public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if (insn.getOpcode() == Opcodes.CHECKCAST && value instanceof BoxedBasicValue) {
            ((BoxedBasicValue) value).addInsn(insn);
            return value;
        }

        if (insn.getOpcode() == Opcodes.CHECKCAST &&
                value != null && value.getType() != null &&
                isProgressionClass(value.getType().getInternalName())) {
            return value;
        }

        if (insn.getOpcode() == Opcodes.CHECKCAST && value instanceof ProgressionIteratorBasicValue) {
            return value;
        }

        return super.unaryOperation(insn, value);
    }

    @Override
    public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
        if (value instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(value);
            return new BasicValue(((BoxedBasicValue) value).getBasicType());
        }

        return super.copyOperation(insn, value);
    }

    @Override
    public BasicValue merge(BasicValue v, BasicValue w) {
        if (v instanceof BoxedBasicValue && w instanceof BoxedBasicValue && v == w) {
            return v;
        }

        if (v instanceof ProgressionIteratorBasicValue &&
                w instanceof ProgressionIteratorBasicValue &&
                w.equals(v)) {
            return v;
        }

        if (v instanceof ProgressionIteratorBasicValue) {
            v = BasicValue.REFERENCE_VALUE;
        }
        if (w instanceof ProgressionIteratorBasicValue) {
            w = BasicValue.REFERENCE_VALUE;
        }

        if (v instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(v);
            v = new BasicValue(((BoxedBasicValue) v).getBasicType());
        }

        if (w instanceof BoxedBasicValue) {
            candidatesBoxedValues.remove(w);
            w = new BasicValue(((BoxedBasicValue) w).getBasicType());
        }

        return super.merge(v, w);
    }

    public List<BoxedBasicValue> getCandidatesBoxedValues() {
        List<BoxedBasicValue> result = new ArrayList<BoxedBasicValue>();

        for (BoxedBasicValue value : candidatesBoxedValues) {
            if (value.wasUnboxed()) {
                result.add(value);
            }
        }

        return result;
    }
}
