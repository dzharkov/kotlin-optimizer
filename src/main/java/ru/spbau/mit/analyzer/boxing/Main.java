package ru.spbau.mit.analyzer.boxing;

import org.apache.commons.io.FileUtils;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import ru.spbau.mit.ClassMethodsTransformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Main implements Runnable {
    private final File root;
    private final ClassMethodsTransformer transformer;
    private final RedundantBoxingAnalyzerTransformer redundantBoxingAnalyzerTransformer = new RedundantBoxingAnalyzerTransformer(null);

    public Main(File root) {
        transformer = new ClassMethodsTransformer(redundantBoxingAnalyzerTransformer);
        this.root = root;
    }

    public static void main(String[] args) throws Exception {
        new Thread(new Main(new File(args[0]))).run();
    }

    @Override
    public void run() {
        try {
            for (Object file : FileUtils.listFiles(root, new String[]{"class"}, true)) {
                checkClass((File)file);
            }
            System.out.println(
                    "Total: " +
                    redundantBoxingAnalyzerTransformer.getTotalProblemsCount() +
                    " " + redundantBoxingAnalyzerTransformer.getTotalRangeProblemsCount()
            );

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkClass(File file) throws IOException {
        ClassReader cr = new ClassReader(new FileInputStream(file));

        ClassNode classNode = new ClassNode();
        cr.accept(classNode, ClassReader.SKIP_DEBUG);

        transformer.transform(classNode);
    }
}
