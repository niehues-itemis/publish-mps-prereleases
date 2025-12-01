# SPDX License Mapper

A Kotlin utility for mapping common license names to their official SPDX identifiers and generating Maven POM license sections. Features a flexible provider architecture for reading license information from various file formats.

## Overview

This utility provides comprehensive mapping of license names to SPDX identifiers, including:
- Main licenses (Apache, BSD, MIT, GPL, LGPL, etc.)
- License exceptions (Classpath, GCC Runtime Library, etc.)
- SPDX license expressions with the `WITH` operator
- Validation of license mappings
- Flexible provider architecture for different file formats
- Direct POM XML generation with SPDX expressions

## Architecture

### Provider-Based Design

The utility uses a provider pattern to separate license data extraction from SPDX mapping:

1. **`LicenseProvider` Interface**: Extracts raw license data from any source (JSON, XML, YAML, etc.)
2. **`LibraryLicense` Data Class**: Represents a library with its license information
3. **`SpdxLicenseMapper`**: Centralized SPDX mapping logic

This design makes it easy to support new file formats without duplicating SPDX mapping logic.

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
```

### 3. Using License Providers

Choose or create a provider based on your file format:

```kotlin
import JsonLicenseProvider
import LibraryLicense
import SpdxLicenseMapper

// Using the built-in JSON provider
val jsonFile = file("third-party-libraries.json")
val provider = JsonLicenseProvider(jsonFile)

// Get raw library licenses
val libraryLicenses = provider.getLibraryLicenses()
// Returns: List<LibraryLicense>

// Map to SPDX identifiers
val spdxLicenses = SpdxLicenseMapper.mapToSpdxLicenses(libraryLicenses)
// Returns: Map<String, String?> mapping SPDX IDs to URLs
```

### 4. Generate POM License Section

The recommended way to add SPDX licenses to your Maven POM:

```kotlin
publishing {
    publications {
        create<MavenPublication>("myPublication") {
            from(components["java"])
            
            pom {
                withXml {
                    val licensesNode = asNode().appendNode("licenses")
                    val jsonFile = file("third-party-libraries.json")
                    
                    // Instantiate the appropriate provider for your file format
                    val licenseProvider = JsonLicenseProvider(jsonFile)
                    
                    // Add licenses to POM (handles SPDX mapping and XML generation)
                    SpdxLicenseMapper.addLicensesToPom(licensesNode, licenseProvider)
                }
            }
        }
    }
}
```

**Convenience method for JSON files:**

```kotlin
pom {
    withXml {
        val licensesNode = asNode().appendNode("licenses")
        val jsonFile = file("third-party-libraries.json")
        
        // Shortcut that uses JsonLicenseProvider internally
        SpdxLicenseMapper.addLicensesToPomFromJson(licensesNode, jsonFile)
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

### 5. JSON File Format

The `JsonLicenseProvider` expects the following structure:

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

When using `addLicensesToPom()` or `addLicensesToPomFromJson()`, the following errors will be thrown:

- **JSON file not found** (JsonLicenseProvider): 
  ```
  License JSON file not found: /path/to/file.json
  ```

- **No licenses found**: 
  ```
  No licenses found. The license source may be empty or malformed.
  ```

- **Unmapped licenses** (from `mapToSpdxLicenses()`):
  ```
  No SPDX identifier found for the following licenses:
    - library-name: Custom License
    - another-lib: Proprietary License
  ```

These errors ensure that POM generation fails early if license information is missing or invalid, preventing publication of artifacts without proper license metadata.

## Implementing Custom Providers

To support a new file format, implement the `LicenseProvider` interface:

```kotlin
class XmlLicenseProvider(private val xmlFile: File) : LicenseProvider {
    override fun getLibraryLicenses(): List<LibraryLicense> {
        if (!xmlFile.exists()) {
            throw GradleException("XML file not found: ${xmlFile.absolutePath}")
        }
        
        // Parse your XML format and extract license information
        val libraries = mutableListOf<LibraryLicense>()
        
        // ... your XML parsing logic ...
        
        return libraries
    }
}
```

Then use it in your build script:

```kotlin
pom {
    withXml {
        val licensesNode = asNode().appendNode("licenses")
        val xmlFile = file("licenses.xml")
        
        // Use your custom provider
        val licenseProvider = XmlLicenseProvider(xmlFile)
        SpdxLicenseMapper.addLicensesToPom(licensesNode, licenseProvider)
    }
}
```

**Key Benefits:**
- SPDX mapping logic is centralized in `SpdxLicenseMapper`
- No need to duplicate validation or mapping code
- Easy to test and maintain
- Consistent error handling across all providers

## Using in Other Projects

To use this utility in another Gradle project:

1. Copy `buildSrc/src/main/kotlin/SpdxLicenseMapper.kt` to your project's `buildSrc/src/main/kotlin/` directory
2. Import and use as shown in the usage examples above

Alternatively, you can reference it from a shared location using Gradle's `apply from:` mechanism.

## Extending the Mapper

### Adding New License Mappings

To add new license mappings:

1. Open `SpdxLicenseMapper.kt`
2. Add your mapping to the appropriate `when` block in `toSpdxIdentifierSimple` or `toSpdxException`
3. Follow the existing pattern:
   ```kotlin
   "License Name", "Alternative Name" -> "SPDX-Identifier"
   ```

### Creating Custom Providers

See the "Implementing Custom Providers" section above for details on supporting new file formats.

## API Reference

### Core Functions

- **`toSpdxIdentifier(licenseName: String): String?`**  
  Converts a license name to its SPDX identifier. Handles both simple licenses and expressions with exceptions.

- **`mapToSpdxLicenses(libraryLicenses: List<LibraryLicense>): Map<String, String?>`**  
  Centralized function to map a list of library licenses to SPDX identifiers. Throws exception for unmapped licenses.

- **`addLicensesToPom(licensesNode: Node, provider: LicenseProvider)`**  
  Adds SPDX license information to a Maven POM XML node using any LicenseProvider.

- **`addLicensesToPomFromJson(licensesNode: Node, jsonFile: File)`**  
  Convenience method that uses `JsonLicenseProvider` internally.

### Data Classes

- **`LibraryLicense(libraryName: String, licenseName: String, licenseUrl: String?)`**  
  Represents a library with its raw license information.

### Interfaces

- **`LicenseProvider`**  
  - `getLibraryLicenses(): List<LibraryLicense>` - Extracts raw license data from a source.

### Built-in Providers

- **`JsonLicenseProvider(jsonFile: File)`**  
  Reads license information from JSON files with the structure shown above.


## References

- [SPDX License List](https://spdx.org/licenses/)
- [SPDX License Exceptions](https://spdx.org/licenses/exceptions-index.html)
- [SPDX Specification](https://spdx.dev/specifications/)
