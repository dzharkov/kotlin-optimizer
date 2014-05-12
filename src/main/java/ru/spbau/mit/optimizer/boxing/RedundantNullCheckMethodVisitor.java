package ru.spbau.mit.optimizer.boxing;

import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;

import java.util.List;

public class RedundantNullCheckMethodVisitor extends AnalyzerMethodVisitor {
    private static final BasicValue NULL_VALUE = new BasicValue(Type.getObjectType("null"));
    private static final BasicValue MIXED_NULLABILITY_VALUE = new BasicValue(Type.getType("mixed_nullability"));

    public RedundantNullCheckMethodVisitor(String owner, int access, String name, String desc, String signature) {
        super(owner, access, name, desc, signature);
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;

        Analyzer<BasicValue> analyzer = new Analyzer<>(new RedundantNullCheckInterpreter());

        int potentialProblemsCount = 0;
        int surelyRedundantCheck = 0;

        try {
            analyzer.analyze(owner, mn);

            Frame<BasicValue>[] frames = analyzer.getFrames();
            AbstractInsnNode[] insnNodes = mn.instructions.toArray();

            for (int i = 0; i < insnNodes.length; i++) {
                int opcode = insnNodes[i].getOpcode();
                if ((opcode == Opcodes.IFNULL || opcode == Opcodes.IFNONNULL)) {
                    BasicValue stackTop = frames[i].getStack(frames[i].getStackSize()-1);

                    if (stackTop == NULL_VALUE) {
                        surelyRedundantCheck++;
                    }

                    if (stackTop == MIXED_NULLABILITY_VALUE) {
                        potentialProblemsCount++;
                    }
                }
            }

        } catch (AnalyzerException e) {
            e.printStackTrace();
        }

        if (surelyRedundantCheck > 0) {
            System.out.println(Integer.valueOf(surelyRedundantCheck).toString() + " redundant checks found in " + owner + "." + mn.name + "(" + mn.signature + ")");
        }

        if (potentialProblemsCount > 0) {
            System.out.println(Integer.valueOf(potentialProblemsCount).toString() + " problems found in " + owner + "." + mn.name + "(" + mn.signature + ")");
        }
    }

    private class RedundantNullCheckInterpreter extends BasicInterpreter {
        @Override
        public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
            switch (insn.getOpcode()) {
                case ACONST_NULL:
                    return NULL_VALUE;
            }
            return super.newOperation(insn);
        }

        @Override
        public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
            BasicValue value = super.naryOperation(insn, values);

            int opcode = insn.getOpcode();

            if (opcode == Opcodes.INVOKESTATIC &&
                    isClassBox(((MethodInsnNode) insn).owner) &&
                    ((MethodInsnNode) insn).name.equals("valueOf")) {

                return BoxedBasicValue.instance(value.getType());
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
                return BoxedBasicValue.instance(value.getType());
            }

            return value;
        }
        private boolean isProgressionClass(String internalName) {
            return internalName.startsWith("kotlin/") && (
                    internalName.endsWith("Progression") ||
                            internalName.endsWith("Range")
            );
        }

        private boolean isClassBox(String owner) {

            if (!owner.startsWith("java/lang/")) {
                return false;
            }

            switch (owner.substring("java/lang/".length())) {
                case "Integer":
                case "Double":
                case "Long":
                case "Char":
                case "Byte":
                case "Boolean":
                case "Void":
                case "Number":
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public BasicValue merge(BasicValue v, BasicValue w) {
            if (v == NULL_VALUE || w == NULL_VALUE || v == MIXED_NULLABILITY_VALUE || w == MIXED_NULLABILITY_VALUE) {
                if (v == NULL_VALUE && w == NULL_VALUE) {
                    return v;
                }

                if ((v instanceof BoxedBasicValue || w instanceof BoxedBasicValue) || v == MIXED_NULLABILITY_VALUE || w == MIXED_NULLABILITY_VALUE) {
                    return MIXED_NULLABILITY_VALUE;
                }
            }
            return super.merge(v, w);
        }
    }
}
