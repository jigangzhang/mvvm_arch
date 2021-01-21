import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.god.asm.TestMethodClassAdapter
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * 参考：https://juejin.cn/post/6844903831646502920#heading-3
 * https://www.jianshu.com/p/83208323826b
 */
class AsmTransform extends Transform {
    Project project

    AsmTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        println('===== ASM Transform =====')
        def isIncremental = transformInvocation.isIncremental()
        println("transform is incremental（是否是增量编译）: $incremental")
        //消费型输入，可以从中获取jar包和class文件夹路径。需要输出给下一个任务
        def inputs = transformInvocation.inputs
        //引用型输入，无需输出
        def refInputs = transformInvocation.referencedInputs
        //管理输出路径，如果消费型输入为空，OutputProvider == null
        def outputProvider = transformInvocation.outputProvider
        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.jarInputs) {
                File dest = outputProvider.getContentLocation(jarInput.file.absolutePath, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                //将修改过的字节码copy到dest，就可以实现编译期间干预字节码的目的
                transformJar(jarInput.file, dest)
            }
            for (DirectoryInput directoryInput : input.directoryInputs) {
                println("== Directory Input = ${directoryInput.file.listFiles().toArrayString()}")
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY)
                transformDir(directoryInput.file, dest)
            }
        }
    }

    @Override
    String getName() {
        return AsmTransform.simpleName
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return true
    }

    private static void transformJar(File input, File dest) {
        FileUtils.copyFile(input, dest)
        println('=== transformJar: copy file ===')
    }

    private static void transformDir(File input, File dest) {
        if (dest.exists())
            FileUtils.forceDelete(dest)
        FileUtils.forceMkdir(dest)
        def srcDirPath = input.getAbsolutePath()
        def destDirPath = dest.absolutePath
        println("=== transform srcDir = $srcDirPath, destDir = $destDirPath")
        for (File file : input.listFiles()) {
            def destFilePath = file.absolutePath.replace(srcDirPath, destDirPath)
            def destFile = new File(destFilePath)
            if (file.isDirectory()) {
                transformDir(file, destFile)
            } else if (file.isFile()) {
                FileUtils.touch(destFile)
                transformSingleFile(file, destFile)
            }
        }
    }

    private static void transformSingleFile(File input, File dest) {
        println('=== transformSingleFile ===')
        weave(input.absolutePath, dest.absolutePath)
    }

    private static void weave(String inputPath, String outputPath) {
        def fis = new FileInputStream(inputPath)
        def cr = new ClassReader(fis)
        def cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES)
        def adapter = new TestMethodClassAdapter(cw)
        cr.accept(adapter, 0)

        def fos = new FileOutputStream(outputPath)
        fos.write(cw.toByteArray())
        fos.close()
    }
}