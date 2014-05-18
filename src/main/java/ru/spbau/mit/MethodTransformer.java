package ru.spbau.mit;

import org.jetbrains.org.objectweb.asm.tree.MethodNode;

public abstract class MethodTransformer {
    private MethodTransformer methodTransformer;

    protected MethodTransformer(MethodTransformer methodTransformer) {
        this.methodTransformer = methodTransformer;
    }

    public void transform(String owner, MethodNode methodNode) {
        if (methodTransformer != null) {
            methodTransformer.transform(owner, methodNode);
        }
    }
}
