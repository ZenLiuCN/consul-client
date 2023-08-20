<p>
<img src="https://img.shields.io/badge/license-GPLv2%20CE-green?style=plastic" alt="license"/>
<img src="https://img.shields.io/badge/java-17+-yellowgreen?style=plastic" alt="java version"/>
<a href="https://central.sonatype.com/search?smo=true&q=modeler&namespace=io.github.zenliucn.domain">
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

