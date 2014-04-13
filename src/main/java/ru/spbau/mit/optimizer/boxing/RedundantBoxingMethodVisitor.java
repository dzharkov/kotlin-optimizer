package ru.spbau.mit.optimizer.boxing;


import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;

import javax.swing.*;
import java.util.List;

public class RedundantBoxingMethodVisitor extends MethodVisitor {
    private final String owner;


    public RedundantBoxingMethodVisitor(String owner, int access, String name, String desc, String signature) {
        super(Opcodes.ASM5, new MethodNode(access, name, desc, signature, null));
        this.owner = owner;
    }

    @Override
    public void visitEnd() {
        MethodNode mn = (MethodNode) mv;

        Analyzer<BasicValue> analyzer = new Analyzer<>(new RedundantBoxingInterpreter());

        int potentialProblemsCount = 0;

        try {
            analyzer.analyze(owner, mn);

            Frame<BasicValue>[] frames = analyzer.getFrames();
            AbstractInsnNode[] insnNodes = mn.instructions.toArray();

            for (int i = 0; i < insnNodes.length; i++) {
                if (isUnboxing(insnNodes[i]) && frames[i].getStack(frames[i].getStackSize()-1) instanceof BoxedBasicValue) {
                    potentialProblemsCount++;
                }
            }

        } catch (AnalyzerException e) {
            e.printStackTrace();
        }

        if (potentialProblemsCount > 0) {
            System.out.println(Integer.valueOf(potentialProblemsCount).toString() + " problems found in " + owner + "." + mn.name + "(" + mn.signature + ")");
        }
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
