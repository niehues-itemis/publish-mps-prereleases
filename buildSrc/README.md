# SPDX License Mapper

A Kotlin utility for mapping common license names to their official SPDX identifiers and generating Maven POM license sections.

## Overview

This utility provides comprehensive mapping of license names to SPDX identifiers, including:
- Main licenses (Apache, BSD, MIT, GPL, LGPL, etc.)
- License exceptions (Classpath, GCC Runtime Library, etc.)
- SPDX license expressions with the `WITH` operator
- Validation of license mappings
- Direct POM XML generation from JSON license files

## Usage in Gradle

### 1. Include buildSrc in your project

Place the `SpdxLicenseMapper.kt` file in your project's `buildSrc/src/main/kotlin/` directory.

### 2. Basic License Mapping

```kotlin
import SpdxLicenseMapper

// Map a single license name
val spdxId = SpdxLicenseMapper.toSpdxIdentifier("Apache 2.0")
// Returns: "Apache-2.0"

// Map license with exception
val gplWithClasspath = SpdxLicenseMapper.toSpdxIdentifier("GPL 2.0 + Classpath")
// Returns: "GPL-2.0-only WITH Classpath-exception-2.0"

// Extract licenses from JSON file
val jsonFile = file("third-party-libraries.json")
val licenses = SpdxLicenseMapper.extractLicensesFromJson(jsonFile)
// Returns: Map<String, String?> mapping SPDX IDs to URLs
```

### 3. Generate POM License Section from JSON

The most convenient way to add SPDX licenses to your Maven POM:

```kotlin
publishing {
    publications {
        create<MavenPublication>("myPublication") {
            from(components["java"])
            
            pom {
                withXml {
                    val licensesNode = asNode().appendNode("licenses")
                    val jsonFile = layout.buildDirectory.file("third-party-libraries.json").get().asFile
                    
                    // Reads JSON, validates licenses, and generates SPDX expression
                    SpdxLicenseMapper.addLicensesToPomFromJson(licensesNode, jsonFile)
                }
            }
        }
    }
}

// Ensure JSON file is available before POM generation
afterEvaluate {
    tasks.named("generatePomFileForMyPublicationPublication").configure {
        dependsOn(downloadTask) // Your task that creates the JSON file
    }
}
```

This will generate a POM with a single license entry containing an SPDX expression:

```xml
<licenses>
  <license>
    <name>Apache-2.0 AND MIT AND BSD-3-Clause</name>
    <url>https://spdx.org/licenses/</url>
    <comments>SPDX License Expression combining 3 licenses. All listed licenses apply simultaneously</comments>
  </license>
</licenses>
```

### 4. Example JSON Format

The `extractLicensesFromJson` and `addLicensesToPomFromJson` functions expect the following JSON structure:

```json
[
  {
    "name": "library-name",
    "license": "Apache 2.0",
    "licenseUrl": "https://www.apache.org/licenses/LICENSE-2.0"
  },
  {
    "name": "another-library",
    "license": "MIT",
    "licenseUrl": "https://opensource.org/licenses/MIT"
  }
]
```

## Supported Licenses

The mapper supports over 180 license identifiers including:

### Popular Licenses
- **Apache**: 1.0, 1.1, 2.0
- **BSD**: 0BSD, 1-Clause, 2-Clause, 3-Clause, 4-Clause (with variants)
- **MIT**: MIT, MIT-0, MIT-Modern-Variant
- **GPL**: 1.0, 2.0, 3.0 (with -only/-or-later variants)
- **LGPL**: 2.0, 2.1, 3.0 (with -only/-or-later variants)
- **AGPL**: 1.0, 3.0 (with -only/-or-later variants)
- **MPL**: 1.0, 1.1, 2.0
- **EPL**: 1.0, 2.0

### Creative Commons
- CC0-1.0, CC-BY (1.0-4.0), CC-BY-SA (1.0-4.0)
- CC-BY-NC (1.0-4.0), CC-BY-ND (1.0-4.0)

### Other Licenses
- ISC, Unlicense, Zlib, W3C, BSL-1.0
- PostgreSQL, PHP, Ruby, OpenSSL, curl, JSON
- Academic Free License (AFL 1.1-3.0)
- Open Software License (OSL 1.0-3.0)
- And many more...

### License Exceptions
- Classpath-exception-2.0
- GCC-exception-2.0, GCC-exception-3.1
- Qt-GPL-exception-1.0, Qt-LGPL-exception-1.1
- LLVM-exception
- And 90+ other SPDX exceptions

## Error Handling

### License Mapping Errors

If a license name cannot be mapped to an SPDX identifier, the utility will throw a `GradleException` listing all unmapped licenses:

```
No SPDX identifier found for the following licenses:
  - Custom License
  - Proprietary License
```

### POM Generation Errors

When using `addLicensesToPomFromJson()`, the following errors will be thrown:

- **JSON file not found**: 
  ```
  License JSON file not found: /path/to/file.json. 
  Please ensure the download task has completed successfully.
  ```

- **No licenses in JSON**: 
  ```
  No licenses found in JSON file: /path/to/file.json. 
  The file may be empty or malformed.
  ```

These errors ensure that POM generation fails early if license information is missing, preventing publication of artifacts without proper license metadata.

## Using in Other Projects

To use this utility in another Gradle project:

1. Copy `buildSrc/src/main/kotlin/SpdxLicenseMapper.kt` to your project's `buildSrc/src/main/kotlin/` directory
2. Import and use as shown in the usage examples above

Alternatively, you can reference it from a shared location using Gradle's `apply from:` mechanism.

## Extending the Mapper

To add new license mappings:

1. Open `SpdxLicenseMapper.kt`
2. Add your mapping to the appropriate `when` block in `toSpdxIdentifierSimple` or `toSpdxException`
3. Follow the existing pattern:
   ```kotlin
   "License Name", "Alternative Name" -> "SPDX-Identifier"
   ```

## References

- [SPDX License List](https://spdx.org/licenses/)
- [SPDX License Exceptions](https://spdx.org/licenses/exceptions-index.html)
- [SPDX Specification](https://spdx.dev/specifications/)
