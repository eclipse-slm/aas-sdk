# aas.test

This module starts the complete BaSyx stack using native Testcontainers, without Docker Compose.

## Included Container Classes

- `AasRegistryTestContainer`
- `SubmodelRegistryTestContainer`
- `AasDiscoveryTestContainer`
- `AasEnvironmentTestContainer`
- `AasWebUiTestContainer`
- `AasInfrastructureTestStack`

All containers provide a `getExternalUrl()` method. The URL host defaults to `localhost` and can be configured either per container builder or centrally via the stack builder.

## Quick Start

```java
import org.eclipse.slm.aas.testcontainers.AasInfrastructureTestStack;

try (var stack = AasInfrastructureTestStack.builder()
        .withExternalUrlHost("localhost")
        .withAasRegistryExternalUrlPort(18082)
        .withSubmodelRegistryExternalUrlPort(18083)
        .withAasDiscoveryExternalUrlPort(18084)
        .withAasEnvironmentExternalUrlPort(18081)
        .withAasWebUiExternalUrlPort(13000)
        .build()) {
    stack.start();

    System.out.println(stack.getAasEnvironmentBaseUrl());
    System.out.println(stack.getAasWebUiBaseUrl());
}
```

## Configure Individual Containers

```java
var registry = AasRegistryTestContainer.builder()
        .withExternalUrlHost("host.docker.internal")
        .build();

System.out.println(registry.getExternalUrl());
```

