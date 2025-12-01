import de.undercouch.gradle.tasks.download.Download
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import groovy.json.JsonSlurper

plugins {
    `maven-publish`
    id("de.undercouch.download") version "5.5.0"
}

val mpsGroupId = "com.jetbrains.mps"
val mpsArtifactId = "mps-prerelease"

fun toSpdxIdentifier(licenseName: String): String? {
    val trimmed = licenseName.trim()
    
    // Handle SPDX expressions with "+" (e.g., "GPL 2.0 + Classpath" -> "GPL-2.0 WITH Classpath-exception-2.0")
    if (trimmed.contains("+")) {
        val parts = trimmed.split("+").map { it.trim() }
        if (parts.size == 2) {
            val mainLicense = toSpdxIdentifierSimple(parts[0])
            val exception = toSpdxException(parts[1])
            
            if (mainLicense == null) {
                return null
            }
            if (exception == null) {
                return null
            }
            
            return "$mainLicense WITH $exception"
        }
    }
    
    return toSpdxIdentifierSimple(trimmed)
}

fun toSpdxException(exceptionName: String): String? {
    return when (exceptionName.trim()) {
        // Common exceptions
        "Classpath", "Classpath Exception", "Classpath exception 2.0" -> "Classpath-exception-2.0"
        "GCC Runtime Library Exception", "GCC Runtime Library exception 3.1" -> "GCC-exception-3.1"
        "GCC Runtime Library exception 2.0" -> "GCC-exception-2.0"
        "GCC Runtime Library exception 2.0 - note variant" -> "GCC-exception-2.0-note"
        "Universal FOSS Exception", "Universal FOSS Exception, Version 1.0" -> "Universal-FOSS-exception-1.0"
        
        // Autoconf exceptions
        "Autoconf exception", "Autoconf exception 2.0" -> "Autoconf-exception-2.0"
        "Autoconf exception 3.0" -> "Autoconf-exception-3.0"
        "Autoconf generic exception" -> "Autoconf-exception-generic"
        "Autoconf generic exception for GPL-3.0" -> "Autoconf-exception-generic-3.0"
        "Autoconf macro exception" -> "Autoconf-exception-macro"
        
        // Bison exceptions
        "Bison exception", "Bison exception 1.24" -> "Bison-exception-1.24"
        "Bison exception 2.2" -> "Bison-exception-2.2"
        
        // Qt exceptions
        "Qt GPL exception 1.0" -> "Qt-GPL-exception-1.0"
        "Qt LGPL exception 1.1" -> "Qt-LGPL-exception-1.1"
        "Digia Qt LGPL Exception version 1.1" -> "Digia-Qt-LGPL-exception-1.1"
        "Nokia Qt LGPL exception 1.1" -> "Nokia-Qt-exception-1.1"
        
        // GPL exceptions
        "GPL-3.0 389 DS Base Exception" -> "GPL-3.0-389-ds-base-exception"
        "GPL-3.0 Interface Exception" -> "GPL-3.0-interface-exception"
        "GPL-3.0 Linking Exception" -> "GPL-3.0-linking-exception"
        "GPL-3.0 Linking Exception (with Corresponding Source)" -> "GPL-3.0-linking-source-exception"
        "GPL Cooperation Commitment 1.0" -> "GPL-CC-1.0"
        
        // LGPL exceptions
        "LGPL-3.0 Linking Exception" -> "LGPL-3.0-linking-exception"
        "OCaml LGPL Linking Exception" -> "OCaml-LGPL-linking-exception"
        
        // Linking exceptions
        "LLVM Exception" -> "LLVM-exception"
        "CGAL Linking Exception" -> "CGAL-linking-exception"
        "Erlang/OTP Linking Exception" -> "erlang-otp-linking-exception"
        "Independent Module Linking exception" -> "Independent-modules-exception"
        
        // OpenSSL exceptions
        "cryptsetup OpenSSL exception" -> "cryptsetup-OpenSSL-exception"
        "OpenVPN OpenSSL Exception" -> "openvpn-openssl-exception"
        "vsftpd OpenSSL exception" -> "vsftpd-openssl-exception"
        "x11vnc OpenSSL Exception" -> "x11vnc-openssl-exception"
        
        // Other exceptions
        "389 Directory Server Exception" -> "389-exception"
        "Asterisk exception" -> "Asterisk-exception"
        "Asterisk linking protocols exception" -> "Asterisk-linking-protocols-exception"
        "Bootloader Distribution Exception" -> "Bootloader-exception"
        "CLISP exception 2.0" -> "CLISP-exception-2.0"
        "DigiRule FOSS License Exception" -> "DigiRule-FOSS-exception"
        "eCos exception 2.0" -> "eCos-exception-2.0"
        "Fawkes Runtime Exception" -> "Fawkes-Runtime-exception"
        "FLTK exception" -> "FLTK-exception"
        "fmt exception" -> "fmt-exception"
        "Font exception 2.0" -> "Font-exception-2.0"
        "FreeRTOS Exception 2.0" -> "freertos-exception-2.0"
        "Gmsh exception" -> "Gmsh-exception"
        "GNAT exception" -> "GNAT-exception"
        "GNOME examples exception" -> "GNOME-examples-exception"
        "GNU Compiler Exception" -> "GNU-compiler-exception"
        "GNU JavaMail exception" -> "gnu-javamail-exception"
        "GStreamer Exception (2005)" -> "GStreamer-exception-2005"
        "GStreamer Exception (2008)" -> "GStreamer-exception-2008"
        "harbour exception" -> "harbour-exception"
        "i2p GPL+Java Exception" -> "i2p-gpl-java-exception"
        "KiCad Libraries Exception" -> "KiCad-libraries-exception"
        "libpri OpenH323 exception" -> "libpri-OpenH323-exception"
        "Libtool Exception" -> "Libtool-exception"
        "Linux Syscall Note" -> "Linux-syscall-note"
        "LLGPL Preamble" -> "LLGPL"
        "LZMA exception" -> "LZMA-exception"
        "Macros and Inline Functions Exception" -> "mif-exception"
        "mxml Exception" -> "mxml-exception"
        "Open CASCADE Exception 1.0" -> "OCCT-exception-1.0"
        "OpenJDK Assembly exception 1.0" -> "OpenJDK-assembly-exception-1.0"
        "PCRE2 exception" -> "PCRE2-exception"
        "Polyparse Exception" -> "polyparse-exception"
        "PS/PDF font exception (2017-08-17)" -> "PS-or-PDF-font-exception-20170817"
        "INRIA QPL 1.0 2004 variant exception" -> "QPL-1.0-INRIA-2004-exception"
        "Qwt exception 1.0" -> "Qwt-exception-1.0"
        "Romic Exception" -> "romic-exception"
        "RRDtool FLOSS exception 2.0" -> "RRDtool-FLOSS-exception-2.0"
        "SANE Exception" -> "SANE-exception"
        "Solderpad Hardware License v2.0" -> "SHL-2.0"
        "Solderpad Hardware License v2.1" -> "SHL-2.1"
        "stunnel Exception" -> "stunnel-exception"
        "SWI exception" -> "SWI-exception"
        "Swift Exception" -> "Swift-exception"
        "Texinfo exception" -> "Texinfo-exception"
        "U-Boot exception 2.0" -> "u-boot-exception-2.0"
        "Unmodified Binary Distribution exception" -> "UBDL-exception"
        "WxWindows Library Exception 3.1" -> "WxWindows-exception-3.1"
        
        else -> null
    }
}

