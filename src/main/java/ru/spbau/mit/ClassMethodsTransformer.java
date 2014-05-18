package ru.spbau.mit;

import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

public class ClassMethodsTransformer {
    private final MethodTransformer methodTransformer;

    public ClassMethodsTransformer(MethodTransformer methodTransformer) {
        this.methodTransformer = methodTransformer;
    }

    public void transform(ClassNode classNode) {
        for (MethodNode methodNode : classNode.methods) {
            methodTransformer.transform(classNode.name, methodNode);
        }
    }
}
