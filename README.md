# Instrumentation Server [![](https://jitpack.io/v/Col-E/InstrumentationServer.svg)](https://jitpack.io/#Col-E/InstrumentationServer)

A minimal client-server library that allows simple interaction with a remote process's `Instrumentation` API over a message system.

## Usage

Maven dependency:
```xml
<dependency>
    <groupId>software.coley</groupId>
    <artifactId>instrumentation-server</artifactId>
    <version>${serverVersion}</version> <!-- See release page for latest version -->
</dependency>
```

Gradle dependency:
```groovy
implementation group: 'software.coley', name: 'instrumentation-server', version: serverVersion
implementation "software.coley:instrumentation-server:${serverVersion}"
```

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
Client client = new Client("127.0.0.1", port, ByteBufferAllocator.HEAP, MessageFactory.create());
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
Client client = new Client("127.0.0.1", openPort, ByteBufferAllocator.HEAP);
if (!client.connect()) System.err.println("Connect failed!");

// Send request + handle reply
MemberData memberData = new MemberData("java/lang/Integer", "MAX_VALUE", "I");
client.sendBlocking(new RequestFieldGetMessage(memberData), reply -> {
    // reply is asserted to be ReplyFieldGetMessage
    System.out.println(reply.getValueText());
});

// Handle general broadcasts
client.setBroadcastListener((type, message) -> { });
```

### API

The `Client` class has four primary methods:

| Method                                                                                               | Usage |
|------------------------------------------------------------------------------------------------------|-------|
| `WriteResult sendAsync(AbstractMessage message)`                                                     | Send a `AbstractMessage` value, return wrapper of of write operation information. |
| `ReplyResult sendAsync(AbstractRequestMessage message, Consumer<AbstractReplyMessage> replyHandler)` | Send a `AbstractMessage` value and handle a reply value _(ideally of an expected type)_, return wrapper of the write operation and read operation for the handled response. |
| `void sendBlocking(AbstractMessage message)`                                                         | Send a `AbstractMessage` value, return when the message has been sent. |
| `void sendBlocking(AbstractRequestMessage message, Consumer<AbstractReplyMessage> replyHandler)`     | Send a `AbstractMessage` value and handle a reply value _(ideally of an expected type)_, return when reply has been handled. |

The available request/response messages:

| Request type                       | Response type                    | Description |
|------------------------------------|----------------------------------|-------------|
| `RequestClassMessage`              | `ReplyClassMessage`              | Get the `byte[]` of a class, wrapped as a `ClassData` type. |
| `RequestClassloaderClassesMessage` | `ReplyClassloaderClassesMessage` | Get the names of classes belonging to a given `ClassLoader`. |
| `RequestClassloadersMessage`       | `ReplyClassloadersMessage`       | Get the `int loaderId` values of all `ClassLoader` values. |
| `RequestFieldGetMessage`           | `ReplyFieldGetMessage`           | Get the `String` representation of a `static` field's value. |
| `RequestFieldSetMessage`           | `ReplyFieldSetMessage`           | Set the value of a `static` field's value. |
| `RequestPingMessage`               | `ReplyPingMessage`               | Ping pong. |
| `RequestPropertiesMessage`         | `ReplyPropertiesMessage`         | Get the `System.getProperties()` values. |
| `RequestRedefineMessage`           | `ReplyRedefineMessage`           | Redefine a class. |
| `RequestSetPropertyMessage`        | `ReplySetPropertyMessage`        | Set a value within the `System.getProperties()`. |
| `RequestThreadsMessage`            | `ReplyThreadsMessage`            | Get thread information about all running threads. |

The available broadcast messages:

| Type                          | Description |
|-------------------------------|-------------|
| `BroadcastClassloaderMessage` | Sent any time a new `ClassLoader` has been used. |
| `BroadcastClassMessage`       | Sent any time a class definition has been updated. |