fun toSpdxIdentifierSimple(licenseName: String): String? {
    return when (licenseName.trim()) {
        // Apache licenses
        "Apache 1.0", "Apache License 1.0" -> "Apache-1.0"
        "Apache 1.1", "Apache License 1.1" -> "Apache-1.1"
        "Apache 2.0", "Apache-2.0", "Apache License 2.0", "Apache License, Version 2.0" -> "Apache-2.0"
        
        // BSD licenses
        "BSD 1-Clause", "BSD 1-Clause License" -> "BSD-1-Clause"
        "BSD 2-Clause", "BSD-2-Clause", "Simplified BSD License", "BSD 2-Clause \"Simplified\" License" -> "BSD-2-Clause"
        "BSD 3-Clause", "BSD-3-Clause", "New BSD License", "BSD 3-Clause \"New\" or \"Revised\" License" -> "BSD-3-Clause"
        "BSD 4-Clause", "BSD-4-Clause", "BSD 4-Clause \"Original\" or \"Old\" License" -> "BSD-4-Clause"
        "BSD 2-Clause Plus Patent License", "BSD-2-Clause-Patent" -> "BSD-2-Clause-Patent"
        "BSD 3-Clause Clear License", "BSD-3-Clause-Clear" -> "BSD-3-Clause-Clear"
        "BSD with attribution", "BSD-3-Clause-Attribution" -> "BSD-3-Clause-Attribution"
        "BSD Zero Clause License", "0BSD" -> "0BSD"
        
        // MIT licenses
        "MIT", "MIT License" -> "MIT"
        "MIT No Attribution", "MIT-0" -> "MIT-0"
        "MIT License Modern Variant", "MIT-Modern-Variant" -> "MIT-Modern-Variant"
        
        // Eclipse Public License
        "EPL 1.0", "Eclipse Public License 1.0", "EPL-1.0" -> "EPL-1.0"
        "EPL 2.0", "Eclipse Public License 2.0", "EPL-2.0" -> "EPL-2.0"
        
        // GNU GPL (current identifiers with -only/-or-later)
        "GPL 1.0 only", "GPL-1.0-only", "GNU General Public License v1.0 only" -> "GPL-1.0-only"
        "GPL 1.0 or later", "GPL-1.0-or-later", "GNU General Public License v1.0 or later" -> "GPL-1.0-or-later"
        "GPL 2.0", "GPL-2.0", "GPL 2.0 only", "GPL-2.0-only", "GNU General Public License v2.0", "GNU General Public License v2.0 only" -> "GPL-2.0-only"
        "GPL 2.0 or later", "GPL-2.0-or-later", "GNU General Public License v2.0 or later" -> "GPL-2.0-or-later"
        "GPL 3.0", "GPL-3.0", "GPL 3.0 only", "GPL-3.0-only", "GNU General Public License v3.0", "GNU General Public License v3.0 only" -> "GPL-3.0-only"
        "GPL 3.0 or later", "GPL-3.0-or-later", "GNU General Public License v3.0 or later" -> "GPL-3.0-or-later"
        
        // GNU LGPL (current identifiers with -only/-or-later)
        "LGPL 2.0 only", "LGPL-2.0-only", "GNU Library General Public License v2 only" -> "LGPL-2.0-only"
        "LGPL 2.0 or later", "LGPL-2.0-or-later", "GNU Library General Public License v2 or later" -> "LGPL-2.0-or-later"
        "LGPL 2.1", "LGPL-2.1", "LGPL 2.1 only", "LGPL-2.1-only", "GNU Lesser General Public License v2.1", "GNU Lesser General Public License v2.1 only" -> "LGPL-2.1-only"
        "LGPL 2.1 or later", "LGPL-2.1-or-later", "GNU Lesser General Public License v2.1 or later" -> "LGPL-2.1-or-later"
        "LGPL 3.0", "LGPL-3.0", "LGPL 3.0 only", "LGPL-3.0-only", "GNU Lesser General Public License v3.0", "GNU Lesser General Public License v3.0 only" -> "LGPL-3.0-only"
        "LGPL 3.0 or later", "LGPL-3.0-or-later", "GNU Lesser General Public License v3.0 or later" -> "LGPL-3.0-or-later"
        
        // GNU AGPL
        "AGPL 1.0 only", "AGPL-1.0-only", "Affero General Public License v1.0 only" -> "AGPL-1.0-only"
        "AGPL 1.0 or later", "AGPL-1.0-or-later", "Affero General Public License v1.0 or later" -> "AGPL-1.0-or-later"
        "AGPL 3.0 only", "AGPL-3.0-only", "GNU Affero General Public License v3.0 only" -> "AGPL-3.0-only"
        "AGPL 3.0 or later", "AGPL-3.0-or-later", "GNU Affero General Public License v3.0 or later" -> "AGPL-3.0-or-later"
        
        // GNU FDL
        "GFDL 1.1 only", "GFDL-1.1-only", "GNU Free Documentation License v1.1 only" -> "GFDL-1.1-only"
        "GFDL 1.1 or later", "GFDL-1.1-or-later", "GNU Free Documentation License v1.1 or later" -> "GFDL-1.1-or-later"
        "GFDL 1.2 only", "GFDL-1.2-only", "GNU Free Documentation License v1.2 only" -> "GFDL-1.2-only"
        "GFDL 1.2 or later", "GFDL-1.2-or-later", "GNU Free Documentation License v1.2 or later" -> "GFDL-1.2-or-later"
        "GFDL 1.3 only", "GFDL-1.3-only", "GNU Free Documentation License v1.3 only" -> "GFDL-1.3-only"
        "GFDL 1.3 or later", "GFDL-1.3-or-later", "GNU Free Documentation License v1.3 or later" -> "GFDL-1.3-or-later"
        
        // Mozilla Public License
        "MPL 1.0", "Mozilla Public License 1.0", "MPL-1.0" -> "MPL-1.0"
        "MPL 1.1", "Mozilla Public License 1.1", "MPL-1.1" -> "MPL-1.1"
        "MPL 2.0", "MPL-2.0", "Mozilla Public License 2.0" -> "MPL-2.0"
        "MPL 2.0 (no copyleft exception)", "MPL-2.0-no-copyleft-exception" -> "MPL-2.0-no-copyleft-exception"
        
        // Common Development and Distribution License
        "CDDL 1.0", "CDDL-1.0", "Common Development and Distribution License 1.0" -> "CDDL-1.0"
        "CDDL 1.1", "CDDL-1.1", "Common Development and Distribution License 1.1" -> "CDDL-1.1"
        
        // Creative Commons
        "CC0 1.0", "CC0-1.0", "Creative Commons Zero v1.0 Universal" -> "CC0-1.0"
        "CC-BY-1.0", "Creative Commons Attribution 1.0 Generic" -> "CC-BY-1.0"
        "CC-BY-2.0", "Creative Commons Attribution 2.0 Generic" -> "CC-BY-2.0"
        "CC-BY-2.5", "Creative Commons Attribution 2.5 Generic" -> "CC-BY-2.5"
        "CC-BY-3.0", "Creative Commons Attribution 3.0 Unported" -> "CC-BY-3.0"
        "CC-BY-4.0", "Creative Commons Attribution 4.0 International" -> "CC-BY-4.0"
        "CC-BY-SA-1.0", "Creative Commons Attribution Share Alike 1.0 Generic" -> "CC-BY-SA-1.0"
        "CC-BY-SA-2.0", "Creative Commons Attribution Share Alike 2.0 Generic" -> "CC-BY-SA-2.0"
        "CC-BY-SA-2.5", "Creative Commons Attribution Share Alike 2.5 Generic" -> "CC-BY-SA-2.5"
        "CC-BY-SA-3.0", "Creative Commons Attribution Share Alike 3.0 Unported" -> "CC-BY-SA-3.0"
        "CC-BY-SA-4.0", "Creative Commons Attribution Share Alike 4.0 International" -> "CC-BY-SA-4.0"
        "CC-BY-NC-1.0", "Creative Commons Attribution Non Commercial 1.0 Generic" -> "CC-BY-NC-1.0"
        "CC-BY-NC-2.0", "Creative Commons Attribution Non Commercial 2.0 Generic" -> "CC-BY-NC-2.0"
        "CC-BY-NC-3.0", "Creative Commons Attribution Non Commercial 3.0 Unported" -> "CC-BY-NC-3.0"
        "CC-BY-NC-4.0", "Creative Commons Attribution Non Commercial 4.0 International" -> "CC-BY-NC-4.0"
        "CC-BY-ND-1.0", "Creative Commons Attribution No Derivatives 1.0 Generic" -> "CC-BY-ND-1.0"
        "CC-BY-ND-2.0", "Creative Commons Attribution No Derivatives 2.0 Generic" -> "CC-BY-ND-2.0"
        "CC-BY-ND-3.0", "Creative Commons Attribution No Derivatives 3.0 Unported" -> "CC-BY-ND-3.0"
        "CC-BY-ND-4.0", "Creative Commons Attribution No Derivatives 4.0 International" -> "CC-BY-ND-4.0"
        
        // Academic Free License
        "AFL-1.1", "Academic Free License v1.1" -> "AFL-1.1"
        "AFL-1.2", "Academic Free License v1.2" -> "AFL-1.2"
        "AFL-2.0", "Academic Free License v2.0" -> "AFL-2.0"
        "AFL-2.1", "Academic Free License v2.1" -> "AFL-2.1"
        "AFL-3.0", "Academic Free License v3.0" -> "AFL-3.0"
        
        // Open Software License
        "OSL-1.0", "Open Software License 1.0" -> "OSL-1.0"
        "OSL-1.1", "Open Software License 1.1" -> "OSL-1.1"
        "OSL-2.0", "Open Software License 2.0" -> "OSL-2.0"
        "OSL-2.1", "Open Software License 2.1" -> "OSL-2.1"
        "OSL-3.0", "Open Software License 3.0" -> "OSL-3.0"
        
        // Other common licenses
        "ISC", "ISC License" -> "ISC"
        "Unlicense", "The Unlicense" -> "Unlicense"
        "OFL-1.0", "SIL Open Font License 1.0" -> "OFL-1.0"
        "OFL", "OFL-1.1", "SIL Open Font License 1.1" -> "OFL-1.1"
        "PSF-2.0", "PSF", "Python Software Foundation License 2.0" -> "PSF-2.0"
        "Python-2.0", "Python License 2.0" -> "Python-2.0"
        "Zlib", "zlib License" -> "Zlib"
        "W3C", "W3C Software Notice and License" -> "W3C"
        "W3C-19980720", "W3C Software Notice and License (1998-07-20)" -> "W3C-19980720"
        "W3C-20150513", "W3C Software Notice and Document License (2015-05-13)" -> "W3C-20150513"
        
        // Boost
        "BSL-1.0", "Boost Software License 1.0" -> "BSL-1.0"
        
        // Artistic License
        "Artistic-1.0", "Artistic License 1.0" -> "Artistic-1.0"
        "Artistic-1.0-Perl", "Artistic License 1.0 (Perl)" -> "Artistic-1.0-Perl"
        "Artistic-2.0", "Artistic License 2.0" -> "Artistic-2.0"
        
        // LaTeX Project Public License
        "LPPL-1.0", "LaTeX Project Public License v1.0" -> "LPPL-1.0"
        "LPPL-1.1", "LaTeX Project Public License v1.1" -> "LPPL-1.1"
        "LPPL-1.2", "LaTeX Project Public License v1.2" -> "LPPL-1.2"
        "LPPL-1.3a", "LaTeX Project Public License v1.3a" -> "LPPL-1.3a"
        "LPPL-1.3c", "LaTeX Project Public License v1.3c" -> "LPPL-1.3c"
        
        // Public Domain
        "CC-PDDC", "Creative Commons Public Domain Dedication and Certification" -> "CC-PDDC"
        
        // Microsoft licenses
        "MS-PL", "Microsoft Public License" -> "MS-PL"
        "MS-RL", "Microsoft Reciprocal License" -> "MS-RL"
        
        // Common Public License
        "CPL-1.0", "Common Public License 1.0" -> "CPL-1.0"
        "CPAL-1.0", "Common Public Attribution License 1.0" -> "CPAL-1.0"
        
        // IBM Public License
        "IPL-1.0", "IBM Public License v1.0" -> "IPL-1.0"
        
        // Zope Public License
        "ZPL-2.0", "Zope Public License 2.0" -> "ZPL-2.0"
        "ZPL-2.1", "Zope Public License 2.1" -> "ZPL-2.1"
        
        // NCSA Open Source License
        "NCSA", "University of Illinois/NCSA Open Source License" -> "NCSA"
        
        // PostgreSQL License
        "PostgreSQL", "PostgreSQL License" -> "PostgreSQL"
        
        // PHP License
        "PHP-3.0", "PHP License v3.0" -> "PHP-3.0"
        "PHP-3.01", "PHP License v3.01" -> "PHP-3.01"
        
        // Ruby License
        "Ruby", "Ruby License" -> "Ruby"
        
        // OpenSSL License
        "OpenSSL", "OpenSSL License" -> "OpenSSL"
        
        // IJG License
        "IJG", "Independent JPEG Group License" -> "IJG"
        
        // Vim License
        "Vim", "Vim License" -> "Vim"
        
        // curl License
        "curl", "curl License" -> "curl"
        
        // JSON License
        "JSON", "JSON License" -> "JSON"
        
        // Universal Permissive License
        "UPL-1.0", "Universal Permissive License v1.0" -> "UPL-1.0"
        
        // European Union Public License
        "EUPL-1.0", "European Union Public License 1.0" -> "EUPL-1.0"
        "EUPL-1.1", "European Union Public License 1.1" -> "EUPL-1.1"
        "EUPL-1.2", "European Union Public License 1.2" -> "EUPL-1.2"
        
        // CeCILL licenses
        "CECILL-2.0", "CeCILL Free Software License Agreement v2.0" -> "CECILL-2.0"
        "CECILL-2.1", "CeCILL Free Software License Agreement v2.1" -> "CECILL-2.1"
        "CECILL-B", "CeCILL-B Free Software License Agreement" -> "CECILL-B"
        "CECILL-C", "CeCILL-C Free Software License Agreement" -> "CECILL-C"
        
        // Additional common licenses
        "Public Domain (CC0)", "CC0", "Creative Commons Zero" -> "CC0-1.0"
        "Unicode", "Unicode License", "Unicode License v3" -> "Unicode-3.0"
        "Unicode-DFS-2015", "Unicode License Agreement - Data Files and Software (2015)" -> "Unicode-DFS-2015"
        "Unicode-DFS-2016", "Unicode License Agreement - Data Files and Software (2016)" -> "Unicode-DFS-2016"
        "Creative Commons 2.5 Attribution", "CC-BY-2.5" -> "CC-BY-2.5"
        "Eclipse Distribution License 1.0", "EDL-1.0" -> "EDL-1.0"
        "zlib/libpng", "Zlib", "zlib License" -> "Zlib"
        "LGPL 2.0", "LGPL-2.0", "GNU Library General Public License v2" -> "LGPL-2.0-only"
        "LGPL-3.0", "LGPL 3.0", "GNU Lesser General Public License v3.0" -> "LGPL-3.0-only"

        // Unofficial licenses
        "JDOM License" -> "LicenseRef-JDOM"
        "codehaus" -> "Plexus"
        
        else -> null // Return null if no mapping found
    }
}

