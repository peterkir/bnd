---
layout: default
title: find
summary: |
   Go through the exports and/or imports and match the given exports/imports globs. If thet match print the file, package and version.
note: AUTO-GENERATED FILE - DO NOT EDIT. You can add manual content via same filename in _ext sub-folder. 
---

### Synopsis: 
	   find [options]  <[file]...>

#### Options: 
- `[ -e --exports <glob;> ]` Glob expression on the exports.
- `[ -i --imports <glob;> ]` Glob expression on the imports.

<!-- Manual content from: ext/find.md --><br /><br />
## Examples

    biz.aQute.bnd (master)$ bnd find -e *service* generated/*.jar
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service-4.1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.action-2.0.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.classparser-1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.diff-1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.extension-1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.progress-1.0.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.repository-1.2
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.resolve.hook-1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.bnd.service.url-1.2
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: org.osgi.service.bindex-1.0
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: aQute.service.reporter-1.0.1
    >/Ws/bnd/biz.aQute.bnd/generated/biz.aQute.bnd.jar: org.osgi.service.repository-1.0
