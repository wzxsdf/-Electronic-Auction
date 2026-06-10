# Java版本兼容性问题修复

## 🐛 问题描述

```
Cannot resolve method 'of(String, String, String)'
```

**原因**：`Map.of()` 是 Java 9+ 引入的方法，项目使用的是 Java 8，所以报错。

---

## ✅ 修复方案

### 修复前（❌ Java 9+ 语法）

```java
// ❌ Java 9+ 的语法，Java 8 不支持
conditions.add(Map.of("bucket", ossConfig.getBucketName()));
conditions.add(Map.of("starts-with", "$key", uploadDir));
conditions.add(Map.of("content-length-range", 0, maxSize));
conditions.add(Map.of("in", "$Content-Type", allowedTypes));
```

### 修复后（✅ Java 8 兼容）

```java
// ✅ Java 8 兼容的方式
Map<String, Object> bucketCondition = new HashMap<>();
bucketCondition.put("bucket", ossConfig.getBucketName());
conditions.add(bucketCondition);

Map<String, Object> keyCondition = new HashMap<>();
keyCondition.put("starts-with", "$key");
keyCondition.put("value", uploadDir);
conditions.add(keyCondition);

// 数值类型的条件
Map<String, Object> sizeCondition = new HashMap<>();
sizeCondition.put("content-length-range", Arrays.asList(0, maxSize));
conditions.add(sizeCondition);

// 列表类型的条件
Map<String, Object> typeCondition = new HashMap<>();
typeCondition.put("in", "$Content-Type");
typeCondition.put("value", Arrays.asList(allowedTypes));
conditions.add(typeCondition);
```

---

## 📋 Java版本差异

### Java 8 (项目当前版本)

```java
// 创建Map的方式
Map<String, Object> map = new HashMap<>();
map.put("key", "value");

// 创建不可变Map（需要使用Guava等库）
Map<String, Object> immutableMap = Collections.unmodifiableMap(map);

// 创建List的方式
List<String> list = Arrays.asList("a", "b", "c");
```

### Java 9+ (新特性)

```java
// 创建不可变Map的简洁方式
Map<String, String> map = Map.of("key", "value");
Map<String, String> map2 = Map.of("key1", "value1", "key2", "value2");
Map<String, String> map3 = Map.ofEntries(
    Map.entry("key1", "value1"),
    Map.entry("key2", "value2")
);

// 创建List的简洁方式
List<String> list = List.of("a", "b", "c");
```

---

## 🔧 其他Java 8兼容性问题

### 1. 使用 `List.of()` vs `Arrays.asList()`

**❌ Java 9+**
```java
List<String> list = List.of("a", "b", "c");
```

**✅ Java 8**
```java
List<String> list = Arrays.asList("a", "b", "c");
// 或
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
```

### 2. 使用 `Optional.stream()`

**❌ Java 9+**
```java
optional.stream();
```

**✅ Java 8**
```java
optional.isPresent() ? Stream.of(optional.get()) : Stream.empty();
// 或使用 Guava的Streams类
```

### 3. 使用 `HttpClient` (Java 11+)

**❌ Java 11+**
```java
HttpClient client = HttpClient.newHttpClient();
```

**✅ Java 8**
```java
// 使用OkHttp或Apache HttpClient
OkHttpClient client = new OkHttpClient();
```

---

## 📝 项目Java版本配置

### 检查当前版本

**pom.xml**
```xml
<properties>
    <java.version>1.8</java.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```

**application.yml**
```yaml
# 没有这个配置的话，默认使用系统的Java版本
```

### 如何升级到Java 11+

**如果想要使用Java 9+特性，需要：**

1. **升级Java版本**
```bash
# 下载并安装Java 11+
java -version  # 确认版本

# 设置JAVA_HOME环境变量
export JAVA_HOME=/path/to/java11
```

2. **更新pom.xml**
```xml
<properties>
    <java.version>11</java.version>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
</properties>
```

3. **更新IDE配置**
```
IDEA：File → Project Structure → Project Language Level → 11
Eclipse：Project Properties → Java Compiler → 11
```

---

## 💡 推荐做法

### 1. 确定项目Java版本

```bash
# 查看当前Java版本
mvn -version

# 查看项目配置
cat pom.xml | grep java.version
```

### 2. 使用兼容的语法

**Java 8 项目应避免使用：**
- ❌ `Map.of()`, `Map.ofEntries()`
- ❌ `List.of()`, `Set.of()`
- ❌ `Optional.stream()`
- ❌ `HttpClient` (Java 11+)
- ❌ `var` 关键字 (Java 10+)
- ❌ `record` (Java 14+)

**Java 8 推荐使用：**
- ✅ `HashMap`, `LinkedHashMap`, `TreeMap`
- ✅ `Arrays.asList()`, `Collections.singletonList()`
- ✅ `Optional.isPresent()`, `ifPresent()`
- ✅ OkHttp, Apache HttpClient
- ✅ 明确类型声明

### 3. 如果想使用Java 9+特性

**方案1：升级项目Java版本（推荐）**
```xml
<java.version>17</java.version>  <!-- 推荐使用LTS版本 -->
```

**方案2：使用兼容库**
```xml
<!-- 使用Guava提供不可变集合 -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>31.1-jre</version>
</dependency>

// 使用方式
ImmutableMap.of("key", "value")
ImmutableList.of("a", "b", "c")
```

---

## 🎯 修复后的代码分析

### 修复前的问题代码

```java
// ❌ 问题：Map.of() 不存在于 Java 8
conditions.add(Map.of("bucket", bucketName));
```

### 修复后的兼容代码

```java
// ✅ 解决方案：使用 HashMap (Java 8 兼容)
Map<String, Object> bucketCondition = new HashMap<>();
bucketCondition.put("bucket", bucketName);
conditions.add(bucketCondition);
```

**等价于Java 9+的写法：**
```java
// Java 9+ 可以这样写
conditions.add(Map.of("bucket", bucketName));

// Java 8 需要这样写
Map<String, Object> condition = new HashMap<>();
condition.put("bucket", bucketName);
conditions.add(condition);
```

---

## 🚀 验证修复

### 1. 重新编译
```bash
mvn clean compile
```

### 2. 检查编译结果
```bash
# 应该没有错误
[INFO] BUILD SUCCESS
```

### 3. 运行测试
```bash
mvn spring-boot:run
```

---

## 📊 总结

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| `Map.of()` 不存在 | Java 8 不支持 | 使用 `HashMap` + `put()` |
| `List.of()` 不存在 | Java 8 不存在 | 使用 `Arrays.asList()` |
| `var` 关键字报错 | Java 10+ 特性 | 使用明确类型声明 |
| `record` 报错 | Java 14+ 特性 | 使用 `class` + getter |

**推荐做法**：
- 如果是生产项目，建议保持 Java 8，使用兼容语法
- 如果是新项目，建议使用 Java 17（LTS版本），可以使用新特性

---

现在代码应该可以在 Java 8 环境下正常编译了！