fun extractLicensesFromJson(jsonFile: File): Map<String, String?> {
    val jsonSlurper = JsonSlurper()
    val libraries = jsonSlurper.parse(jsonFile) as List<Map<String, Any?>>
    
    val licenseMap = mutableMapOf<String, String?>()
    val unmappedLicenses = mutableListOf<String>()
    
    libraries.forEach { lib ->
        val licenseName = (lib["license"] as? String)?.trim()
        val licenseUrl = lib["licenseUrl"] as? String
        
        if (!licenseName.isNullOrEmpty()) {
            val spdxIdentifier = toSpdxIdentifier(licenseName)
            if (spdxIdentifier == null) {
                unmappedLicenses.add(licenseName)
            } else {
                if (!licenseMap.containsKey(spdxIdentifier)) {
                    licenseMap[spdxIdentifier] = licenseUrl
                }
            }
        }
    }
    
    if (unmappedLicenses.isNotEmpty()) {
        throw GradleException("No SPDX identifier found for the following licenses:\n${unmappedLicenses.sorted().joinToString("\n") { "  - $it" }}")
    }
    
    return licenseMap
}

version = object {
    override fun toString(): String {
        return getenvRequired("ARTIFACT_VERSION")
    }
}

tasks.register("checkAlreadyPublished") {
    doLast {
        val alreadyPublished = doesArtifactExistInRepository()
        println("##teamcity[setParameter name='alreadyPublished' value='$alreadyPublished']")

        val status = if (alreadyPublished)
            "MPS $version already exists in the repository"
        else
            "MPS $version will be uploaded to the repository"

        println("##teamcity[buildStatus text='$status']")
    }
}

