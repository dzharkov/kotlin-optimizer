package ru.spbau.mit.optimizer;

import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import ru.spbau.mit.MethodTransformer;

import java.util.List;

public class RedundantBoxingMethodTransformer extends MethodTransformer {
    public RedundantBoxingMethodTransformer(MethodTransformer methodTransformer) {
        super(methodTransformer);
    }

    @Override
    public void transform(String owner, MethodNode node) {

        if (!owner.contains("Test") || !node.name.equals("test2")) {
            return;
        }


        for (BoxedBasicValue value : analyze(owner, node)) {
            if (value.isFromProgression()) {
                processProgressionBoxing(value, node);
            } else {
                processCommonBoxing(value, node);
            }
        }

        super.transform(owner, node);
    }

    private void processCommonBoxing(BoxedBasicValue value, MethodNode node) {
        for (AbstractInsnNode insnNode : value.getAssociatedInsns()) {
            node.instructions.remove(insnNode);
        }
    }

    private void processProgressionBoxing(BoxedBasicValue value, MethodNode node) {
        AbstractInsnNode boxingLocation = value.getBoxingInsn();
        String typeName = progressionType(value);

        String iteratorOwner = "kotlin/" + typeName + "Iterator";
        node.instructions.insertBefore(
                boxingLocation,
                new TypeInsnNode(
                        Opcodes.CHECKCAST,
                        iteratorOwner
                )
        );
        node.instructions.insertBefore(
                boxingLocation,
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        iteratorOwner,
                        "next" + typeName,
                        Type.getMethodDescriptor(value.getBoxedType()),
                        false
                )
        );

        for (AbstractInsnNode insnNode : value.getAssociatedInsns()) {
            node.instructions.remove(insnNode);
        }
    }

    private String progressionType(BoxedBasicValue value) {
        Type boxedType = value.getBoxedType();
        switch (boxedType.getSort()) {
            case Type.BYTE: return "Byte";
            case Type.CHAR: return "Char";
            case Type.SHORT: return "Short";
            case Type.INT: return "Int";
            case Type.LONG: return "Long";
            case Type.FLOAT: return "Float";
            case Type.DOUBLE: return "Double";
            case Type.BOOLEAN: return "Boolean";
            default:
                throw new RuntimeException("Unknown type: " + boxedType.getSort());
        }
    }

    private static List<BoxedBasicValue> analyze(String owner, MethodNode node) {
        RedundantBoxingInterpreter interpreter = new RedundantBoxingInterpreter(node.instructions);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(interpreter);

        try {
            analyzer.analyze(owner, node);
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }

        return interpreter.getCandidatesBoxedValues();
    }

}
