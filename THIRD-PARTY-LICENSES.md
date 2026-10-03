# Third-Party Software and Licenses

Circuit Chaos uses third-party open-source software. The following list
documents the dependencies of the project and their applicable licenses.

The versions below correspond to the current Gradle configuration and the
resolved runtime dependency tree where applicable. Transitive dependencies
are included because they are part of the runtime software stack.

## Runtime dependencies

| Component | Version | License |
|---|---:|---|
| Spring Boot | 3.5.16 | Apache License 2.0 |
| Spring Framework | 6.2.19 | Apache License 2.0 |
| Micrometer Observation / Commons | 1.15.12 | Apache License 2.0 |
| Jackson | 2.21.4 | Apache License 2.0 |
| Jackson Annotations | 2.21 | Apache License 2.0 |
| SnakeYAML | 2.4 | Apache License 2.0 |
| Jakarta Annotation API | 2.1.1 | EPL 2.0 / GPL 2.0 with Classpath Exception |
| Tomcat Embed | 10.1.55 | Apache License 2.0 |
| Hibernate Validator | 8.0.3.Final | Apache License 2.0 |
| Jakarta Validation API | 3.0.2 | Apache License 2.0 |
| JBoss Logging | 3.6.3.Final | Apache License 2.0 |
| Classmate | 1.7.3 | Apache License 2.0 |
| Logback | 1.5.34 | EPL 1.0 / LGPL 2.1 |
| Log4j | 2.24.3 | Apache License 2.0 |
| SLF4J | 2.0.18 | MIT License |

## Build-time dependencies

The following libraries are used while building Circuit Chaos and are not
part of the application's runtime dependency set:

| Component | Version | License |
|---|---:|---|
| Project Lombok | 1.18.46 | MIT License |
| Spring Boot Gradle Plugin | 3.5.16 | Apache License 2.0 |
| Spring Dependency Management Gradle Plugin | 1.1.7 | Apache License 2.0 |
| Spring Boot Configuration Processor | 3.5.16 | Apache License 2.0 |

## License texts and project pages

The authoritative license texts and project information are maintained by
the respective projects:

- Spring Boot / Spring Framework:
  https://spring.io/projects/spring-boot
  https://spring.io/projects/spring-framework
  https://www.apache.org/licenses/LICENSE-2.0

- Micrometer:
  https://micrometer.io/
  https://www.apache.org/licenses/LICENSE-2.0

- Jackson:
  https://github.com/FasterXML/jackson
  https://www.apache.org/licenses/LICENSE-2.0

- SnakeYAML:
  https://bitbucket.org/snakeyaml/snakeyaml
  https://www.apache.org/licenses/LICENSE-2.0

- Jakarta Annotation:
  https://github.com/jakartaee/jakartaee-annotations-api
  https://www.eclipse.org/legal/epl-2.0/
  https://www.gnu.org/licenses/old-licenses/gpl-2.0.html

- Tomcat:
  https://tomcat.apache.org/
  https://www.apache.org/licenses/LICENSE-2.0

- Hibernate Validator:
  https://hibernate.org/validator/
  https://www.apache.org/licenses/LICENSE-2.0

- JBoss Logging:
  https://github.com/jboss-logging/jboss-logging
  https://www.apache.org/licenses/LICENSE-2.0

- Classmate:
  https://github.com/FasterXML/java-classmate
  https://www.apache.org/licenses/LICENSE-2.0

- Logback:
  https://logback.qos.ch/
  https://www.eclipse.org/legal/epl-1.0/
  https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

- Apache Log4j:
  https://logging.apache.org/log4j/
  https://www.apache.org/licenses/LICENSE-2.0

- SLF4J:
  https://www.slf4j.org/
  https://www.slf4j.org/license.html

- Project Lombok:
  https://projectlombok.org/
  https://github.com/projectlombok/lombok/blob/master/LICENSE

- Spring Dependency Management Plugin:
  https://github.com/spring-gradle-plugins/dependency-management-plugin
  https://www.apache.org/licenses/LICENSE-2.0

## Important note for binary distributions

This repository does not contain third-party JAR files. If Circuit Chaos is
distributed together with third-party binaries, the corresponding license
texts and attribution/notice files shipped by those components should be
retained as required by their respective licenses.

In particular, Apache-licensed components require that recipients receive a
copy of the Apache License and that applicable copyright, patent, trademark,
and attribution notices are retained. The exact obligations depend on how
Circuit Chaos is distributed.