val download by tasks.registering(Download::class) {
    src(::getArtifactDownloadUrl)
    dest(layout.buildDirectory.dir("download"))

    overwrite(false)
}

val repackage by tasks.registering(Zip::class) {
    val downloadedFile = download.map { it.outputFiles.single() }
    from(zipTree(downloadedFile))
    archiveFileName = downloadedFile.map { it.name.substringBeforeLast('.') + "-repackaged.zip" }
    destinationDirectory = layout.buildDirectory.dir("repackage")

    eachFile {
        this.path = this.sourcePath.substringAfter('/')
    }

    includeEmptyDirs = false
}

val extractLicenses by tasks.registering {
    dependsOn(download)
    
    val licensesFile = layout.buildDirectory.file("extracted-licenses.txt")
    outputs.file(licensesFile)
    
    doLast {
        val downloadedFile = download.get().outputFiles.single()
        val zipFile = zipTree(downloadedFile)
        
        // Find the third-party-libraries.json file in the zip
        val thirdPartyJsonFile = zipFile.files.find { it.name == "third-party-libraries.json" && it.path.contains("license") }
        
        if (thirdPartyJsonFile != null) {
            val licenses = extractLicensesFromJson(thirdPartyJsonFile)
            licensesFile.get().asFile.writeText(
                licenses.entries.joinToString("\n") { "${it.key}|${it.value ?: ""}" }
            )
            logger.lifecycle("Extracted ${licenses.size} unique licenses")
        } else {
            logger.warn("third-party-libraries.json not found in downloaded ZIP")
            licensesFile.get().asFile.writeText("")
        }
    }
}

