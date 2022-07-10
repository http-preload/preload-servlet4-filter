# preload-servlet4-filter

HTTP Preload / Resource Hints / Early Hints support for Tomcat 9.0, Java 1.8+ web apps.

Note: For Tomcat 10.0+, please use [preload-servlet-filter](../preload-servlet-filter).



## Install

In your maven project, edit pom.xml

```xml
<dependency>
  <groupId>com.example</groupId>
  <artifactId>preload-servlet4-filter</artifactId>
  <version>0.1.0</version>
</dependency>
```

In your maven project, edit web.xml

```xml
  <filter>
    <filter-name>preload</filter-name>
    <filter-class>com.example.httppreload.web.filter.PreloadFilter</filter-class>
    <async-supported>true</async-supported>
    <init-param>
      <param-name>manifestFile</param-name>
      <param-value>/WEB-INF/preload.json</param-value>
    </init-param>
    <init-param>
      <param-name>watch</param-name>
      <param-value>true</param-value>
    </init-param>
    <init-param>
      <param-name>prefersEarlyHints</param-name>
      <param-value>false</param-value>
    </init-param>
  </filter>
  <filter-mapping>
    <filter-name>preload</filter-name>
    <url-pattern>*.html</url-pattern>
    <dispatcher>REQUEST</dispatcher>
    <dispatcher>FORWARD</dispatcher>
  </filter-mapping>
```



## Examples

See [web.xml](./src/main/webapp/WEB-INF/web.xml), [index.html](./src/main/webapp/index.html) and [index.jsp](./src/main/webapp/index.jsp) 



## License

[Apache License 2.0](./LICENSE)
