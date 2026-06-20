package software.coley.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import software.coley.instrument.util.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

/**
 * Patches {@code sun.instrument.InstrumentationImpl#addTransformer(ClassFileTransformer, boolean)}.
 * <p>
 * Any time a third-party agent adds a transformer, we want to be notified so we can add our own
 * transformer after it and ensure we see all class load events.
 * <p>
 * We also assume ASM is available, making this without it and keeping Java 8 compatibility would be ass.
 * Use {@link Extractor#addExtractionContext(Class)} to ensure in your calling context <i>(if bundled as a library)</i>
 * the extracted agent jar includes ASM classes.
 */
public class InstrumentationImplHookTransformer implements ClassFileTransformer {
	private static final String TARGET_METHOD_NAME = "addTransformer";
	private static final String TARGET_METHOD_DESC = "(Ljava/lang/instrument/ClassFileTransformer;Z)V";
	private static final String HOOK_OWNER = "sun/instrument/InstrumentationHookBridge";
	private static final String HOOK_NAME = "transformerAdded";
	private static final String HOOK_DESC = TARGET_METHOD_DESC;
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
		if (!targetClassName.equals(className))
			return null;
		try {
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(reader, 0);
			HookClassVisitor visitor = new HookClassVisitor(writer);
			reader.accept(visitor, 0);
			injected = visitor.wasInjected();
			return injected ? writer.toByteArray() : null;
		} catch (Throwable t) {
			Logger.warn("Failed to patch InstrumentationImpl.addTransformer: " + t);
			return null;
		}
	}

	/**
	 * Class visitor to find the target {@code addTransformer} method and patch it with a callback to our hook.
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
		private boolean emittedHook;

		private HookMethodVisitor(MethodVisitor methodVisitor, HookClassVisitor owner) {
			super(Opcodes.ASM9, methodVisitor);
			this.owner = owner;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			// If we see a call to our hook method, then we know the hook is already present, and we can skip emitting it again.
			if (opcode == Opcodes.INVOKESTATIC
					&& HOOK_OWNER.equals(owner)
					&& HOOK_NAME.equals(name)
					&& HOOK_DESC.equals(descriptor)) {
				sawHook = true;
			}
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}

		@Override
		public void visitInsn(int opcode) {
			// The 'addTransformer' method has a single return at the end, so we can just inject our hook before it.
			if (opcode == Opcodes.RETURN && !sawHook) {
				super.visitVarInsn(Opcodes.ALOAD, 1);
				super.visitVarInsn(Opcodes.ILOAD, 2);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, HOOK_OWNER, HOOK_NAME, HOOK_DESC, false);
				emittedHook = true;
				owner.injected = true;
			}
			super.visitInsn(opcode);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			// The method should already have enough stack size, but just in case.
			super.visitMaxs(emittedHook ? Math.max(maxStack, 2) : maxStack, maxLocals);
		}
	}
}
