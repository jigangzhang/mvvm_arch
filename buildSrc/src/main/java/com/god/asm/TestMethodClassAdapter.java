package com.god.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Create Time：2021/1/21 on 16:38
 * Description:
 * Author     :zhangjigang 123
 */
public class TestMethodClassAdapter extends ClassVisitor implements Opcodes {

    public TestMethodClassAdapter(ClassVisitor classVisitor) {
        super(ASM7, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        return mv == null ? null : new TestMethodVisitor(mv);
    }
}