fun getArtifactDownloadUrl(): String {
    val artifactBuildId = getenvRequired("ARTIFACT_BUILD_ID")
    return "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/id:${artifactBuildId}/artifacts/content/MPS-${version}.zip"
}

val repo = publishing.repositories.maven("https://artifacts.itemis.cloud/repository/maven-mps-prereleases") {
    name = "Maven"
    if (project.findProperty("artifacts.itemis.cloud.user") != null) {
        credentials {
            username = project.findProperty("artifacts.itemis.cloud.user") as String?
            password = project.findProperty("artifacts.itemis.cloud.pw") as String?
        }
    }
}

fun doesArtifactExistInRepository(): Boolean {
    val url =
        URL("${repo.url}/${mpsGroupId.replace('.', '/')}/${mpsArtifactId}/${version}/${mpsArtifactId}-${version}.pom")
    logger.info("Checking URL $url")

    val connection = url.openConnection() as HttpURLConnection
    val credentials = repo.credentials
    if (!credentials.username.isNullOrEmpty() && !credentials.password.isNullOrEmpty()) {
        logger.info("Using Basic authentication with username ${credentials.username}")
        val basicHeader = "Basic " + Base64.getEncoder()
            .encodeToString("${credentials.username}:${credentials.password}".toByteArray())
        connection.setRequestProperty("Authorization", basicHeader)
    }

    try {
        connection.requestMethod = "HEAD"
        connection.doOutput = false

        val responseCode = connection.responseCode
        logger.info("Received HTTP response code $responseCode")

        return when (responseCode) {
            200 -> true
            404 -> false
            else -> throw RuntimeException("Server returned unexpected response code $responseCode for HEAD $url")
        }
    } finally {
        connection.disconnect()
    }
}

