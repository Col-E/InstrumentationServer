# Instrumentation Server

A minimal client-server library that allows simple interaction with a remote process's `Instrumentation` API over a command system.

## Usage

### Starting a new process

Example logic to programmatically launch a new process with the agent active:
```java
// Extract the agent jar to some path
Path agentJarPath = // ...
Extractor.extractToPath(agentJarPath);
// Start new process
String agent = "-javaagent:" + agentJarPath.toString().replace("\\", "/");
Process remote = new ProcessBuilder("java", agent, "-cp", "<classpath>", "<main-class>").start();
// Connect
int port = Server.DEFAULT_PORT;
Client client = new Client("localhost", port, ByteBufferAllocator.HEAP);
if (!client.connect()) System.err.println("Connect failed!");
```

### Connecting to an existing process

Example logic to programmatically connect to an existing JVM and load the agent:
```java
// Extract the agent jar to some path
Path agentJarPath = // ...
Extractor.extractToPath(agentJarPath);
// Use attach API to connect to remote VM.
// Options string is optional, but can be used to run the server on a unique port.
int openPort = SocketAvailability.findAvailable();
String optionsStr = "port=" + openPort;
for (VirtualMachineDescriptor descriptor : VirtualMachine.list()) {
	// Filter to only apply to a target VM
	if (!match(descriptor)) continue;
	// Attach to the VM
	try {
		descriptor.provider().attachVirtualMachine(descriptor)
				.loadAgent(agentJarPath.toAbsolutePath().toString(), optionsStr);
	} catch (AgentLoadException e) {
		e.printStackTrace();
	} catch (AgentInitializationException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} catch (AttachNotSupportedException e) {
		e.printStackTrace();
	}
}
// Connect
Client client = new Client("localhost", openPort, ByteBufferAllocator.HEAP);
if (!client.connect()) System.err.println("Connect failed!");
```

### API

The `Client` class has four primary methods:

| Method                                                                     | Usage |
|----------------------------------------------------------------------------|-------|
| `WriteResult sendAsync(AbstractCommand command)`                           | Send a `AbstractCommand` value, return wrapper of of write operation information. |
| `ReplyResult sendAsync(AbstractCommand command, Consumer<R> replyHandler)` | Send a `AbstractCommand` value and handle a reply value _(ideally of an expected type)_, return wrapper of the write operation and read operation for the handled response. |
| `void sendBlocking(AbstractCommand command)`                               | Send a `AbstractCommand` value, return when the command has been sent. |
| `void sendBlocking(AbstractCommand command, Consumer<R> replyHandler)`     | Send a `AbstractCommand` value and handle a reply value _(ideally of an expected type)_, return when reply has been handled. |

The available commands:

| Request type                       | Response type                    | Description |
|------------------------------------|----------------------------------|-------------|
| `RequestClassCommand`              | `ReplyClassCommand`              | Get the `byte[]` of a class, wrapped as a `ClassData` type. |
| `RequestClassloaderClassesCommand` | `ReplyClassloaderClassesCommand` | Get the names of classes belonging to a given `ClassLoader`. |
| `RequestClassloadersCommand`       | `ReplyClassloadersCommand`       | Get the `int loaderId` values of all `ClassLoader` values. |
| `RequestFieldGetCommand`           | `ReplyFieldGetCommand`           | Get the `String` representation of a `static` field's value. |
| `RequestFieldSetCommand`           | `ReplyFieldSetCommand`           | Set the value of a `static` field's value. |
| `RequestPingCommand`               | `ReplyPingCommand`               | Ping pong. |
| `RequestPropertiesCommand`         | `ReplyPropertiesCommand`         | Get the `System.getProperties()` values. |
| `RequestRedefineCommand`           | `ReplyRedefineCommand`           | Redefine a class. |
| `RequestSetPropertyCommand`        | `ReplySetPropertyCommand`        | Set a value within the `System.getProperties()`. |
