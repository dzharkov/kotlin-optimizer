package ru.spbau.mit.analyzer.boxing;


import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;
import ru.spbau.mit.MethodTransformer;

import java.util.List;

public class RedundantBoxingAnalyzerTransformer extends MethodTransformer {
    private int totalProblemsCount = 0;
    private int totalRangeProblemsCount = 0;

    public RedundantBoxingAnalyzerTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(String owner, MethodNode methodNode) {

        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new RedundantBoxingInterpreter());

        int potentialProblemsCount = 0;
        int rangeUnboxingCount = 0;

        try {
            analyzer.analyze(owner, methodNode);

            Frame<BasicValue>[] frames = analyzer.getFrames();
            AbstractInsnNode[] insnNodes = methodNode.instructions.toArray();

            for (int i = 0; i < insnNodes.length; i++) {

                if (frames[i] == null || frames[i].getStackSize() == 0) {
                    continue;
                }

                BasicValue topValue = frames[i].getStack(frames[i].getStackSize()-1);
                if (isUnboxing(insnNodes[i]) && topValue instanceof BoxedBasicValue) {

                    if (((BoxedBasicValue) topValue).isFromRange()) {
                        rangeUnboxingCount++;
                    }

                    potentialProblemsCount++;
                }

            }

        } catch (AnalyzerException e) {
            e.printStackTrace();
        }

        if (potentialProblemsCount > 0) {
            totalProblemsCount += potentialProblemsCount;
            totalRangeProblemsCount += rangeUnboxingCount;

            System.out.println(
                    potentialProblemsCount + "/" + rangeUnboxingCount +
                    " problems found in " +
                    owner + "." + methodNode.name + "(" + methodNode.signature + ")");
        }
    }

    public int getTotalProblemsCount() {
        return totalProblemsCount;
    }

    public int getTotalRangeProblemsCount() {
        return totalRangeProblemsCount;
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

    class RedundantBoxingInterpreter extends BasicInterpreter {


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

        @Override
        public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
            BasicValue value = super.naryOperation(insn, values);

            int opcode = insn.getOpcode();

            if (opcode == Opcodes.INVOKESTATIC &&
                    isClassBox(((MethodInsnNode) insn).owner) &&
                    ((MethodInsnNode) insn).name.equals("valueOf")) {

                return new BoxedBasicValue(value.getType());
            }

            if (opcode == Opcodes.INVOKEINTERFACE &&
                    values.get(0).getType() != null &&
                    isProgressionClass(values.get(0).getType().getInternalName()) &&
                    ((MethodInsnNode) insn).name.equals("iterator")) {
                return BoxedBasicValue.PROGRESSION_ITERATOR;
            }

            if (opcode == Opcodes.INVOKEINTERFACE &&
                    values.get(0) == BoxedBasicValue.PROGRESSION_ITERATOR &&
                    ((MethodInsnNode) insn).name.equals("next")) {
                return new BoxedBasicValue(value.getType(), true);
            }

            return value;
        }

        private boolean isProgressionClass(String internalName) {
            return internalName.startsWith("kotlin/") && (
                        internalName.endsWith("Progression") ||
                        internalName.endsWith("Range")
            );
        }

        @Override
        public BasicValue unaryOperation(AbstractInsnNode insn, BasicValue value) throws AnalyzerException {
            if (insn.getOpcode() == Opcodes.CHECKCAST && value instanceof BoxedBasicValue) {
                return value;
            }

            if (insn.getOpcode() == Opcodes.CHECKCAST &&
                    value != null && value.getType() != null &&
                    isProgressionClass(value.getType().getInternalName())) {
                return value;
            }

            return super.unaryOperation(insn, value);
        }

        @Override
        public BasicValue merge(BasicValue v, BasicValue w) {
            if (v instanceof BoxedBasicValue && w instanceof BoxedBasicValue && v.equals(w)) {
                return v;
            }

            v = new BasicValue(v.getType());
            w = new BasicValue(w.getType());

            return super.merge(v, w);
        }

    }

}