// Create a provider for the licenses file content that's evaluated at execution time
val licensesFileProvider = extractLicenses.map { task ->
    val licensesFile = task.outputs.files.singleFile
    if (licensesFile.exists() && licensesFile.length() > 0) {
        licensesFile.readLines()
    } else {
        emptyList()
    }
}

val prereleasePublication = publishing.publications.create<MavenPublication>("mpsPrerelease") {
    groupId = mpsGroupId
    artifactId = mpsArtifactId
    artifact(repackage)
    
    pom {
        withXml {
            val licensesNode = asNode().appendNode("licenses")
            // Read licenses from the provider - this is lazily evaluated at execution time
            val licenseLines = licensesFileProvider.get()
            
            if (licenseLines.isNotEmpty()) {
                // Collect all SPDX identifiers
                val spdxIdentifiers = mutableListOf<String>()
                licenseLines.forEach { line ->
                    if (line.isNotBlank()) {
                        val parts = line.split("|")
                        val licenseName = parts[0]
                        spdxIdentifiers.add(licenseName)
                    }
                }
                
                // Create SPDX expression by joining with AND
                val spdxExpression = spdxIdentifiers.sorted().joinToString(" AND ")
                
                val licenseNode = licensesNode.appendNode("license")
                licenseNode.appendNode("name", spdxExpression)
                licenseNode.appendNode("url", "https://spdx.org/licenses/")
                licenseNode.appendNode("comments", "SPDX License Expression combining ${spdxIdentifiers.size} licenses. All listed licenses apply simultaneously")
            } else {
                // Fallback if licenses couldn't be extracted
                val licenseNode = licensesNode.appendNode("license")
                licenseNode.appendNode("name", "Various")
                licenseNode.appendNode("comments", "This artifact bundles various third-party libraries under different licenses")
            }
        }
    }
}

fun getenvRequired(name: String) =
    System.getenv(name) ?: throw GradleException("Environment variable '$name' must be set")

// Ensure proper task dependencies for publishing
afterEvaluate {
    tasks.named("generatePomFileForMpsPrereleasePublication").configure {
        // Add extractLicenses output as input to establish proper task dependency
        inputs.file(extractLicenses.map { it.outputs.files.singleFile })
        dependsOn(repackage)
    }
    
    tasks.named("publishMpsPrereleasePublicationToMavenRepository").configure {
        dependsOn(repackage)
    }
}

tasks.publish {
    doLast {
        println("##teamcity[buildStatus text='MPS $version successfully published']")
    }
}
