<p>
<img src="https://img.shields.io/badge/license-GPLv2%20CE-green?style=plastic" alt="license"/>
<img src="https://img.shields.io/badge/java-17+-yellowgreen?style=plastic" alt="java version"/>
<a href="https://central.sonatype.com/search?smo=true&q=consul&namespace=io.github.zenliucn">
<img src="https://img.shields.io/maven-central/v/io.github.zenliucn.domain/parent?style=plastic" alt="maven central"/>
</a>
</p>

# Consul client

## Why

Not found a suitable jvm consul client. Mostly inspired by [consul-api](https://github.com/Ecwid/consul-api).

## What

1. The core `consul-api` with minial dependencies, only one `netty-buffer`.
2. Ship with`consul-codec-jackson` and `consul-codec-gson` both supported, also possible and easy to replace with other
   json libraries.
3. Ship with `consul-transport-http` and `consult-transport-reactor-netty` for two default transport implement.

## Usage

For maven pom

```xml

<project xmlns="http://maven.apache.org/POM/4.0.0">
   <!-- ...-->
   <dependencyManagement>
      <dependencies>
         <dependency>
            <groupId>io.github.zenliucn</groupId>
            <artifactId>consul</artifactId>
            <version>0.0.1</version>
            <type>pom</type>
            <scope>import</scope>
         </dependency>
      </dependencies>
   </dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>io.github.zenliucn</groupId>
         <artifactId>consul-codec-jackson</artifactId>
      </dependency>
      <dependency>
         <groupId>io.github.zenliucn</groupId>
         <artifactId>consul-transport-reactor-netty</artifactId>
      </dependency>
   </dependencies>
</project>

```

For java code

```java
package some;

import cn.zenliu.java.consul.Client;

public class Main {
   public static void main(String[] args) {
      try(var  client = Client.create(null, "http://127.0.01:8500", false)){
         //fetch an agent client
         var agent = client.agent(null, null); //no token, no extra parameter.
         try {
            //request then member api
            System.out.println(agent.member().get());
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }
}
```
## TODO

1. Add basic test cases.
2. Add more recently API from [Consul](https://www.consul.io).
3. Add more test cases.

## Notes

Current just finish basic API, lack of testing cases. public API may not stable, until reached `v1.0.0`.

## Other consul clients

see [consul-api](https://github.com/Ecwid/consul-api)

## License

License as `GPL v2 with classpath exception`.

