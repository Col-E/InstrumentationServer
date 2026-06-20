package software.coley.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.instrument.util.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.*;

/**
 * Patches {@code sun.instrument.TransformerManager#getSnapshotTransformerList()}.
 * <p>
 * This lets us append our helper transformer to the snapshot used by {@code TransformerManager#transform(...)}
 * without registering the helper as a normal listener when the hook is active.
 * <p>
 * We also assume ASM is available, making this without it and keeping Java 8 compatibility would be ass.
 * Use {@link Extractor#addExtractionContext(Class)} to ensure in your calling context <i>(if bundled as a library)</i>
 * the extracted agent jar includes ASM classes.
 */
public class InstrumentationImplHookTransformer implements ClassFileTransformer {
	private static final String TRANSFORMER_HELPER = InstrumentationHelper.class.getName();
	private static final String TARGET_METHOD_NAME = "getSnapshotTransformerList";
	private static final String TARGET_METHOD_DESC = "()[Lsun/instrument/TransformerManager$TransformerInfo;";
	private static final String TRANSFORMER_INFO_NAME = "sun/instrument/TransformerManager$TransformerInfo";
	private static final String TRANSFORMER_METHOD_NAME = "transformer";
	private static final String TRANSFORMER_METHOD_DESCRIPTOR = "()Ljava/lang/instrument/ClassFileTransformer;";
	private final String targetClassName;
	private volatile boolean injected;

	InstrumentationImplHookTransformer(Class<?> instrumentationClass) {
		targetClassName = instrumentationClass.getName().replace('.', '/');
	}

	/**
	 * @return {@code true} if the hook was successfully injected, {@code false} otherwise.
	 */
	public boolean wasInjected() {
		return injected;
	}

	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
	                        ProtectionDomain protectionDomain, byte[] classfileBuffer) {
		try {
			if (!targetClassName.equals(className))
				return null;
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
			HookClassVisitor visitor = new HookClassVisitor(writer);
			reader.accept(visitor, ClassReader.EXPAND_FRAMES);
			boolean wasInjected = visitor.wasInjected();
			if (wasInjected) {
				byte[] modified = writer.toByteArray();
				injected = true; // Only write this after the hook was injected AND the modified bytes were generated successfully.
				return modified;
			}
		} catch (Throwable t) {
			Logger.warn("Failed to patch TransformerManager.getSnapshotTransformerList: " + t);
		}
		return null;
	}

	/**
	 * Class visitor to find the target {@code getSnapshotTransformerList} method and patch it with a callback to our hook.
	 *
	 * @see HookMethodVisitor
	 */
	private static final class HookClassVisitor extends ClassVisitor {
		private boolean injected;

		private HookClassVisitor(ClassVisitor classVisitor) {
			super(Opcodes.ASM9, classVisitor);
		}

		boolean wasInjected() {
			return injected;
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor,
		                                 String signature, String[] exceptions) {
			MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
			if (!TARGET_METHOD_NAME.equals(name) || !TARGET_METHOD_DESC.equals(descriptor))
				return methodVisitor;
			return new HookMethodVisitor(methodVisitor, this);
		}
	}

	/**
	 * Method visitor to find the target method's return and inject a callback to our hook before it.
	 */
	private static final class HookMethodVisitor extends MethodVisitor {
		private final HookClassVisitor owner;
		private boolean sawHook;

		private HookMethodVisitor(MethodVisitor methodVisitor, HookClassVisitor owner) {
			super(Opcodes.ASM9, methodVisitor);
			this.owner = owner;
		}

		@Override
		public void visitLdcInsn(Object value) {
			if (TRANSFORMER_HELPER.equals(value)) {
				sawHook = true;
			}
			super.visitLdcInsn(value);
		}

		@Override
		public void visitInsn(int opcode) {
			/*
			TransformerInfo[] transformerArray = returnedValue;

			for (int currentIndex = 0; currentIndex < transformerArray.length; currentIndex++) {
			    TransformerInfo transformerInfo = transformerArray[currentIndex];

			    String helperClassName = transformerInfo
			            .transformer()
			            .getClass()
			            .getName();

			    if (helperClassName.equals(InstrumentationHelper.class.getName())) {
			        ArrayList<TransformerInfo> transformers =  new ArrayList<>(Arrays.asList(transformerArray));

			        TransformerInfo matchingTransformer = transformers.remove(currentIndex);
			        transformers.add(matchingTransformer);

			        returnedValue = transformers.toArray(new TransformerInfo[0]);
			        break;
			    }
			}
			
			return returnedValue;
			 */
			if (opcode == Opcodes.ARETURN && !sawHook) {
				final int TRANSFORMER_ARRAY = 0;
				final int CURRENT_INDEX = 1;
				Label done = new Label();
				Label notFound = new Label();
				Label loop = new Label();

				super.visitVarInsn(ASTORE, TRANSFORMER_ARRAY);
				super.visitInsn(ICONST_0);
				super.visitVarInsn(ISTORE, CURRENT_INDEX);

				super.visitLabel(loop);
				super.visitVarInsn(ILOAD, CURRENT_INDEX);
				super.visitVarInsn(ALOAD, TRANSFORMER_ARRAY);
				super.visitInsn(ARRAYLENGTH);
				super.visitJumpInsn(IF_ICMPEQ, done);
				super.visitVarInsn(ALOAD, TRANSFORMER_ARRAY);
				super.visitVarInsn(ILOAD, CURRENT_INDEX);
				super.visitInsn(AALOAD);
				super.visitMethodInsn(INVOKEVIRTUAL, TRANSFORMER_INFO_NAME, TRANSFORMER_METHOD_NAME, TRANSFORMER_METHOD_DESCRIPTOR, false);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
				super.visitLdcInsn(TRANSFORMER_HELPER);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "equals", "(Ljava/lang/Object;)Z", false);
				super.visitJumpInsn(IFEQ, notFound);

				super.visitTypeInsn(NEW, "java/util/ArrayList");
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, TRANSFORMER_ARRAY);
				super.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
				super.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "(Ljava/util/Collection;)V", false);
				super.visitInsn(DUP);
				super.visitVarInsn(ILOAD, CURRENT_INDEX);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "remove", "(I)Ljava/lang/Object;", false);
				super.visitInsn(POP);
				super.visitInsn(DUP);
				super.visitVarInsn(ALOAD, TRANSFORMER_ARRAY);
				super.visitVarInsn(ILOAD, CURRENT_INDEX);
				super.visitInsn(AALOAD);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "add", "(Ljava/lang/Object;)Z", false);
				super.visitInsn(POP);
				super.visitInsn(ICONST_0);
				super.visitTypeInsn(ANEWARRAY, TRANSFORMER_INFO_NAME);
				super.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;", false);
				super.visitTypeInsn(CHECKCAST, "[L" + TRANSFORMER_INFO_NAME + ';');
				super.visitVarInsn(ASTORE, TRANSFORMER_ARRAY);

				super.visitJumpInsn(GOTO, done);

				super.visitLabel(notFound);
				super.visitIincInsn(CURRENT_INDEX, 1);
				super.visitJumpInsn(GOTO, loop);

				super.visitLabel(done);
				super.visitVarInsn(ALOAD, TRANSFORMER_ARRAY);

				owner.injected = true;
			}
			super.visitInsn(opcode);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack, Math.max(maxLocals, 2));
		}
	}
}
